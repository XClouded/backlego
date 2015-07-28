package com.taobao.lightapk;

import android.content.Intent;
import android.os.Environment;
import android.os.StatFs;
import android.taobao.atlas.bundleInfo.AtlasBundleInfoManager;
import android.taobao.atlas.bundleInfo.BundleListing;
import android.taobao.atlas.framework.Atlas;
import android.text.TextUtils;
import android.util.Log;
import com.taobao.lightapk.dataobject.MtopTaobaoClientGetBundleListRequest;
import com.taobao.lightapk.dataobject.MtopTaobaoClientGetBundleListResponse;
import com.taobao.lightapk.dataobject.MtopTaobaoClientGetBundleListResponseData;
import com.taobao.tao.Globals;
import com.taobao.tao.homepage.preference.AppPreference;
import com.taobao.tao.util.TaoHelper;
import mtopsdk.mtop.domain.MethodEnum;
import mtopsdk.mtop.domain.MtopResponse;
import mtopsdk.mtop.intf.Mtop;
import mtopsdk.mtop.util.MtopConvert;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by guanjie on 14-9-15.
 */
public class BundleInfoManager {

    public final String[] HIGH_PRIORITY_BUNDLES = new String[]{"com.taobao.passivelocation"};
    public final String TAG = "BundleInfoManager";
    private static BundleInfoManager sManager;
    /**
     * 本地含有的bundle,包括安装和未安装的
     */
    public static List<String> sInternalBundles;
    private BundleListing currentListing;

    private BundleInfoManager(){
    }

    public static synchronized BundleInfoManager instance(){
        if(sManager==null){
            sManager = new BundleInfoManager();
        }
        return sManager;
    }

    public BundleListing getBundleListing(){
        return currentListing;
    }

    /*
     * 同步处理
     */
    public boolean resoveBundleUrlFromServer(){
        MtopTaobaoClientGetBundleListRequest request = new MtopTaobaoClientGetBundleListRequest();
        request.setGroup("android_taobao_bundle");
        request.setMainVersion(Globals.getVersionName());
        MtopResponse response = Mtop.instance(Globals.getApplication()).build(request, TaoHelper.getTTID()).reqMethod(MethodEnum.POST).syncRequest();
        if(response.isApiSuccess()){
            // 请求成功
            MtopTaobaoClientGetBundleListResponse outputDo= (MtopTaobaoClientGetBundleListResponse) MtopConvert.jsonToOutputDO(response.getBytedata(), MtopTaobaoClientGetBundleListResponse.class);
            if(outputDo!=null && outputDo.getData()!=null && outputDo.getData().getBundleList()!=null){
                updateBundleDownloadInfo(outputDo.getData());
                return true;
            }

        }else if (response.isSessionInvalid()) {
            return false;
        }else if(response.isSystemError() || response.isNetworkError() || response.isExpiredRequest()
                || response.is41XResult() || response.isApiLockedResult()||response.isMtopSdkError()){
            return false;
        }else {
            return false;
        }
        return false;
    }

    public BundleListing.BundleInfo getBundleInfoByPkg(String name)
    {
        InitBundleInfoByVersionIfNeed();
        BundleListing listing = currentListing;
        if(listing!=null && listing.getBundles()!=null) {
            for (BundleListing.BundleInfo info : listing.getBundles()){
                if(info.getPkgName().equals(name)){
                    return info;
                }
            }
        }
        return null;
    }
    /**
     * 更新bundle相关的下载信息
     */
    public void updateBundleDownloadInfo(MtopTaobaoClientGetBundleListResponseData dataList){
        InitBundleInfoByVersionIfNeed();
        BundleListing listing = currentListing;
        for(BundleListing.BundleInfo info : listing.getBundles()){
            for(MtopTaobaoClientGetBundleListResponseData.Item item : dataList.getBundleList()){
                if(item!=null && item.getName().equals(info.getPkgName())){
                    info.setSize(item.getSize());
                    info.setMd5(item.getMd5());
                    info.setVersion(item.getVersion());
                    info.setUrl(item.getPackageUrl());
                    info.setDependency(item.getDependency());
                }
            }
        }
        AtlasBundleInfoManager.instance().persistListToFile(listing,Globals.getVersionName());
    }

    /**
     * 根据bundle包名获取下载地址
     * @param pkgs
     * @return
     */
    public BatchBundleDownloader.DownloadItem[] getBundleDownloadInfoByPkg(List<String> pkgs){
        if(pkgs==null){
            return null;
        }
        InitBundleInfoByVersionIfNeed();
        BundleListing listing = currentListing;
        BatchBundleDownloader.DownloadItem[] item = new BatchBundleDownloader.DownloadItem[pkgs.size()];
        for(int x=0; x<pkgs.size(); x++){
            String pkg = pkgs.get(x);
            for(BundleListing.BundleInfo info : listing.getBundles()){
                if(info.getPkgName().equals(pkg)){
                    item[x] = new BatchBundleDownloader.DownloadItem();
                    item[x].setDownloadUrl(info.getUrl());
                    item[x].setMd5(info.getMd5());
                    item[x].setSize(info.getSize());
                    item[x].setPkg(pkg);
                }
            }
        }
        return item;
    }

