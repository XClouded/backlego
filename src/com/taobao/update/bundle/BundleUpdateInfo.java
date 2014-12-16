package com.taobao.update.bundle;

import java.util.List;

/**
 * Bundle更新信息（主客520版，atlas重构引入bundle OSGI理念而引入Bundle更新，走主客更新接口）
 * @author leinuo
 *
 */
public class BundleUpdateInfo {
    
    /**
     * bundle名
     */
    public String mBundleName = null;
    
    /**
     * bundle下载路径
     */
    public String mBundleDLUrl = null;
    /**
     * patch补丁下载路径
     */
    public String mPatchDLUrl = null;
    /**
     * bundle文件大小
     */
    public long mBundleSize = 0;
    /**
     * patch补丁大小
     */
    public long mPatchSize = 0;
    /**
     * 新版本版本号
     */
    public String mVersion = null;
   
    /**
     * 新版本bundle md5值
     */
    public String mNewBundleMD5 = null;
    
    /**
     * 来源服务端的客户端bundle md5值：与客户端md5比较，相同做差量，然后与新md5比较
     */
    public String mlocalBundleMD5 = null;
    
    /**
     * 依赖的bundle列表
     */
    public List<String> dependencies = null;

}
