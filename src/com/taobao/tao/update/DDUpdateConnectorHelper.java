package com.taobao.tao.update;

import android.taobao.apirequest.ApiResponse;
import android.taobao.util.TaoLog;
import android.text.TextUtils;
import com.taobao.update.UpdateInfo;
import com.taobao.update.bundle.BundleBaselineInfo;
import com.taobao.update.bundle.BundleUpdateInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * 只用来组装url，不用来解析返回数据。
 * @author luohou.lbb
 * 接口协议文档：
 * API1.0:http://dev.wireless.taobao.net/mediawiki/index.php?title=Mtop.atlas.getBaseUpdateList#.E8.BF.94.E5.9B.9E.E7.BB.93.E6.9E.9C.E6.A0.B7.E4.BE.8B
 * API2.0:http://dev.wireless.taobao.net/mediawiki/index.php?title=Mtop.atlas.getBaseUpdateList2.0
 *  atlas 动态部署更新ConnectorHelper
 *  2013.04.23
 */
public class DDUpdateConnectorHelper {

    //the client request
    private final String ANDROID_VERSION = "androidVersion";
    private final String VERSION = "version";
    private final String NET_STATUS = "netStatus";
    private final String GROUP = "group";
    private final String    BRAND               = "brand";
    private final String    MODEL               = "model";
    private final String    CITY                = "city";
    private final String CURRENT_VERSION_MD5 = "md5";
    
    
    private final String NAME = "name";
    
    
    //the server response
    
	//服务端返回代码转义
	
    // error code
    public static final int SERVICE_ERR = -1;
    
    private static String API_NAME = "mtop.atlas.getBaseUpdateList";
    
    private String mVersion ;
    private String mNetStatus ;
    private String mGroup;
    private String mClientName;
    private String mMd5;
    private String          mBrand;
    private String          mModel;
    private String          mCity;
    private String mApiVersion = "2.0";
    private String mAndroidVersion;
    
    
    /**
     * @author luohou.lbb
     * 所需资源由参数传入，消除对外部二方、三方模块依赖耦合
     * @param mainVersion
     * @param atlasVersion
     * @param netStatus
     * @param pluginList
     */
    public DDUpdateConnectorHelper(String version, String netStatus, String clientName, String group, String md5,
                                   String brand, String model, String city) {
        mVersion = version;
        mNetStatus = netStatus;
        mGroup = group;
        mMd5 = md5;
        mClientName = clientName;
        mBrand = brand;
        mModel = model;
        mCity = city;
    }

    public void setAPIVersion(String version){
        mApiVersion = version;
    }
    
