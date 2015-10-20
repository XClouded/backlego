package com.taobao.tao.update;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.*;
import android.taobao.atlas.framework.Atlas;
import android.taobao.atlas.wrapper.BaselineInfoManager;
import android.taobao.util.NetWork;
import android.taobao.util.TaoLog;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.alibaba.mtl.appmonitor.AppMonitor;
import com.taobao.lightapk.BundleInfoManager;
import com.taobao.lightapk.LightActivityManager;
import com.taobao.lightapk.dataobject.MtopTaobaoClientGetBundleListResponseData;
import com.taobao.tao.Globals;
import com.taobao.tao.TaoApplication;
import com.taobao.tao.update.business.MtopTaobaoClientAppUpdateTrackRequest;
import com.taobao.tao.update.ui.ForceUpdateNotification;
import com.taobao.tao.update.ui.UpdateNotification;
import com.taobao.tao.util.ConfigReader;
import com.taobao.tao.util.Constants;
import com.taobao.tao.util.StringUtil;
import com.taobao.tao.util.TaoHelper;
import com.taobao.taobaocompat.R;
import com.taobao.update.*;
import com.taobao.update.Downloader.OnDownloaderListener;
import com.taobao.update.Update.DownloadConfirm;
import com.taobao.update.bundle.BundleBaselineInfo;
import com.taobao.update.bundle.BundleDownloader;
import com.taobao.update.bundle.BundleInstaller;
import com.taobao.update.bundle.BundleUpdateInfo;

import android.taobao.apirequest.ApiResponse;
import mtopsdk.mtop.domain.MethodEnum;
import mtopsdk.mtop.intf.Mtop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author luohou.lbb 主客户端动态更新版本检测 为修改原淘宝客户端更新逻辑，参见Update类
 */
public class Updater implements OnUpdateListener{
	
	public static boolean dynamicdeployForTest = false;
	public static final String UPDATER_NOTIFICATION = "update_notification";
	
	public static final String INSTALL_PATH = "install_path";
	
	public static final int INSTALL = 1;
	public static final int CANCEL_DOWNLOAD = 2;
	public static final int FORCE_INSTALL = 3;
	public static final int CANCEL_FORCE_DOWNLOAD = 4;
	
	private static final long CHECK_INTERVAL = 60 * 60 * 1000;//一个小时
	
    // public static final String NOTIFICATION_RECORD = "notification_record";
    // public static final String NOTIFICATION_VERSION = "version";
    // public static final String NOTIFICATION_TIMES = "time";
	
    // public final int MAX_NOTIFICATION_TIMES = 2;
	
	private static Updater sInstance = null;
	private UpdateNotification mNotification; //更新提示
	public int mUpdateMode = 1;// 0:正常模式，1：差量，动态部署尝试模式

	private boolean mBackgroundRequest = true;
	private boolean mBackgroundDownload = false;
	private boolean mForceDownload = false;
	private Application mApp;
	private Context mContext;
	private ForceUpdateNotification mForceNotification;//强制更新提示
	private static boolean sAtlasDDSuccess = false;
	private UpdateReceiver mReceiver;
	
	private Update mUpdate;
	private File mUpdateDir;
	private UpdateInfo mTmpUpdateInfo = null;
	private DownloadConfirm mTmpConfirm = null;
	private BackgroundInstallTask mBackgroundInstall = null;
	
	// 引入bundle下载器
	private BundleDownloader mBundleDownloader = null;
	
	private static long sLastCheckTime = 0;
    public static boolean notifyUserInstallNow = false;
    private static String sApkPath;
    private static UpdateInfo sUpdateInfo;
    public static boolean sPatchInstall = false;
    public static UpdateInfo sForgroundUpdateInfo = null;
    private UpdateLister mUpdateListener;
    private boolean mControlByOutSide = false;


    protected Updater(Application app) {
		mApp = app;
		mContext = app.getApplicationContext();
		mBundleDownloader = new BundleDownloader(mContext);
		mNotification = new UpdateNotification(app);
		mForceNotification = new ForceUpdateNotification(app);
		mUpdate = new Update(new DefaultDownloader(mContext),new RequestImp(),this);
		IntentFilter filter = new IntentFilter();
		filter.addAction(UPDATER_NOTIFICATION);
		mReceiver = new UpdateReceiver();
		mContext.registerReceiver(mReceiver, filter);
		
		File cache = mContext.getExternalCacheDir();
		if(cache == null)
			cache = mContext.getCacheDir();
		mUpdateDir = new File(cache.toString(),"TaoUpdate");
		mUpdateDir.mkdirs();
		
		mUpdate.setApkStorePath(mUpdateDir.getAbsolutePath());
		
	}

