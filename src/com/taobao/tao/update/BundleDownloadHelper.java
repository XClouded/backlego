package com.taobao.tao.update;
import java.util.ArrayList;

import android.taobao.apirequest.ApiResponse;

import com.taobao.lightapk.dataobject.MtopTaobaoClientGetBundleListResponseData;
import com.taobao.lightapk.dataobject.MtopTaobaoClientGetBundleListResponseData.Item;
import com.taobao.update.UpdateInfo;
import com.taobao.update.bundle.BundleBaselineInfo;
import com.taobao.update.bundle.BundleUpdateInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BundleDownloadHelper {

	
	public static void parseResponse(ApiResponse response,String str,MtopTaobaoClientGetBundleListResponseData bundleListInfo){

	        try {
	        	MtopTaobaoClientGetBundleListResponseData bundleList = new MtopTaobaoClientGetBundleListResponseData();
	            //json数据解析成功
	            JSONObject jsObj = response.parseResult(str).data;
	            
	            if (jsObj.has("bundleList")) {
	            	parseBundleList(jsObj.getJSONArray("bundleList"), bundleListInfo);
	                }
	        } catch (JSONException e) {
	            
	            e.printStackTrace();
	        }
	 }
	 
	 private static void parseBundleList(JSONArray bundleArray,MtopTaobaoClientGetBundleListResponseData updateInfo){
	        ArrayList<MtopTaobaoClientGetBundleListResponseData.Item> bundleList = new ArrayList<MtopTaobaoClientGetBundleListResponseData.Item>();
	        MtopTaobaoClientGetBundleListResponseData.Item item ;
	        try {
	            if(bundleArray.length() > 0){
	                for(int i=0;i<bundleArray.length();i++){
	                    JSONObject jsObj = bundleArray.getJSONObject(i);
	                    item = new MtopTaobaoClientGetBundleListResponseData().new Item();
	                    if(jsObj.has("name")){
	                    	item.setName(jsObj.getString("name"));
	                    }
	                    if(jsObj.has("dependency")){
	                        JSONArray jArray = jsObj.getJSONArray("dependency");
	                        if(jArray.length() > 0){
	                            ArrayList<String> depends = new ArrayList<String>(jArray.length());
	                            for(int j = 0; j < jArray.length(); j++){
	                                depends.add(jArray.getString(j));
	                            }
	                            item.setDependency(depends);
	                        }
	                    }
	                    if(jsObj.has("packageUrl")){
	                    	item.setPackageUrl(jsObj.getString("packageUrl"));
	                    }
	                    if(jsObj.has("version")){
	                        item.setVersion(jsObj.getString("version"));
	                    }
	                    if(jsObj.has("title")){
	                        item.setTitle(jsObj.getString("title"));
	                    }
	                    if(jsObj.has("info")){
	                        item.setInfo(jsObj.getString("info"));
	                    }
	                    if(jsObj.has("type")){
	                        item.setType(Integer.parseInt(jsObj.getString("type")));
	                    }
	                    if(jsObj.has("size")){
	                        item.setSize(Long.parseLong(jsObj.getString("size")));
	                    }
	                    if(jsObj.has("md5")){
	                        item.setMd5(jsObj.getString("md5"));
	                    }
	                    bundleList.add(item);
	                                    
	                }
	            }
	        } catch (JSONException e) {
	            e.printStackTrace();
	        }
	        updateInfo.setBundleList(bundleList);
	    }
}
