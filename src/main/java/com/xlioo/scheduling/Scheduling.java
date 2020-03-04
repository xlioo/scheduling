package com.xlioo.scheduling;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xlioo.scheduling.entity.Employee;
import com.xlioo.scheduling.entity.Plan;
import com.xlioo.scheduling.unit.Condition;
import com.xlioo.scheduling.unit.Score;

import java.security.SecureRandom;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Create by 一初 on 2020/1/28
 */
public class Scheduling {




    // 组内人员洗牌
    public static Map<String, List> employeesMap = new HashMap<String, List>();

    public static void main(String[] args) throws ParseException {
        System.out.println("args[0]: " + args[0]);
        // 解析json
        JSONObject inputJSON = JSONObject.parseObject(args[0]);

        Condition condition = new Condition(inputJSON);
        // 根据输入参数，计算不可变的计划条件
        condition.executeAllCondition();

        // 在所有可行组合中随机一种
        condition.randomPlan();

        // 等待用户输入
        Scanner  input= new Scanner(System.in);
        System.out.println("Please enter your choice : \n" +
                "0: Exit\n" +
                "1: Next Lucky Plan\n");
        while (true){
            int op=input.nextInt();
            System.out.println("Your input is "+ op);
            switch (op){
                case 0:
                    System.out.println("Thank you for coming!");
                    break;
                case 1:
                    // 在所有可行组合中随机一种
                    condition.randomPlan();
                    break;

                default:
                    System.out.println("Error input，please retype!");
                    continue;
            }
        }

    }

}