	/**
	 * 获取Updater实例
	 * @param context	Context实例
	 * @return	Updater实例
	 */
	public static synchronized Updater getInstance(Context context) {
		if(context == null)
			return null;
		if (sInstance == null) {
			
			if(context instanceof Application){
				sInstance = new Updater((Application)context);
			} else if(context instanceof Activity){
				Application app = ((Activity)context).getApplication();
				sInstance = new Updater(app);
			} else if(context instanceof Service){
				Application app = ((Service)context).getApplication();
				sInstance = new Updater(app);
			} else {
				return null;
			}
		}
		return sInstance;
	}

    /**
     * 开发环境下测试动态部署
     * @param url
     */
    public void triggerDynamicDeployment(String targetVersionName,String url){
        dynamicdeployForTest = true;
        if(targetVersionName == null || !targetVersionName.equals(com.taobao.tao.Globals.getVersionName())){
            Toast.makeText(Globals.getApplication(),"此动态部署不匹配当前的客户端版本",Toast.LENGTH_SHORT).show();
            return;
        }
        if(mTmpConfirm != null)
            mTmpConfirm.cancel();
        if(mBackgroundInstall != null)
            mBackgroundInstall.cancel(true);
        if(mUpdate.requestForTestDynamicDeploy(getApkPath(), TaoApplication.getTTIDNum(), Globals.getVersionName(),url)){
            //启动新的更新
            mBackgroundRequest = true;
            mTmpConfirm = null;
//            mTmpUpdateInfo = null;
            Constants.showToast(Globals.getApplication(),"动态部署开始...(仅内测使用)");
        }else{
            Constants.showToast(Globals.getApplication(),"当前正在更新中");
        }
    }

    
    
    /**
     * 开发环境下测试bundle下载
     * @param url
     */
    public void triggerBundleDownload(String url){
    	DownloadTask dTask = new DownloadTask();  
        dTask.execute(url);  
        
    }
    
    public class DownloadTask extends AsyncTask<String, Void, String>{
    	
    	@Override  
        protected String doInBackground(String... params) { 
    		try {		 
    		URL resonseUrl = new URL(params[0]);
            HttpURLConnection urlConn = (HttpURLConnection) resonseUrl.openConnection();
            urlConn.setConnectTimeout(5 * 1000);
            urlConn.connect();
            if (urlConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int n = 0;
                while (-1 != (n = urlConn.getInputStream().read(buffer))) {
                    output.write(buffer, 0, n);
                }
                byte[] resultData = output.toByteArray();
                String rawStr = new String(resultData,"UTF-8");
                return rawStr;
    	
            }
    		}catch(MalformedURLException e){
                return null;
            }catch(IOException e){
                return null;
            }
            return null;
    	}
    	   	
    	@Override  
        protected void onPostExecute(String result) {  
             MtopTaobaoClientGetBundleListResponseData mockDataList = new MtopTaobaoClientGetBundleListResponseData();
    		 ApiResponse response = new ApiResponse();
//             JSONObject jsObj = response.parseResult(result).data;
             BundleDownloadHelper.parseResponse(response, result, mockDataList);
             
    	    BundleInfoManager.instance().updateBundleDownloadInfo(mockDataList);
    	        Constants.showToast(Globals.getApplication(),"bundle下载更新完成...(仅内测使用)"); 
            super.onPostExecute(result);  
        }  
    }

    public void setUpdateListener(UpdateLister listener){
        mUpdateListener = listener;
    }

    public void setControlByOutSide(boolean flag){
        mControlByOutSide = flag;
    }


    public static long getUsableSpace(File path) {
        if (path == null) {
            return -1;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return path.getUsableSpace();
        } else {
            if (!path.exists()) {
                return 0;
            } else {
                final StatFs stats = new StatFs(path.getPath());
                return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
            }
        }
    }


