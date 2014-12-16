package com.taobao.lightapk;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.net.ConnectivityManagerCompat;
import android.taobao.atlas.framework.Atlas;
import android.taobao.util.SafeHandler;
import android.taobao.util.TaoLog;
import android.text.TextUtils;
import android.util.Log;
import com.taobao.android.task.Coordinator;
import com.taobao.tao.util.NetWorkUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * 后台下载bundle
 * Created by guanjie on 14/11/25.
 */
public class BackgroundBundleGetService extends Service implements Handler.Callback{

    public static final String BROADCAST_APPLICATION_FORGROUND = "application_forground";
    public static final String BROADCAST_APPLICATION_BACKGROUND= "application_background";

    public static final int DL_FINISH = 1234;
    public static final String TAG = "BackgroundBundleGetService";
    private ArrayList<String> mBundleToInstall = new ArrayList<String>();
    public static final String BUNDLES_TOINSTALL = "bundles";
    private ServiceGetRunnable mServiceGetRunnable;
//    private ServiceInstallRunnable mServiceInstallRunnable;
    private NetWorkStateChangeReceiver mNetWorkStateChangeReceiver;
//    private ApplicationStateReceiver mApplicationStateReceiver;
    private SafeHandler mSafeHandler;
    private boolean mIsApplicationForground;


    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter mNetWorkIntentFilter = new IntentFilter();
        mNetWorkIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mNetWorkStateChangeReceiver = new NetWorkStateChangeReceiver();
        getBaseContext().registerReceiver(mNetWorkStateChangeReceiver,mNetWorkIntentFilter);

        IntentFilter mApplicationStateFilter = new IntentFilter();
        mApplicationStateFilter.addAction(BROADCAST_APPLICATION_FORGROUND);
        mApplicationStateFilter.addAction(BROADCAST_APPLICATION_BACKGROUND);
//        mApplicationStateReceiver = new ApplicationStateReceiver();
//        getBaseContext().registerReceiver(mApplicationStateReceiver,mApplicationStateFilter);
        mSafeHandler = new SafeHandler(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent !=null){
            TaoLog.Logd(TAG,"start download service");
            ArrayList<String> bundles = intent.getStringArrayListExtra(BUNDLES_TOINSTALL);
            if(bundles!=null){
                for(String pkg : bundles){
                    if(!TextUtils.isEmpty(pkg) && !mBundleToInstall.contains(pkg) && Atlas.getInstance().getBundle(pkg)==null){
                        mBundleToInstall.add(pkg);
                    }
                }
            }
            if(NetWorkUtils.isNetworkAvailable(getBaseContext()) &&
                    !ConnectivityManagerCompat.isActiveNetworkMetered((ConnectivityManager)getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE))) {
                if (mServiceGetRunnable == null) {
                    mServiceGetRunnable = new ServiceGetRunnable("LightApk_For_Get_Bundle"+System.currentTimeMillis());
                    Coordinator.postTask(mServiceGetRunnable);
                    Coordinator.scheduleIdleTasks();
                }
            }
//            if(!isApplicationForground(getBaseContext())){
//                if(mServiceInstallRunnable==null){
//                    mServiceInstallRunnable = new ServiceInstallRunnable("LightApk_For_Install_Bundle");
//                }
//                Coordinator.postTask(mServiceInstallRunnable);
//                Coordinator.scheduleIdleTasks();
//            }
        }
        return Service.START_NOT_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getBaseContext().unregisterReceiver(mNetWorkStateChangeReceiver);
//        getBaseContext().unregisterReceiver(mApplicationStateReceiver);
    }

    @Override
    public boolean handleMessage(Message message) {
        switch(message.what){
            case DL_FINISH:
                Log.d(TAG,"stop service");
                mServiceGetRunnable = null;
                if(mSafeHandler!=null){
                    mSafeHandler.destroy();
                }
                this.stopSelf();
                break;
        }
        return false;
    }

//    /**
//     *判断当前应用程序处于前台还是后台
//     */
//    public static boolean isApplicationForground(final Context context) {
//        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
//        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
//        if (!tasks.isEmpty()) {
//            ComponentName topActivity = tasks.get(0).topActivity;
//            if (topActivity.getPackageName().equals(context.getPackageName())) {
//                return true;
//            }
//        }
//        return false;
//    }

    public boolean checkResumeTask(){
        if(NetWorkUtils.isNetworkAvailable(getBaseContext()) &&
                !ConnectivityManagerCompat.isActiveNetworkMetered((ConnectivityManager)getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE))){
            if(mServiceGetRunnable==null && mBundleToInstall.size()>0){
                TaoLog.Logd(TAG,"resume task");
                mServiceGetRunnable = new ServiceGetRunnable("LightApk_For_Get_Bundle"+System.currentTimeMillis());
                Coordinator.postIdleTask(mServiceGetRunnable);
                Coordinator.scheduleIdleTasks();
            }
            return true;
        }else{
            return false;
        }
    }

    public class ServiceGetRunnable extends Coordinator.TaggedRunnable{

        private boolean isCanceled = false;

        public ServiceGetRunnable(String tag) {
            super(tag);
        }

        public void cancel(){
            isCanceled = true;
        }

        @Override
        public void run() {
            while(mBundleToInstall.size()>0 && !isCanceled){
//                if(isApplicationForground(getBaseContext())){
//                    break;
//                }
                String bundlePkg = mBundleToInstall.get(0);
                mBundleToInstall.remove(bundlePkg);
                if(!TextUtils.isEmpty(bundlePkg)){
                    BundleListing.BundleInfo mTargetBundleInfo = BundleInfoManager.instance().getBundleInfoByPkg(bundlePkg);
                    if(mTargetBundleInfo!=null) {
                        TaoLog.Logd(TAG,"download and installBundle "+mTargetBundleInfo.getPkgName());
                        BundleInfoManager.instance().downLoadAndInstallBundle(mTargetBundleInfo,true);
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
            mSafeHandler.sendEmptyMessage(DL_FINISH);
        }
    }

    public class ServiceInstallRunnable extends Coordinator.TaggedRunnable{

        private BatchBundleInstaller mBatchBundleInstaller;
        private boolean isCanceled = false;
        public ServiceInstallRunnable(String tag){
            super(tag);
            mBatchBundleInstaller = new BatchBundleInstaller(getBaseContext());
        }

        public void cancel(){
            isCanceled = true;
        }

        @Override
        public void run() {
            String bundleDirectoryStr = getBaseContext().getFilesDir()+ File.separator+ "com/taobao/lightapk";
            File bundleDirectory = new File(bundleDirectoryStr);
            if(bundleDirectory!=null){
                File[] bundles = bundleDirectory.listFiles();
                for(File bundle : bundles){
                    if(isCanceled){
                        break;
                    }
                    if(bundle.isDirectory() && bundle.getAbsolutePath().endsWith("_notinstall")){
                        mBatchBundleInstaller.installBundleSync(bundle.getAbsolutePath());
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public class NetWorkStateChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                if(!checkResumeTask()){
                    TaoLog.Logd(TAG,"stop task");
                    if(mServiceGetRunnable!=null){
                        mServiceGetRunnable.cancel();
                        mServiceGetRunnable=null;
                    }
                }
            }
        }

    }

    public class ApplicationStateReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction()!=null && intent.getAction().equals(BROADCAST_APPLICATION_FORGROUND)){
                if(mServiceGetRunnable!=null)
                    mServiceGetRunnable.cancel();
            }else if(intent.getAction()!=null && intent.getAction().equals(BROADCAST_APPLICATION_BACKGROUND)){
                checkResumeTask();
            }
        }
    }

}
