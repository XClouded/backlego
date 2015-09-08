package com.taobao.update.bundle;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.taobao.atlas.bundleInfo.AtlasBundleInfoManager;
import android.taobao.atlas.framework.Atlas;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import com.alibaba.mtl.appmonitor.AppMonitor;
import com.taobao.bspatch.BSPatch;
import com.taobao.lightapk.BundleInfoManager;
import android.taobao.atlas.bundleInfo.BundleListing;
import com.taobao.tao.BaselineInfoProvider;
import com.taobao.tao.Globals;
import com.taobao.tao.TaoApplication;
import com.taobao.tao.update.UpdateActivityLifecycleObserver;
import com.taobao.tao.update.Updater;
import com.taobao.tao.util.ActivityHelper;
import com.taobao.update.UpdateUserTrack;
import com.taobao.update.UpdateUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * bundle安装
 * @author leinuo
 *
 */
public class BundleInstaller extends AsyncTask<Void, Void, Boolean>{
    
    private BundleBaselineInfo mBaselineInfo;
    private Atlas mAtlas;
    private String mUpatePath;
    private Map<String,String> packageInstall;
    private Map<String,String> patchInstall;
    private boolean mPreCheck = false;
    private List<File> preparedInstallBundle = new ArrayList<File>();
    private static boolean sBundlesInstallSuccess = false;
    private static boolean sBundlesRevertSuccess = false;
    private static final String KILLER_ACTION = "com.taobao.update.bundle.action.BUNDLEINSTALLED_EXIT_APP";
    private String oldVersion;
        
    public BundleInstaller(BundleBaselineInfo baselineInfo,String path){
        this.mBaselineInfo = baselineInfo;
        this.mUpatePath = path;
        this.mAtlas = Atlas.getInstance();
        this.packageInstall = mBaselineInfo.getPackageMD5();
        this.patchInstall = mBaselineInfo.getPatchMD5();
    }

    public BundleInstaller(){}
    
    @Override
    protected void onPreExecute() {
        File bundleFiles = new File(mUpatePath);
        if(bundleFiles==null || !bundleFiles.exists()){
            return;
        }
        boolean isSameMD5 = false;
        File[] fileArray = bundleFiles.listFiles();
        for(File file:fileArray){ 
            String bundleName = file.getName();
            if(packageInstall!=null && packageInstall.containsKey(bundleName)){
                String md5 = UpdateUtils.getMD5(file.getAbsolutePath());
                isSameMD5 = packageInstall.get(bundleName).equals(md5);
            }
            if(patchInstall!=null && patchInstall.containsKey(bundleName)){
                String md5 = UpdateUtils.getMD5(Atlas.getInstance().getBundleFile(bundleName).getAbsolutePath());
                isSameMD5 = patchInstall.get(bundleName).equals(md5);
            }
        }
        if(isSameMD5){
            mPreCheck = true;
        }else{
            UpdateUserTrack.bundleUpdateTrack("BundleInstaller","bundle重新下载失败，MD5是否一致："+isSameMD5);
        }
        
    }
    
