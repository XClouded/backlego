package com.taobao.tao.update.alipay;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.taobao.util.TaoLog;
import android.text.TextUtils;
import android.util.Log;
import com.taobao.android.task.Coordinator;
import com.taobao.statistic.CT;
import com.taobao.statistic.TBS;
import com.taobao.tao.Globals;
import com.taobao.tao.TaoApplication;
import com.taobao.tao.homepage.preference.AppPreference;
import com.taobao.tao.update.RequestImp;
import com.taobao.tao.update.business.MtopTaobaoFerrariNoviceSendsilentMessageRequest;
import com.taobao.tao.util.NetWorkUtils;
import com.taobao.tao.util.TaoHelper;
import com.taobao.update.*;
import mtopsdk.mtop.domain.MethodEnum;
import mtopsdk.mtop.domain.MtopResponse;
import mtopsdk.mtop.intf.Mtop;

import java.io.*;
import java.util.List;

/**
 * Created by guanjie on 14-7-15.
 */
public class AlipaySilentDownloader implements OnUpdateListener {

    public  final String TAG="AlipaySilentDownloader";
    public  final String[] OUI_MANUFACTOR_ARRAY = new String[]{"00-EE-BD","04-18-0F"};
    public  static final String FILENAME = "AliPay_Extension.alipay";
    private static String APK_STORE_PATH;
    private static AlipaySilentDownloader sInstance;
    private Context mContext;
    private Update mUpdate;
    private NetWorkStateChangeReceiver mNetWorkStateChangeReceiver;
    private static long lastRequestTime = 0;
    public static  boolean NOT_UPDATE = false;


