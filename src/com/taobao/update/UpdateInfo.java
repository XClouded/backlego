package com.taobao.update;

import com.taobao.update.bundle.BundleBaselineInfo;


/**
 * 更新信息
 * @author bokui
 *
 */
public class UpdateInfo {
	/**
	 * 错误码
	 */
	public int mErrCode = 0;
	/**
	 * apk下载路径，用于全量下载
	 */
	public String mApkDLUrl = null;
	/**
	 * patch补丁下载路径，用于增量下载
	 */
	public String mPatchDLUrl = null;
	/**
	 * apk文件大小
	 */
	public long mApkSize = 0;
	/**
	 * patch补丁大小
	 */
	public long mPatchSize = 0;
	/**
	 * 新版本版本号
	 */
	public String mVersion = null;
	/**
	 * 更新优先级
	 */
	public int mPriority = -1;
	/**
	 * 提示信息
	 */
	public String mNotifyTip = null;
	/**
	 * 新版本apk md5值
	 */
	public String mNewApkMD5 = null;

    /**
     * 版本提醒次数，默认1次
     */
    public int    mRemindNum  = 1;

    /**
     * 动态部署回滚版本
     */
    public String rollback = null;
    
    /**
     * bundle更新基线信息
     */
    private BundleBaselineInfo bundleBaselineInfo = null ;
    
    public BundleBaselineInfo getBundleBaselineInfo() {
        return bundleBaselineInfo;
    }

    public void setBundleBaselineInfo(BundleBaselineInfo bundleBaselineInfo) {
        this.bundleBaselineInfo = bundleBaselineInfo;
    } 

}
