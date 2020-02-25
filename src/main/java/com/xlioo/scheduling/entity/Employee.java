package com.xlioo.scheduling.entity;

import java.util.List;

/**
 * Create by 一初 on 2020/1/31
 */
public class Employee {
    public Employee(String name, String group, int times, int totalScore){
        setName(name);
        setGroup(group);
        setTimes(times);
        setTotalScore(totalScore);

    }

    public static Employee pullMinScore(List<Employee> employeeList){
        employeeList.sort((employee1,employee2)->{
            return employee1.getTotalScore() - employee2.getTotalScore() ;
        });
        return employeeList.remove(0);
    }

    @Override
    public String toString() {
        return "Employee{" +
                "name='" + name + '\'' +
                ", group='" + group + '\'' +
                ", totalScore=" + totalScore +
                ", times=" + times +
                '}';
    }

    String name;
    String group;
    int totalScore;
    int times;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public int getTimes() {
        return times;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getTotalScore() {
        return totalScore;
    }
}