    /**
	 * 请求更新
	 * @param background	是否为后台请求
	 */
	public void update(boolean background) {
	    if(background && (System.currentTimeMillis() - sLastCheckTime < CHECK_INTERVAL)){
	        TaoLog.Logd("Updater", "no need to update");
	        return;
	    }

        if(getUsableSpace(Environment.getDataDirectory())<50*1024*1024){
            if(mUpdateListener!=null){
                mUpdateListener.onNoUpdate();
                return;
            }
        }

        if(mTmpUpdateInfo!=null && mTmpConfirm!=null && !background && mControlByOutSide && mUpdateListener!=null){
            if((mTmpUpdateInfo.mApkDLUrl == null || mTmpUpdateInfo.mApkDLUrl.length() == 0)
                    && (mTmpUpdateInfo.mPatchDLUrl == null || mTmpUpdateInfo.mPatchDLUrl.length() == 0)){
                mUpdateListener.onNoUpdate();
            }else{
                mUpdateListener.onNeedUpdate();
            }
            return;
        }

        sLastCheckTime = System.currentTimeMillis();
		// 罗喉，去掉所有渠道更新时间延迟判断。
		// int status = getUpdateStatus();
		// TaoLog.Logd(TaoLog.TAOBAO_TAG, "init autoUpdateCheck status "+status);
		// if(UPDATE_UNKNOW==status||UPDATE_DISABLE==status){
		// return ;
		// }
		TaoLog.Logd("Updater", "update:"+background);
		// 如果是联想手机，手动升级提示去联想商店升级
		if (Constants.isLephone()) {
			if (!mBackgroundRequest) {
				Constants.showToast(R.string.updata_lephone_text);
			}
			// 联想手机无自动升级功能??
			return;
		}

		if(mBackgroundDownload){
			//正在进行后台下载，用户点击更新时，重新发起流程
			if(mTmpConfirm != null)
				mTmpConfirm.cancel();
			if(mBackgroundInstall != null)
				mBackgroundInstall.cancel(true);
		}
		String version = "";
		if(sAtlasDDSuccess){
			//FIXME: PackageInfo packageInfo = AtlasDD.getInstance().getLastestApkInfo();
		    PackageInfo packageInfo = null;
			if(packageInfo != null)
				version = packageInfo.versionName;
		}else{
			version = Globals.getVersionName();
		}
		//TODO:主客更新请求及bundle更新请求版本，需要服务端处理！
		if(mUpdate.request(getApkPath(), TaoApplication.getTTIDNum(), version)){
			//启动新的更新
			mBackgroundRequest = background;
			mTmpConfirm = null;
//			mTmpUpdateInfo = null;
		}else{
			//更新已经启动
			if(!mBackgroundRequest){
				//上次进行的是用于启动更新，则提示用户稍等
				Constants.showToast(R.string.notice_update_checking);
			}
			if(!background){

				//已经在更新，用户点击更新可以改变更新状态
				mBackgroundRequest = background;

			}
		}
	}
	
	
	public void isHaveNewVersion(){
		String version = TaoApplication.getVersion();
		String apkPath=getApkPath();
		
	}

	/**
	 * 是否进行了动态部署
	 * @return	true 进过了动态部署	false 未进行动态部署
	 */
	public static boolean isAtlasDDSuccess(){
		return sAtlasDDSuccess;
	}

	@Override
	public void onRequestResult(UpdateInfo info,
			DownloadConfirm confirm) {
        sPatchInstall = false;
		if(info == null){
			//无更新
			TaoLog.Logd("Updater", "no update");
			//更新结束   销毁
			release();
			if(mBackgroundRequest)
				//后台更新不提示
				return;
			else{
				//提示无更新错误
				if(info == null){
					Constants.showToast(Globals.getApplication().getString(R.string.update_no_network));
				}else if(info.mErrCode == 0){
					Constants.showToast(Globals.getApplication().getString(R.string.notice_noupdate));
				}else{
					Constants.showToast(Globals.getApplication().getString(R.string.notice_errorupdate));
				}
                if(mUpdateListener!=null && mControlByOutSide){
                    mUpdateListener.onNoUpdate();
                }

                return;
			}
		}else{
		    if(info.getBundleBaselineInfo() !=null && mBackgroundRequest && !BundleInstaller.isInstallSuccess()){
                AppMonitor.Counter.commit("dynamicDeploy", "updateReceived",info.getBundleBaselineInfo().getmBaselineVersion(), 1);
		        buildBundleBaselineInfo(info);
		        File bundleUpdatePath = new File(mContext.getFilesDir().toString(), File.separator +"bundleupdate"+File.separator);
		        if(!bundleUpdatePath.exists()){
		            bundleUpdatePath.mkdir();
		        }
		        mBundleDownloader.download(info, bundleUpdatePath.getAbsolutePath(), info.getBundleBaselineInfo().getAllSize());
		        return ;
		    }
            if(!TextUtils.isEmpty(info.rollback)){
                BaselineInfoManager.instance().rollback();
                return;
            }
		    if((info.mApkDLUrl == null || info.mApkDLUrl.length() == 0)
                    && (info.mPatchDLUrl == null || info.mPatchDLUrl.length() == 0)){
                if(mUpdateListener!=null && mControlByOutSide){
                    mUpdateListener.onNoUpdate();
                }
                //无更新
	            TaoLog.Logd("Updater", "no update");
	            //更新结束   销毁
	            release();
                ApkUpdateMonitor.count(ApkUpdateMonitor.NO_UPDATE, null);
	            return ;
		    }
			//存在更新
            ApkUpdateMonitor.count(ApkUpdateMonitor.HAS_UPDATE, null);
			TaoLog.Logd("Updater", "update priority: "+info.mPriority);
			mTmpUpdateInfo = info;
			mTmpConfirm = confirm;
			
			/*
			 * 静默更新策略交由服务端完全控制
			 * 
			 * 静默更新A：只WIFI下静默更新，在非WIFI下无更新；（这个非常适用于灰度发布，不再用担心一个版本多次灰度对用户的骚扰；当然也可以用于正式发布需要短时间达到较高的更新率）
			 * 静默更新B：在WIFI下静默更新，在非WIFI下提示更新；（主客户端控制的目前的策略，请保留）
			 * 静默更新C：不区分网络，全静默更新。（此功能会偷偷吃用户流量，但一般不开启，留作应急用，比如客户端出现重大bug等）
			 * 
			 */
			if(info.mPriority == 2 && mBackgroundRequest){// mPriority＝2，静默更新，交由服务端完全控制更新策略，正常状态返回 0 或者 1，或者默认值 -1
				mBackgroundDownload = true;
				confirm.download();
//
//			}else if(NetWork.CONN_TYPE_WIFI.equals(NetWork.getNetConnType(Globals.getApplication())) && mBackgroundRequest){
//				//wifi网络，先下载更新，再提示用户
//				mBackgroundDownload = true;
//				if(info.mPriority == 1)
//					mForceDownload = false;
//				confirm.download();
//
			}else {
				//提示用户更新
                if(!mControlByOutSide) {
                    notifyUserUpdate(info, confirm,false);
                }else{
                    if(mUpdateListener!=null){
                        mUpdateListener.onNeedUpdate();
                    }
                }

            }
		}
	}


