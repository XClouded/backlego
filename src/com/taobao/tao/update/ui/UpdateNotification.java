package com.taobao.tao.update.ui;

import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RemoteViews;
import com.taobao.android.taoapp.api.TaoappUtils;
import com.taobao.statistic.CT;
import com.taobao.statistic.TBS;
import com.taobao.tao.update.Updater;
import com.taobao.tao.util.BuiltConfig;
import com.taobao.tao.util.Constants;
import com.taobao.tao.util.TBDialog;
import com.taobao.lego.R;
import com.taobao.update.ApkUpdateMonitor;
import com.taobao.update.Update.DownloadConfirm;
import com.taobao.update.UpdateInfo;

public class UpdateNotification{
	/**
	 * Unique notification id
	 */
	private int mNotificationId = 34858;

	/**
	 * Manager that deals with notification
	 */
	private NotificationManager mNotificationManager;
	private Notification mNotification;
	
	private Application mApp;
	private Context mContext;
//	private TaoappProxy mProxy;

	/**
	 * the path to save the file
	 */
	


	/**
	 * Constructor, creates new status notification for file being downloaded
	 * 
	 * @param id
	 *            Unique notification id
	 * @param url
	 *            Url of file being downloaded
	 * @param path
	 *            File path
	 * @param size
	 *            Free size for download
	 */
	public UpdateNotification(Application app) {
		// call parent
		mApp = app;
		mContext = app.getApplicationContext();
		mNotification = new Notification();
		mNotificationManager = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotification.contentView = new RemoteViews(mContext.getPackageName(),
				R.layout.update_notification);
		mNotification.contentView.setImageViewBitmap(R.id.downloadImage,
				BitmapFactory.decodeResource(mContext.getResources(),R.drawable.tao_mag_icon));
		mNotification.icon = android.R.drawable.stat_sys_download;
		mNotification.tickerText = BuiltConfig.getString(R.string.update_notification_start);
		mNotification.flags = Notification.FLAG_ONGOING_EVENT;
	}
	
    public void popUpInstallDlg(String apkPath, String info) {
		Activity activity = Updater.getCurrentActivity();
		if(activity == null)
			return;
		if(info == null || info.length() == 0)
			info = BuiltConfig.getString(R.string.confirm_install_hint1);
        int tipText = R.string.CancelNow;
		new TBDialog.Builder(activity)
		.setTitle(R.string.prompt_title).setMessage(info)
		.setPositiveButton(R.string.install, new InstallConfirm(apkPath,activity.getApplicationContext()))
		.setNegativeButton(tipText, null)
		.setOnCancelListener(null).show();
	}
	
