package com.xlioo.scheduling.unit;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xlioo.scheduling.entity.Employee;
import com.xlioo.scheduling.entity.Plan;

import java.io.*;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Create by 一初 on 2020/3/1
 *
 */
public class Condition {
    // 输入的json对象
    JSONObject inputJSON = new JSONObject();
    JSONObject employeesJSON = new JSONObject();
    JSONObject historyJSON = new JSONObject();
    JSONArray history_planJSON = new JSONArray();

    // map结构的每组人员清单
    Map<String,List> employeesInTeamMap = new HashMap<>();

    // 总人数
    int empTotal = 0;
    // 人员分值统计map
    // eg: {邹明伟=Employee{name='邹明伟', group='FI', totalScore=0, times=0}, 希兴杰=Employee{name='希兴杰', group='FI', totalScore=0, times=0}}
    Map<String, Employee> employeeMap = new HashMap<>();

    // 各组未安排的人数计数器
    Map<String, Integer> teamMap = new HashMap<String, Integer>();

    // 各组不相邻的条件下，所有排班可能
    List<List> teamsNotAdjacentCombinations = new ArrayList<List>();
    // 本次新计划的列表，列表中存放了计划的日期，值班人员，该日期的分值等信息
    List<Plan> planList = new ArrayList<>();
    // 最终所有符合条件计划列表的合集
    List<List<Plan>> endPlans = new ArrayList();



    public Condition(JSONObject inputJSON){
        // 解析JSON
        this.inputJSON=inputJSON;
        this.employeesJSON = inputJSON.getJSONObject("employees");
        this.historyJSON = inputJSON.getJSONObject("history");
        this.history_planJSON = historyJSON.getJSONArray("plan");

        System.out.println("history: " + historyJSON.toString());

        this.employeesInTeamMap = employeesJSON.toJavaObject(Map.class);
        System.out.println("employeesInTeamMap: "+ employeesInTeamMap);

        // 获取各组总人数,各组未安排的人数计数器,人员分值统计map
        for(Map.Entry entry:employeesJSON.entrySet()){
            List list = new ArrayList();
            list = (List) entry.getValue();
            this.empTotal += list.size();
            list.forEach((a)->{
                Employee e = new Employee(a.toString(),entry.getKey().toString(),0,0);
                this.employeeMap.put(a.toString(),e);
            });
            this.teamMap.put(entry.getKey().toString(),list.size());
//            employeesMap.put(entry.getKey().toString(),list);
        }
        System.out.println("empTotal: "+ empTotal);
        System.out.println("employeeMap: "+ employeeMap);
        System.out.println("teamMap: "+ teamMap);
    }




    /**
     * 计划中相邻人员所在的组不能相同
     * Teams must not be adjacent
     * teamsNotAdjacent
     */
    private void teamsNotAdjacent() {
        // 排班计划
        List<String> teamsNotAdjacentCombination = new LinkedList<String>();

        System.out.println("teamMap(Unassigned number counters for each group): " + teamMap.toString());

        // 队列初始化，用来可以从后向前排班
        for(int i = 0 ; i<= empTotal; i++) {
            teamsNotAdjacentCombination.add("-");
        }
        System.out.println("teamsNotAdjacentCombination: " + teamsNotAdjacentCombination.toString());

        // 历史中最后一个值班人员
        Employee lastEmployee = new Employee();

        // 获取历史最后一个值班人员的信息
        if(history_planJSON.size() > 0){
            lastEmployee.setName(history_planJSON.get(history_planJSON.size()-1).toString());
            // 获取最后一人的分组
            for(Map.Entry entry:employeesJSON.entrySet()){
                List<String> list = (List) entry.getValue();

                if(list.contains(lastEmployee.getName())){
                    lastEmployee.setGroup(entry.getKey().toString());
                }
            }
        }
        System.out.println("lastEmployee: "+ lastEmployee);

        // 如果历史最后一个值班人员有小组，第一日小组需要和这个小组区别
        // 结果：所有可能的值班计划列表plans
        if(lastEmployee.getGroup().isEmpty()){
            findTeamsNotAdjacentCombination(empTotal, teamMap,teamsNotAdjacentCombination);
        }else {
            findTeamsNotAdjacentCombination(empTotal, teamMap,teamsNotAdjacentCombination,lastEmployee.getGroup());
        }
        System.out.println("teamsNotAdjacentCombinations Size: " + teamsNotAdjacentCombinations.size());
    }

