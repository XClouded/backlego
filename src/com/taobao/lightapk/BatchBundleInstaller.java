package com.taobao.lightapk;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.taobao.atlas.framework.Atlas;
import android.taobao.atlas.framework.BundleImpl;
import android.text.TextUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量包安装器
 * Created by guanjie on 14-9-17.
 */
public class BatchBundleInstaller {
    public static final int PACKAGE_PARSE_ERROR = 1;
    public static final int PACKAGE_INSTALL_ERROR = 2;
    public static final int PACKAGE_INSTALL_SUCCESS = 0;

    private BatchBundleInstallerListener mBatchBundleInstallerListener;
    private Context mApplicationContext;
    private ArrayList<String> pkgList = new ArrayList<String>();
    public BatchBundleInstaller(Context applicationContext){
        mApplicationContext = applicationContext;
    }

    /**
     * 异步安装bundle
     */
    public void installBundleAsync(String bundleDirectory){
        if(bundleDirectory==null){
            return;
        }
        new BundleInstallTask().execute(bundleDirectory);
    }

    /**
     * 同步安装bundle
     */
    public void installBundleSync(String bundleDirectory){
        int resultCode = installBundles(bundleDirectory);
        notifyResult(resultCode);
    }

    public void setBatchBundleInstallerListener(BatchBundleInstallerListener listener){
        mBatchBundleInstallerListener = listener;
    }

    private int installBundles(String s){
        File bundles[] = new File(s).listFiles();
        for(File param : bundles){
            if(!TextUtils.isEmpty(param.getAbsolutePath()) && param.getName().indexOf("_")!=-1){
                PackageInfo info = mApplicationContext.getPackageManager().getPackageArchiveInfo(param.getAbsolutePath(),0);
                if(info!=null) {
                    try {
                        if(Atlas.getInstance().getBundle(info.packageName)!=null){
                            pkgList.add(info.packageName);
                            continue;
                        }
                        Bundle bundle = Atlas.getInstance().installBundle(info.packageName,param);
                        pkgList.add(info.packageName);
                        if(bundle!=null) {
                            BundleImpl bundleImpl = (BundleImpl)bundle;
                            if(!bundleImpl.getArchive().isDexOpted()){
                                bundleImpl.optDexFile();
                            }
                            Atlas.getInstance().enableComponent(info.packageName);
                        }else{
                            return PACKAGE_INSTALL_ERROR;
                        }
                    } catch (BundleException e) {
                        e.printStackTrace();
                        return PACKAGE_INSTALL_ERROR;
                    }
                }else{
                    return PACKAGE_PARSE_ERROR;
                }
            }
        }
        File file = new File(s);
        if(file.exists()){
            file.delete();
        }
        return PACKAGE_INSTALL_SUCCESS;
    }

    private void notifyResult(int resultCode){
        if(mBatchBundleInstallerListener!=null) {
            if (resultCode == PACKAGE_INSTALL_SUCCESS) {
                mBatchBundleInstallerListener.onInstallSuccess(pkgList);
            }else{
                mBatchBundleInstallerListener.onInstallFailed(resultCode);
            }
        }
    }

    /**
     * Bundle异步批量安装
     */
    public class BundleInstallTask extends AsyncTask<String,Integer,Integer>{
        @Override
        protected Integer doInBackground(String... params) {
            return installBundles(params[0]);
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            notifyResult(result.intValue());

        }
    }

    /**
     * 安装回调
     */
    public interface BatchBundleInstallerListener{
        public void onInstallSuccess(List<String> pkgList) ;
        public void onInstallFailed(int errorCode);
    }
}