	class InstallConfirm implements OnClickListener{
		private String mApkPath;
		private Context mContext;
		public InstallConfirm(String apkPath,Context context){
			mApkPath = apkPath;
			mContext = context;
		}
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(); 
			intent.setAction(Updater.UPDATER_NOTIFICATION);
			intent.putExtra(Updater.UPDATER_NOTIFICATION, Updater.INSTALL);
			intent.putExtra(Updater.INSTALL_PATH, mApkPath);
			mContext.sendBroadcast(intent);
//			Intent installIntent = new Intent(Intent.ACTION_VIEW);
//			installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			installIntent.setDataAndType(Uri.fromFile(new File(mApkPath)),
//					"application/vnd.android.package-archive");
//			mContext.startActivity(installIntent);
		}
		
	}
	private TBDialog mDialog;
	/*
	 * 弹出下载提示对话框
	 */
    public void popUpUpdateDlg(UpdateInfo info, DownloadConfirm confirm, String apkSize) {
		Activity activity = Updater.getCurrentActivity();
		if(activity == null){
			//不存在activity则结束下载
			Intent intent = new Intent();
			intent.setAction(Updater.UPDATER_NOTIFICATION);
			intent.putExtra(Updater.UPDATER_NOTIFICATION, Updater.CANCEL_DOWNLOAD);
			mContext.sendBroadcast(intent);
			return;
		}
		if(!activity.isFinishing()) {
            try {
                int tipText = R.string.CancelNow;
                mDialog = new TBDialog.Builder(activity)
                        .setTitle(R.string.prompt_title).setMessage(info.mNotifyTip + "\n\n更新包大小：" + apkSize).setShowPhoneTaoHelpHit(false)
                        .setPositiveButton(Constants.BACKUPDATE, new UpdateConfirm(activity, confirm, false))
                        .setNegativeButton(tipText, new UpdateCancel(confirm))
                        .setOnCancelListener(new UpdateCancel(confirm)).show();
            }catch(Throwable e){}
        }
	}
	//确认下载
	static class UpdateConfirm implements OnClickListener{
		private DownloadConfirm mTmpDownloadConfirm;
		private Activity activity;
		private boolean mGotoTaoapp;
		public UpdateConfirm(Activity activity, DownloadConfirm downloadConfirm,boolean gotoTaoapp){
			this.activity = activity;
			mTmpDownloadConfirm = downloadConfirm;
			mGotoTaoapp = gotoTaoapp;
		}
		@Override
		public void onClick(View v) {
			ApkUpdateMonitor.count(ApkUpdateMonitor.CONFIRM_DOWNLOAD,null);
				if(mGotoTaoapp && TaoappUtils.gotoTaobaoDetail(activity)){
					mTmpDownloadConfirm.cancel();
					TBS.Adv.ctrlClicked(CT.Button,"UpdateFromPhoneTaoHelpConfirm");
				}else{
					mTmpDownloadConfirm.download();
					TBS.Adv.ctrlClicked(CT.Button,"UpdateConfirm");
				}
		}
	}
	//取消下载
	static class UpdateCancel implements OnClickListener,OnCancelListener{
		private DownloadConfirm mTmpDownloadConfirm;
		public UpdateCancel(DownloadConfirm downloadConfirm){
			mTmpDownloadConfirm = downloadConfirm;
		}
		@Override
		public void onClick(View v) {
			ApkUpdateMonitor.count(ApkUpdateMonitor.CANCEL_DOWNLOAD,null);
			mTmpDownloadConfirm.cancel();
			TBS.Adv.ctrlClicked(CT.Button, "UpdateCancel");
		}
		@Override
		public void onCancel(DialogInterface dialog) {
			ApkUpdateMonitor.count(ApkUpdateMonitor.CANCEL_DOWNLOAD,null);
			mTmpDownloadConfirm.cancel();
			TBS.Adv.ctrlClicked(CT.Button, "UpdateCancel");
		}
	}
	
	/**
	 * Update status notificaion with the new progress
	 * 
	 * @param progress
	 *            Progress of file download
	 */
	public void updateDlProgress(int progress) {
		// update file download progress
		Intent intent = new Intent();
		intent.setAction(Updater.UPDATER_NOTIFICATION);
		intent.putExtra(Updater.UPDATER_NOTIFICATION, Updater.CANCEL_DOWNLOAD);
		mNotification.contentIntent = PendingIntent.getBroadcast(
				mContext, 0, intent,
				PendingIntent.FLAG_CANCEL_CURRENT);
		// set fields in the custom view
		mNotification.contentView.setTextViewText(R.id.downloadText, BuiltConfig.getString(R.string.update_notification_downloading)
				+ progress + "%，点击取消下载");
		mNotification.contentView.setProgressBar(R.id.downloadBar, 100, progress, false);
		// change notification for user
		mNotificationManager.notify(mNotificationId, mNotification);
	}

	/**
	 * Update status notification that file download has finished
	 */
	public void finished(String mApkPath) {
		// change flags and icon
//		mNotification.flags = 0;
		mNotification.icon = android.R.drawable.stat_sys_download_done;
		mNotification.tickerText = BuiltConfig.getString(R.string.update_notification_finish);

		// set fields in the custom view
		mNotification.contentView.setTextViewText(R.id.downloadText, BuiltConfig.getString(R.string.update_notification_finish) + "，点击安装");
		mNotification.contentView.setProgressBar(R.id.downloadBar, 100, 100, false);

		Intent intent = new Intent(); 
		intent.setAction(Updater.UPDATER_NOTIFICATION);
		intent.putExtra(Updater.UPDATER_NOTIFICATION, Updater.INSTALL);
		intent.putExtra(Updater.INSTALL_PATH, mApkPath);
		PendingIntent installIntent = PendingIntent.getBroadcast(mContext, 0,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);
		mNotification.contentIntent = installIntent;
//		contentView.setOnClickPendingIntent(R.id.cancelBtn, installIntent);

		// change notification for user
		mNotificationManager.notify(mNotificationId, mNotification);
	}

	/**
	 * Update status notification that file download has failed
	 */
	public void err(String errInfo) {

		// change flags and icon
//		 flags = ~Notification.FLAG_ONGOING_EVENT;
		mNotification.flags = Notification.FLAG_AUTO_CANCEL;
		mNotification.icon = android.R.drawable.stat_sys_warning;
		mNotification.tickerText = BuiltConfig.getString(R.string.update_notification_fail);

		mNotification.contentView.setTextViewText(R.id.downloadText, BuiltConfig.getString(R.string.update_notification_fail) + "，" + errInfo);

		Intent intent = new Intent();
		PendingIntent downloadIntent = PendingIntent.getBroadcast(mContext, 0,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);
		mNotification.contentIntent = downloadIntent;
//		contentView.setOnClickPendingIntent(R.id.cancelBtn, downloadIntent);

		// change notification for user
		mNotificationManager.notify(mNotificationId, mNotification);
	}

	/**
	 * Remove status notification
	 */
	public void remove() {
		// remove notification for user
		mNotificationManager.cancel(mNotificationId);
	}

}
