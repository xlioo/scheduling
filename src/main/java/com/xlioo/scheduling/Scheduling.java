package com.xlioo.scheduling;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xlioo.scheduling.entity.Employee;
import com.xlioo.scheduling.entity.Plan;
import com.xlioo.scheduling.unit.Score;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Create by 一初 on 2020/1/28
 */
public class Scheduling {
    // plan 使用LinkedList是因为方便修改内容，plans使用ArrayList是为了存储
    public static List<List> plans = new ArrayList<List>();

    // 最终计划列表
    public static List<List<Plan>> endPlans = new ArrayList();

    // 组内人员洗牌
    public static Map<String, List> employeesMap = new HashMap<String, List>();

    public static void main(String[] args) throws ParseException {
        System.out.println("args[0]: " + args[0]);
        // 解析json
        JSONObject inputJSON = JSONObject.parseObject(args[0]);
        JSONObject employeesJSON = inputJSON.getJSONObject("employees");
        System.out.println("employees: " + employeesJSON.toString());
        JSONArray employeesArray = employeesJSON.getJSONArray("App");
        System.out.println("App: " + employeesArray);
//        Map employeesMap = inputJSON.getObject("employees",Map.class);
//        System.out.println("emp.size: "+ employeesMap.size());
        System.out.println("empJSON.size: " + employeesJSON.size());

        /**
         * 相邻日期的组不能相同
         */
        // 总人数
        int empTotal = 0;
        // 各组未安排的人数计数器
        Map<String, Integer> iTeamMap = new HashMap<String, Integer>();
        // 排班计划
        List<String> iPlan = new LinkedList<String>();
        // 人员分值统计map
        Map<String, Employee> employeeMap = new HashMap<>();

        for(Map.Entry entry:employeesJSON.entrySet()){
            List list = new ArrayList();
            list = (List) entry.getValue();
            empTotal += list.size();
            list.forEach((a)->{
                Employee e = new Employee(a.toString(),entry.getKey().toString(),0,0);
                employeeMap.put(a.toString(),e);
            });
//            Collections.shuffle(list);
            iTeamMap.put(entry.getKey().toString(),list.size());
            employeesMap.put(entry.getKey().toString(),list);
        }

        System.out.println("employeesMap: " + employeesMap.toString());
        System.out.println("iTeamMap: " + iTeamMap.toString());

        // 队列初始化，用来可以从后向前排班
        for(int i = 0 ; i<= empTotal; i++) {
            iPlan.add("-");
        }
        System.out.println("plan: " + iPlan.toString());

        findTeamPlan(empTotal,iTeamMap,iPlan);

        System.out.println("Plans Size: " + plans.size());

//        for(List l : plans){
//            System.out.println(l.toString());
//        }

        /**
         * 每人平均分值不能超过总平均分值
         * 总平均分 = 每天分值总和/总天数
         * 每人平均分值 = 每人每次值班分值总和/每人总次数
         */
        // 获取历史排班计划，计算总平均分、每人每次值班分值总和、每人总次数
        JSONObject historyJSON = inputJSON.getJSONObject("history");
        System.out.println("history: " + historyJSON.toString());
        JSONArray planJSON = historyJSON.getJSONArray("plan");

        // 获取本周期开始时间
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date tmpDate = format.parse(historyJSON.getString("startDate"));
        cal.setTime(tmpDate);
//        cal.add(Calendar.DATE, planJSON.size());
        System.out.println("now :" + cal.getTime());


        // 积分计算工具初始化
        Score score = new Score(inputJSON.getJSONObject("ScoreRules"));

        // 获取值班计划中每人值班分值总和
        // 循环List
        // 先将值班人员放入map中，不在map中的人员不统计。
        // 计算本次计划开始时间
        for(int i =0; i < planJSON.size(); i++){
            String name = planJSON.getString(i);
            // 计算当日日期
            if (0 != i) cal.add(Calendar.DATE, 1);
            // 计算当日分值
            int pScore = score.calculate(cal);
            System.out.println("name|date|score: "+ name + "|" + format.format(cal.getTime()) + "|" + pScore);

            if(employeeMap.containsKey(name)){
                System.out.println("find name");
                Employee emp = employeeMap.get(name);
                emp.setTotalScore(emp.getTotalScore() + pScore);
                emp.setTimes(emp.getTimes() + 1);
            }else {
                System.out.println("New name, No statistical");
//                Employee emp = new Employee(name,1,pScore);
//                employeeMap.put(name,emp);
            }
        }
        System.out.println("employeeMap" + employeeMap.toString());
        System.out.println("Start time of plan: " + format.format(cal.getTime()) );
        List<Plan> planList = new ArrayList<>();
        for (int i =0; i < employeeMap.size(); i++){
            cal.add(Calendar.DATE, 1);
            int pScore = score.calculate(cal);
            Plan plan = new Plan(i,format.format(cal.getTime()),"",pScore,false);
            planList.add(plan);
        };
        System.out.println("planList: " + planList.toString());

        // 筛选排班的规则
        // 1、目前分值平均分值最低的员工需要优先安排分值最高的值班日期。
        // 2、目前分值平均分值最高的员工需要优先安排分值最低的值班日期。
        List<Employee> employeeList = new ArrayList<>();
        employeeMap.forEach((a,b)->employeeList.add(b));

        Map<String,List> empMap = employeesJSON.toJavaObject(Map.class);
        System.out.println("empMap: "+ empMap);
        // 找到分值最低的人员，找到人员所在组，安排人员参与分值最高的值班日期。查找符合的值班计划
        List<List> tempPlans = new ArrayList();
        while (true){
            Employee minEmp = Employee.pullMinScore(employeeList);
            System.out.println("emp: " + minEmp);
            // 找到分值最高的值班日期
            Plan maxPlan = Plan.pullMaxScore(planList);
            System.out.println("maxPlan: " + maxPlan);

            // 匹配符合条件的计划
            tempPlans.clear();
            tempPlans.addAll(plans);
            tempPlans = tempPlans.stream()
                    .filter(p -> p.get(maxPlan.getIndex()).equals(minEmp.getGroup()))
                    .collect(Collectors.toList());
            if (tempPlans.size() != 0) {
                maxPlan.setArranged(true);
                maxPlan.setName(minEmp.getName());
                empMap.get(minEmp.getGroup()).remove(minEmp.getName());
                plans.clear();
                plans.addAll(tempPlans);
            }else {
                break;
            }
            System.out.println("employeeList2 :"+ employeeList);
            System.out.println("planList2 :" + planList);
            System.out.println("step1Plans.size: " + plans.size());
        }

        // 随机安排剩余人员
        System.out.println("planList2 :" + planList);
        System.out.println("empMap: "+ empMap);
        System.out.println("plans.size: " + plans.size());

        plans.forEach(
                (a)->{
//                    System.out.println("a: " + a);
                    try {
                        findEmployeePlan(0,a,planList,empMap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
        );

        System.out.println("endPlans.size: "+ endPlans.size());

    }



    /**
     * 递归遍历树节点，查找所有的组合
     *
     */
    public static void findEmployeePlan(int floor,List<String> teamPlan,List<Plan> planList,Map<String,List> empMap) throws IOException, ClassNotFoundException {


        if(floor >= planList.size()){
            // 到达计划列表末尾，将计划存储
            endPlans.add(planList);
//            System.out.println("endPlan: "+ planList);
        }else {
            if(planList.get(floor).isArranged()){
                findEmployeePlan(floor+1,teamPlan,planList,empMap);
            }else {
                List<String> team = empMap.get(teamPlan.get(floor));
                for(int i=0; i < team.size(); i++){
                    // 设置传递给下一层次的值，需要不影响本层次的值
                    // map等容器对象都是地址，直接复制只是复制地址，所以内容变化后，原容器也会发生改变。这里进行深度拷贝。
                    // 最终结果
                    List<Plan> tempPlan = deepCopy(planList);
                    tempPlan.get(floor).setName(team.get(i));
                    tempPlan.get(floor).setArranged(true);
                    //剩余值班人员
                    Map<String, List> tempEmpMap = deepCopy(empMap);
                    tempEmpMap.get(teamPlan.get(floor)).remove(i);

                    findEmployeePlan(floor + 1, teamPlan, tempPlan, tempEmpMap);
                }

            }
        }




    }



    /**
     *
     * 递归遍历树节点，查找所有的组合
     *
     */
    public static void findTeamPlan(int floor, Map<String, Integer> team, List<String> plan){
//        System.out.println("Input: "+floor+"floor,Map:"+team.toString());
        for (Map.Entry<String, Integer> entry: team.entrySet()) {
            // 每次使用临时容器，每次临时容器都一致
//            System.out.println("I'm on the "+ floor + " floor " + entry.getKey() + " room! There are " + entry.getValue() + " people in room!");
            // map等容器对象都是地址，直接复制只是复制地址，所以内容变化后，原容器也会发生改变，这里使用putAll（数据拷贝）。
            Map<String, Integer> tempTeam = new HashMap<String, Integer>();
            tempTeam.putAll(team);
            List<String> tempPlan = new LinkedList<String>();
            tempPlan.addAll(plan);
//            System.out.println("tempPlan: " + tempPlan.toString());

            // 判断该组人数是否已经使用完，使用完则不安排
            if(entry.getValue() > 0) {
                // 后一个元素和本元素不同则插入
                if (!entry.getKey().equals(plan.get(floor))) {
                    // 插入并为该组人数减去一人
                    tempPlan.set(floor - 1, entry.getKey().toString());
                    tempTeam.replace(entry.getKey(), entry.getValue() - 1);

                    // 如果到达第一层，结束递归
                    if (0 == floor - 1) {
                        plans.add(tempPlan);
//                        System.out.println("Finish! " + tempPlan.toString());
                    } else {
                        // 没有到达第一层则继续递归
                        findTeamPlan(floor - 1,tempTeam,tempPlan);
                    }
                }

            }
        }
//        System.out.println("End Input: "+floor+"floor,Map:"+team.toString());
    }


    public static <T> List<T> listDeepCopy(List<T> src) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteOut);
        out.writeObject(src);

        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        ObjectInputStream in = new ObjectInputStream(byteIn);
        @SuppressWarnings("unchecked")
        List<T> dest = (List<T>) in.readObject();
        return dest;
    }
    public static <K,V> Map<K,V> mapDeepCopy(Map<K,V> src) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteOut);
        out.writeObject(src);

        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        ObjectInputStream in = new ObjectInputStream(byteIn);
        @SuppressWarnings("unchecked")
        Map<K,V> dest = (Map<K,V>) in.readObject();
        return dest;
    }
    public static <T> T deepCopy(T src) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteOut);
        out.writeObject(src);

        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        ObjectInputStream in = new ObjectInputStream(byteIn);
        @SuppressWarnings("unchecked")
        T dest = (T) in.readObject();
        return dest;
    }
}
