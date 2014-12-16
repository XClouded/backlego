package com.taobao.update;

import com.taobao.update.Update.DownloadConfirm;


/**
 * 更新callback，用于监听更新状态
 * @author bokui
 *
 */
public interface OnUpdateListener {
	public static final  int MD5_VERIFY_FAILED = -1;
	public static final String MD5_VERIFY_FAILEDSTR = "Apk'md5 verify failed";
	/**
	 * 更新结果回调，UpdateInfo字段包含了更新请求的结果，UI根据结果提示用户，
	 * 当用户确认下载时，调用DownloadConfirm download方法启动下载
	 * @param info	更新结果
	 * @param confirm	下载确认器
	 */
	public void onRequestResult(UpdateInfo info, DownloadConfirm confirm);
	/**
	 * 下载进度回调
	 * @param process	进度，值为0-100
	 * @param bps	下载速度Byte per second
	 */
	public void onDownloadProgress(int progress);
	/**
	 * 下载出错
	 * code：
	 * 1、MD5_VERIFY_FAILED	差量合并后md5校验失败
	 * 
	 * @param errorCode	出错code
	 * @param msg		出错信息
	 */
	public void onDownloadError(int errorCode, String msg);
	/**
	 * 下载完成
	 * @param apkPath	保存的apk文件
	 */
	public void onDownloadFinsh(String apkPath);
}
