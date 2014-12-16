package com.taobao.update.bundle;

import com.taobao.tao.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * bundle每次更新基线信息类
 * @author leinuo
 *
 */
public class BundleBaselineInfo {
    
    /**
     * bundle更新的基线版本
     */
    private String mBaselineVersion = null;
    
    /**
     * bundle全量更新url
     */
    private Map<String,String> mPackageUrlMap = null;
    
    /**
     * bundle差量更新url
     */
     private Map<String,String> mPatchUrlMap = null;
     
     /**
      * bundle全量更新size
      */
     private Map<String,Long> mPackageSize = null;
     
     /**
      * bundle差量更新size
      */
     private Map<String,Long> mPatchSize = null;
     
     /**
      * 本次基线所有包大小
      */
     private Long allPackageSize = null;
    
     /**
      * 本次基线所有补丁大小
      */
     private Long allPatchSize = null;
     
     /**
      * 本次基线所有下载文件大小 
      */
     private Long allSize = null;
     
     /**
      * 本次基线所有下载地址
      */
     private List<String> allDownloadUrl = null;
     
     /**
      * 本次基线packageMD5
      */
     private Map<String,String> packageMD5 = null;
     
     /**
      * 本次基线patchMD5
      */
     private Map<String,String> patchMD5 = null;
     
     /**
      * 本次基线patch包对应的全量url
      */
     private Map<String,String> packageURLWithPatch = null;
     
     /**
      * 本次基线patch包对应的全量MD5
      */
     private Map<String,String> packageMD5WithPatch = null;
     
     /**
      * Bundle更新信息（主客520版，atlas重构引入bundle OSGI理念而引入Bundle更新，走主客更新接口）
      */
     private List<BundleUpdateInfo> bundleUpdateList;

     
     public List<BundleUpdateInfo> getBundleUpdateList() {
         return bundleUpdateList;
     }

     
     public void setBundleUpdateList(List<BundleUpdateInfo> bundleUpdateList) {
         this.bundleUpdateList = bundleUpdateList;
     }
     
    
     public String getmBaselineVersion() {
         return mBaselineVersion;
     }

    
    public void setmBaselineVersion(String mBaselineVersion) {
        this.mBaselineVersion = mBaselineVersion;
    }

    
    public Map<String, String> getmPackageUrlMap() {
        return mPackageUrlMap;
    }

    
    public void setmPackageUrlMap(Map<String, String> mPackageUrlMap) {
        this.mPackageUrlMap = mPackageUrlMap;
    }

    
    public Map<String, String> getmPatchUrlMap() {
        return mPatchUrlMap;
    }

    
    public void setmPatchUrlMap(Map<String, String> mPatchUrlMap) {
        this.mPatchUrlMap = mPatchUrlMap;
    }


    
    public Map<String, Long> getmPackageSize() {
        return mPackageSize;
    }


    
    public void setmPackageSize(Map<String, Long> mPackageSize) {
        this.mPackageSize = mPackageSize;
    }


    
    public Map<String, Long> getmPatchSize() {
        return mPatchSize;
    }


    
    public void setmPatchSize(Map<String, Long> mPatchSize) {
        this.mPatchSize = mPatchSize;
    }


    
    public Long getAllPackageSize() {
        return allPackageSize;
    }


    
    public void setAllPackageSize(Long allPackageSize) {
        if(allPackageSize !=null && allPackageSize.longValue() >0 ){
            this.allPackageSize = allPackageSize;
            return ;
        }
        long size = 0;
        if(this.mPackageSize != null){
            for (Map.Entry<String,Long> entry : mPackageSize.entrySet()) {
                size += entry.getValue();
            }
        }
        this.allPackageSize = size;
    }


    
    public Long getAllPatchSize() {
        return allPatchSize;
    }


    
    public void setAllPatchSize(Long allPatchSize) {
        if(allPatchSize !=null && allPatchSize.longValue() >0 ){
            this.allPatchSize = allPatchSize;
            return ;
        }
        long size = 0;
        if(this.mPatchSize != null){
            for (Map.Entry<String,Long> entry : mPatchSize.entrySet()) {
                size += entry.getValue();
            }
        }
        this.allPatchSize = size;
    }


    
    public List<String> getAllDownloadUrl() {
        return allDownloadUrl;
    }

