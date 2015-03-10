package com.taobao.update;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StatFs;
import android.taobao.apirequest.ApiResponse;
import android.taobao.util.TaoLog;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.taobao.bspatch.BSPatch;
import com.taobao.tao.Globals;
import com.taobao.tao.update.DDUpdateConnectorHelper;
import com.taobao.update.Downloader.OnDownloaderListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 更新模块，用户客户端的更新下载功能<br>
 * 模块采用差量更新算法，用于减少用户流量。默认差量更新算法使用bsdiff。<br>
 * <br>
 * 差量更新逻辑如下：<br>
 * 1、服务端将计算低版本与最新版本的差量包，用于客户端下载<br>
 * 2、客户端请求服务端，上传本地低版本包的md5值，以提供服务端做容错校验。
 * 当服务端低版本md5值与客户端不同时，将关闭差量更新功能，只提供全量apk下载链接。<br>
 * 3、当服务端返回时，提供最新版本的md5值，以提供客户端做最后容错校验。
 * 当客户端下载到差量包合并完成最新包时，拿本地md5与服务端返回md5比较，以完成最新版本的确认。<br>
 * <br>
 * 以下情况服务端只提供全量下载链接：<br>
 * 1、服务端未存储当前客户端版本所对应的差量包。<br>
 * 2、客户端版本与服务端对应版本的md5值不同。<br>
 * <br>
 * 使用：<br>
 * 1、用户只需获取update实例。<br>
 * 2、通过request启动更新。<br>
 * 3、等待OnUpdateListener通知完成后续UI交互。<br>
 * <br>
 * 线程安全：<br>
 * 为了性能考虑，未提供线程安全保护。
 * 为了简化使用，Update内部完全封装了网络操作的异步性，所以使用过程中只需在主线程调用所有接口即可。<br>
 * @author bokui
 *
 */
public class Update{
	
	private Downloader mDownloader;
	private UpdateRequest mRequest;
	private OnUpdateListener mListener;
	private boolean mIsRunning = false;
	private String mApkStorePath;
	private String mOldApkPath;
	private UpdateRequestTask mUpdateRequestTask;
	
	private DownloadConfirm mTmpDownloadConfirm;
	private final int FOR_ENOUGH_SPACE = 2*1024*1024;
    private String mDynamicDeployTestUrl = null;
	
	/**
	 * 更新初始化
	 * @param downloader	文件下载器
	 * @param request		更新请求
	 * @param listener		时间监听
	 * @param groupId		更新包所属组
	 */
	public Update(Downloader downloader, UpdateRequest request, OnUpdateListener listener){
		mDownloader = downloader;
		mRequest = request;
		mListener = listener;
	}
	
	
	
	/**
	 * 发起请求，请求结果将通过OnUpdateListener通知。<br>
	 * 为了屏蔽重复调用，当一个更新请求流程未完成时，再次调用该方法将忽略。更新请求流程包括更新api及下载流程。<br>
	 * 注意：<br>
	 * 1.当未存在更新时，更新请求流程不包含下载环节。<br>
	 * 2.当存在更新时，必须调用DownloadConfirm的download或cancel接口完成更新流程。未调用则更新请求流程将无法结束，无法发起新的更新请求。<br>
	 * <br>
	 * 更新api请求完成后，将在OnUpdateListener的onRequestResult接口返回下载器——DownloadConfirm。
	 * 通过DownloadConfirm接口可以发起下载或取消下载<br>
	 * <br>
	 * 在未发起新的更新请求前，DownloadConfirm一直有效，当下载出现网络错误时，可以再次调用download方法重新启动下载。<br>
	 * 发起新的更新请求后，上一个DownloadConfirm将失效并cancel下载。<br>
	 * 
	 * @param apkPath		当前运行apk文件路径，用户差量包合并
	 * @param appName		请求更新的app name
	 * @param version		版本信息
	 * 
	 * @return false 上次更新未完成，true 启动更新成功
	 */
	public boolean  request(String apkPath, String appName,String version){
		if(mIsRunning)
			return false;
		mIsRunning = true;
		//失效上一个下载器
		if(mTmpDownloadConfirm != null)
			mTmpDownloadConfirm.disable();
		
		mOldApkPath = apkPath;
		//发起新的请求
		mUpdateRequestTask = new UpdateRequestTask(apkPath,appName,version);
		if(Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB)
			mUpdateRequestTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		else
			mUpdateRequestTask.execute();
		return true;
//		if(mRequest != null) {
//			mRequest.request(appName, version, UpdateUtils.getMD5(apkPath));
//			return true;
//		} else {
//			return false;
//		}
		
	}

