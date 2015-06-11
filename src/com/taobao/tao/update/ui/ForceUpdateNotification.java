package com.taobao.tao.update.ui;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.taobao.statistic.CT;
import com.taobao.statistic.TBS;
import com.taobao.tao.Globals;
import com.taobao.tao.update.Updater;
import com.taobao.tao.util.BuiltConfig;
import com.taobao.tao.util.Constants;
import com.taobao.tao.util.TBDialog;
import com.taobao.lego.R;
import com.taobao.update.Update.DownloadConfirm;
import com.taobao.update.UpdateInfo;

public class ForceUpdateNotification {
	private RelativeLayout mProgressBarLayout;
	private ProgressBar mProgress = null;
	private TextView mPercentText = null;
	private TBDialog myProgressDialog = null;
	private Application mApp;
	private Context mContext;
	private TBDialog mDialog;
	
	public ForceUpdateNotification(Application context){
		mApp = context;
		mContext = context.getApplicationContext();
	}
	/**
	 * 强制更新 提示
	 */
	public void popUpForceUpdateDlg(UpdateInfo info, DownloadConfirm confirm,String apkSize){
		//当前是否存在activity
		final Activity currentActivity = Updater.getCurrentActivity();
		if (currentActivity == null) {
			//没有则结束下载
			Intent intent = new Intent();
            intent.setPackage(Globals.getApplication().getPackageName());
			intent.setAction(Updater.UPDATER_NOTIFICATION);
			intent.putExtra(Updater.UPDATER_NOTIFICATION, Updater.CANCEL_FORCE_DOWNLOAD);
			mContext.sendBroadcast(intent);
			return;
		}
		mDialog = new TBDialog.Builder(currentActivity)
		.setTitle(R.string.prompt_title)
		.setMessage(info.mNotifyTip+"\n\n更新包大小："+apkSize)
		.setPositiveButton(Constants.BACKUPDATE, new ForceUpdateConfirm(confirm,currentActivity))
		.setNegativeButton(R.string.Cancel, new ForceUpdateCancel(confirm,currentActivity))
		.setOnCancelListener(new ForceUpdateCancel(confirm,currentActivity))
		.show();
	}
	
	/**
	 * 强制更新确认下载
	 * @author bokui
	 *
	 */
	class ForceUpdateConfirm implements OnClickListener,OnCancelListener{
		private DownloadConfirm mTmpDownloadConfirm;
		private Activity mActivity;
		public ForceUpdateConfirm(DownloadConfirm downloadConfirm,Activity activity){
			mTmpDownloadConfirm = downloadConfirm;
			mActivity = activity;
		}
		@Override
		public void onClick(View v) {
					mTmpDownloadConfirm.download();
					TBS.Adv.ctrlClicked(CT.Button,"UpdateConfirm");
		}
		@Override
		public void onCancel(DialogInterface dialog) {
			mTmpDownloadConfirm.download();
			TBS.Adv.ctrlClicked(CT.Button,"UpdateConfirm");
		}
	}
	
	/**
	 * 强制更新退出确认框
	 */
	private void popUpForceConfirmDlg(DownloadConfirm confirm,Activity activity){
		
		
		String message = activity.getString(R.string.confirm_forceupdate_cancel);
		
		new TBDialog.Builder(activity)
		.setTitle(R.string.prompt_title)
		.setMessage(message)
		.setPositiveButton(R.string.Ensure, new ForceUpdateExit(confirm) )
		.setNegativeButton(R.string.Cancel, new ForceUpdateConfirm(confirm,activity))
		.setOnCancelListener(new ForceUpdateConfirm(confirm,activity))
		.show();
	}
	
	/**
	 * 退出客户端
	 * @author bokui
	 *
	 */
	class ForceUpdateExit implements OnClickListener,OnCancelListener{
		private DownloadConfirm mTmpDownloadConfirm;
		public ForceUpdateExit(DownloadConfirm downloadConfirm){
			mTmpDownloadConfirm = downloadConfirm;
		}
		public ForceUpdateExit(){
		}
		@Override
		public void onClick(View v) {
			if(mTmpDownloadConfirm != null)
				mTmpDownloadConfirm.cancel();
//			PanelManager.getInstance().removeAllPanel();
			TBS.Adv.ctrlClicked(CT.Button, "ForceUpdateCancel");
			exitApp();
		}
		@Override
		public void onCancel(DialogInterface dialog) {
			if(mTmpDownloadConfirm != null)
				mTmpDownloadConfirm.cancel();
//			PanelManager.getInstance().removeAllPanel();
			TBS.Adv.ctrlClicked(CT.Button, "ForceUpdateCancel");
			exitApp();
		}
	}
	
	/**
	 * 强制更新取消
	 * @author bokui
	 *
	 */
	class ForceUpdateCancel implements OnClickListener,OnCancelListener{
		private DownloadConfirm mTmpDownloadConfirm;
		private Activity mActivity;
		public ForceUpdateCancel(DownloadConfirm downloadConfirm,Activity activity){
			mTmpDownloadConfirm = downloadConfirm;
			mActivity = activity;
		}
		@Override
		public void onClick(View v) {
			popUpForceConfirmDlg(mTmpDownloadConfirm,mActivity);
			mActivity = null;
		}

		@Override
		public void onCancel(DialogInterface dialog) {
			popUpForceConfirmDlg(mTmpDownloadConfirm,mActivity);
			mActivity = null;
		}
		
	}
	