    public void setAllDownloadUrl(List<String> allDownloadUrl) {
        if(allDownloadUrl !=null && allDownloadUrl.size() >0){
            this.allDownloadUrl = allDownloadUrl;
            return;
        }
        allDownloadUrl = new ArrayList<String>();
        if(this.mPackageUrlMap != null){
            allDownloadUrl.addAll(mPackageUrlMap.values());
        }
        if(this.mPatchUrlMap != null){
            allDownloadUrl.addAll(mPatchUrlMap.values());
        }
        this.allDownloadUrl = allDownloadUrl;
    }


    
    public Long getAllSize() {
        return allSize;
    }


    
    public void setAllSize(Long allSize) {
        if(allSize !=null && allSize.longValue() > 0){
            this.allSize = allSize;
        }
        if(allPackageSize != null && allPatchSize != null){
            this.allSize = this.getAllPackageSize()+this.getAllPatchSize();
        }else if(allPackageSize != null){
            this.allSize = this.getAllPackageSize();
        }else if(allPatchSize != null){
            this.allSize = this.getAllPatchSize();
        }else{
            this.allSize = Long.valueOf(0);
        }
        
    }
    
    /**
     * 通过bundle包名，获取指定bundle大小
     * @param bundleName
     * @return
     */
    public long getBundleSizeByName(String bundleName){
        if(StringUtil.isEmpty(bundleName)){
            return 0;
        }
        Map<String,Long> bundleMap = getmPackageSize();
        Map<String,Long> pathchMap = getmPatchSize();
        if(bundleMap != null && bundleMap.containsKey(bundleName)){
            Long size = bundleMap.get(bundleName);
            return size== null ? 0:size;
        }
        if(pathchMap != null && pathchMap.containsKey(bundleName)){
            Long size = pathchMap.get(bundleName);
            return size== null ? 0:size;
        }
        return 0;
    }
    
    /**
     * 通过下载url，获取指定bundle包名
     * @param url
     * @return
     */
    public String getBundleNameByURL(String url){
        if(StringUtil.isEmpty(url)){
            return "";
        }
        Map<String,String> packageMap = getmPackageUrlMap();
        Map<String,String> patchMap = getmPatchUrlMap();
        if(packageMap != null){
            for (String entry : packageMap.keySet()) {
                if(packageMap.get(entry).equals(url)){
                    return entry;
                }
            }
        }
        if(patchMap != null){
            for (String entry : patchMap.keySet()) {
                if(patchMap.get(entry).equals(url)){
                    return entry;
                }
            }
        }
        return "";
    }
    
    /**
     * 通过bundle包名，获取下载url
     * @param bundleName
     * @return
     */
    public String getBundleURLByName(String bundleName){
        if(StringUtil.isEmpty(bundleName)){
            return "";
        }
        Map<String,String> packageMap = getmPackageUrlMap();
        Map<String,String> patchMap = getmPatchUrlMap();
        if(packageMap != null){
            for (Map.Entry<String,String> entry : packageMap.entrySet()) {
                if(entry.getKey().equals(bundleName)){
                    return entry.getValue();
                }
            }
        }
        if(patchMap != null){
            for (Map.Entry<String,String> entry : patchMap.entrySet()) {
                if(entry.getKey().equals(bundleName)){
                    return entry.getValue();
                }
            }
        }
        return "";
    }
    
    /**
     * 通过bundle包名，获取MD5
     * @param bundleName
     * @return
     */
    public String getMD5ByName(String bundleName){
        if(StringUtil.isEmpty(bundleName)){
            return "";
        }
        Map<String,String> packageMD5 = this.getPackageMD5();
        Map<String,String> patchMD5 = this.getPatchMD5();
        if(packageMD5 != null){
            for (Map.Entry<String,String> entry : packageMD5.entrySet()) {
                if(entry.getKey().equals(bundleName)){
                    return entry.getValue();
                }
            }
        }
        if(patchMD5 != null){
            for (Map.Entry<String,String> entry : patchMD5.entrySet()) {
                if(entry.getKey().equals(bundleName)){
                    return entry.getValue();
                }
            }
        }
        return "";
    }
    

    
    public Map<String, String> getPackageMD5() {
        return packageMD5;
    }


    
    public void setPackageMD5(Map<String, String> packageMD5) {
        this.packageMD5 = packageMD5;
    }


    
    public Map<String, String> getPatchMD5() {
        return patchMD5;
    }


    
    public void setPatchMD5(Map<String, String> patchMD5) {
        this.patchMD5 = patchMD5;
    }


    
    /**
     * @return the packageURLWithPatch
     */
    public Map<String, String> getPackageURLWithPatch() {
        return packageURLWithPatch;
    }


    
    /**
     * @param packageURLWithPatch the packageURLWithPatch to set
     */
    public void setPackageURLWithPatch(Map<String, String> packageURLWithPatch) {
        this.packageURLWithPatch = packageURLWithPatch;
    }


    
    /**
     * @return the packageMD5WithPatch
     */
    public Map<String, String> getPackageMD5WithPatch() {
        return packageMD5WithPatch;
    }


    
    /**
     * @param packageMD5WithPatch the packageMD5WithPatch to set
     */
    public void setPackageMD5WithPatch(Map<String, String> packageMD5WithPatch) {
        this.packageMD5WithPatch = packageMD5WithPatch;
    }

}