    /**
     * 按分值排班规则
     * 1、目前分值平均分值最低的员工需要优先安排分值最高的值班日期。
     * 2、目前分值平均分值最高的员工需要优先安排分值最低的值班日期。(未实现)
     * 3、总平均分 = 每天分值总和/总天数
     * 4、每人平均分值 = 每人每次值班分值总和/每人总次数
     */
    private void ScorePreferred() throws ParseException {

        // 获取本周期开始时间
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date tmpDate = format.parse(historyJSON.getString("startDate"));
        cal.setTime(tmpDate);
        System.out.println("history Start date:" + cal.getTime());

        // 积分计算工具初始化
        Score score = new Score(inputJSON.getJSONObject("ScoreRules"));

        // 获取值班计划中每人值班分值总和
        // 循环List
        // 先将值班人员放入map中，不在map中的人员不统计。
        // 计算本次计划开始时间
        for(int i =0; i < history_planJSON.size(); i++){
            String name = history_planJSON.getString(i);
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
            }
        }
        System.out.println("employeeMap after calculate score: " + employeeMap.toString());
        System.out.println("Start date of this new plan: " + format.format(cal.getTime()) );

        // 初始化planList
        for (int i =0; i < employeeMap.size(); i++){
            cal.add(Calendar.DATE, 1);
            int pScore = score.calculate(cal);
            Plan plan = new Plan(i,format.format(cal.getTime()),"",pScore,false);
            planList.add(plan);
        };
        System.out.println("planList: " + planList.toString());

        // 筛选排班的规则
        // 1、目前分值平均分值最低的员工需要优先安排分值最高的值班日期。
        // 2、目前分值平均分值最高的员工需要优先安排分值最低的值班日期。(未实现)

        // 用于值班人员积分大小排序
        List<Employee> employeeList = new ArrayList<>();
        employeeMap.forEach((a,b)->employeeList.add(b));


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
            tempPlans.addAll(teamsNotAdjacentCombinations);
            tempPlans = tempPlans.stream()
                    .filter(p -> p.get(maxPlan.getIndex()).equals(minEmp.getGroup()))
                    .collect(Collectors.toList());
            if (tempPlans.size() != 0) {
                maxPlan.setArranged(true);
                maxPlan.setName(minEmp.getName());
                employeesInTeamMap.get(minEmp.getGroup()).remove(minEmp.getName());
                teamsNotAdjacentCombinations.clear();
                teamsNotAdjacentCombinations.addAll(tempPlans);
            }else {
                break;
            }
            System.out.println("Remaining employeeList:"+ employeeList);
            System.out.println("Latest planList :" + planList);
            System.out.println("All remaining possible teamsNotAdjacentCombinations.size: " + teamsNotAdjacentCombinations.size());
        }
    }

    /**
     * 递归遍历树节点，查找所有的组合,本次新计划前一天值班人员不在现有小组内，或者前一天没有人
     *
     */
    private void findTeamsNotAdjacentCombination(int floor, Map<String, Integer> team, List<String> plan){
//        System.out.println("Input: "+floor+"floor,Map:"+team.toString());
        for (Map.Entry<String, Integer> entry: team.entrySet()) {
            // 每次使用临时容器，每次临时容器都一致
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
                        teamsNotAdjacentCombinations.add(tempPlan);
//                        System.out.println("Finish! " + tempPlan.toString());
                    } else {
                        // 没有到达第一层则继续递归
                        findTeamsNotAdjacentCombination(floor - 1,tempTeam,tempPlan);
                    }
                }

            }
        }
//        System.out.println("End Input: "+floor+"floor,Map:"+team.toString());
    }

    /**
     *
     * 递归遍历树节点，查找所有的组合,第一天的值班人员所在的小组需要和前一天值班人员的小组不同
     *
     */
    private void findTeamsNotAdjacentCombination(int floor, Map<String, Integer> team, List<String> plan, String lastTeamName){
//        System.out.println("Input: "+floor+"floor,Map:"+team.toString());
        for (Map.Entry<String, Integer> entry: team.entrySet()) {
            // 每次使用临时容器，每次临时容器都一致
            // map等容器对象都是地址，直接复制只是复制地址，所以内容变化后，原容器也会发生改变，这里使用putAll（数据拷贝）。
            Map<String, Integer> tempTeam = new HashMap<String, Integer>();
            tempTeam.putAll(team);
            List<String> tempPlan = new LinkedList<String>();
            tempPlan.addAll(plan);
//            System.out.println("tempPlan: " + tempPlan.toString());

            // 历史值班中最后一个人的小组和第一个值班的小组不能相同
            if(0 == floor - 1 && entry.getKey().equalsIgnoreCase(lastTeamName)){
                continue;
            }
            // 判断该组人数是否已经使用完，使用完则不安排
            if(entry.getValue() > 0) {
                // 后一个元素和本元素不同则插入
                if (!entry.getKey().equals(plan.get(floor))) {
                    // 插入并为该组人数减去一人
                    tempPlan.set(floor - 1, entry.getKey().toString());
                    tempTeam.replace(entry.getKey(), entry.getValue() - 1);

                    // 如果到达第一层，结束递归
                    if (0 == floor - 1) {
                        teamsNotAdjacentCombinations.add(tempPlan);
//                        System.out.println("Finish! " + tempPlan.toString());
                    } else {
                        // 没有到达第一层则继续递归
                        findTeamsNotAdjacentCombination(floor - 1,tempTeam,tempPlan,lastTeamName);
                    }
                }

            }
        }
//        System.out.println("End Input: "+floor+"floor,Map:"+team.toString());
    }


