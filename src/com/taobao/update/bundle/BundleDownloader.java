
package com.taobao.update.bundle;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StatFs;
import android.taobao.atlas.framework.Atlas;
import android.util.Log;
import com.alibaba.mtl.appmonitor.AppMonitor;
import com.taobao.tao.util.StringUtil;
import com.taobao.update.*;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * bundle下载
 * @author leinuo
 *
 */
public class BundleDownloader implements Downloader{

    private final int BUFFER_SIZE = 1024;
    private final long FOR_ENOUGH_SAPCE = 2*1024*1024;// 2M byte大小

    private static final String TEMP_SUFFIX = ".download";

    private OnDownloaderListener mListener;
    private Context mContext;
    private DownloadTask mDownloadTask;
    private BundleBaselineInfo mBaselineInfo;
    
    private final int DOWNLOAD_NOT_STARTED = 0;
    private final int DOWNLOAD_SUCCESS = 1;
    private final int DOWNLOAD_ERROR = 2;
    
    //无网络
  	public static final int ERROR_NO_NETWORK = -3;
  	//无sdcard
  	public static final int ERROR_NO_SD_CARD = -4;
  	//连接错误
  	public static final int ERROR_URL = -5;
  	//IO错误
  	public static final int ERROR_IO = -6;
  	//网络错误
  	public static final int ERROR_NETWORK = -7;
  	//未知异常
  	public static final int ERROR_UNKNOW = -10;
  	
    public BundleDownloader(Context context){
        mContext = context.getApplicationContext();
    }
    