    /**
     * 根据activity名字查找bundle信息
     * @param className
     * @return
     */
    public BundleListing.BundleInfo findBundleByActivity(String className){
        InitBundleInfoByVersionIfNeed();
        BundleListing listing = currentListing;
        if(listing!=null){
             return listing.resolveBundle(className,BundleListing.CLASS_TYPE_ACTIVITY);
        }
        return null;
    }

    private final String HIGH_PRIORITY_BUNDLES_FORDOWNLOAD = "HIGH_PRIORITY_BUNDLES_FORDOWNLOAD";
    private void storeHighPriorityBundles(List<String> pkgs){
        String joined = TextUtils.join(", ", pkgs);
        AppPreference.putString(HIGH_PRIORITY_BUNDLES_FORDOWNLOAD,joined);
    }

    public void updateHighPriorityBundleByRemove(String[] pkgs){
        if(pkgs==null){
            return;
        }
        String joined = AppPreference.getString(HIGH_PRIORITY_BUNDLES_FORDOWNLOAD,"");
        if(joined!=null){
            List<String> pkgList = Arrays.asList(joined.split(","));
            if(pkgList!=null){
                for(String pkg : pkgs){
                    if(pkgList.contains(pkg)){
                        pkgList.remove(pkg);
                    }
                }
                storeHighPriorityBundles(pkgList);
            }
        }

    }

    public static String getPackageNameFromEntryName(String entryName) {
        String packageName = entryName.substring(entryName.indexOf("lib/armeabi/lib") + "lib/armeabi/lib".length(),
                entryName.indexOf(".so"));
        packageName = packageName.replace("_", ".");
        return packageName;
    }

    public synchronized void resolveInternalBundles() {
        if(sInternalBundles !=null && sInternalBundles.size()!=0)
            return ;
        String prefix = "lib/armeabi/libcom_";
        String suffix = ".so";
        sInternalBundles = new ArrayList<String>();
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(Globals.getApplication().getApplicationInfo().sourceDir);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                String entryName = zipEntry.getName();
                if (entryName.startsWith(prefix) && entryName.endsWith(suffix)) {
                    sInternalBundles.add(getPackageNameFromEntryName(entryName));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception while get bundles in assets or lib", e);
        }finally {
            try{
                if(zipFile!=null){
                    zipFile.close();
                }
            }catch(Exception e){}
        }
    }

    /**
     * 静默下载高优先级bundle
     */
    public void downAndInstallHightPriorityBundleIfNeed(){
        InitBundleInfoByVersionIfNeed();
        if(sInternalBundles ==null){
            resolveInternalBundles();
        }
        if(sInternalBundles ==null){
            return ;
        }
        String joined = AppPreference.getString(HIGH_PRIORITY_BUNDLES_FORDOWNLOAD,"");
        final ArrayList<String> pkgs = new ArrayList<String>();
        for(String pkg : HIGH_PRIORITY_BUNDLES){
            if(!sInternalBundles.contains(pkg) && Atlas.getInstance().getBundle(pkg)==null){
                pkgs.add(pkg);
            }
        }
        if(!TextUtils.isEmpty(joined)){
            List<String> list = Arrays.asList(joined.split(","));
            pkgs.addAll(list);
        }
        if(canExtendSilentDownload()) {
            BundleListing listing = currentListing;
            if (listing != null && listing.getBundles() != null) {
                ArrayList<BundleListing.BundleInfo> infos = (ArrayList<BundleListing.BundleInfo>) listing.getBundles();
                if (infos != null) {
                    for (BundleListing.BundleInfo info : infos) {
                        if(info!=null && info.getPkgName()!=null){
                            if(Atlas.getInstance().getBundle(info.getPkgName())==null && !sInternalBundles.contains(info.getPkgName()) && !pkgs.contains(info.getPkgName())){
                                pkgs.add(info.getPkgName());
                            }
                        }
                    }
                }
            }
        }
        if(pkgs==null || pkgs.size()==0){
            return;
        }
        Intent intent = new Intent();
        intent.setClass(Globals.getApplication(),BackgroundBundleGetService.class);
        intent.putStringArrayListExtra(BackgroundBundleGetService.BUNDLES_TOINSTALL,pkgs);
        Globals.getApplication().startService(intent);
        /**
        new AsyncTask<String,Integer,Integer>(){
            @Override
            protected Integer doInBackground(String... strings) {
                for(int x=0; x<pkgs.size(); x++){
                    BundleListing.BundleInfo mTargetBundleInfo = BundleInfoManager.instance().getBundleInfoByPkg(pkgs.get(x));
                    if(mTargetBundleInfo!=null){
                        downLoadAndInstallBundle(mTargetBundleInfo);
                    }
                }
                return null;
            }
        }.execute();
         **/
    }

