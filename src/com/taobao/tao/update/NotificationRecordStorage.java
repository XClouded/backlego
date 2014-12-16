package com.taobao.tao.update;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.taobao.util.StringUtils;
import com.taobao.tao.Globals;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 版本更新通知存储
 * 
 * @author zhuwang
 * @date 2013年12月20日
 */
public class NotificationRecordStorage {

    private static final String STORAGE_DATABASE = "notification_record";

    /**
     * 更新提醒次数
     * 
     * @param newVersion
     * @param notificationTimes
     */
    public static void update(String newVersion, int notificationTimes) {
        
        if (StringUtils.isEmpty(newVersion) || notificationTimes <= 0) {
            return;
        }
        
        int newVersionTimes = get(newVersion);

        SharedPreferences sp = getDatabase();
        Editor editor = sp.edit();
        String key = getKey(newVersion);
        if (newVersionTimes <= 0) {// 今天没有对该新版本的记录，则清理所有数据，并对新版本初始化次数
            
            editor.clear();
            editor.putInt(key, 1);
        } else if (newVersionTimes < notificationTimes) {// 今天的提醒次数还没有满，则提醒次数+1; 若提醒次数已满，则不再提醒，不做任何处理
            
            editor.putInt(key, ++newVersionTimes);
        }
        editor.commit();

    }

    /**
     * 获取今天的提醒次数
     * 
     * @param newVersion empty? -1 : notificationTimes
     * @return
     */
    public static int get(String newVersion) {

        if (StringUtils.isEmpty(newVersion)) {
            return -1;
        }

        SharedPreferences sp = getDatabase();

        String key = getKey(newVersion);

        int newVersionTimes = sp.getInt(key, -1);
        return newVersionTimes;
    }

    /**
     * 获取今天对应新版本的key
     * 
     * @param newVersion
     * @return
     */
    private static String getKey(String newVersion) {

        if (StringUtils.isEmpty(newVersion)) {
            return "";
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String key = newVersion + today;

        return key;
    }

    public static SharedPreferences getDatabase(){
        
        SharedPreferences sp = Globals.getApplication().getSharedPreferences(STORAGE_DATABASE, Context.MODE_PRIVATE);
        return sp;
    }

}