    public boolean requestForTestDynamicDeploy(String apkPath,String appName,String version,String url){
        if(mIsRunning) {
            Toast.makeText(Globals.getApplication(),"正在更新中...,动态部署测试终止",Toast.LENGTH_SHORT).show();
            return false;
        }
        mIsRunning = true;
        //失效上一个下载器
        if(mTmpDownloadConfirm != null)
            mTmpDownloadConfirm.disable();

        mOldApkPath = apkPath;
        mDynamicDeployTestUrl = url;
        //发起新的请求
        mUpdateRequestTask = new UpdateRequestTask(apkPath,appName,version);
        mUpdateRequestTask.setTestForDynamicDeploy(true);
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB)
            mUpdateRequestTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else
            mUpdateRequestTask.execute();
        return true;
    }
	
	/**
	 * 修改下载保存目录
	 * @param apkStorePath	apk保存路径
	 */
	public void setApkStorePath(String apkStorePath){
		if(apkStorePath != null)
			mApkStorePath = apkStorePath;
	}
	
	
	/**
	 * 更新任务
	 * @author bokui
	 *
	 */
	
	class UpdateRequestTask extends AsyncTask<Void,Void,UpdateInfo>{

		private String mApkPath;
		private String mAppName;
		private String mVersion;
        private boolean mTestForDynamicDeploy = false;
		
		public UpdateRequestTask(String apkPath,String appName,String version){
			mApkPath = apkPath;
			mAppName = appName;
			mVersion = version;
		}

        public void setTestForDynamicDeploy(boolean flag){
            mTestForDynamicDeploy = flag;
        }
		
		@Override
		protected UpdateInfo doInBackground(Void... params) {
		    if(!mTestForDynamicDeploy) {
                if (mRequest == null)
                    return null;
                //请求更新
                String appMd5 = "";
                if (mApkPath != null)
                    appMd5 = UpdateUtils.getMD5(mApkPath);
                Log.d("Update", "start request");
                return mRequest.request(mAppName, mVersion, appMd5);
            }else{
                try {
                    URL url = new URL(mDynamicDeployTestUrl);
                    HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
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
                        ApiResponse response = new ApiResponse();
                        UpdateInfo updateInfo = new UpdateInfo();
                        DDUpdateConnectorHelper.parseResponse(response,rawStr,updateInfo);
                        return updateInfo;
                    }
                }catch(MalformedURLException e){
                    Log.d("DynamicDeploy",e.getMessage());
                    return null;
                }catch(IOException e){
                    Log.d("DynamicDeploy",e.getMessage());
                    return null;
                }
                return null;
            }
		}

		@Override
		protected void onPostExecute(UpdateInfo result) {
			super.onPostExecute(result);
			//不存在更新，则本次更新结束
			if(result == null || 
					((result.mApkDLUrl == null || result.mApkDLUrl.length() == 0)
					&& (result.mPatchDLUrl == null || result.mPatchDLUrl.length() == 0))){
				mIsRunning = false;
				Log.d("Update", "no update");
				//创建无效的下载器
				mTmpDownloadConfirm = new DownloadConfirm(null,mAppName);
			}else{
				Log.d("Update", "has update");
				//创建正常的下载器
				mTmpDownloadConfirm = new DownloadConfirm(result,mAppName);
			}
			//通知
			if(mListener != null)
				mListener.onRequestResult(result, mTmpDownloadConfirm);
			
		}
	}

	/*
	class MyMtopListener implements IRemoteBaseListener {
		
		private String mAppName;
		
		public MyMtopListener(String appName){
			mAppName = appName;
		}
		
		@Override
		public void onError(int arg0, MtopResponse arg1, Object arg2) {
			// TODO Auto-generated method stub
			UpdateInfo result = (UpdateInfo) DDUpdateConnectorHelper.syncPaser(arg1.getBytedata());
			mTmpDownloadConfirm = new DownloadConfirm(null,mAppName);
			//通知
			if(mListener != null)
				mListener.onRequestResult(result, mTmpDownloadConfirm);
			Log.i("TaosdkToMtop", "com.taobao.update.Update : request --> onError : " + result.toString());
		}

		@Override
		public void onSuccess(int arg0, MtopResponse arg1, BaseOutDo arg2,
				Object arg3) {
			// TODO Auto-generated method stub
			UpdateInfo result = (UpdateInfo) DDUpdateConnectorHelper.syncPaser(arg1.getBytedata());
			if(result == null || 
					((result.mApkDLUrl == null || result.mApkDLUrl.length() == 0)
					&& (result.mPatchDLUrl == null || result.mPatchDLUrl.length() == 0))){
				mIsRunning = false;
				Log.d("Update", "no update");
				//创建无效的下载器
				mTmpDownloadConfirm = new DownloadConfirm(null,mAppName);
				Log.i("TaosdkToMtop", "com.taobao.update.Update : request --> onSuccess : " + result.toString());
				Log.i("TaosdkToMtop", "no update");
			}else{
				Log.d("Update", "has update");
				//创建正常的下载器
				mTmpDownloadConfirm = new DownloadConfirm(result,mAppName);
				Log.i("TaosdkToMtop", "com.taobao.update.Update : request --> onSuccess : " + result.toString());
				Log.i("TaosdkToMtop", "has update");
			}
			//通知
			if(mListener != null) {
				mListener.onRequestResult(result, mTmpDownloadConfirm);
				Log.i("TaosdkToMtop", "com.taobao.update.Update : request --> onSuccess : " + result.toString());
				Log.i("TaosdkToMtop", "notify");
			}
		}

		@Override
		public void onSystemError(int arg0, MtopResponse arg1, Object arg2) {
			// TODO Auto-generated method stub
			if(arg1 != null) {
				UpdateInfo result = (UpdateInfo) DDUpdateConnectorHelper.syncPaser(arg1.getBytedata());
				mTmpDownloadConfirm = new DownloadConfirm(null,mAppName);
				//通知
				if(mListener != null)
					mListener.onRequestResult(result, mTmpDownloadConfirm);
				Log.i("TaosdkToMtop", "com.taobao.update.Update : request --> onSystemError : " + arg1.toString());
			}
			Log.i("TaosdkToMtop", "com.taobao.update.Update : request --> onSystemError : arg1 is null!");
		}
		
	}
	*/
	
	/**
	 * 取消下载
	 */
	public void cancelDownload(){
		if(mTmpDownloadConfirm != null)
			mTmpDownloadConfirm.cancel();
	}
	/**
	 * 下载器，当更新请求完成后，如果有更新版本，则通过OnUpdateListener.onRequestResult接口通知用户。<br>
	 * 用户确认下载后，通过该类发起下载。当用户取消时调用cancel接口，以完成本次流程。<br>
	 * 如果调用cancel接口时，已经启动下载，则本次下载被取消<br>
	 * @author bokui
	 *
	 */
	public class DownloadConfirm{
		//通过标记该变量来失效该下载器
		private boolean mDisabled = false;
		private UpdateInfo mUpdateInfo;
		private String mAppName;
		private int mPreProgress = -1;
		private PatchTask mPatchTask;
		
		private DownloadConfirm(UpdateInfo info,String appName){
			
			if(info == null)//无效的下载器
				mDisabled = true;
			
			mUpdateInfo = info;
			mAppName = appName;
			
		}
		
		/**
		 * 失效该下载器
		 */
		protected void disable(){
			cancel();
			mDisabled = true;
		}
		
		/**
		 * 启动下载
		 */
		public void download(){
			//无下载链接
			if(mDisabled || mUpdateInfo == null ||
					(mUpdateInfo.mApkDLUrl == null || mUpdateInfo.mApkDLUrl.length() == 0)
					&& (mUpdateInfo.mPatchDLUrl == null || mUpdateInfo.mPatchDLUrl.length() == 0)){
				mIsRunning = false;
				return;
			}
			
			mDownloader.setListener(new DownloaderListener());
			
			if(mApkStorePath == null) {
				File cache = Globals.getApplication().getExternalCacheDir();
				if(cache == null)
					cache = Globals.getApplication().getCacheDir();
				mApkStorePath = cache.getAbsolutePath();
			}
			
			File storePath = new File(mApkStorePath);
			StatFs statfs = null;
			try{
				statfs = new StatFs(storePath.getAbsolutePath());
			}catch(Exception e){
				e.printStackTrace();
			}
			long totalSpace = 0;
			if(statfs != null)
				totalSpace = (long)statfs.getAvailableBlocks()*statfs.getBlockSize();
			totalSpace -= mUpdateInfo.mApkSize + FOR_ENOUGH_SPACE;
			if(mUpdateInfo.mPatchDLUrl == null || mUpdateInfo.mPatchDLUrl.length() == 0){
				if(totalSpace >= 0){
					Log.d("Update", "start download");
					mDownloader.download(mUpdateInfo.mApkDLUrl, mApkStorePath, mUpdateInfo.mApkSize);
				}
			}else{
				//存储空间是否足够
				totalSpace -= mUpdateInfo.mPatchSize;
				if(totalSpace >= 0){
					Log.d("Update", "start download");
					mDownloader.download(mUpdateInfo.mPatchDLUrl, mApkStorePath, mUpdateInfo.mPatchSize);
				}
			}
			
			if(totalSpace < 0){
				//空间不足，提示并失效该下载器
				if(mListener != null)
					mListener.onDownloadError(OnDownloaderListener.ERROR_NOT_ENOUGH_SPACE
							,OnDownloaderListener.ERROR_NOT_ENOUGH_SPACE_STR);
				mIsRunning = false;
			}
		}
		
		/**
		 * 取消下载，用户取消更新时调用。
		 * 也可以用于取消正在下载流程。但建议采用Update的cancelDownload来取消下载。
		 */
		public void cancel(){
			mIsRunning = false;
			if(mDisabled)
				return;
			mDownloader.cancelDownload();
			if(mPatchTask != null)
				mPatchTask.cancel(true);
		}
		
		class DownloaderListener  implements OnDownloaderListener{
			@Override
			public void onDownloadProgress(int process) {
				if(mPreProgress == process)
					return;
				mPreProgress = process;
				//进度通知
				if(mListener != null)
					mListener.onDownloadProgress(process);
			}
	
			@Override
			public void onDownloadError(int errorCode, String msg) {
				//下载出错通知
				if(mListener != null)
					mListener.onDownloadError(errorCode, msg);
				mIsRunning = false;
			}
	
	
			@SuppressLint("NewApi")
			@Override
			public void onDownloadFinsh(String apkPath) {
				
				if(apkPath.endsWith(".apk")){
				    // 增加MD5检查 by leinuo
                    File apkFile = new File(apkPath);
                    String newMD5 = UpdateUtils.getMD5(apkFile.getAbsolutePath());
                    if(newMD5 ==null || !newMD5.equals(mUpdateInfo.mNewApkMD5)){
                        if(mListener != null)
                            mListener.onDownloadError(OnUpdateListener.MD5_VERIFY_FAILED, OnUpdateListener.MD5_VERIFY_FAILEDSTR);
                        mIsRunning = false;
                        return ;
                    }
                    //apk下载完成则通知
                    if(mListener != null)
                        mListener.onDownloadFinsh(apkPath);
                    mIsRunning = false;
				}else{
					if(mUpdateInfo != null){
						//合并差量包
						URL url = null;
						try {
							url = new URL(mUpdateInfo.mApkDLUrl);
						} catch (MalformedURLException e2) {
							e2.printStackTrace();
						}
						String fileName = null;
						if(url == null)
							fileName = mAppName+"@"+mUpdateInfo.mVersion;
						else
							fileName = new File(url.getFile()).getName();
						
						if(mApkStorePath == null) {
							File cache = Globals.getApplication().getExternalCacheDir();
							if(cache == null)
								cache = Globals.getApplication().getCacheDir();
							mApkStorePath = cache.getAbsolutePath();
						}					
						File newApkFile = new File(mApkStorePath,fileName);
						mPatchTask = new PatchTask(newApkFile.getAbsolutePath());
						if(Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB)
							mPatchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,apkPath,mUpdateInfo.mNewApkMD5);
						else
							mPatchTask.execute(apkPath,mUpdateInfo.mNewApkMD5);
					}
				}
			}
		}
	}
	
	class PatchTask extends  AsyncTask<String,Void,Boolean>{
		
		@Override
		protected void onCancelled() {
			super.onCancelled();
			//合并过程被cancel，则结束流程
			mIsRunning = false;
		}

		private String mTmpNewApkFile;
		
		public PatchTask(String newApkFile){
			mTmpNewApkFile = newApkFile;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if(result){
				//合并成功
				if(mListener != null)
					mListener.onDownloadFinsh(mTmpNewApkFile);
			}else{
				//合并失败
				if(mListener != null)
					mListener.onDownloadError(OnUpdateListener.MD5_VERIFY_FAILED, OnUpdateListener.MD5_VERIFY_FAILEDSTR);
			}
			mIsRunning = false;
		}

		@Override
		protected Boolean doInBackground(String... params) {
			//合并
			int ret = 0;
            try {
                ret = BSPatch.bspatch(mOldApkPath, mTmpNewApkFile, params[0]);
            } catch (Error e) {
                TaoLog.Loge("Update", "Exception while call bspatch() >>>" + e.getMessage());
            }
			if(ret == 1){
				//合并成功则校验MD5
				String newApkMD5 = UpdateUtils.getMD5(mTmpNewApkFile);
				if(newApkMD5 != null && newApkMD5.equals(params[1]))
					return true;
			}
			return false;
		}
	}
}