    private boolean canExtendSilentDownload(){
        StatFs statfs = null;
        try{
            statfs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        }catch(Exception e){
            e.printStackTrace();
        }
        long totalSpace = 0;
        if(statfs != null)
            totalSpace = (long)statfs.getAvailableBlocks()*statfs.getBlockSize();
        if(totalSpace>200*1024*1024 && getNumCores()>=2){
            return true;
        }
        return false;
    }

    public void downLoadAndInstallBundle(BundleListing.BundleInfo mTargetBundleInfo,final boolean installImmediately){
        if(Atlas.getInstance().getBundle(mTargetBundleInfo.getPkgName())!=null){
            return;
        }
        boolean flag = true;
        Log.d(TAG,"url = hhh "+mTargetBundleInfo.getUrl());
        if(mTargetBundleInfo!=null && TextUtils.isEmpty(mTargetBundleInfo.getUrl())){
            flag =  BundleInfoManager.instance().resoveBundleUrlFromServer();
        }else if(mTargetBundleInfo!=null){
            List<String> dependencyPkg = mTargetBundleInfo.getDependency();
            if(dependencyPkg!=null){
                for(String pkg : dependencyPkg){
                    BundleListing.BundleInfo info =  BundleInfoManager.instance().getBundleInfoByPkg(pkg);
                    if(info!=null && TextUtils.isEmpty(info.getUrl())){
                        flag =  BundleInfoManager.instance().resoveBundleUrlFromServer();
                    }
                }
            }
        }
        if(!flag){
            Log.d(TAG,"获取bundle下载地址失败");
            return ;
        }
        /**
         * 开始下载bundle
         */
        BatchBundleDownloader mBatchBundleDownloader = null;
        if(mBatchBundleDownloader==null){
            mBatchBundleDownloader = BatchBundleDownloader.obtainBatchBundleDownloader(Globals.getApplication(),mTargetBundleInfo.getPkgName());
            mBatchBundleDownloader.addDownloadListener(new BatchBundleDownloader.BatchDownloadListener() {
                @Override
                public void onDownloadProgress(int i) {
                }

                @Override
                public void onDownloadError(int i, String s) {
                }

                @Override
                public void onDownloadFinish(String s) {
                    if(installImmediately) {
                        startInstallBundleSync(s);
                    }else{
                        File file = new File(s);
                        if(file!=null){
                            file.renameTo(new File(String.format("%s%s%s",file.getParent(),File.separator,file.getName()+"_notinstall")));
                        }
                    }
                }
            });
        }

        if(!mBatchBundleDownloader.isRunning()) {
            ArrayList<String> pkgs = new ArrayList<String>();
            pkgs.add(mTargetBundleInfo.getPkgName());
            if (mTargetBundleInfo.getDependency() != null && mTargetBundleInfo.getDependency().size()>0) {
                pkgs.addAll(mTargetBundleInfo.getDependency());
            }

            for (int x = 0; x < pkgs.size(); ) {
                if (Atlas.getInstance().getBundle(pkgs.get(x)) != null) {
                    pkgs.remove(x);
                } else {
                    x++;
                }
            }
            mBatchBundleDownloader.startBatchDownload(
                    BundleInfoManager.instance().getBundleDownloadInfoByPkg(pkgs));
        }
    }

    /**
     * 启动安装bundle
     */
    private void startInstallBundleSync(String bundlePath){
        BatchBundleInstaller mBatchBundleInstaller = null;
        if(mBatchBundleInstaller==null){
            mBatchBundleInstaller = new BatchBundleInstaller(Globals.getApplication());
            mBatchBundleInstaller.setBatchBundleInstallerListener(new BatchBundleInstaller.BatchBundleInstallerListener() {
                @Override
                public void onInstallSuccess(List<String> pkgList) {
                    updateHighPriorityBundleByRemove(pkgList.toArray(new String[pkgList.size()]));
                }

                @Override
                public void onInstallFailed(int errorCode) {
                    Log.d(TAG,"bundle "+" 静默安装失败");

                }
            });
            mBatchBundleInstaller.installBundleSync(bundlePath);
        }
    }

    private int getNumCores() {
        //Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                //Check if filename is "cpu", followed by a single digit number
                if(Pattern.matches("cpu[0-9]", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }

        try {
            //Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            //Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            Log.d(TAG, "CPU Count: "+files.length);
            //Return the number of cores (virtual CPU devices)
            return files.length;
        } catch(Exception e) {
            //Print exception
            Log.d(TAG, "CPU Count: Failed.");
            e.printStackTrace();
            //Default to return 1 core
            return 1;
        }
    }

    /**
     * 根据版本载入清单
     */
    private void InitBundleInfoByVersionIfNeed(){
        if(currentListing==null){
            currentListing = AtlasBundleInfoManager.instance().getBundleInfo();
        }

    }
}
