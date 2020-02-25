package com.xlioo.scheduling.unit;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Create by 一初 on 2020/1/31
 */
public class Score {
    public static JSONArray weak = new JSONArray();
    public static JSONObject specialDate = new JSONObject();

    public Score(JSONObject rule){
        weak = rule.getJSONArray("weak");
        specialDate = rule.getJSONObject("specialDate");
    }

    public int calculate(Calendar cal){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String iDate = format.format(cal.getTime());
        System.out.println("date :" + iDate);
        if (specialDate.containsKey(iDate)){
            System.out.println(" Hive the date !");
            return specialDate.getInteger(iDate);
        } else {
            // DAY_OF_WEEK 是 周日为1，周一为2
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            System.out.println("Don't have the date! Today is " + dayOfWeek);
            // weak数组中周日的下标是0，所以要-1
            return weak.getInteger(dayOfWeek -1);

        }
    }

}
