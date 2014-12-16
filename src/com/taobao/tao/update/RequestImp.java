package com.taobao.tao.update;

import android.os.Build;
import android.taobao.utconfig.ConfigCenterLifecycleObserver;
import android.taobao.util.NetWork;
import android.text.TextUtils;
import android.util.Log;
import com.taobao.passivelocation.aidl.LocationDTO;
import com.taobao.passivelocation.aidl.LocationServiceManager;
import com.taobao.tao.Globals;
import com.taobao.tao.TaoApplication;
import com.taobao.tao.remotebusiness.RemoteBusiness;
import com.taobao.update.UpdateInfo;
import com.taobao.update.UpdateRequest;
import com.taobao.wswitch.api.business.ConfigContainerAdapter;
import mtopsdk.mtop.domain.MtopResponse;

/**
 * 联网请求及协议处理
 * 
 * @author luohou.lbb
 * 
 */
public class RequestImp implements UpdateRequest {
    private String mRequestApiVersion = "2.0";
    private String mUpdateGroupId = "taobao4android";
    private String mAndroidVersion;
    public RequestImp(){
        mRequestApiVersion = "2.0";
        if (Globals.getApplication().getPackageName().equals("com.taobao.alpha")){
        	mUpdateGroupId = "taobao4androidalpha";
        } else {
            if(!Globals.isMiniPackage()) {
                mUpdateGroupId = "taobao4android";
            }else{
                mUpdateGroupId = ConfigContainerAdapter.getInstance().getConfig(
                        ConfigCenterLifecycleObserver.CONFIG_GROUP_SYSTEM, "minitao_update_group", "minitao4android");
                if(TextUtils.isEmpty(mUpdateGroupId)){
                    mUpdateGroupId = "minitao4android";
                }
            }
        }
    }
    public RequestImp(String version,String group){
        this(version, group, null);
    }
    
    public RequestImp(String version,String group, String androidVersion){
        mRequestApiVersion = version;
        mUpdateGroupId = group;
        mAndroidVersion = androidVersion;
    }

	@Override
	public UpdateInfo request(String appName, String version, String appMd5) {
        return request(appName,version,appMd5,mRequestApiVersion);
	}

//    public UpdateInfo request(String appName, String version, String appMd5,String apiVersion, MtopListener listener){
//        String netStates = NetWork.CONN_TYPE_WIFI.equals(NetWork.getNetConnType(Globals.getApplication()))? "10" : "1";
//        String model = Build.MODEL;
//        String brand = Build.BRAND;
//        String city = "";
//
//        LocationDTO dto = LocationServiceManager.getCachedLocation();
//        city = (dto!=null && dto.getCityName()!=null) ? dto.getCityName() : "";
//
//        DDUpdateConnectorHelper helper = new DDUpdateConnectorHelper(version, netStates,
//                appName,
//                mUpdateGroupId,
//                appMd5, brand, model,
//                city);
//        helper.setAPIVersion(apiVersion);
//        helper.setAndroidVersion(mAndroidVersion);
//        return (UpdateInfo) ApiRequestMgr.getInstance().syncConnect(helper, null);
//
//    }
    
    public UpdateInfo request(String appName, String version, String appMd5,String apiVersion) {
    	long netStates = NetWork.CONN_TYPE_WIFI.equals(NetWork.getNetConnType(Globals.getApplication()))? 10L : 1L;
        String model = Build.MODEL;
        String brand = Build.BRAND;
        String city = "";

        LocationDTO dto = LocationServiceManager.getCachedLocation();
        city = (dto!=null && dto.getCityName()!=null) ? dto.getCityName() : "";
        MtopAtlasGetBaseUpdateListRequest request = new MtopAtlasGetBaseUpdateListRequest();
        request.setVersion(version);
        request.setNetStatus(netStates);
        request.setName(appName);
        request.setGroup(mUpdateGroupId);
        request.setMd5(appMd5);
        request.setBrand(brand);
        request.setModel(model);
        request.setCity(city);
        request.setVERSION(apiVersion);
        if (TextUtils.isEmpty(mAndroidVersion)){
            mAndroidVersion = ""+ Build.VERSION.SDK_INT;
        }
        request.setAndroidVersion(mAndroidVersion);
        Log.i("TaosdkToMtop", "RequestImp appName : " + appName + " version:" + version + " netStates:" + netStates + " mUpdateGroupId : " + mUpdateGroupId +
        		" brand : " + brand + " model : " + model + " city : " + city + " apiVersion : " + apiVersion + " mAndroidVersion : " + mAndroidVersion);
        RemoteBusiness business = RemoteBusiness.build(request, TaoApplication.getTTID());
//        business.addListener(listener);
//        business.startRequest();
        MtopResponse response = business.syncRequest();
        UpdateInfo result = null;
        if(response != null)
        	result = (UpdateInfo) DDUpdateConnectorHelper.syncPaser(response.getBytedata());
        return result;
    }
    
}
