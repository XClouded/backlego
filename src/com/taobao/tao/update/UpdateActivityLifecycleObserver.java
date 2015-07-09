/**
 * 
 */
package com.taobao.tao.update;

import android.app.Activity;
import android.os.Bundle;
import com.taobao.android.compat.ApplicationCompat.AbstractActivityLifecycleCallbacks;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


public class UpdateActivityLifecycleObserver extends AbstractActivityLifecycleCallbacks {

    private static List<WeakReference<Activity>> sRunningActivityList = new ArrayList<WeakReference<Activity>>();
    @Override
    public void onActivityResumed(final Activity a) {
    	Updater.updateCurrentActivity(a);
    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        try {
            int size = sRunningActivityList.size();
            for (int x = 0; x < size; x++) {
                WeakReference<Activity> activityWeakReference = sRunningActivityList.get(x);
                if (activityWeakReference != null && activityWeakReference.get() != null && activityWeakReference.get() == activity) {
                    sRunningActivityList.remove(x);
                    break;
                }
            }
        }catch(Throwable e){

        }

    }

    @Override
    public void onActivityStarted(Activity activity) {
        if(!activity.getClass().getName().equals("com.taobao.tao.welcome.Welcome")){
            Updater.getInstance(activity.getApplicationContext()).update(true);
        }
    }

    @Override
    public void onActivityCreated(Activity activity,
            @Nullable Bundle savedInstanceState) {
        sRunningActivityList.add(new WeakReference<Activity>(activity));
        /**
         * 暂时先关闭
         */
//        if(Globals.isMiniPackage()) {
//            BundleInfo bundleInfo = BundleInfoManager.instance()
//                    .findBundleByActivity(activity.getClass().getName());
//            if (bundleInfo != null) {
//                StorageManager.getInstance(activity.getApplicationContext()).onBundleStarted(bundleInfo.getPkgName());
//            }
//        }
    }

    public static void clearActivityStack(){
        int size = sRunningActivityList.size();
        for(int x=0; x<size; x++){
            WeakReference<Activity> activityWeakReference = sRunningActivityList.get(x);
            if(activityWeakReference!=null){
                Activity activity = activityWeakReference.get();
                if(activity!=null && !activity.isFinishing()){
                    activity.finish();
                }
            }
        }
    }
    
}