    public void showDownloadTip(){
        if(mTmpConfirm!=null && mTmpUpdateInfo!=null){
            notifyUserUpdate(mTmpUpdateInfo, mTmpConfirm,true);
        }
    }


    private void notifyUserUpdate(UpdateInfo info,
			DownloadConfirm confirm,boolean force){
		//有更新
		final Activity currentActivity = getCurrentActivity();
		if (currentActivity == null) {
			//无activity 则结束更新
			confirm.cancel();
			//更新结束   销毁
			release();
			return;
		}
		
		//文件包大小
		String apkSize;
		if(info.mPatchSize != 0)
			apkSize = size(info.mPatchSize);
		else
			apkSize = size(info.mApkSize);
		
		
		if(info.mPriority == 1){
			//强制更新
			mBackgroundDownload = false;
			mForceDownload = true;
			mForceNotification.popUpForceUpdateDlg(info,confirm,apkSize);
		}else{

            int notifyTimes = NotificationRecordStorage.get(info.mVersion);
            // 服务器设置的提醒次数大于0，提示次数有剩余 而且是后台更新
            if ((info.mRemindNum <= 0 || notifyTimes >= info.mRemindNum) && mBackgroundRequest && !force) {
				confirm.cancel();
				//更新结束   销毁
				release();
				return;
			}
			//普通更新
			mBackgroundDownload = false;
			mForceDownload = false;
			
            mNotification.popUpUpdateDlg(info, confirm, apkSize);
            if (mBackgroundRequest) {
                NotificationRecordStorage.update(info.mVersion, info.mRemindNum);
            }
		}
	}
	
	@Override
	public void onDownloadProgress(int progress) {
		TaoLog.Logd("Updater", "update DownloadProgress: "+progress);
		//后台下载则不提示
		if(!mBackgroundDownload){
			if(mForceDownload){
				//强制更新dialog
				mForceNotification.updateDlProgress(progress);
			}else{
				//更新notification进度
				mNotification.updateDlProgress(progress);
			}
		}
	}

	
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
	public void onDownloadFinsh(String apkPath) {
		TaoLog.Logd("Updater", "update DownloadFinsh: "+apkPath);
		UpdateUserTrack.mTaoUpdateTrack("Updater","主客下载成功"+apkPath);
		if(mBackgroundDownload){
			//wifi 网络，后台下载成功
			if(mTmpUpdateInfo.mPriority == 2){
				//后台动态部署
				mBackgroundInstall = new BackgroundInstallTask(apkPath);
				if(Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB)
					mBackgroundInstall.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				else
					mBackgroundInstall.execute();
			}else {
                if(getCurrentActivity()!=null && getCurrentActivity().getClass().getName().contains("MainActivity3")) {
                    notifyUserInstall(apkPath, mTmpUpdateInfo);
                }else{
                    sApkPath = apkPath;
                    sUpdateInfo = mTmpUpdateInfo;
                    notifyUserInstallNow = true;
                }
            }
			
		}else{
			//移动网络   提示用户后下载
			if(mForceDownload){
				//强制更新dialog
				mForceNotification.finished(apkPath,mTmpUpdateInfo.mNotifyTip);
			}else{
				//更新notification
				mNotification.finished(apkPath);
			}
			
		}
	}

	//提示用户安装
	private void notifyUserInstall(String apkPath,UpdateInfo updateInfo){
		if (updateInfo.mPriority == 1){
			//强制更新dialog
			mForceNotification.finished(apkPath,updateInfo.mNotifyTip);
		}else{
			//普通更新dialog
			final Activity currentActivity = getCurrentActivity();
			if (currentActivity == null) {
				return;
			}
            int notifyTimes = NotificationRecordStorage.get(updateInfo.mVersion);
            // 服务器设置的提醒次数大于0，提示次数有剩余 而且是后台更新
            if ((updateInfo.mRemindNum <= 0 || notifyTimes >= updateInfo.mRemindNum) && mBackgroundRequest) {
				//更新结束   销毁
				release();
				return;
			}
            mNotification.popUpInstallDlg(apkPath, updateInfo.mNotifyTip);
            if (mBackgroundRequest) {
                NotificationRecordStorage.update(updateInfo.mVersion, updateInfo.mRemindNum);
            }
		}
	}
		