	private boolean createDlg(Activity activity){
		if(myProgressDialog == null || !myProgressDialog.isShowing()){
			
			LayoutInflater inflater = LayoutInflater.from(activity);
			mProgressBarLayout = (RelativeLayout)inflater.inflate(R.layout.update_coerce, null);
			mProgress = (ProgressBar) mProgressBarLayout.findViewById(R.id.pb1);
			mPercentText = (TextView) mProgressBarLayout.findViewById(R.id.tvUpdatePercent);
			
			//强制升级建议不能取消，直接退出
			myProgressDialog = new TBDialog.Builder(activity).setTitle(R.string.dialog_title_update_progress)
															 .setView(mProgressBarLayout)
															 .setPositiveButton(R.string.exit, new ForceDlCancel())
															 .setOnCancelListener(new ForceDlCancel()).show();
		}
		return true;
	}
	/**
	 * 更新下载进度
	 * @param progress	进度值0-100
	 */
	public void updateDlProgress(int progress) {
		//当前是否存在activity
		final Activity currentActivity = Updater.getCurrentActivity();
		if (currentActivity == null) {
			//没有则结束下载
			Intent intent = new Intent();
			intent.setAction(Updater.UPDATER_NOTIFICATION);
			intent.putExtra(Updater.UPDATER_NOTIFICATION, Updater.CANCEL_FORCE_DOWNLOAD);
			mContext.sendBroadcast(intent);
			return;
		}
		//创建 dlg
		if(!createDlg(currentActivity))
			return;
		
		//更新进度
		if (null==mProgressBarLayout||null==mProgress){
			return;
		}
		mProgress.setProgress(progress);
		if(null!=mPercentText){
			mPercentText.setText(progress + "%");
			return;
		}
	}
	
	class ForceDlCancel implements OnClickListener,OnCancelListener{

		@Override
		public void onCancel(DialogInterface dialog) {
			Intent intent = new Intent();
			intent.setAction(Updater.UPDATER_NOTIFICATION);
            intent.setPackage(Globals.getApplication().getPackageName());
            intent.putExtra(Updater.UPDATER_NOTIFICATION, Updater.CANCEL_FORCE_DOWNLOAD);
			mContext.sendBroadcast(intent);
//			PanelManager.getInstance().removeAllPanel();
			TBS.Adv.ctrlClicked(CT.Button, "ForceUpdateCancel");
			exitApp();
		}

		@Override
		public void onClick(View v) {
			Intent intent = new Intent();
			intent.setAction(Updater.UPDATER_NOTIFICATION);
			intent.putExtra(Updater.UPDATER_NOTIFICATION, Updater.CANCEL_FORCE_DOWNLOAD);
            intent.setPackage(Globals.getApplication().getPackageName());
            mContext.sendBroadcast(intent);
//			PanelManager.getInstance().removeAllPanel();
			TBS.Adv.ctrlClicked(CT.Button, "ForceUpdateCancel");
			exitApp();
		}
		
	}
	

	/**
	 * 显示安装进度
	 */
	public void finished(String apkPath,String info){
		//当前是否存在activity
		final Activity currentActivity = Updater.getCurrentActivity();
		if (currentActivity == null) {
			//没有则结束
			return;
		}
		if(myProgressDialog != null && myProgressDialog.isShowing())
			myProgressDialog.dismiss();
		//创建 dlg
		if(!createDlg(currentActivity))
			return;
		
		//提示安装
		if(info == null || info.length() == 0)
			info = BuiltConfig.getString(R.string.update_notification_finish);
		if(myProgressDialog != null && myProgressDialog.isShowing()){
			mProgress.setVisibility(View.INVISIBLE);
			mPercentText.setText(info);
			myProgressDialog.setPositiveButtonText(mContext.getText(R.string.install));
			myProgressDialog.setPositiveButton(new ForceDlInstall(apkPath));
//			myProgressDialog.setNegativeButtonText(mContext.getText(R.string.exit));
//			myProgressDialog.setNegativeButton(new ForceUpdateExit());
			myProgressDialog.setOnCancelListener(new ForceUpdateExit());
		}
	}
	
	
	class ForceDlInstall implements OnClickListener{
		private String mApkPath;
		public ForceDlInstall(String apkPath){
			mApkPath = apkPath;
		}
		@Override
		public void onClick(View v) {
			Intent intent = new Intent();
			intent.setAction(Updater.UPDATER_NOTIFICATION);
            intent.setPackage(Globals.getApplication().getPackageName());
            intent.putExtra(Updater.UPDATER_NOTIFICATION, Updater.FORCE_INSTALL);
			intent.putExtra(Updater.INSTALL_PATH, mApkPath);
			mContext.sendBroadcast(intent);
//			PanelManager.getInstance().removeAllPanel();
			//exitApp();
		}
		
	}
	
	private void exitApp(){
		int pid = android.os.Process.myPid();
		android.os.Process.killProcess(pid);
	}
	
	/**
	 * 显示错误dialog
	 */
	public void err(String errMsg){
		//当前是否存在activity
		final Activity currentActivity = Updater.getCurrentActivity();
		if (currentActivity == null) {
			//没有则结束
			return;
		}
		//创建 dlg
		if(!createDlg(currentActivity))
			return;
		
		if(myProgressDialog != null && myProgressDialog.isShowing()){
			mProgress.setVisibility(View.INVISIBLE);
			mPercentText.setText(errMsg);
			myProgressDialog.setPositiveButtonText(mContext.getText(R.string.Ensure));
			myProgressDialog.setPositiveButton(new ForceUpdateExit());
			myProgressDialog.setNegativeButtonText(mContext.getText(R.string.exit));
			myProgressDialog.setNegativeButton(new ForceUpdateExit());
			myProgressDialog.setOnCancelListener(new ForceUpdateExit());
		}
	}
}