    public void download(UpdateInfo info, String filePath, long allSize){
        mBaselineInfo = info.getBundleBaselineInfo();
        mDownloadTask = new DownloadTask(filePath,allSize);
        setListener(new BundleDownloaderListener());
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB)
    		mDownloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,info);
    	else
    		mDownloadTask.execute(info);
    }

    @Override
    public void download(String url, String filePath, long size) {
       
    }

    @Override
	public void setListener(OnDownloaderListener listener) {
		mListener = listener;
	}

	@Override
	public void cancelDownload() {
		
	}

    /**
     * bundle文件下载
     * 
     */
    class DownloadTask  extends AsyncTask<UpdateInfo, Void, Integer> {
    	private String mFileStorePath;	//文件存储路径
    	private long mAllFileSize;			//所有下载文件大小
    	private long mFileSize;         //单次下载文件大小
    	    	    	
    	private AndroidHttpClient mClient;
    	private HttpResponse mResponse;
        // bundle批量操作，异常时候资源释放使用（bundleBuffer，bundleChannel,bundleInput,bundleOutput）
        private BufferedInputStream bundleBuffer;
        private FileChannel bundleChannel;
        private InputStream bundleInput;
        private RandomAccessFile bundleOutput;
        private BundleBaselineInfo mBaselineInfo;
        
    	private boolean mPreCheck = false;
    	
    	public DownloadTask(String apkStorePath, long fileSize){
    		mFileStorePath = apkStorePath;
    		mAllFileSize = fileSize;
    	}
    	@Override
		protected void onPreExecute() {
    		//预校验，网络和存储空间
			super.onPreExecute();
			/*
	         * 检查网络
	         */
	        if (!isNetworkAvailable()) {
	        	if(mListener != null)
	        		mListener.onDownloadError(ERROR_NO_NETWORK, "网络异常，请稍后再试");
	        	return;
	        }

	        /*
	         * 检查存储空间
	         */
	        File storePath = new File(mFileStorePath);
	        if(!storePath.exists()){
                storePath.mkdirs();
            }
	        StatFs statfs = null;
	        try{
	        	statfs = new StatFs(storePath.getAbsolutePath());
	        }catch(Exception e){
	        	e.printStackTrace();
	        }
	        long totalSpace = 0;
	        if(statfs != null)
	        	totalSpace = (long)statfs.getAvailableBlocks()*statfs.getBlockSize();
	        if(totalSpace  < mAllFileSize+FOR_ENOUGH_SAPCE){
	        	if(mListener != null)
					mListener.onDownloadError(OnDownloaderListener.ERROR_NOT_ENOUGH_SPACE, OnDownloaderListener.ERROR_NOT_ENOUGH_SPACE_STR);
	        	return;
	        }

	        mPreCheck = true;
		}

		@Override
	    protected Integer doInBackground(UpdateInfo... params) {
			
			if(!mPreCheck)
				return DOWNLOAD_NOT_STARTED;
			// bundle更新文件保存路径
            File updateFilePath = new File(mFileStorePath);
            mBaselineInfo = params[0].getBundleBaselineInfo();
            if(mBaselineInfo == null){
                return DOWNLOAD_NOT_STARTED;
            }
            List<String> urlList = mBaselineInfo.getAllDownloadUrl();
            if(urlList.isEmpty()){
                return DOWNLOAD_NOT_STARTED;
            }
            Log.d("DefaultDownloader", "开始下载更新的bundle。。。");
            List<HttpGet> httpGetList = downloadResume(urlList,updateFilePath,mBaselineInfo);
            if(httpGetList !=null && !httpGetList.isEmpty()){
                for(HttpGet httpGet:httpGetList){
                    downloadFile(mBaselineInfo,httpGet);
                }
            }else{
                for(String strUrl:urlList){
                    downloadFile(mBaselineInfo, new HttpGet(strUrl));
                }
                
            }
            
            if(updatedBundleCheck()){
                releaseResources();
                return DOWNLOAD_SUCCESS;
            }else{
                releaseResources();
                return DOWNLOAD_ERROR;
            }
		}
		
		@Override
        protected void onPostExecute(Integer result) {
		    super.onPostExecute(result);
		    if(result != DOWNLOAD_NOT_STARTED){
		        if( result == DOWNLOAD_SUCCESS){
		            if (mListener != null) {
		                mListener.onDownloadFinsh(mFileStorePath);
		            } 
		        }else{
		            if (mListener != null) {
		                switch(result){
                            case ERROR_URL:
                                mListener.onDownloadError(ERROR_URL,"url错误");
                                break;
                            case ERROR_IO:
                                mListener.onDownloadError(ERROR_IO,"文件读写错误");
                                break;
                            case ERROR_NETWORK:
                                 mListener.onDownloadError(ERROR_NETWORK,"网络错误");
                                 break;
                            case DOWNLOAD_ERROR:
                                mListener.onDownloadError(DOWNLOAD_ERROR,"bundle下载出错");
                                break;
                            default :
                                mListener.onDownloadError(ERROR_UNKNOW,"未知错误");
                        }
                    }
		        }
		    }
		    
		}
		private void releaseResources(){
		    if(mClient != null){
		        mClient.close();
		        mClient = null;
		    }
		    if(bundleBuffer != null){
		        try {
                    bundleBuffer.close();
                    bundleBuffer = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
		    }
		    if(bundleChannel != null){
		        try {
                    bundleChannel.close();
                    bundleChannel = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
		    if(bundleOutput != null){
		        try {
		            bundleOutput.close();
		            bundleOutput = null;
                } catch (IOException e) {
                    e.printStackTrace();
                } 
		    }
		    if(bundleInput != null){
                try {
                    bundleInput.close();
                    bundleInput = null;
                } catch (IOException e) {
                    e.printStackTrace();
                } 
            }
		}    	
		private List<HttpGet> downloadResume(List<String> urlList,File fileStorePath,BundleBaselineInfo baselineInfo){
	        
	        File[] fileArray = fileStorePath.listFiles();
	        if(fileArray==null || fileArray.length==0 || fileArray.length > urlList.size()){
	            return null;
	        }
	        List<HttpGet> httpGetList = new ArrayList<HttpGet>();
	        int suffixIndex = -1;
	        File suffixFile = null;
	        for(File file:fileArray){
	            suffixFile = file;
	            suffixIndex = suffixFile.getName().indexOf(TEMP_SUFFIX);
	            if(suffixIndex > 0){
	                long prviousFileSize = 0;
	                //断点续传
	                prviousFileSize = suffixFile.length();
	                long tempSize = baselineInfo.getBundleSizeByName(suffixFile.getName().substring(0, suffixIndex));
	                if(prviousFileSize > tempSize){
	                    //错误文件
	                    suffixFile.delete();
	                    prviousFileSize = 0;
	                }else if(prviousFileSize < tempSize){
	                    String url = baselineInfo.getBundleURLByName(suffixFile.getName().substring(0, suffixIndex));
	                    HttpGet get = new HttpGet(url);
	                    get.addHeader("Range", "bytes=" + prviousFileSize + "-");
	                    httpGetList.add(get);
	                    urlList.remove(url);
	                }
	            }else{
	                String bundleName = file.getName();
	                String bundleUrl = baselineInfo.getBundleURLByName(bundleName);
	                urlList.remove(bundleUrl);
	            }
         
	        }
	        for(String url:urlList){
                httpGetList.add(new HttpGet(url));
            }
	        return httpGetList;
	    }
		
		private boolean downloadFile(BundleBaselineInfo baselineInfo,HttpGet httpGet){
            
            String bundleName = "";
            try {
                bundleName = baselineInfo.getBundleNameByURL(httpGet.getURI().toURL().toString());
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
                return false;
            }
            if(StringUtil.isEmpty(bundleName)){
                return false;
            }
            mFileSize = baselineInfo.getBundleSizeByName(bundleName);
            File apkfile = new File(mFileStorePath, bundleName);
            File tempFile = new File(mFileStorePath, bundleName + TEMP_SUFFIX);
            if(apkfile.exists() && apkfile.length()==mFileSize){
                return true;
            }
            //建立网络连接
            mClient = AndroidHttpClient.newInstance("BundleDownloader");
            try {
                mResponse = mClient.execute(httpGet);
                bundleInput = mResponse.getEntity().getContent();
                bundleOutput = new RandomAccessFile(tempFile, "rw");
                byte[] buffer = new byte[BUFFER_SIZE];
                bundleBuffer = new BufferedInputStream(bundleInput, BUFFER_SIZE);
                bundleChannel = null;
                int length = 0;
                bundleChannel = bundleOutput.getChannel();
                bundleChannel.position(bundleOutput.length());
                while( (length = bundleBuffer.read(buffer, 0, BUFFER_SIZE)) > 0){
                    bundleChannel.write(ByteBuffer.wrap(buffer,0,length));
                }
                
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }catch (IllegalStateException e) {
                e.printStackTrace();
                return false;
            }
            if(tempFile.exists() && tempFile.length()==mFileSize){
                tempFile.renameTo(apkfile);
            }
            if(apkfile.exists() && apkfile.length() != mFileSize){
                apkfile.delete();
            }
            return true;
        }
		    
	    private boolean updatedBundleCheck(){
	        File bundleFiles = new File(mFileStorePath);
	        if(!bundleFiles.exists()){
	            return false;
	        }
	        long sumLength = 0;
	        boolean isSameMD5 = false;
	        Map<String,String> packageMD5Map = mBaselineInfo.getPackageMD5();
	        Map<String,String> patchMD5Map = mBaselineInfo.getPatchMD5();
	        File[] fileArray = bundleFiles.listFiles();
	        for(File file:fileArray){
	            String bundleName = file.getName();
	            long fileLength = file.length();
	            sumLength += fileLength;
	            if(packageMD5Map!=null && packageMD5Map.containsKey(bundleName)){
	                String md5 = UpdateUtils.getMD5(file.getAbsolutePath());
	                boolean isSame = packageMD5Map.get(bundleName).equals(md5);
	                if(isSame){
	                    isSameMD5 = true;
	                }else{
	                    UpdateUserTrack.bundleUpdateTrack("BundleDownloader","bundle全量下载失败MD5不一致(for CDN)："+getHeadInfoForCDN(bundleName,md5,null));
	                    file.delete();
	                    isSameMD5 = downloadFile(mBaselineInfo,new HttpGet(mBaselineInfo.getBundleURLByName(bundleName)));
	                }
	                if(fileLength != mBaselineInfo.getBundleSizeByName(bundleName)){
	                    UpdateUserTrack.bundleUpdateTrack("BundleDownloader","bundle全量下载失败文件大小不一致(for CDN)："+getHeadInfoForCDN(bundleName,md5,fileLength));
	                }
	            }
	            if(patchMD5Map!=null && patchMD5Map.containsKey(bundleName)){
	                String md5 = UpdateUtils.getMD5(Atlas.getInstance().getBundleFile(bundleName).getAbsolutePath());
	                boolean isSame = patchMD5Map.get(bundleName).equals(md5);
                    if(isSame){
                        isSameMD5 = true;
                    }else{ 
                        UpdateUserTrack.bundleUpdateTrack("BundleDownloader","bundle差量下载失败MD5错误(for CDN)："+getHeadInfoForCDN(bundleName,md5,null));
                        file.delete();
                        patchMD5Map.remove(bundleName);
                        if(packageMD5Map == null){
                            packageMD5Map = new HashMap<String,String>();
                            mBaselineInfo.setPackageMD5(packageMD5Map);
                        }
                        packageMD5Map.put(bundleName, mBaselineInfo.getPackageMD5WithPatch().get(bundleName));
                        isSameMD5 = downloadFile(mBaselineInfo,new HttpGet(mBaselineInfo.getPackageURLWithPatch().get(bundleName)));
                    }
                    if(fileLength != mBaselineInfo.getBundleSizeByName(bundleName)){
                        UpdateUserTrack.bundleUpdateTrack("BundleDownloader","bundle差量下载失败大小不一致(for CDN)："+getHeadInfoForCDN(bundleName,md5,fileLength));
                    }
                }
	        }
	        if(sumLength==mAllFileSize && isSameMD5){
	            return true;
	        }else{
	            String detailMessage = "bundle安装前检查没有通过:MD5是否一致"+isSameMD5+"大小是否一致(下载文件大小："+sumLength+" 服务端传值大小："+mAllFileSize+")";
	            UpdateUserTrack.bundleUpdateTrack("BundleDownloader","bundle安装前检查失败(检查项：bundle大小和MD5)"+detailMessage);
	            return false;
	        }
	    }
	    	    
	    /**
	     * CDN下载大小、MD5不对，错误跟踪信息
	     * @param bundleName
	     * @param errorMD5
	     * @param fileLength
	     * @return
	     */
	    private String getHeadInfoForCDN(String bundleName,String errorMD5,Long fileLength){
            if(mResponse ==null){
                return "HttpResponse is empty!";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(" statusCode-->"+mResponse.getStatusLine().getStatusCode());
            sb.append(" Via-->");
            StringBuilder viaInfo = new StringBuilder();
            boolean isVia = mResponse.containsHeader("Via");
            if(isVia){
                Header[] viaHeaders = mResponse.getHeaders("Via");
                for(Header header:viaHeaders){
                    viaInfo.append(header.getValue());
                    viaInfo.append("||");
                }
                sb.append(viaInfo.toString());
            }else{
                sb.append("is empty!");
            }
            sb.append(" URL-->"+mBaselineInfo.getBundleURLByName(bundleName));
            sb.append(" MD5 Server-->"+mBaselineInfo.getMD5ByName(bundleName));
            sb.append(" MD5 File-->"+errorMD5);
            if(fileLength != null){
                sb.append(" Size Server-->"+mBaselineInfo.getBundleSizeByName(bundleName));
                sb.append(" Size File-->"+fileLength);
            }
            Log.d("CDN BUG TRACEINFO:", sb.toString());
            return sb.toString();
           
        }
    }
    
	private boolean isNetworkAvailable() {
		ConnectivityManager connectivity = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
			return false;
		} else {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++) {
					if (info[i].getState() == NetworkInfo.State.CONNECTED
							|| info[i].getState() == NetworkInfo.State.CONNECTING) {
						return true;
					}
				}
			}
		}
		return false;
	}
	class BundleDownloaderListener  implements OnDownloaderListener{
        @Override
        public void onDownloadProgress(int process) {
        }

        @Override
        public void onDownloadError(int errorCode, String msg) {
            Log.d("BundleDownloader", "下载出错了。。。"+msg);
            UpdateUserTrack.bundleUpdateTrack("BundleDownloader","bundle下载出错"+msg);
        }

        @Override
        public void onDownloadFinsh(String apkPath) {
            Log.d("BundleDownloader", "下载成功，开始安装(安装路径："+apkPath+")。。。");
            AppMonitor.Counter.commit("dynamicDeploy", "bundleDownloaded",mBaselineInfo.getmBaselineVersion(), 1);
            UpdateUserTrack.bundleUpdateTrack("BundleDownloader","bundle下载成功!");
            BundleInstaller installer = new BundleInstaller(mBaselineInfo,apkPath);
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB)
                installer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                installer.execute();
        }
    }
}
