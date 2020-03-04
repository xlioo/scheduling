package com.xlioo.scheduling.entity;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Create by 一初 on 2020/2/9
 */
public class Plan implements Serializable {


    int index;
    String date;
    String name;
    int score;
    boolean isArranged;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Plan(int index,String date,String name,int score, boolean isArranged){
        setIndex(index);
        setDate(date);
        setName(name);
        setScore(score);
        setArranged(isArranged);

    }

    @Override
    public String toString() {
        return "Plan{" +
                "index=" + index +
                ", date='" + date + '\'' +
                ", name='" + name + '\'' +
                ", score=" + score +
                ", isArranged=" + isArranged +
                "}\n";
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isArranged() {
        return isArranged;
    }

    public void setArranged(boolean arranged) {
        isArranged = arranged;
    }

    public static Plan pullMaxScore(List<Plan> planList){
//        planList.sort((plan1,plan2)->{
//            return plan1.getScore() - plan2.getScore() ;
//        });
        // 过滤掉已安排的人员

//        for (int i=0; i< planList.size();i++) {
//            if (planList.get(i).isArranged){
//                planList.remove(i);
//            }
//        }

        return Collections.max(planList,(plan1,plan2)->{
            // 过滤掉已安排的人员
            int a = -1;
            int b = -1;

            if(!plan1.isArranged) a = plan1.getScore();
            if(!plan2.isArranged) b = plan2.getScore();
            return a - b ;
        });
    }
}