    @Override
    protected Boolean doInBackground(Void... params) {
        
        if(!mPreCheck){
            return false;
        }
        boolean isSucess = true;
        File bundleLocation = new File(mUpatePath);
        if(bundleLocation==null || !bundleLocation.exists()){
            return isSucess;
        }
        File[] bundleArray = bundleLocation.listFiles();
        if(bundleArray.length==0){
            return isSucess;
        }
        for(File bundle:bundleArray){
            String bundleName = bundle.getName();
            if(packageInstall !=null && packageInstall.containsKey(bundleName)){
                preparedInstallBundle.add(bundle);
            }else if(patchInstall!=null && patchInstall.containsKey(bundleName)){
                isSucess = patchInstaller(bundle);
            }else{
                isSucess = false;
            }
            
        }
        if(isSucess){
            String[] packageName = new String[preparedInstallBundle.size()];
            File[] bundleFiles = new File[preparedInstallBundle.size()];
            for(int i=0;i<preparedInstallBundle.size();i++){
                File bundle = preparedInstallBundle.get(i);
                packageName[i]= bundle.getName();
                bundleFiles[i]= bundle;
            }
            try {
                mAtlas.installOrUpdate(packageName, bundleFiles);
            } catch (Throwable e) {
                e.printStackTrace();
                isSucess = false;
                UpdateUserTrack.bundleUpdateTrack("BundleInstaller","Atlas bundle批量安装失败! "+e.getMessage());
            }
            if(isSucess){
//                InstrumentationHook.notifyAppUpdated();
                AppMonitor.Counter.commit("dynamicDeploy", "bundleInstalled", mBaselineInfo.getmBaselineVersion(),1);
                UpdateUserTrack.bundleUpdateTrack("BundleInstaller","Atlas bundle安装成功!!本次bundle更新主版本：" + TaoApplication.getVersion() + "本次bundle更新基线版本："+ mBaselineInfo.getmBaselineVersion());
                oldVersion = Globals.getVersionName();
                saveBaselineInfo();
                sBundlesInstallSuccess = true;
            }
        }else{
            UpdateUserTrack.bundleUpdateTrack("BundleInstaller","Atlas bundle安装失败! ");
        }
        clearUpdatePath(mUpatePath);
        return isSucess;
    }
    
    @Override
    protected void onPostExecute(Boolean isSucess) {
	    super.onPostExecute(isSucess);
	    if(isSucess){
//	    	String newVersion = Globals.getVersionName();
            buildBundleInventory(oldVersion,mBaselineInfo.getmBaselineVersion(),mBaselineInfo);
            if(Updater.dynamicdeployForTest && BundleInstaller.isInstallSuccess()){
                Toast.makeText(Globals.getApplication(),"动态部署成功,待手淘自动退出后点击手淘启动",Toast.LENGTH_SHORT).show();
                WindowManager windowManager = (WindowManager)Globals.getApplication().getSystemService(Context.WINDOW_SERVICE);

                TextView tip = new TextView(Globals.getApplication());
                tip.setTextSize(TypedValue.COMPLEX_UNIT_DIP,20);
                tip.setText("动态部署成功,点击退出客户端并点击淘宝图标重新启动");
                tip.setClickable(true);
                tip.setTextColor(Color.BLACK);
                tip.setBackgroundColor(Color.WHITE);
                tip.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                });
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_PHONE,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT);

                params.gravity = Gravity.CENTER;

