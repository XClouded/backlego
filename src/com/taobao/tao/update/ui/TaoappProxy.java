package com.taobao.tao.update.ui;

import android.app.DownloadManager;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.taobao.util.SafeHandler;
import android.taobao.util.TaoLog;
import android.text.TextUtils;

import com.taobao.android.service.Services;
import com.taobao.android.taoapp.api.ITaoapp;
import com.taobao.android.taoapp.api.TaoappUtils;
import com.taobao.android.task.Coordinator;
import com.taobao.android.task.Coordinator.TaggedRunnable;
import com.taobao.tao.Globals;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaoappProxy {
	private static final String TAG = "TaoappProxy";

	public static interface TaoappProxyCallback {
		void onLoad(boolean success);

		void onUpdate(int count);
	}

	private SafeHandler mHandler = new SafeHandler(Looper.getMainLooper());

	private ITaoapp mService;
	private TaoappProxyCallback mCallback;
	// private boolean mAsync;
	private Context context;
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (TaoappUtils.Action_Update_App_Update.equals(intent.getAction())) {
				int count = intent.getIntExtra(TaoappUtils.Key_UpdateCount, 0);
				if (mCallback != null) {
					mCallback.onUpdate(count);
				}
			}
		}
	};

	public TaoappProxy(Context ctx) {
		context = ctx.getApplicationContext();
		IntentFilter filter = new IntentFilter(
				TaoappUtils.Action_Update_App_Update);
		context.registerReceiver(mReceiver, filter);
	}

	public void setCallback(TaoappProxyCallback callback) {
		mCallback = callback;
	}

	//
	// public void setAsynchronized(boolean asyn) {
	// mAsync = asyn;
	// }

	public void load() {

		/*
		 * new SingleTask(new Runnable() {
		 * 
		 * @Override public void run() { if (mService != null) { return; } try {
		 * mService = Services.get(context, ITaoapp.class); } catch (Exception
		 * e) { e.printStackTrace(); }
		 * 
		 * mHandler.post(new Runnable() {
		 * 
		 * @Override public void run() { if (mCallback != null) {
		 * mCallback.onLoad(mService != null); } } });
		 * 
		 * } }, Priority.PRIORITY_HIGH).start();
		 */
		if(!mRunning.get()){
			mRunning.set(true);
			Coordinator.postTask(mTask);
		}
	}
	
	class TaoappProxyConnection implements ServiceConnection {

		@Override public void onServiceDisconnected(final ComponentName name) {
			
		}

		@Override public void onServiceConnected(ComponentName name, IBinder service) {
			mService = ITaoapp.Stub.asInterface(service);
			
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					if (mCallback != null) {
						mCallback.onLoad(mService != null);
					}
				}
			});
			mRunning.set(false);
		}
	}
	
	private TaoappProxyConnection mConnection = new TaoappProxyConnection();
	
	private TaggedRunnable mTask =  new TaggedRunnable("TaoappProxy_load") {

		@Override
		public void run() {
			if (mService != null) {
				return;
			}
			try {
				Services.bind(context.getApplicationContext(), ITaoapp.class, mConnection);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
	private AtomicBoolean mRunning = new AtomicBoolean();
	public boolean for360() {
		if (mService != null) {
			try {
				return mService.isConfigFor360();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	public int getUpdateAppSize() {
		if (mService != null) {
			try {
				return mService.getUpdateAppSize();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return 0;
	}

	public void setAutoDownload() {
		if (mService != null) {
			try {
				mService.setAutoDownload();
			} catch (Exception e) {
			}
		}
	}

	public void install() {
		if (mService != null) {
			try {
				mService.install();
			} catch (Exception e) {
			}
		}
	}

	public void startAutoDownload() {
		if (mService != null) {
			try {
				mService.startAutoDownload();
			} catch (Exception e) {
			}
		}
	}

	public void download() {
		if (mService != null) {
			try {
				mService.download();
			} catch (Exception e) {
			}
		}
	}

	public boolean hasAutoDownloaded() {
		if (mService != null) {
			try {
				return mService.hasAutoDownloaded();
			} catch (Exception e) {
			}
		}
		return false;
	}

	public boolean hasSetAutoDownload() {
		if (mService != null) {
			try {
				mService.hasSetAutoDownload();
			} catch (Exception e) {
			}
		}
		return false;
	}

	public void destroy() {
		if (mService != null) {
			Services.unbind(context.getApplicationContext(), mConnection);
			mService = null;
		}
		try {
			context.unregisterReceiver(mReceiver);
		} catch (Exception e) {
		}
		mCallback = null;
	}

	private static BroadcastReceiver sReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			long completeDownloadId = intent.getLongExtra(
					DownloadManager.EXTRA_DOWNLOAD_ID, -1);
			if (sDownloadId != completeDownloadId) {
				return;
			}
			Uri uri = null;
			try {
				DownloadManager downloadManager = (DownloadManager) Globals
						.getApplication().getSystemService(
								Context.DOWNLOAD_SERVICE);
				uri = downloadManager
						.getUriForDownloadedFile(completeDownloadId);

			} catch (Exception e) {
			} catch (Throwable e) {
			}
			String path = null;
			if (uri != null) {
				try {
					path = getRealPath(context, uri);
				} catch (Exception e) {
				} catch (Throwable e) {
				}
			}
			if (!TextUtils.isEmpty(path)) {
				install(context, path);
			}
		}
	};

	private static boolean sRegistered;
	private static long sDownloadId;
	private static final String sSPName = "taoapp_proxy_download", Key = "id";

	public static void addDownload(long id) {
		sDownloadId = id;
		if (!sRegistered) {
			sRegistered = true;
			IntentFilter filter = new IntentFilter(
					DownloadManager.ACTION_DOWNLOAD_COMPLETE);
			Globals.getApplication().registerReceiver(sReceiver, filter);
		}
		SharedPreferences sp = Globals.getApplication().getSharedPreferences(
				sSPName, Context.MODE_PRIVATE);
		Editor editor = sp.edit().putLong(Key, sDownloadId);
		if (Build.VERSION.SDK_INT >= 9) {
			editor.apply();
		}
		else {
			editor.commit();
		}
	}

	public static String getDownloadPath() {
		SharedPreferences sp = Globals.getApplication().getSharedPreferences(
				sSPName, Context.MODE_PRIVATE);
		long id = sp.getLong(Key, 0);
		if (id <= 0) {
			return null;
		}
		Uri uri = null;
		try {
			DownloadManager downloadManager = (DownloadManager) Globals
					.getApplication()
					.getSystemService(Context.DOWNLOAD_SERVICE);
			uri = downloadManager.getUriForDownloadedFile(id);

		} catch (Exception e) {
		} catch (Throwable e) {
		}
		String path = null;
		if (uri != null) {
			try {
				path = getRealPath(Globals.getApplication(), uri);
			} catch (Exception e) {
			} catch (Throwable e) {
			}
		}
		return path;
	}

	private static String getRealPath(Context context, Uri fileUrl) {
		String fileName = null;
		Uri filePathUri = fileUrl;
		if (fileUrl != null) {
			if (fileUrl.getScheme().toString().compareTo("content") == 0) {
				try {
					// content://开头的uri
					Cursor cursor = context.getContentResolver().query(fileUrl,
							null, null, null, null);
					if (cursor != null && cursor.moveToFirst()) {
						int column_index = cursor
								.getColumnIndexOrThrow("_data");
						fileName = cursor.getString(column_index); // 取出文件路径
						// if (!fileName.startsWith("/mnt")) {
						// // 检查是否有”/mnt“前缀
						// fileName = "/mnt" + fileName;
						// }
						cursor.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (fileUrl.getScheme().compareTo("file") == 0) {
				// file:///开头的uri
				fileName = filePathUri.toString();
				fileName = filePathUri.toString().replace("file://", "");
				// 替换file://
				// if (!fileName.startsWith("/mnt")) {
				// // 加上"/mnt"头
				// fileName = "/mnt"+fileName;
				// }
			}
		}
		return fileName;
	}

	private static boolean install(Context context, String path) {
		Intent installIntent = new Intent(Intent.ACTION_VIEW);
		boolean hasSetDataType = false;
		try {
			if (context == null) {
				return false;
			}
			if (TextUtils.isEmpty(path)) {
				return false;
			}

			File file = new File(path);
			if (!(file.isFile() && file.exists())) {
				return false;
			}

			installIntent.setDataAndType(Uri.fromFile(file),
					"application/vnd.android.package-archive");
			hasSetDataType = true;

			String str = context.getPackageName();
			installIntent.putExtra(
					"android.intent.extra.INSTALLER_PACKAGE_NAME", str);
			ComponentName localComponentName = new ComponentName(
					"com.android.packageinstaller",
					"com.android.packageinstaller.PackageInstallerActivity");
			installIntent.setComponent(localComponentName);
			installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_NO_HISTORY);
			context.startActivity(installIntent);
			return true;
		} catch (Exception e) {
			if (context == null || installIntent == null) {
				return false;
			}
			try {
				installIntent.setComponent(null);
				if (!hasSetDataType) {
					if (TextUtils.isEmpty(path)) {
						return false;
					}
					File file = new File(path);
					if (!(file.isFile() && file.exists())) {
						return false;
					}
					installIntent.setDataAndType(Uri.fromFile(file),
							"application/vnd.android.package-archive");
				}
				context.startActivity(installIntent);
			} catch (Exception e2) {
				e2.printStackTrace();
			}
			return false;
		}
	}

	public static boolean isNormal(String versionName) {
		if (TextUtils.isEmpty(versionName)) {
			return false;
		}
		String[] strs = versionName.split("\\.");
		if (strs != null && strs.length == 3) {
			return true;
		} else {
			return false;
		}
	}
}
