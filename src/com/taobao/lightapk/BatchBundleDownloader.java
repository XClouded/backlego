package com.taobao.lightapk;

import android.content.Context;
import android.os.StatFs;
import com.taobao.update.DefaultDownloader;
import com.taobao.update.Downloader;
import com.taobao.update.UpdateUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 批量下载文件，目前仅支持要么全部下载成功，要么下载失败，暂不支持部分下载成功的情况
 * Created by guanjie on 14-9-16.
 */
public class BatchBundleDownloader implements Downloader.OnDownloaderListener {
    private final int FOR_ENOUGH_SPACE = 5*1024*1024;
    private DefaultDownloader mDownloader;
    private Context mContext;
    private String mFileStorePath = "";
    private int mCurrentDownloadingIndex = 0;
    private int mMaxRetryCount = 1;
    private int mCurrentRetryCount = 1;
    private List<BatchDownloadListener> mListeners = new ArrayList<BatchDownloadListener>();
    private int mCurrentPercent;
    private long mTotalSize = 0;
    private DownloadItem[] mDownloadItems;
    private static HashMap<String,BatchBundleDownloader> mSRunningBundleDownloaders = new HashMap<String, BatchBundleDownloader>();
    private boolean isRunning = false;
    private String  mBundlePkgToDownload;

    protected BatchBundleDownloader(Context context,String pkg){
        mContext = context;
        File file = new File(mContext.getFilesDir().toString(), File.separator + "com/taobao/lightapk" +File.separator+pkg);
        if(!file.exists()){
            file.mkdirs();
        }
        mBundlePkgToDownload = pkg;
        mFileStorePath = file.getAbsolutePath();
    }

    public boolean isRunning(){
        return isRunning;
    }

    public static BatchBundleDownloader obtainBatchBundleDownloader(Context context,String pkg){
        BatchBundleDownloader downloader = mSRunningBundleDownloaders.get(pkg);
        return downloader==null ? new BatchBundleDownloader(context,pkg.replace(".","_")) : downloader;
    }

    private void addRunningDownloaders(String pkg,BatchBundleDownloader loader){
        mSRunningBundleDownloaders.put(pkg,loader);
    }

    private void removeRunningDownloaders(String pkg){
        mSRunningBundleDownloaders.remove(pkg);
    }

    /**
     * 设置文件下载目录
     * @param path
     */
    public void setApkStorePath(String path){
        mFileStorePath = path;
    }

    /**
     * 设置下载回调的监听
     * @param listener
     */
    public void addDownloadListener(BatchDownloadListener listener){

        mListeners.add(listener);
    }

    /**
     * 每个item均有同样的重试次数，只要某个bundle达到重试次数切没有成功，则下载失败
     * @param count
     */
    public void setRetryCount(int count){
        mMaxRetryCount = count;
    }

    /**
     * 取消下载(文件不删除)
     */
    public void cancel(){
        if(mDownloader!=null){
            mDownloader.cancelDownload();
            isRunning = false;
            removeRunningDownloaders(mBundlePkgToDownload);
        }
    }

    /**
     * 开始批量下载
     * @param items
     */
    public void startBatchDownload(DownloadItem[] items){
        if(items==null || items.length==0){
            return;
        }
        mDownloadItems = items;
        for(DownloadItem item : items){
            mTotalSize += item.size;
        }
        /**
         * 空间大小校验
         */
        File storePath = new File(mFileStorePath);
        StatFs statfs = null;
        try{
            statfs = new StatFs(storePath.getAbsolutePath());
        }catch(Exception e){
            e.printStackTrace();
        }
        long totalSpace = 0;
        if(statfs != null)
            totalSpace = (long)statfs.getAvailableBlocks()*statfs.getBlockSize();

        totalSpace -= mTotalSize+FOR_ENOUGH_SPACE;
        if(totalSpace<0){
            if(mListeners!=null){
                for(BatchDownloadListener listener : mListeners) {
                    listener.onDownloadError(Downloader.OnDownloaderListener.ERROR_NOT_ENOUGH_SPACE
                            , Downloader.OnDownloaderListener.ERROR_NOT_ENOUGH_SPACE_STR);
                }
            }
        }else {
            mCurrentDownloadingIndex = 0;
            if (mDownloader == null) {
                mDownloader = new DefaultDownloader(mContext);
                mDownloader.setListener(this);
            }
            mDownloader.download(items[0].getDownloadUrl(), mFileStorePath, items[0].getSize());
            isRunning = true;
            addRunningDownloaders(mBundlePkgToDownload,this);
        }
    }