	class BackgroundInstallTask extends AsyncTask<Void,Void,Boolean>{
		private String mApkPath;
		public BackgroundInstallTask(String apkPath){
			mApkPath = apkPath;
		}
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if(!result){
				TaoLog.Logd("Updater", "background atlas install failed");
				UpdateUserTrack.mTaoUpdateTrack("Updater","主客安装失败  "+mTmpUpdateInfo.mVersion+mTmpUpdateInfo.mApkDLUrl);
				notifyUserInstall(mApkPath,mTmpUpdateInfo);
			}
			UpdateUserTrack.mTaoUpdateTrack("Updater","主客安装成功  "+mTmpUpdateInfo.mVersion+mTmpUpdateInfo.mApkDLUrl);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			TaoLog.Logd("Updater", "update install background");
			String version = "";
			try {
				version = mContext.getPackageManager().getPackageInfo(TaoHelper.getPackageName(), 0).versionName;
			} catch (NameNotFoundException e1) {
				e1.printStackTrace();
			}
			// 获取从网络来的配置
			boolean bAtlasDDModeConfigFromNet = version.compareTo(ConfigReader.readConfig(Globals.getApplication()).ATLAS_DD_MODE)>0;
			if(!bAtlasDDModeConfigFromNet)
				//已配置降级
				return false;
			boolean atlasInstall = false;
//FIXME:
//			try {
//				AtlasDD.getInstance().init(mContext);
//				atlasInstall = AtlasDD.getInstance().swallow(mApkPath);
//			} catch (AssertionArrayException e) {
//				e.printStackTrace();
//			} catch (ZipException e) {
//				e.printStackTrace();
//				TBS.Ext.commitEvent("Atlas", Constants.EventID_ATLAS_DD_INSTALLERR,e.getMessage());
//			} catch (SecurityException e) {
//				TBS.Ext.commitEvent("Atlas", Constants.EventID_ATLAS_DD_INSTALLERR,e.getMessage());
//				e.printStackTrace();
//			} catch (IOException e) {
//				TBS.Ext.commitEvent("Atlas", Constants.EventID_ATLAS_DD_INSTALLERR,e.getMessage());
//				e.printStackTrace();
//			} catch (IllegalFileException e) {
//				TBS.Ext.commitEvent("Atlas", Constants.EventID_ATLAS_DD_INSTALLERR,e.getMessage());
//				e.printStackTrace();
//			}
			if (atlasInstall) {
				TaoLog.Logd("Updater", "background atlas install success");
				if(getCurrentActivity() == null)
					//无activity则退出重启
					exitApp();
				else
					//记录动态部署成功
					sAtlasDDSuccess = true;
				//更新结束   销毁
				release();
			}
			
