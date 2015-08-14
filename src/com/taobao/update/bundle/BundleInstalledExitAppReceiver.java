package com.taobao.update.bundle;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.taobao.tao.Globals;
import com.taobao.tao.TaoApplication;
import com.taobao.tao.update.UpdateActivityLifecycleObserver;
import com.taobao.update.UpdateUserTrack;
import com.taobao.tao.util.ActivityHelper;

public class BundleInstalledExitAppReceiver extends BroadcastReceiver {
    private static final String KILLER_ACTION = "com.taobao.update.bundle.action.BUNDLEINSTALLED_EXIT_APP";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(KILLER_ACTION)){
            ActivityManager am = (ActivityManager) Globals.getApplication().getSystemService(Context.ACTIVITY_SERVICE);
            String taoPackageName = Globals.getApplication().getPackageName();
            try {
                ComponentName cnTop = am.getRunningTasks(1).get(0).topActivity;
                ComponentName cnBase = am.getRunningTasks(1).get(0).baseActivity;
                if ((!taoPackageName.equals(cnTop.getPackageName())) && (!taoPackageName.equals(cnBase.getPackageName()))) {
                    kill(taoPackageName);
                }
            }catch(Exception e){
                kill(taoPackageName);
            }
        }
    }

    private void kill(String taoPackageName){
        if ((TaoApplication.getProcessName(Globals.getApplication())).equals(taoPackageName)) {
            cancelAlarmService();
            UpdateUserTrack.bundleUpdateTrack("BundleInstalledExitAppReceiver", "Bundle安装成功，开始杀进程");
            UpdateActivityLifecycleObserver.clearActivityStack();
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    ActivityHelper.kill();
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            }, 500);
            UpdateUserTrack.bundleUpdateTrack("BundleInstalledExitAppReceiver", "Bundle安装成功，杀进程失败");
        }
    }
    public static void cancelAlarmService(){
        AlarmManager am = (AlarmManager)Globals.getApplication().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(Globals.getApplication(),com.taobao.update.bundle.BundleInstalledExitAppReceiver.class);
        intent.setAction(KILLER_ACTION);
        PendingIntent sender = PendingIntent.getBroadcast(Globals.getApplication(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(sender);
        Log.d("BundleInstalledExitAppReceiver", "取消ALARM_SERVICE！！");
    }
}
