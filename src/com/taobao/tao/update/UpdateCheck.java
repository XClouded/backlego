package com.taobao.tao.update;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import com.taobao.tao.Globals;
import com.taobao.tao.TaoApplication;
import com.taobao.tao.util.Constants;
import com.taobao.update.UpdateInfo;
import com.taobao.update.UpdateLister;
import com.taobao.update.UpdateRequest;
import com.taobao.update.UpdateUtils;

public class UpdateCheck {
	private static UpdateCheck sInstance = null;
	private Context mContext;
	private UpdateLister mListener;
	private UpdateRequest mRequest;
	
	protected UpdateCheck(Context context) {
		mContext = context.getApplicationContext();
		mRequest=new RequestImp();
	}
	/**
	 * 获取Updater实例
	 * @param context	Context实例
	 * @return	Updater实例
	 */
	
	public static synchronized UpdateCheck getInstance(Context context) {
		if(context == null)
			return null;
		if (sInstance == null) {
			sInstance = new UpdateCheck(context);
		}
		return sInstance;
	}
	
	
	public void setmListener(UpdateLister mListener) {
		this.mListener = mListener;
	}

   @SuppressLint("NewApi")
@SuppressWarnings("deprecation")
   public void checkUpdate(){
	   if (Constants.isLephone()) {
		   if(mListener != null){
				mListener.onOtherCondition();;
			}
		   return;
	   }
	    String apkPath=UpdateUtils.getApkPath(mContext);
		String version = Globals.getVersionName();
		UpdateRequestTask task=new UpdateRequestTask(apkPath,TaoApplication.getTTIDNum(),version);
		if(Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB)
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		else
			task.execute();
//		if(mRequest != null)
//			mRequest.request(TaoApplication.getTTIDNum(), version, UpdateUtils.getMD5(apkPath), new MyMtopListener());
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
		
		public UpdateRequestTask(String apkPath,String appName,String version){
			mApkPath = apkPath;
			mAppName = appName;
			mVersion = version;
		}
		
		@Override
		protected UpdateInfo doInBackground(Void... params) {
			
			if(mRequest == null)
				return null;
			//请求更新
			String appMd5 = "";
			if(mApkPath != null)
				appMd5 = UpdateUtils.getMD5(mApkPath);
			return mRequest.request(mAppName, mVersion,appMd5);
		}

		@Override
		protected void onPostExecute(UpdateInfo result) {
			super.onPostExecute(result);
			//不存在更新，则本次更新结束
			if(result == null || 
					((result.mApkDLUrl == null || result.mApkDLUrl.length() == 0)
					&& (result.mPatchDLUrl == null || result.mPatchDLUrl.length() == 0))){
				//通知
				if(mListener != null){
					mListener.onNoUpdate();
				}
				
			}else{
				//通知
				if(mListener != null){
					mListener.onNeedUpdate();
				}
				
			}
		}
	}
	
	/*
	class MyMtopListener implements IRemoteBaseListener {

		@Override
		public void onError(int arg0, MtopResponse arg1, Object arg2) {
			// TODO Auto-generated method stub
			mListener.onNoUpdate();
			Log.i("TaosdkToMtop", "UpdateCheck : checkUpdate --> onError : " + arg1.toString());
		}

		@Override
		public void onSuccess(int arg0, MtopResponse arg1, BaseOutDo arg2,
				Object arg3) {
			// TODO Auto-generated method stub
			UpdateInfo result = (UpdateInfo) DDUpdateConnectorHelper.syncPaser(arg1.getBytedata());
			if(result == null || 
					((result.mApkDLUrl == null || result.mApkDLUrl.length() == 0)
					&& (result.mPatchDLUrl == null || result.mPatchDLUrl.length() == 0))){
				//通知
				if(mListener != null){
					mListener.onNoUpdate();
				}
				Log.i("TaosdkToMtop", "UpdateCheck : checkUpdate --> onSucess : " + result.toString());
			}else{
				//通知
				if(mListener != null){
					mListener.onNeedUpdate();
				}
				
			}
		}

		@Override
		public void onSystemError(int arg0, MtopResponse arg1, Object arg2) {
			// TODO Auto-generated method stub
			mListener.onNoUpdate();
			Log.i("TaosdkToMtop", "UpdateCheck : checkUpdate --> onSystemError : " + arg1.toString());
		}
	}
	*/
	
}