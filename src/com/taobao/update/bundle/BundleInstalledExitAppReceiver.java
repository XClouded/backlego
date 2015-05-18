package com.taobao.update.bundle;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.taobao.tao.Globals;
import com.taobao.tao.TaoApplication;
import com.taobao.tao.util.ActivityHelper;
import com.taobao.update.UpdateUserTrack;

public class BundleInstalledExitAppReceiver extends BroadcastReceiver {
    private static final String KILLER_ACTION = "com.taobao.update.bundle.action.BUNDLEINSTALLED_EXIT_APP";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(KILLER_ACTION)){
            ActivityManager am = (ActivityManager) Globals.getApplication().getSystemService(Context.ACTIVITY_SERVICE);
            ComponentName cnTop = am.getRunningTasks(1).get(0).topActivity;
            ComponentName cnBase = am.getRunningTasks(1).get(0).baseActivity;
            String taoPackageName = Globals.getApplication().getPackageName();
            if((!taoPackageName.equals(cnTop.getPackageName())) && (!taoPackageName.equals(cnBase.getPackageName()))){
                if((TaoApplication.getProcessName(Globals.getApplication())).equals(taoPackageName)){
                    cancelAlarmService(); 
                    UpdateUserTrack.bundleUpdateTrack("BundleInstalledExitAppReceiver","Bundle安装成功，开始杀进程");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    android.os.Process.killProcess(android.os.Process.myPid());
                    ActivityHelper.kill();
                    UpdateUserTrack.bundleUpdateTrack("BundleInstalledExitAppReceiver","Bundle安装成功，杀进程失败");
                }
            } 
        }
    }
    private static void cancelAlarmService(){
        AlarmManager am = (AlarmManager)Globals.getApplication().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(Globals.getApplication(),com.taobao.update.bundle.BundleInstalledExitAppReceiver.class);
        intent.setAction(KILLER_ACTION);
        PendingIntent sender = PendingIntent.getBroadcast(Globals.getApplication(), 0, intent, 0);
        am.cancel(sender);
        Log.d("BundleInstalledExitAppReceiver", "取消ALARM_SERVICE！！");
    }
}
