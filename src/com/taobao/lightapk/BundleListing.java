package com.taobao.lightapk;

import android.taobao.common.i.IMTOPDataObject;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

/**
 * Bundle清单信息
 * Created by guanjie on 14-9-15.
 */
public class BundleListing implements IMTOPDataObject{
    public static final int CLASS_TYPE_ACTIVITY = 1;
    public static final int CLASS_TYPE_SERVICE  = 2;

    private List<BundleInfo> bundles;

    public List<BundleInfo> getBundles() {
        return bundles;
    }

    public void setBundles(List<BundleInfo> bundles) {
        this.bundles = bundles;
    }

    /**
     * 根据类名查找bundle信息
     * @param className
     * @param type
     * @return
     */
    public BundleInfo resolveBundle(String className,int type){
        if(className==null){
            return null;
        }
        if(bundles!=null){
            for(BundleInfo info : bundles){
                if(info!=null && info.contains(className,type)){
                  return info;
                }
            }
        }
        return null;
    }

    public static class BundleInfo {
        private String name;
        private String pkgName;
		private String applicationName;
        private long size;
        private String version;
        private String desc;
        private String url;
        private String md5;
        private String host;
        private List<String> dependency;
        private List<String> activities;
        private List<String> services;
        private List<String> receivers;
        private List<String> contentProviders;
        private boolean hasSO;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }
        
        public String getApplicationName() {
			return applicationName;
		}

		public void setApplicationName(String applicationName) {
			this.applicationName = applicationName;
		}
		
        public List<String> getReceivers() {
            return receivers;
        }

        public void setReceivers(List<String> receivers) {
            this.receivers = receivers;
        }

        public List<String> getContentProviders() {
            return contentProviders;
        }

        public void setContentProviders(List<String> contentProviders) {
            this.contentProviders = contentProviders;
        }

        public boolean isHasSO() {
            return hasSO;
        }

        public void setHasSO(boolean hasSO) {
            this.hasSO = hasSO;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {

            Log.d("BundleListing","url = "+url);
            this.url = url;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPkgName() {
            return pkgName;
        }

        public void setPkgName(String pkgName) {
            this.pkgName = pkgName;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public List<String> getDependency() {
            return dependency;
        }

        public void setDependency(List<String> dependency) {
            if(dependency!=null && dependency.size()>0){
                for(int x=0; x<dependency.size();){
                    if(TextUtils.isEmpty(dependency.get(x))){
                        dependency.remove(x);
                    }else{
                        x++;
                    }
                }
            }
            this.dependency = dependency;
        }

        public List<String> getActivities() {
            return activities;
        }

        public void setActivities(List<String> activities) {
            this.activities = activities;
        }

        public List<String> getServices() {
            return services;
        }

        public void setServices(List<String> services) {
            this.services = services;
        }

        /**
         * 判断bundle内部是否包含该Activity或者service
         * @param className class名
         * @param type      类型
         * @return
         */
        public boolean contains(String className,int type){
            if(className==null){
                return false;
            }
            if(type==CLASS_TYPE_ACTIVITY && activities!=null){
                for(String name : activities){
                    if(name!=null && name.equals(className)){
                        return true;
                    }
                }
            }else if(type==CLASS_TYPE_SERVICE && services!=null){
                for(String name : services){
                    if(name!=null && name.equals(className)){
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
