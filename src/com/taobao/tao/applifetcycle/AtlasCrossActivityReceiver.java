package com.taobao.tao.applifetcycle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.taobao.atlas.framework.Atlas;
import android.taobao.utconfig.ConfigCenterLifecycleObserver;
import android.taobao.util.SafeHandler;
import android.taobao.util.TaoLog;
import android.text.TextUtils;
import com.alibaba.fastjson.JSON;
import com.taobao.android.lifecycle.PanguApplication;
import com.taobao.lightapk.ActivityStateLifeCycleCallback;
import com.taobao.lightapk.BundleInfoManager;
import com.taobao.tao.AppStateBroadcastManager;
import com.taobao.tao.ClassNotFoundInterceptor;
import com.taobao.tao.Globals;
import com.taobao.tao.update.UpdateActivityLifecycleObserver;
import com.taobao.tao.util.ActivityHelper;
import com.taobao.update.bundle.BundleInstaller;
import com.taobao.wswitch.api.business.ConfigContainerAdapter;
import org.osgi.framework.BundleException;

import java.util.List;

/**
 * Created by guanjie on 14/12/12.
 */
public class AtlasCrossActivityReceiver extends BroadcastReceiver{

    private SafeHandler mSafeHandler;

    public AtlasCrossActivityReceiver(){
        mSafeHandler = new SafeHandler(Looper.getMainLooper());
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent!=null && intent.getAction().equals("com.taobao.intent.action.APP_STATE")){
            String value = intent.getStringExtra(AppStateBroadcastManager.KEY);
           if(value.equals(AppStateBroadcastManager.START)){
               TaoLog.Logd("AtlasCrossActivityLifeCycleObserver", "Application onStart");
               if(context instanceof PanguApplication) {
                   ((PanguApplication)context).registerActivityLifecycleCallbacks(new ActivityStateLifeCycleCallback());
                   ((PanguApplication)Globals.getApplication()).registerActivityLifecycleCallbacks(new UpdateActivityLifecycleObserver());

               }
               if(Globals.isMiniPackage()) {
                   mSafeHandler = new SafeHandler(Looper.getMainLooper());
                   mSafeHandler.postDelayed(new Runnable() {
                       @Override
                       public void run() {
                           BundleInfoManager.instance().downAndInstallHightPriorityBundleIfNeed();
                       }
                   }, 25000);
               }
           }else if(value.equals(AppStateBroadcastManager.STOP)){
               BundleInstaller.exitApp();

           }else if(value.equals(AppStateBroadcastManager.EXIT)){
               TaoLog.Logd("AtlasCrossActivityLifeCycleObserver","Application onDestroyed");
               if(mSafeHandler!=null){
                   mSafeHandler.destroy();
               }
               ClassNotFoundInterceptor.resetGoH5BundlesIfNotExists();
               /**
                * 删除无用的bundle
                */
               String bundleValue = ConfigContainerAdapter.getInstance().getConfig(
                       ConfigCenterLifecycleObserver.CONFIG_GROUP_SYSTEM, "bundles_touninstall", "");
               if(!TextUtils.isEmpty(bundleValue)){
                   List<String> uninstallBundles = JSON.parseArray(bundleValue, String.class);
                   if(uninstallBundles!=null && uninstallBundles.size()>0){
                       for(String pkgName : uninstallBundles){
                           try {
                               Atlas.getInstance().uninstallBundle(pkgName);
                           } catch (BundleException e) {
                               e.printStackTrace();
                           }
                           ActivityHelper.kill();
                           System.exit(0);
                       }
                   }
               }
           }
        }
    }
}