			return atlasInstall;
		}
		
	}
	
	@Override
	public void onDownloadError(int errorCode, String msg) {
        ApkUpdateMonitor.count(ApkUpdateMonitor.DOWNLOAD_ERROR,errorCode + ":" + msg);
		TaoLog.Logd("Updater", "update DownloadError: " + msg);
		UpdateUserTrack.mTaoUpdateTrack("Updater","主客下载失败  "+msg+" APK INFO: "+getErrorApkInfoForCDN());
		switch(errorCode){
		case DefaultDownloader.ERROR_IO:
			msg = mContext.getResources().getString(R.string.notice_update_err_io);
			break;
		case DefaultDownloader.ERROR_NETWORK:
			msg = mContext.getResources().getString(R.string.notice_update_err_network);
			break;
		case DefaultDownloader.ERROR_NO_NETWORK:
			msg = mContext.getResources().getString(R.string.notice_update_err_nonetwork);
			break;
		case DefaultDownloader.ERROR_URL:
			msg = mContext.getResources().getString(R.string.notice_update_err_url);
			break;
		case OnUpdateListener.MD5_VERIFY_FAILED:
			msg = mContext.getResources().getString(R.string.notice_update_err_md5);
			// fixed bug by leinuo md5失败无法重新下载安装
            clearUpdatePath(mUpdateDir);
            if(mUpdate.canRetry()) {
                 mUpdate.retry();
                return;
            }
			break;
		case OnDownloaderListener.ERROR_NOT_ENOUGH_SPACE:
			msg = mContext.getResources().getString(R.string.notice_undercapacity);
			break;
			
		}
		if(mBackgroundDownload){
			//后台下载不提示错误，但进入重新下载的流程
			if(mTmpUpdateInfo.mPriority == 2 && errorCode != OnDownloaderListener.ERROR_NOT_ENOUGH_SPACE){
				//后台更新，并且非磁盘空间不足，则不提示错误
				release();
				return;
			}
			//wifi 网络，后台下载
			notifyUserUpdate(mTmpUpdateInfo,mTmpConfirm,false);
		}else{
			//前台下载则直接提示出错，结束
			if(mForceDownload){
				//强制更新dialog
				mForceNotification.err(msg);
			}else{
				//下载失败错误提示
				mNotification.err(msg);
			}
			//更新结束   销毁
			release();
		}
		
	}
	
	class InstallTask extends AsyncTask<Void,Void,Boolean>{
		@Override
		protected void onPostExecute(Boolean result) {
			if(result){
				Toast toast = Toast.makeText(mContext,R.string.atlasdd_deploy_sucess_tip, Toast.LENGTH_LONG);
				toast.setDuration(4000);
				toast.setGravity(Gravity.CENTER, 0, 0);
				toast.show();
			}
			super.onPostExecute(result);
		}

		private String mApkPath;
		
		public InstallTask(String apkPath){
			mApkPath = apkPath;
		}
		
		@Override
		protected Boolean doInBackground(Void... params) {
			boolean atlasInstall = false;
//FIXME:			
//			try {
//				String version = "";
//				try {
//					version = mContext.getPackageManager().getPackageInfo("com.taobao.taobao", 0).versionName;
//				} catch (NameNotFoundException e) {
//					e.printStackTrace();
//				}
//				// 获取从网络来的配置
//				boolean bAtlasDDModeConfigFromNet = version.compareTo(ConfigReader.readConfig(Globals.getApplication()).ATLAS_DD_MODE)>0;
//				if(bAtlasDDModeConfigFromNet){
//					//是否已配置降级
//					AtlasDD.getInstance().init(mContext);
//					atlasInstall = AtlasDD.getInstance().swallow(mApkPath);
//				}
//			} catch (AssertionArrayException e) {
//				e.printStackTrace();
//			} catch (ZipException e) {
//				e.printStackTrace();
//				TBS.Ext.commitEvent("Atlas", Constants.EventID_ATLAS_DD_INSTALLERR,e.getMessage());
//			} catch (SecurityException e) {
//				TBS.Ext.commitEvent("Atlas", Constants.EventID_ATLAS_DD_INSTALLERR,e.getMessage());
//				e.printStackTrace();
//			} catch (IOException e) {
//				TBS.Ext.commitEvent("Atlas", Constants.EventID_ATLAS_DD_INSTALLERR,e.getMessage());
//				e.printStackTrace();
//			} catch (IllegalFileException e) {
//				TBS.Ext.commitEvent("Atlas", Constants.EventID_ATLAS_DD_INSTALLERR,e.getMessage());
//				e.printStackTrace();
//			}
			//删除旧apk
			String[] apks = mUpdateDir.list();
			if(apks != null){
				for(String apk:apks){
					
					if(!mApkPath.endsWith(apk)){
//						TaoLog.Logd("Updater", "delete apk"+apk);
						new File(mUpdateDir,apk).delete();
//						if(deleteRet)
//							TaoLog.Logd("Updater", "delete apk success"+apk);
					}
				}
			}
			if (atlasInstall) {
				TaoLog.Logd("Updater", "atlas install success");
				if(getCurrentActivity() == null)
					//无activity则退出重启
					exitApp();
				else{
					sAtlasDDSuccess = true;
				}
			}else{
                if(mTmpUpdateInfo!=null)
                    Updater.logUpdateState(mTmpUpdateInfo.mVersion,Updater.STEP_INSTALL,sPatchInstall ? MODE_PATCH:MODE_ENTIRE);
				TaoLog.Logd("Updater", "system install ");
				Intent installIntent = new Intent(Intent.ACTION_VIEW);
				installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				installIntent.setDataAndType(Uri.fromFile(new File(mApkPath)),
						"application/vnd.android.package-archive");
				mContext.startActivity(installIntent);
			}
			return atlasInstall;
		}

		
		
	}
	/**
	 * 
	 * @author bokui
	 *
	 */
	class UpdateReceiver extends BroadcastReceiver{

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
		public void onReceive(Context context, Intent intent) {
			int operation = intent.getIntExtra(UPDATER_NOTIFICATION, 0);
			switch(operation){
				case CANCEL_DOWNLOAD:{
					//停止下载
					TaoLog.Logd("Updater", "cancel download");
					mUpdate.cancelDownload();
					mNotification.remove();
					//更新结束   销毁
					release();
                    ApkUpdateMonitor.count(ApkUpdateMonitor.CANCEL_DOWNLOAD, null);
					break;
				}
				case CANCEL_FORCE_DOWNLOAD:{
					TaoLog.Logd("Updater", "force update cancel download");
					mUpdate.cancelDownload();
					//更新结束   销毁
					release();
                    ApkUpdateMonitor.count(ApkUpdateMonitor.CANCEL_DOWNLOAD, null);
					break;
				}
				case INSTALL:{
					TaoLog.Logd("Updater", "update install");
					
					String apkFile = intent.getStringExtra(INSTALL_PATH);
					//安装
					if(Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB)
						new InstallTask(apkFile).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					else
						new InstallTask(apkFile).execute();
					mNotification.remove();
					//更新结束   销毁
					release();
                    ApkUpdateMonitor.count(ApkUpdateMonitor.SUCCESS, null);
					break;
				}
				case FORCE_INSTALL:{
					//强制更新安装
					TaoLog.Logd("Updater", "force update install");
                    if(mTmpUpdateInfo!=null)
                        Updater.logUpdateState(mTmpUpdateInfo.mVersion,Updater.STEP_INSTALL,sPatchInstall ? MODE_PATCH:MODE_ENTIRE);
                    String apkFile = intent.getStringExtra(INSTALL_PATH);
					Intent installIntent = new Intent(Intent.ACTION_VIEW);
					installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					installIntent.setDataAndType(Uri.fromFile(new File(apkFile)),
							"application/vnd.android.package-archive");
					mContext.startActivity(installIntent);
					//更新结束   销毁
					//安装过程中退出app，防止取消安装还能回来正常使用
                    ApkUpdateMonitor.count(ApkUpdateMonitor.SUCCESS_FORCE, null);
					release();
                    exitApp();
					break;
				}
				
				
			}
		}
	}
	
	/*
	 * 获取运行中的apk文件路径
	 */
	private String getApkPath(){
		// 安装版模式运行
		String dir = null;
		try {
			dir = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(), 0).publicSourceDir;
		}
		catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return dir;
	}

	private static WeakReference<Activity> wActivity = null;
	public static void updateCurrentActivity(Activity a){
		
		if(a == null)
			wActivity = null;
		else
			wActivity = new WeakReference<Activity>(a);

        if(notifyUserInstallNow && a.getClass().getName().contains("MainActivity3") && sInstance!=null){
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(sInstance!=null && notifyUserInstallNow && getCurrentActivity()!=null && getCurrentActivity().getClass().getName().contains("MainActivity3")){
                        notifyUserInstallNow = false;
                        sInstance.notifyUserInstall(sApkPath, sUpdateInfo);
                    }
                }
            },1000);
        }
	}
	
    public static Activity getCurrentActivity() {

    	if(wActivity == null)
    		return null;
    	else {
    		return wActivity.get();
    	}    	
	}

	private String size(long size) {

        if (size / (1024 * 1024) > 0) {
            float tmpSize = (float) (size) / (float) (1024 * 1024);
            DecimalFormat df = new DecimalFormat("#.##");
            return "" + df.format(tmpSize) + "MB";
        } else if (size / 1024 > 0) {
            return "" + (size / (1024)) + "KB";
        } else
            return "" + size + "B";
    }
	
	/**
	 * 销毁自己
	 */
	protected void release(){
//		if(mContext != null && mReceiver != null){
//			mContext.unregisterReceiver(mReceiver);
//			mReceiver = null;
//		}
//		sInstance = null;
	}

	//统一退出app的逻辑，process.killprocess 会导致
	// 1. Scheduling restart of crashed service
	// 2. 设置页面短暂黑屏后又恢复到设置页面
	public static void exitApp(){

		try{
			LightActivityManager.finishAll();
		}catch (Throwable e){

		}

		int pid = android.os.Process.myPid();
		TaoLog.Logd("Updater", "atlas killprocess:"+pid);
		android.os.Process.killProcess(pid);
	}
	
	/**
     * 构建本次更新bundle的基线信息详情
     * @param updateInfo
     */
    private void buildBundleBaselineInfo(UpdateInfo updateInfo){
        if(updateInfo == null || updateInfo.getBundleBaselineInfo()==null || updateInfo.getBundleBaselineInfo().getBundleUpdateList()==null){
            return;
        }
        List<BundleUpdateInfo> bundleUpdateList = updateInfo.getBundleBaselineInfo().getBundleUpdateList();
        BundleBaselineInfo bbInfo = updateInfo.getBundleBaselineInfo();
        if(bbInfo == null){
            bbInfo = new BundleBaselineInfo();
        }
        Map<String,String> urlMap = new HashMap<String,String>();
        Map<String,Long> packagesizeMap = new HashMap<String,Long>();
        Map<String,Long> patchsizeMap = new HashMap<String,Long>();
        Map<String,String> packageMD5Map = new HashMap<String,String>();
        Map<String,String> patchMD5Map = new HashMap<String,String>();
        Map<String,String> packageURLWithpatchMap = new HashMap<String,String>();
        Map<String,String> packageMD5WithpatchMap = new HashMap<String,String>();

        for(BundleUpdateInfo info:bundleUpdateList){
            String bundleName = info.mBundleName;
            if(!Atlas.getInstance().isBundleNeedUpdate(bundleName,info.mVersion)){
                continue;
            }
            if(!TextUtils.isEmpty(info.mPatchDLUrl) && info.mPatchSize>0){
                File atlasBundle = Atlas.getInstance().getBundleFile(bundleName);
                String lbMD5 = "";
                if(atlasBundle!=null && atlasBundle.exists()){
                    lbMD5 = UpdateUtils.getMD5(atlasBundle.getAbsolutePath());
                }
                if(lbMD5.equals(info.mlocalBundleMD5)){
                    urlMap.put(bundleName, info.mPatchDLUrl);
                    patchsizeMap.put(bundleName, Long.valueOf(info.mPatchSize));
                    patchMD5Map.put(bundleName,info.mlocalBundleMD5);
                    packageURLWithpatchMap.put(bundleName,info.mBundleDLUrl);
                    packageMD5WithpatchMap.put(bundleName, info.mNewBundleMD5);
                    bbInfo.setmPatchUrlMap(urlMap);
                    bbInfo.setmPatchSize(patchsizeMap);
                    bbInfo.setPatchMD5(patchMD5Map);
                    bbInfo.setPackageURLWithPatch(packageURLWithpatchMap);
                    bbInfo.setPackageMD5WithPatch(packageMD5WithpatchMap);
                    bbInfo.setPatchMD5(patchMD5Map);
                }else{
                    //执行全量下载
                    if(info.mBundleSize > 0 && !StringUtil.isEmpty(info.mBundleDLUrl)){
                        urlMap.put(bundleName, info.mBundleDLUrl);
                        packagesizeMap.put(bundleName, Long.valueOf(info.mBundleSize));
                        packageMD5Map.put(bundleName, info.mNewBundleMD5);
                        bbInfo.setmPackageUrlMap(urlMap);
                        bbInfo.setmPackageSize(packagesizeMap);
                        bbInfo.setPackageMD5(packageMD5Map);
                    }
                }
            }else{
                //执行全量下载
                if(info.mBundleSize > 0 && !StringUtil.isEmpty(info.mBundleDLUrl)){
                    urlMap.put(bundleName, info.mBundleDLUrl);
                    packagesizeMap.put(bundleName, Long.valueOf(info.mBundleSize));
                    packageMD5Map.put(bundleName, info.mNewBundleMD5);
                    bbInfo.setmPackageUrlMap(urlMap);
                    bbInfo.setmPackageSize(packagesizeMap);
                    bbInfo.setPackageMD5(packageMD5Map);
                }
            }
        }
        bbInfo.setAllDownloadUrl(null);
        bbInfo.setAllPackageSize(null);
        bbInfo.setAllPatchSize(null);
        bbInfo.setAllSize(null);
        updateInfo.setBundleBaselineInfo(bbInfo);
    }
    
    /**
     * 获取下载出错时文件大小及MD5信息（CDN跟踪排查使用）
     * @return
     */
    private String getErrorApkInfoForCDN(){
        if(!mUpdateDir.exists()){
            return "apk not exit!";
        }
        URL url = null;
        try {
            url = new URL(mTmpUpdateInfo.mApkDLUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "MalFormedURLException "+ url;
        }
        String fileName = new File(url.getFile()).getName();
        File apk = new File(mUpdateDir, fileName);
        if(!apk.exists()){
            return "apk not exit!";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(" URL-->"+mTmpUpdateInfo.mApkDLUrl);
        sb.append(" MD5 From Server-->"+mTmpUpdateInfo.mNewApkMD5);
        sb.append(" MD5 From File-->"+UpdateUtils.getMD5(apk.getAbsolutePath()));
        sb.append(" Size From Server-->"+mTmpUpdateInfo.mApkSize);
        sb.append(" Size From File-->"+apk.length());
        Log.d("CDN BUG TRACEINFO:", sb.toString());
        return sb.toString();
    }
    /**
     * 删除更新目录内的文件及目录
     * @param updatePath
     */
    private void clearUpdatePath(File updatePath){
        if(updatePath.exists()){
            File[] updateFiles = updatePath.listFiles();
            for(File file:updateFiles){
                if(file.isDirectory()){
                    clearUpdatePath(file);
                }
                file.delete();
            }
        }
    }

    public static int MODE_ENTIRE = 1;
    public static int MODE_PATCH   = 2;

    public static int STEP_REQUEST = 1;
    public static int STEP_DOWNLOAD= 3;
    public static int STEP_INSTALL = 5;
    public static int STEP_FINISH  = 7;

    public static void logUpdateState(String newVersion,int updateStep, int updateMode){
        try {
            MtopTaobaoClientAppUpdateTrackRequest request = new MtopTaobaoClientAppUpdateTrackRequest();
            request.setAppGroup("taobao4android");
            request.setCurVersion(TaoApplication.getVersion());
            request.setNewVersion(newVersion);
            request.setUpdateStep(updateStep);
            request.setUpdateModel(updateMode);
            Mtop.instance(Globals.getApplication()).build(request, TaoHelper.getTTID()).reqMethod(MethodEnum.POST).asyncRequest();
        }catch(Throwable e){
            e.printStackTrace();
        }
    }
}