    /**
     * 获取Updater实例
     * @param context	Context实例
     * @return	Updater实例
     */
    public static synchronized AlipaySilentDownloader getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AlipaySilentDownloader(context);
            
        }
        return sInstance;
    }

    protected AlipaySilentDownloader(Context context){
        mContext = context;
        mUpdate = new Update(new DefaultDownloader(context),new RequestImp("1.0","alipay4android"),this);
        File cache = mContext.getExternalCacheDir();
        if(cache == null)
        	cache = mContext.getCacheDir();
        APK_STORE_PATH = cache.getAbsolutePath()+"/taobao/alipay_support/";
        File file = new File(APK_STORE_PATH);
        if(!file.exists()){
            file.mkdirs();
        }
        mUpdate.setApkStorePath(APK_STORE_PATH);
    }

    public void stopDownload(){
        if(mUpdate!=null){
            mUpdate.cancelDownload();
        }
    }

    /**
     * 检查是否进行静默下载
     */
    public void checkDownload(){
        /**
         * 需要下载的条件
         * 1 没有安装支付宝
         * 2 没有静默下载包
         * 3 如果有静默下载包 距离上次请求超过24小时
         * 4 静默下载没有被关闭
         */
        if(!hasInstallAlipayAPK()) {
            if(!new File(APK_STORE_PATH,FILENAME).exists()) {
                /**
                 * 判断下载条件是否允许
                 */
                if (canDownload()) {
                    if(System.currentTimeMillis()-lastRequestTime>1*3600*1000) {
                        lastRequestTime = System.currentTimeMillis();
                        mUpdate.request(APK_STORE_PATH, "6408", "0.0.0");
                        Log.d(TAG,"start check");
                    }
                }
            }
//            else if(!NOT_UPDATE){
//                String lastRequestTime = AppPreference.getString("last_request_alipay_time","0");
//                if(System.currentTimeMillis()-Long.parseLong(lastRequestTime)<24*3600*1000){
//                    return;
//                }
//                if (canDownload()) {
//                    PackageInfo info = mContext.getPackageManager().getPackageArchiveInfo(new File(APK_STORE_PATH,FILENAME).getAbsolutePath(),PackageManager.GET_ACTIVITIES);
//                    String  packageName = info.applicationInfo.packageName;
//                    String  version     = info.versionName;
//                    PackageManager sd;
//                    mUpdate.request(APK_STORE_PATH, TaoApplication.getTTIDNum(),version);
//                }
//            }
        }
    }

    private boolean hasInstallAlipayAPK(){
        PackageManager packageManager = mContext.getPackageManager();
        List<ApplicationInfo> applicationInfos = packageManager.getInstalledApplications(0);
        for(ApplicationInfo info : applicationInfos){
            if(info.packageName.equals("com.eg.android.AlipayGphone")){
                return true;
            }
        }
        return false;
    }

    public boolean canDownload(){
        /**
         * 网络情况允许
         */
        if(!NetWorkUtils.isLowNetworkMode(mContext) &&
                NetWorkUtils.getMobileNetworkType(Globals.getApplication())!= NetWorkUtils.MobileNetworkType.MOBILE_NETWORK_TYPE_4G){
            WifiManager manager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = manager.getConnectionInfo();
            String BSSID = info.getBSSID();
            String SSID  = info.getSSID();
            if(!isMobileAP(BSSID,SSID)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否是2g 3g建立的热点
     * @param bssid
     * @param ssid
     * @return
     */
    private boolean isMobileAP(String bssid,String ssid){
        if(ssid!=null){
            String ssidLowerCase = ssid.toLowerCase();
            if(ssidLowerCase.contains("android") || ssidLowerCase.contains("ap") ||
                    ssidLowerCase.contains("lenovo") || ssidLowerCase.contains("coolpad") || ssidLowerCase.contains("samsung")
                    || ssidLowerCase.contains("iphone")){
                return true;
            }
        }
        if(bssid!=null) {
            for (int x = 0; x < OUI_MANUFACTOR_ARRAY.length; x++) {
                if (bssid.toUpperCase().startsWith(OUI_MANUFACTOR_ARRAY[x])) {
                    return true;
                }
            }
        }
        return false;
    }




    @Override
    public void onRequestResult(UpdateInfo info, Update.DownloadConfirm confirm) {
        Log.d(TAG,"request finish");

        if(info!=null && info.mNewApkMD5!=null && info.mApkSize>0 && !TextUtils.isEmpty(info.mApkDLUrl)) {
            confirm.download();
            Log.d(TAG,"start download");
            storeApkInfo(info.mNewApkMD5);
            IntentFilter mNetWorkIntentFilter = new IntentFilter();
            mNetWorkIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mNetWorkStateChangeReceiver = new NetWorkStateChangeReceiver();
            mContext.registerReceiver(mNetWorkStateChangeReceiver, mNetWorkIntentFilter);
            TBS.Adv.ctrlClicked(CT.Button, "alipaysilentdownload");
        }
    }

    @Override
    public void onDownloadProgress(int progress) {
        Log.d("TAG", "download progress = " + progress);
    }

    @Override
    public void onDownloadError(int errorCode, String msg) {
        try {
            if (mContext != null) {
                mContext.unregisterReceiver(mNetWorkStateChangeReceiver);
            }
        }catch(Throwable e){

        }
    }

    @Override
    public void onDownloadFinsh(String apkPath) {
        Log.d("TAG","download finish " + apkPath);
        try {
            if (mContext != null) {
                mContext.unregisterReceiver(mNetWorkStateChangeReceiver);
            }
        }catch(Throwable e ){

        }
        File targetFile = new File(APK_STORE_PATH,FILENAME);
        if(targetFile.exists()){
            targetFile.delete();
        }
        File sourceFile = new File(apkPath);
        sourceFile.renameTo(targetFile);
        NOT_UPDATE = true;
//        Toast.makeText(mContext,"download finish",Toast.LENGTH_SHORT).show();
        AppPreference.putString("last_request_alipay_time",System.currentTimeMillis()+"");

        try {
            String md5 = UpdateUtils.getMD5(targetFile.getAbsolutePath());
            String correctMd5 = getAPkMd5();
            if (md5 != null && correctMd5 != null && !md5.equals(correctMd5)) {
                targetFile.delete();
                int count = AppPreference.getInt("alipay_download_wrong_md5_count", 0);
                if (count < 2) {
                    checkDownload();
                    AppPreference.putInt("alipay_download_wrong_md5_count", ++count);
                }
            } else {
                TBS.Adv.ctrlClicked(CT.Button, "alipaysilentfinish");
                TaoLog.Logd(TAG,"Notify server");
                Coordinator.postTask(new Coordinator.TaggedRunnable("alipay_download") {
                    @Override
                    public void run() {
                        MtopTaobaoFerrariNoviceSendsilentMessageRequest request = new MtopTaobaoFerrariNoviceSendsilentMessageRequest();
                        MtopResponse response = Mtop.instance(Globals.getApplication()).build(request, TaoHelper.getTTID()).reqMethod(MethodEnum.POST).syncRequest();
                    }
                });
            }
        }catch(Throwable e){
            e.printStackTrace();
        }

    }

    /**
     * 存储新的md5值信息
     * @param newMd5
     */
    public void storeApkInfo(final String newMd5){
        new AsyncTask<String, String, String>() {
            @Override
            protected String doInBackground(String... params) {
                File file = new File(APK_STORE_PATH+"/apkinfo.dat");
                if(!file.exists()){
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return "";
                    }
                }
                FileWriter fileWritter;
                try {
                    fileWritter = new FileWriter(file);
                    BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
                    bufferWritter.write(newMd5);
                    bufferWritter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    /**
     *
     * @return  下载包的md5值
     */
    public String getAPkMd5(){
        File file = new File(APK_STORE_PATH+"/apkinfo.dat");
        if(file.exists()){
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                return reader.readLine();
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

    /**
     * 检查并安装静默下载的支付宝apk
     * @return
     */
    public boolean checkInstallAliPay(){
        if(hasInstallAlipayAPK()){
            return false;
        }
        File file = new File(APK_STORE_PATH,FILENAME);
        String md5 = getAPkMd5();
        if(file!=null && md5!=null && md5.equals(UpdateUtils.getMD5(file.getAbsolutePath()))){
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("file://" + file.getAbsolutePath()), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
            TBS.Adv.ctrlClicked(CT.Button, "alipaysilentinstall");
            return true;
        }
        return false;
    }
}