    @Override
    public void onDownloadProgress(int i) {
        if(mListeners!=null){
            long downloadedSize = 0;
            for(int x=0;x<mCurrentDownloadingIndex;x++){
                downloadedSize+=mDownloadItems[x].size;
            }
            float percent = ((float)(downloadedSize+mDownloadItems[mCurrentDownloadingIndex].size/100*i))/mTotalSize;
            for(BatchDownloadListener listener : mListeners) {
                listener.onDownloadProgress((int) (percent * 100));
            }
        }

    }

    @Override
    public void onDownloadError(int i, String s) {
        if(mCurrentRetryCount>0){
            /**
             * 重试该item下载
             */
            --mCurrentRetryCount;
            mDownloader.download(mDownloadItems[mCurrentDownloadingIndex].getDownloadUrl(), mFileStorePath, mDownloadItems[mCurrentDownloadingIndex].getSize());
        }else {
            if (mListeners != null) {
                for(BatchDownloadListener listener : mListeners) {
                    listener.onDownloadError(i, s);
                }
                isRunning = false;
            }
            removeRunningDownloaders(mBundlePkgToDownload);
        }
    }

    @Override
    public void onDownloadFinsh(String s) {
        if(s!=null){
            String md5 = UpdateUtils.getMD5(s);
            if(md5!=null && md5.equals(mDownloadItems[mCurrentDownloadingIndex].getMd5())){
                File bundleFile = new File(s);
                File file = new File(s.substring(0,s.lastIndexOf(File.separator))+File.separator+mDownloadItems[mCurrentDownloadingIndex].getPkg().replace(".","_"));
                bundleFile.renameTo(file);
                if(mCurrentDownloadingIndex==mDownloadItems.length-1){
                    /**
                     * 通知批量下载完成
                     */
                    for(BatchDownloadListener listener : mListeners) {
                        listener.onDownloadFinish(mFileStorePath);
                    }
                    isRunning = false;
                    removeRunningDownloaders(mBundlePkgToDownload);
                    return;
                }else {
                    /**
                     * 继续下载下一个
                     */
                    mCurrentDownloadingIndex++;
                    mCurrentRetryCount = mMaxRetryCount;
                    mDownloader.download(mDownloadItems[mCurrentDownloadingIndex].getDownloadUrl(), mFileStorePath, mDownloadItems[mCurrentDownloadingIndex].getSize());
                    return;
                }
            }else{
                try {
                    new File(s).delete();
                }catch(Throwable e){
                    e.printStackTrace();
                }
            }
        }
        if(mCurrentRetryCount>0){
            /**
             * 重试该item的下载
             */
            --mCurrentRetryCount;
            mDownloader.download(mDownloadItems[mCurrentDownloadingIndex].getDownloadUrl(), mFileStorePath, mDownloadItems[mCurrentDownloadingIndex].getSize());
        }else{
            isRunning = false;
            removeRunningDownloaders(mBundlePkgToDownload);
            /**
             * 通知批量下载失败
             */
            for(BatchDownloadListener listener : mListeners) {
                listener.onDownloadError(-1,"文件校验失败");
            }
        }
    }

    public static class DownloadItem {
        private String downloadUrl;
        private long size;
        private String md5;
        private String pkg;

        public String getPkg() {
            return pkg;
        }

        public void setPkg(String pkg) {
            this.pkg = pkg;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public void setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public interface BatchDownloadListener{
        public void onDownloadProgress(int i);
        public void onDownloadError(int i, String s);
        public void onDownloadFinish(String s);
    }
}
