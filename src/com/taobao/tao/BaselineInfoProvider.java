package com.taobao.tao;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.taobao.tao.Globals;

public class BaselineInfoProvider {
    /**
     * bundle更新对应的基线版本，仅供全局初始化使用
     */
    private String baselineVersion = "";
    /**
     * bundle更新对应的versionName
     */
    private String mainVersionName = "";
    
    /**
     * bundle更新对应的versionCode
     */
    private int mainVersionCode = 0;
    /**
     * 格式：包名@版本@大小；。。。
     * 扩展信息，暂无使用
     */
    private String bundleList = "";
    
    private static BaselineInfoProvider instance;
    private BaselineInfoProvider(){        
    }
    public static synchronized BaselineInfoProvider getInstance() {
        if (instance == null) {
            instance = new BaselineInfoProvider();
            instance.initBaselineInfo();
        }
        //instance.initBaselineInfo();
        return instance;
    }
    public void initBaselineInfo(){
    	File fileDir = Globals.getApplication().getFilesDir();
    	if(fileDir ==null){
    		fileDir = Globals.getApplication().getFilesDir();
    	}
        String path = fileDir.getAbsolutePath()+File.separatorChar+"bundleBaseline"+File.separatorChar;
        File baseinfoFile = new File(path,"baselineInfo");
        if(baseinfoFile.exists()){
            try {
                DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(baseinfoFile)));
                mainVersionName = input.readUTF();
                mainVersionCode = input.readInt();
                baselineVersion = input.readUTF();
                bundleList = input.readUTF();
                input.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * @return the baselineVersion
     */
    public String getBaselineVersion() {
        return baselineVersion;
    }
            
    /**
     * @return the bundleList
     */
    public String getBundleList() {
        return bundleList;
    }
    
    /**
     * @return the mainVersionCode
     */
    public int getMainVersionCode() {
        return mainVersionCode;
    }
    
    /**
     * @return the mainVersionName
     */
    public String getMainVersionName() {
        return mainVersionName;
    }

    public List<String> getLastDynamicDeployBunldes(){
        if(bundleList!=null){
            String[] bundles = bundleList.split(";");
            if(bundles!=null){
                List<String> bundleNameList = new ArrayList<String>(bundles.length);
                for(String bundleInfo : bundles){
                    String[] infoItems = bundleInfo.split("@");
                    if(infoItems!=null && infoItems[0]!=null){
                        bundleNameList.add(infoItems[0]);
                    }
                }
                return bundleNameList;
            }
        }
        return null;
    }
}