//    public static <T> List<T> listDeepCopy(List<T> src) throws IOException, ClassNotFoundException {
//        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
//        ObjectOutputStream out = new ObjectOutputStream(byteOut);
//        out.writeObject(src);
//
//        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
//        ObjectInputStream in = new ObjectInputStream(byteIn);
//        @SuppressWarnings("unchecked")
//        List<T> dest = (List<T>) in.readObject();
//        return dest;
//    }
//    public static <K,V> Map<K,V> mapDeepCopy(Map<K,V> src) throws IOException, ClassNotFoundException {
//        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
//        ObjectOutputStream out = new ObjectOutputStream(byteOut);
//        out.writeObject(src);
//
//        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
//        ObjectInputStream in = new ObjectInputStream(byteIn);
//        @SuppressWarnings("unchecked")
//        Map<K,V> dest = (Map<K,V>) in.readObject();
//        return dest;
//    }
    private static <T> T deepCopy(T src) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteOut);
        out.writeObject(src);

        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        ObjectInputStream in = new ObjectInputStream(byteIn);
        @SuppressWarnings("unchecked")
        T dest = (T) in.readObject();
        return dest;
    }

    /**
     * 执行所有的过滤条件
     * @throws ParseException
     */
    public void executeAllCondition() throws ParseException {
        // 相邻两天的值班人员不能同组
        teamsNotAdjacent();
        // 值班积分最小者安排分值最大的日期
        ScorePreferred();

        System.out.println("[KEY INFO]Fixed arrangement:\n" + planList);
    }

    /**
     * 递归遍历树节点，查找所有的组合
     * @param floor 递归层次
     * @param teamPlan 小组值班计划（由每个小组不可相邻计算得出）
     * @param planList 已经计算出的值班计划
     * @param empMap 人员列表（按小组）
     * @param last3 历史上最后3天值班人员
     */
    private void findEmployeePlan(int floor,
                                         List<String> teamPlan,
                                         List<Plan> planList,
                                         Map<String,List> empMap,
                                         List<String> last3
    ) throws IOException, ClassNotFoundException {

        if(floor >= planList.size()){
            // 到达计划列表末尾，将计划存储
            endPlans.add(planList);
//            System.out.println("endPlan: "+ planList);
        }else {
            // 该日期是否已经安排人员值班
            if(planList.get(floor).isArranged()){
                findEmployeePlan(floor+1,teamPlan,planList,empMap,last3);
            }else {
                List<String> team = empMap.get(teamPlan.get(floor));
                for(int i=0; i < team.size(); i++){
                    // 计划开始之前3天的人，不能在开头3天安排值班
                    if(floor < 3 && last3.contains(team.get(i))){
                        continue;
                    }

                    // 设置传递给下一层次的值，需要不影响本层次的值
                    // map等容器对象都是地址，直接复制只是复制地址，所以内容变化后，原容器也会发生改变。这里进行深度拷贝。
                    // 最终结果
                    List<Plan> tempPlan = deepCopy(planList);
                    tempPlan.get(floor).setName(team.get(i));
                    tempPlan.get(floor).setArranged(true);
                    //剩余值班人员
                    Map<String, List> tempEmpMap = deepCopy(empMap);
                    tempEmpMap.get(teamPlan.get(floor)).remove(i);

                    findEmployeePlan(floor + 1, teamPlan, tempPlan, tempEmpMap,last3);
                }
            }
        }
    }


    /**
     * 随机获取一种团队组合并安排人员
     */
    public void randomPlan(){
        // 随机获取一种团队组合并安排人员
        SecureRandom secureRandom = new SecureRandom();
        int luckyNum = secureRandom.nextInt(teamsNotAdjacentCombinations.size());
        System.out.println("teamsNotAdjacentCombinations.get(" + luckyNum + "): " + teamsNotAdjacentCombinations.get(luckyNum));

        // 倒数三天人员名单,用于计划前三天排除这些人员
        List<String> last3 = history_planJSON.toJavaList(String.class).subList(history_planJSON.size()-3,history_planJSON.size()-1);

        try {
            // 保护现场，确保该方法可重复执行。
            List<Plan> tempPlan = deepCopy(planList);
            Map<String, List> empMap = deepCopy(employeesInTeamMap);
            // one luck team组合安排人员
            findEmployeePlan(0, teamsNotAdjacentCombinations.get(luckyNum),tempPlan,empMap,last3);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("All possible shift plans.size: "+endPlans.size());

        // 随机获取一种团队组合并安排人员
        luckyNum = secureRandom.nextInt(endPlans.size());
        System.out.println("No." + luckyNum + " lucky dog is ...\n " + endPlans.get(luckyNum));
    }

}
