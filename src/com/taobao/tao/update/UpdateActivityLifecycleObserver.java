/**
 * 
 */
package com.taobao.tao.update;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.taobao.util.Globals;
import com.taobao.android.compat.ApplicationCompat.AbstractActivityLifecycleCallbacks;
import com.taobao.lightapk.BundleInfoManager;
import com.taobao.lightapk.BundleListing.BundleInfo;
import com.taobao.storagespace.StorageManager;

import javax.annotation.Nullable;


public class UpdateActivityLifecycleObserver extends AbstractActivityLifecycleCallbacks {

    @Override
    public void onActivityResumed(final Activity a) {
    	Updater.updateCurrentActivity(a);
    }

    @Override
    public void onActivityPaused(Activity activity) {

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
    
}
