package com.taobao.lightapk;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StatFs;
import android.taobao.atlas.framework.Atlas;
import android.text.TextUtils;
import android.util.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.mtl.appmonitor.AppMonitor;
import com.taobao.lightapk.dataobject.MtopTaobaoClientGetBundleListRequest;
import com.taobao.lightapk.dataobject.MtopTaobaoClientGetBundleListResponse;
import com.taobao.lightapk.dataobject.MtopTaobaoClientGetBundleListResponseData;
import com.taobao.statistic.TBS;
import com.taobao.tao.Globals;
import com.taobao.tao.homepage.preference.AppPreference;
import com.taobao.tao.util.TaoHelper;
import mtopsdk.mtop.domain.MethodEnum;
import mtopsdk.mtop.domain.MtopResponse;
import mtopsdk.mtop.intf.Mtop;
import mtopsdk.mtop.util.MtopConvert;
import com.taobao.statistic.TBS;

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
    private final String BUNDLE_LIST_FILE_PREFIX = "bundleInfo-";
    private final String LIST_FILE_DIR;
    private HashMap<String,BundleListing> listingHashMap = new HashMap<String, BundleListing>(2);
    private List<String> mDownloadList;
    private static final String CHARSET = "UTF-8";

    private BundleInfoManager(){
        LIST_FILE_DIR = Globals.getApplication().getFilesDir().getAbsolutePath()+ File.separatorChar+"bundlelisting"+File.separatorChar;
        File file = new File(LIST_FILE_DIR);
        if(!file.exists()){
            file.mkdirs();
        }
    }

    public static synchronized BundleInfoManager instance(){
        if(sManager==null){
            sManager = new BundleInfoManager();
        }
        return sManager;
    }

    public BundleListing getBundleListing(){
        InitBundleInfoByVersionIfNeed(Globals.getVersionName());
        return listingHashMap.get(Globals.getVersionName());
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
        InitBundleInfoByVersionIfNeed(Globals.getVersionName());
        BundleListing listing = listingHashMap.get(Globals.getVersionName());
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
        InitBundleInfoByVersionIfNeed(Globals.getVersionName());
        BundleListing listing = listingHashMap.get(Globals.getVersionName());
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
        if(listingHashMap.get(Globals.getVersionName())!=null){
            listingHashMap.put(Globals.getVersionName(),listing);
        }
        persistListToFile(listing,Globals.getVersionName());
    }

    public void persistListToFile(final BundleListing listing,final String version){
        new AsyncTask<String, String, String>() {
            @Override
            protected String doInBackground(String... params) {
                String content= JSON.toJSONString(listing.getBundles());
                Log.d(TAG,"new listing = "+content);
                String fileName = String.format("%s%s.json",BUNDLE_LIST_FILE_PREFIX,version);
                File bundleInfoFile = new File(String.format("%s%s",LIST_FILE_DIR,fileName));
                if(!bundleInfoFile.exists()){
                    try {
                        bundleInfoFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    BufferedWriter bufferWritter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(bundleInfoFile),CHARSET));
                    bufferWritter.write(content);
                    bufferWritter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return "";
            }
        }.execute();

    }

    /**
     * 更新当前清单
     * @param updateBundles 更新的bundle列表
     * @param oldVersion 主客老版本
     * @param newVersion 主客新版本
     */
    public void mergeCurrentListWithUpdate(List<BundleListing.BundleInfo> updateBundles,String oldVersion,String newVersion){
        Log.d(TAG,"mergeCurrentListWithUpdate");
        InitBundleInfoByVersionIfNeed(oldVersion);
        BundleListing currentListing = listingHashMap.get(oldVersion);
        Log.d(TAG,"old listing size = "+ currentListing.getBundles().size());

        for(BundleListing.BundleInfo updateInfo : updateBundles){
            boolean newBundle = true;
            for(BundleListing.BundleInfo oldInfo : currentListing.getBundles()){
                if(updateInfo.getPkgName().equals(oldInfo.getPkgName())){
                    Log.d(TAG,"info "+ updateInfo.getPkgName());
                    newBundle = false;
                    oldInfo.setVersion(updateInfo.getVersion());
                    oldInfo.setMd5(updateInfo.getMd5());
                    oldInfo.setSize(updateInfo.getSize());
                    oldInfo.setDependency(updateInfo.getDependency());
                    oldInfo.setUrl(updateInfo.getUrl());
                }
            }
            if(newBundle){
                BundleListing.BundleInfo info = new BundleListing.BundleInfo();
                info.setPkgName(updateInfo.getPkgName());
                info.setVersion(updateInfo.getVersion());
                info.setMd5(updateInfo.getMd5());
                info.setSize(updateInfo.getSize());
                info.setDependency(updateInfo.getDependency());
                info.setUrl(updateInfo.getUrl());
                currentListing.insertBundle(info);
            }
        }
        if(listingHashMap.get(newVersion)!=null){
            listingHashMap.put(newVersion,currentListing);
        }
        persistListToFile(currentListing,newVersion);
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
        InitBundleInfoByVersionIfNeed(Globals.getVersionName());
        BundleListing listing = listingHashMap.get(Globals.getVersionName());
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
        InitBundleInfoByVersionIfNeed(Globals.getVersionName());
        BundleListing listing = listingHashMap.get(Globals.getVersionName());
        if(listing!=null){
             return listing.resolveBundle(className,BundleListing.CLASS_TYPE_ACTIVITY);
        }
        return null;
    }

    /**
     * 根据service名字查找bundle信息
     * @param className
     * @return
     */
    public BundleListing.BundleInfo findlBundleByService(String className) {
        InitBundleInfoByVersionIfNeed(Globals.getVersionName());
        BundleListing listing = listingHashMap.get(Globals.getVersionName());
        if (listing != null) {
            return listing.resolveBundle(className, BundleListing.CLASS_TYPE_SERVICE);
        }
        return null;
    }

    /**
     * 删除老的清单
     * @param mainVersion
     */
    public void removeBundleListing(){
//        Log.d(TAG,"remove bundle version= "+mainVersion);
//        String fileName = String.format("%s%s.json",BUNDLE_LIST_FILE_PREFIX,mainVersion);
//        String filePath = String.format("%s%s",LIST_FILE_DIR,fileName);
//        File file = new File(filePath);
//        if(file.exists()){
//            file.delete();
//        }
        File file =new File(LIST_FILE_DIR);
        if(file.exists() && file.isDirectory()){
            File[] files = file.listFiles();
            for(File item : files){
                item.delete();
            }

        }
    }

    /**
     * 查找已安装的版本兼容的bundle
     * @param bundlePkgHasInstalled 已经安装的bundle列表
     * @param oldMainVersion    老的主客版本
     * @param newMainVersion    新的主客版本
     * @param ifAutoUpdateDifferentVersionBundle  是否自动更新已安装的不兼容bundle
     * @return  返回不需要更新的bundle
     */
    public ArrayList<String> resolveSameVersionBundle(String[] bundlePkgHasInstalled,String oldMainVersion,String newMainVersion,
                                             boolean ifAutoUpdateDifferentVersionBundle){
        ArrayList<String> sameListWhichHasInstalled = new ArrayList<String>();
        ArrayList<String> differentWhichHasInstalled= new ArrayList<String>();

        if(bundlePkgHasInstalled==null || TextUtils.isEmpty(oldMainVersion) || TextUtils.isEmpty(newMainVersion)){
            return null;
        }
        InitBundleInfoByVersionIfNeed(oldMainVersion);
        InitBundleInfoByVersionIfNeed(newMainVersion);
        BundleListing oldListing = listingHashMap.get(oldMainVersion);
        BundleListing newListing = listingHashMap.get(newMainVersion);
        if(oldListing==null || newListing==null || oldListing.getBundles()==null || newListing.getBundles()==null){
            return null;
        }

        for(BundleListing.BundleInfo info : oldListing.getBundles()){
            Log.d(TAG,info.getPkgName() +"|"+info.getVersion()+";");
        }
        for(BundleListing.BundleInfo info : newListing.getBundles()){
            Log.d(TAG,info.getPkgName() +"|"+info.getVersion()+";");
        }
        ArrayList<String> sameList = resolveSameVersion(newListing,oldListing);
        Log.d(TAG,"size = "+ sameList.size());

        if(sameList==null || sameList.size()==0){
            return sameListWhichHasInstalled;
        }
        Log.d(TAG,"installed size = "+ bundlePkgHasInstalled.length+"");
        for(String pkg : bundlePkgHasInstalled){
            if(pkg!=null && sameList.contains(pkg)){
                sameListWhichHasInstalled.add(pkg);
            }else{
                differentWhichHasInstalled.add(pkg);
            }
        }
        Log.d(TAG,"same size = "+ sameListWhichHasInstalled.size()+"");
        if(differentWhichHasInstalled!=null){
            storeHighPriorityBundles(differentWhichHasInstalled);
            downAndInstallHightPriorityBundleIfNeed();
        }
        return sameListWhichHasInstalled;
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

    public void updateHighPriorityBundleByAdd(String pkg){
        if(pkg==null){
            return;
        }
        String joined = AppPreference.getString(HIGH_PRIORITY_BUNDLES_FORDOWNLOAD,"");
        if(joined!=null){
            List<String> pkgList = Arrays.asList(joined.split(","));
            pkgList.add(pkg);
            storeHighPriorityBundles(pkgList);
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
        try {
            ZipFile zipFile = new ZipFile(Globals.getApplication().getApplicationInfo().sourceDir);
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
        }
    }

    /**
     * 静默下载高优先级bundle
     */
    public void downAndInstallHightPriorityBundleIfNeed(){
        InitBundleInfoByVersionIfNeed(Globals.getVersionName());
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
            BundleListing listing = listingHashMap.get(Globals.getVersionName());
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
        if(mTargetBundleInfo!=null && mTargetBundleInfo.getUrl()==null){
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
            if (mTargetBundleInfo.getDependency() != null) {
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


    /**
     * 清单对比，查找两个版本里面版本兼容的bundle
     * @param newListing
     * @param oldListing
     * @return
     */
    private ArrayList<String> resolveSameVersion(BundleListing newListing,BundleListing oldListing){
        ArrayList<String> sameList = new ArrayList<String>();
        if(newListing==null || oldListing==null){
            return sameList;
        }
        for(BundleListing.BundleInfo oldInfo : oldListing.getBundles()){
            for(BundleListing.BundleInfo newInfo : newListing.getBundles()){
                if(oldInfo!=null && newInfo!=null && oldInfo.getVersion()!=null && newInfo.getVersion()!=null &&  oldInfo.getPkgName().equals(newInfo.getPkgName())
                        && oldInfo.getVersion().equals(newInfo.getVersion())){
                    sameList.add(oldInfo.getPkgName());
                }
            }
        }
        return sameList;
    }



    /**
     * 根据版本载入清单
     */
    private void InitBundleInfoByVersionIfNeed(String mainVersion){
        if(listingHashMap.get(mainVersion)==null){
            String fileName = String.format("%s%s.json",BUNDLE_LIST_FILE_PREFIX,mainVersion);
            String bundleInfoStr = getFromFile(String.format("%s%s",LIST_FILE_DIR,fileName));
            if(bundleInfoStr==null){
                bundleInfoStr = getFromAssets(fileName,Globals.getApplication());
            }
			if (bundleInfoStr==null){
				TBS.Ext.commitEvent(61005, -3, "", "", "bundleInfoStr is null. file is:" + fileName);
			}
            if(!TextUtils.isEmpty(bundleInfoStr)) {
                try {
                    List<BundleListing.BundleInfo> infos = JSON.parseArray(bundleInfoStr, BundleListing.BundleInfo.class);
                    BundleListing listing = new BundleListing();
                    listing.setBundles(infos);
                    if (listing != null) {
                        listingHashMap.put(mainVersion, listing);
                    }
                }catch(Throwable e){
                	TBS.Ext.commitEvent(61005, -3, "", "", "parse failed for bundleinfolist " + fileName);
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 更新bundle信息
     * @param mainVersion
     */
    private void updateBundleInfoByVersion(String mainVersion){
        if(listingHashMap.get(mainVersion)!=null){
            String fileName = String.format("%s%s.json",BUNDLE_LIST_FILE_PREFIX,mainVersion);
            String bundleInfoStr = getFromFile(String.format("%s%s",LIST_FILE_DIR,fileName));
            if(bundleInfoStr==null){
                bundleInfoStr = getFromAssets(fileName,Globals.getApplication());
            }
            List<BundleListing.BundleInfo> infos = JSON.parseArray(bundleInfoStr, BundleListing.BundleInfo.class);
            BundleListing listing = new BundleListing();
            listing.setBundles(infos);
            if(listing!=null){
                listingHashMap.put(mainVersion,listing);
            }
        }
    }

    private String getFromAssets(String fileName,Context context){
        BufferedReader bufReader = null;
        try {
            InputStreamReader inputReader = new InputStreamReader(context.getResources().getAssets().open(fileName), CHARSET);
            bufReader = new BufferedReader(inputReader);
            String line="";
            String result="";
            while((line = bufReader.readLine()) != null)
                result += line;
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if(bufReader!=null){
                try {
                    bufReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getFromFile(String fileName){
        File file = new File(fileName);
        if(file.exists()){
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), CHARSET));
                String line="";
                String result="";
                while((line = reader.readLine()) != null)
                    result += line;
                return result;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if(reader!=null){
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        return null;
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
}