                windowManager.addView(tip, params);
            }
	    }
    }
    
    private boolean patchInstaller(File bundle){
        String bundleName = bundle.getName();
        File atlasBundle = mAtlas.getBundleFile(bundleName);
        if(atlasBundle!=null && atlasBundle.exists() && patchMerge(atlasBundle.getAbsolutePath(), mUpatePath, bundle)){
            String mergePath = mUpatePath+File.separator+"patchmerge"+File.separator;
            File mergeFile = new File(mergePath,bundle.getName());
            preparedInstallBundle.add(mergeFile);
        }else{
           return false;
        }
        return true;
    }
    private boolean patchMerge(String installedBundlePath,String updatePath,File patchFile){
        File mergeInstallPath = new File(updatePath+File.separator+"patchmerge"+File.separator);
        if(!mergeInstallPath.exists()){
            mergeInstallPath.mkdir();
        }
        File newAPK = new File(mergeInstallPath.getAbsolutePath(),patchFile.getName());
        //合并
        int ret = 0;
        try {
            ret = BSPatch.bspatch(installedBundlePath,newAPK.getAbsolutePath(),patchFile.getAbsolutePath());
        } catch (Throwable e) {
            UpdateUserTrack.bundleUpdateTrack("BundleInstaller","Atlas bundle差量合并失败(call bspatch())! "+e.getMessage());
            return false;
        }
        if(ret == 1){
            //合并成功则校验MD5
            String newApkMD5 = UpdateUtils.getMD5(newAPK.getAbsolutePath());
            String oldApkMD5 =  mBaselineInfo.getPackageMD5WithPatch().get(patchFile.getName());
            if(newApkMD5 != null && newApkMD5.equals(oldApkMD5)){
                return true;
            }else{
                UpdateUserTrack.bundleUpdateTrack("BundleInstaller","Atlas bundle差量合并失败MD5不一致! "+newApkMD5+" 前后MD5不相等 "+oldApkMD5);
                return false;
            }
        }
        return false;
    }
    private void clearUpdatePath(String bundleUpdatePath){
        File updatePath = new File(bundleUpdatePath);
        if(updatePath.exists()){
            File[] updatePathArray = updatePath.listFiles();
            for(File file:updatePathArray){
                if(file.isDirectory()){
                    clearUpdatePath(file.getAbsolutePath());
                }else{
                    file.delete();
                }
            }
            updatePath.delete();
        }
    }

    public static boolean needRollback(){
        File baseLineDir = new File(Globals.getApplication().getFilesDir().getAbsolutePath() + File.separatorChar + "bundleBaseline");
        if(baseLineDir.exists()){
            File deprecated = new File(baseLineDir, "deprecated_mark");
            if(deprecated.exists()){
                return true;
            }
        }
        return false;
    }

    public static void removeBaseLineInfo(){
        try {
            File file = new File(Globals.getApplication().getFilesDir() + File.separator + "bundleBaseline");
            File[] baseLineFiles = file.listFiles();
            if (baseLineFiles != null) {
                for (File baseLineFile : baseLineFiles) {
                    baseLineFile.delete();
                }
            }
            AtlasBundleInfoManager.instance().removeBundleListing();
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public void rollbackHardly(){
        try {
            File baseLineDir = new File(Globals.getApplication().getFilesDir().getAbsolutePath() + File.separatorChar + "bundleBaseline");
            if (!baseLineDir.exists()) {
                baseLineDir.mkdirs();
            }
            File deprecated = new File(baseLineDir, "deprecated_mark");
            if(!deprecated.exists()) {
                deprecated.createNewFile();
            }
            String baseLineInfoPath = Globals.getApplication().getFilesDir()+File.separator+"bundleBaseline"+File.separator+"baselineInfo";
            File file = new File(baseLineInfoPath);
            if(file.exists()){
                file.delete();
            }
            sBundlesRevertSuccess = true;
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void rollback(){
        String lastMainVersion = BaselineInfoProvider.getInstance().getMainVersionName();
        if(!TextUtils.isEmpty(lastMainVersion)) {
            List<String> bundles = BaselineInfoProvider.getInstance().getLastDynamicDeployBunldes();
            if (!Globals.getVersionName().equals(lastMainVersion) && bundles.size() > 0) {
                //回滚到上个版本
                if (!Atlas.getInstance().restoreBundle(bundles.toArray(new String[bundles.size()]))) {
                    rollbackHardly();
                    return;
                }
                String path = Globals.getApplication().getFilesDir().getAbsolutePath() + File.separatorChar + "bundleBaseline" + File.separatorChar;
                File baseinfoFile = new File(path);
                if (!baseinfoFile.exists()) {
                    baseinfoFile.mkdir();
                }
                try {
                    DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(baseinfoFile.getAbsolutePath(), "baselineInfo"))));
                    out.writeUTF(lastMainVersion);
                    out.writeInt(getVersionCode());
                    out.writeUTF(lastMainVersion);
                    out.writeUTF("");
                    out.flush();
                    out.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sBundlesRevertSuccess = true;
            } else {
                //回滚到安装时期
                rollbackHardly();
            }
        }

    }
    
    private void saveBaselineInfo(){
        StringBuilder bundleList = new StringBuilder("");
        List<BundleUpdateInfo> bundleUpdateList = mBaselineInfo.getBundleUpdateList();
        for(BundleUpdateInfo info:bundleUpdateList){
            bundleList.append(info.mBundleName);
            bundleList.append("@");
            bundleList.append(info.mVersion);
            bundleList.append("@");
            bundleList.append(info.mBundleSize);
            bundleList.append(";");
        }
        String path = Globals.getApplication().getFilesDir().getAbsolutePath()+File.separatorChar+"bundleBaseline"+File.separatorChar;
        File baseinfoFile = new File(path);
        if(!baseinfoFile.exists()){
            baseinfoFile.mkdir();
        }
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(baseinfoFile.getAbsolutePath(), "baselineInfo"))));
            out.writeUTF(TaoApplication.getVersion());
            out.writeInt(getVersionCode());
            out.writeUTF(mBaselineInfo.getmBaselineVersion());
            out.writeUTF(bundleList.toString());
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("BundleInstaller","保存基线信息成功！主版本： "+TaoApplication.getVersion()+" 基线版本： "+mBaselineInfo.getmBaselineVersion()+" 安装bundle："+bundleList.toString());
    }
    private int getVersionCode(){
        String packageName =  Globals.getApplication().getPackageName();
        int versionCode = 0;
        try {
            versionCode = Globals.getApplication().getPackageManager().getPackageInfo(packageName, 0).versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }
    public static boolean isInstallSuccess(){
        return sBundlesInstallSuccess;
    }
    public static void exitApp(boolean immediately){
        if(sBundlesInstallSuccess || sBundlesRevertSuccess){
            if(immediately){
                UpdateActivityLifecycleObserver.clearActivityStack();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(isAppRunningBackground()) {
                            BundleInstalledExitAppReceiver.cancelAlarmService();
                            UpdateUserTrack.bundleUpdateTrack("BundleInstalledExitAppReceiver", "Bundle安装成功，开始杀进程");
                            ActivityHelper.kill();
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    }
                },8000);
            }else {
                killProcess();
            }
        }
    }

    public static boolean isAppRunningBackground(){
        ActivityManager am = (ActivityManager) Globals.getApplication().getSystemService(Context.ACTIVITY_SERVICE);
        String taoPackageName = Globals.getApplication().getPackageName();
        try {
            ComponentName cnTop = am.getRunningTasks(1).get(0).topActivity;
            ComponentName cnBase = am.getRunningTasks(1).get(0).baseActivity;
            if ((!taoPackageName.equals(cnTop.getPackageName())) && (!taoPackageName.equals(cnBase.getPackageName()))) {
                return true;
            }
        }catch(Exception e){
            return true;
        }
        return false;
    }

    private static void killProcess(){
        if((TaoApplication.getProcessName(Globals.getApplication())).equals(Globals.getApplication().getPackageName())){
            AlarmManager am = (AlarmManager)Globals.getApplication().getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(Globals.getApplication(),com.taobao.update.bundle.BundleInstalledExitAppReceiver.class);
            intent.setAction(KILLER_ACTION);
            long triggerAtTime = SystemClock.elapsedRealtime() + 10*60*1000;
            long interval = 10*60*1000;
            PendingIntent sender = PendingIntent.getBroadcast(Globals.getApplication(), 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,interval,sender);
            Log.d("BundleInstaller", "设置杀掉进程定时器成功,开始触发时间："+triggerAtTime+" 间隔重复时间： "+interval); 
        }else{
            Log.d("BundleInstaller", "taobao 主进程不存在，现在的进程是： "+TaoApplication.getProcessName(Globals.getApplication())); 
        }
        
    }
    // 动态部署成功后，更新清单信息
    private void buildBundleInventory(String oldVersion,String newVersion,BundleBaselineInfo mBaselineInfo){
    	if(!oldVersion.equals(newVersion)){
    		List<BundleUpdateInfo> bundleList = mBaselineInfo.getBundleUpdateList();
    		List<BundleListing.BundleInfo> inventoryList = new ArrayList<BundleListing.BundleInfo>();   		
    		if(bundleList!=null && bundleList.size() >0){
    			for(BundleUpdateInfo bundleInfo:bundleList){
                    if(bundleInfo.mBundleName!=null && !bundleInfo.mBundleName.equals("com.taobao.maindex")) {
                        BundleListing.BundleInfo inventory = new BundleListing.BundleInfo();
                        inventory.setPkgName(bundleInfo.mBundleName);
                        inventory.setDependency(bundleInfo.dependencies);
                        inventory.setUrl(bundleInfo.mBundleDLUrl);
                        inventory.setSize(bundleInfo.mBundleSize);
                        inventory.setVersion(bundleInfo.mVersion);
                        inventory.setMd5(bundleInfo.mNewBundleMD5);
                        inventoryList.add(inventory);
                    }
    			}
    		}
    		AtlasBundleInfoManager.instance().saveNewBundlelisingWithMerge(inventoryList, newVersion);
    	}
    	
    }
}