    public void setAndroidVersion(String androidVersion){
        mAndroidVersion = androidVersion;
    }

//    public String getApiUrl() {
//        android.taobao.apirequest.TaoApiRequest request = new android.taobao.apirequest.TaoApiRequest();
//        
//        
//        
//        request.addParams("api",API_NAME);
//        request.addParams("v",mApiVersion);//520 bundle 更新版本由1.0换成2.0  added by leinuo
//        if (TextUtils.isEmpty(mAndroidVersion)){
//            mAndroidVersion = ""+android.os.Build.VERSION.SDK_INT;
//        }
//        request.addDataParam(ANDROID_VERSION, mAndroidVersion); 
//        request.addDataParam(NET_STATUS, mNetStatus);
//        request.addDataParam(GROUP, mGroup);
//        request.addDataParam(NAME, mClientName);
//        request.addDataParam(VERSION, mVersion);
//        request.addDataParam(CURRENT_VERSION_MD5, mMd5);
//        request.addDataParam(CITY, mCity);
//        request.addDataParam(BRAND, mBrand);
//        request.addDataParam(MODEL, mModel);
//        
//        
//        String requestS =  request.generalRequestUrl(GlobalApiBaseUrl.getApiBaseUrl());
//        TaoLog.Logi("DynamicDeploy", "Update request url="+requestS);
//        return requestS;
//    }
    
//    @Override
    public static Object syncPaser(byte[] all) {
    	if(all == null){
    		return null;
    	}else{
    		String str;
			try {
				str = new String(all,"UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return null;
			}
	        TaoLog.Logi("DDUpdateConnectorHelper", str);
	        if(TextUtils.isEmpty(str)){
	        	return null;
	        } 
	        ApiResponse response = new ApiResponse();
	        UpdateInfo updateInfo = new UpdateInfo();
			if(response.parseResult(str).success){
                parseResponse(response,str,updateInfo);
			}else{
				updateInfo.mErrCode = SERVICE_ERR;
			}
			return updateInfo;
	        
    	}

    }

    public static String buildUrl(String rawUrl){
        if(TextUtils.isEmpty(rawUrl)){
            return rawUrl;
        }
        if(!rawUrl.startsWith("http:")){
            rawUrl = String.format("%s%s","http:",rawUrl);
        }
        return rawUrl;
    }

    public static void parseResponse(ApiResponse response,String str,UpdateInfo updateInfo){

        try {
            BundleBaselineInfo baselineInfo= new BundleBaselineInfo();
            //json数据解析成功
            JSONObject jsObj = response.parseResult(str).data;
            boolean hasUpdate = false;
            if(jsObj.has("hasAvailableUpdate")){
                hasUpdate = "true".equals(jsObj.get("hasAvailableUpdate"));
            }
            if(jsObj.has("rollback")){
                updateInfo.rollback = jsObj.getString("rollback");
            }
            if (jsObj.has("remindNum")) {
                updateInfo.mRemindNum = jsObj.getInt("remindNum");
            }
            if (jsObj.has("baseVersion")) {
                baselineInfo.setmBaselineVersion(jsObj.getString("baseVersion"));
            }
            if(hasUpdate){
                if(jsObj.has("updateInfo")){
                    jsObj = jsObj.getJSONObject("updateInfo");
                    if(jsObj.has("url"))
                        updateInfo.mApkDLUrl = buildUrl(jsObj.getString("url"));
                    if(jsObj.has("patchUrl"))
                        updateInfo.mPatchDLUrl = buildUrl(jsObj.getString("patchUrl"));
                    if(jsObj.has("size"))
                        updateInfo.mApkSize = Long.parseLong(jsObj.getString("size"));
                    if(jsObj.has("patchSize"))
                        updateInfo.mPatchSize = Long.parseLong(jsObj.getString("patchSize"));
                    if(jsObj.has("md5"))
                        updateInfo.mNewApkMD5 = jsObj.getString("md5");
                    if(jsObj.has("pri"))
                        updateInfo.mPriority = Integer.parseInt(jsObj.getString("pri"));
                    if(jsObj.has("version"))
                        updateInfo.mVersion = jsObj.getString("version");
                    if(jsObj.has("info"))
                        updateInfo.mNotifyTip = jsObj.getString("info");
//							updateInfo.mPriority = 2;
                }else if(jsObj.has("bundleList")){
                    baselineInfo.setBundleUpdateList(parseBundleUpdate(jsObj.getJSONArray("bundleList"), updateInfo));
                    updateInfo.setBundleBaselineInfo(baselineInfo);
                }else{
                    updateInfo.mErrCode = SERVICE_ERR;
                }
            }
        } catch (JSONException e) {
            updateInfo.mErrCode = SERVICE_ERR;
            e.printStackTrace();
        }
    }
    
    /**
     * 解析bundle更新信息，组装至UpdateInfo类，返回给调用者，处理bundle更新 
     * @param bundleArray
     * @param updateInfo
     * @author leinuo
     * @return
     */
    private static List<BundleUpdateInfo> parseBundleUpdate(JSONArray bundleArray,UpdateInfo updateInfo){
        List<BundleUpdateInfo> bundleList = new ArrayList<BundleUpdateInfo>();
        BundleUpdateInfo bundleUpdateInfo ;
        try {
            if(bundleArray.length() > 0){
                for(int i=0;i<bundleArray.length();i++){
                    JSONObject jsObj = bundleArray.getJSONObject(i);
                    bundleUpdateInfo = new BundleUpdateInfo();

                    if(jsObj.has("name")){
                        bundleUpdateInfo.mBundleName = jsObj.getString("name");
                    }
                    if(jsObj.has("dependency")){
                        JSONArray jArray = jsObj.getJSONArray("dependency");
                        if(jArray.length() > 0){
                            List<String> depends = new ArrayList<String>(jArray.length());
                            for(int j = 0; j < jArray.length(); j++){
                                depends.add(jArray.getString(j));
                            }
                            bundleUpdateInfo.dependencies = depends;
                        }
                    }
                    if(jsObj.has("packageUrl")){
                        bundleUpdateInfo.mBundleDLUrl = buildUrl(jsObj.getString("packageUrl"));
                    }
                    if(jsObj.has("version")){
                        bundleUpdateInfo.mVersion = jsObj.getString("version");
                    }
                    if(jsObj.has("size")){
                        bundleUpdateInfo.mBundleSize = Long.parseLong(jsObj.getString("size"));
                    }
                    if(jsObj.has("md5")){
                        bundleUpdateInfo.mNewBundleMD5 = jsObj.getString("md5");
                    }
                    if(jsObj.has("pastMd5")){
                        bundleUpdateInfo.mlocalBundleMD5 = jsObj.getString("pastMd5");
                    }
                    if(jsObj.has("patchUrl")){
                        bundleUpdateInfo.mPatchDLUrl = buildUrl(jsObj.getString("patchUrl"));
                    }
                    if(jsObj.has("patchSize")){
                        bundleUpdateInfo.mPatchSize = Long.parseLong(jsObj.getString("patchSize"));
                    }
                    bundleList.add(bundleUpdateInfo);
                                    
                }
            }else{
                updateInfo.mErrCode = SERVICE_ERR;
            }
        } catch (JSONException e) {
            updateInfo.mErrCode = SERVICE_ERR;
            e.printStackTrace();
        }
        return bundleList;
    }


}
