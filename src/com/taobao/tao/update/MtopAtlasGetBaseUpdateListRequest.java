
package com.taobao.tao.update;

import mtopsdk.mtop.domain.IMTOPDataObject;


/**
 * 增加BundleUpdate,当前仅主客使用，参数不变，返回会因bundle发布呈现bundleList.
 * 
 */
public class MtopAtlasGetBaseUpdateListRequest implements IMTOPDataObject {

    /**
     * API的名称
     * (Required)
     * 
     */
	public String API_NAME = "mtop.atlas.getBaseUpdateList";
    /**
     * API的版本号
     * (Required)
     * 
     */
	public String VERSION = "2.0";
    /**
     * API的签名方式
     * (Required)
     * 
     */
	public boolean NEED_ECODE = false;
    /**
     * 淘宝无线用户会话ID
     * (Required)
     * 
     */
	public boolean NEED_SESSION = false;
    /**
     * model
     * 
     */
	public String model = null;
    /**
     * androidVersion
     * (Required)
     * 
     */
	public String androidVersion = null;
    /**
     * netStatus
     * 
     */
	public long netStatus = 0L;
    /**
     * name
     * (Required)
     * 
     */
	public String name = null;
    /**
     * md5
     * 
     */
	public String md5 = null;
    /**
     * brand
     * 
     */
	public String brand = null;
    /**
     * group
     * (Required)
     * 
     */
	public String group = null;
    /**
     * city
     * 
     */
	public String city = null;
    /**
     * version
     * 
     */
	public String version = null;

    /**
     * API的名称
     * (Required)
     * 
     */
    public String getAPI_NAME() {
        return API_NAME;
    }

    /**
     * API的名称
     * (Required)
     * 
     */
    public void setAPI_NAME(String API_NAME) {
        this.API_NAME = API_NAME;
    }

    /**
     * API的版本号
     * (Required)
     * 
     */
    public String getVERSION() {
        return VERSION;
    }

    /**
     * API的版本号
     * (Required)
     * 
     */
    public void setVERSION(String VERSION) {
        this.VERSION = VERSION;
    }

    /**
     * API的签名方式
     * (Required)
     * 
     */
    public boolean isNEED_ECODE() {
        return NEED_ECODE;
    }

    /**
     * API的签名方式
     * (Required)
     * 
     */
    public void setNEED_ECODE(boolean NEED_ECODE) {
        this.NEED_ECODE = NEED_ECODE;
    }

    /**
     * 淘宝无线用户会话ID
     * (Required)
     * 
     */
    public boolean isNEED_SESSION() {
        return NEED_SESSION;
    }

    /**
     * 淘宝无线用户会话ID
     * (Required)
     * 
     */
    public void setNEED_SESSION(boolean NEED_SESSION) {
        this.NEED_SESSION = NEED_SESSION;
    }

    /**
     * model
     * 
     */
    public String getModel() {
        return model;
    }

    /**
     * model
     * 
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * androidVersion
     * (Required)
     * 
     */
    public String getAndroidVersion() {
        return androidVersion;
    }

    /**
     * androidVersion
     * (Required)
     * 
     */
    public void setAndroidVersion(String androidVersion) {
        this.androidVersion = androidVersion;
    }

    /**
     * netStatus
     * 
     */
    public long getNetStatus() {
        return netStatus;
    }

    /**
     * netStatus
     * 
     */
    public void setNetStatus(long netStatus) {
        this.netStatus = netStatus;
    }

    /**
     * name
     * (Required)
     * 
     */
    public String getName() {
        return name;
    }

    /**
     * name
     * (Required)
     * 
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * md5
     * 
     */
    public String getMd5() {
        return md5;
    }

    /**
     * md5
     * 
     */
    public void setMd5(String md5) {
        this.md5 = md5;
    }

    /**
     * brand
     * 
     */
    public String getBrand() {
        return brand;
    }

    /**
     * brand
     * 
     */
    public void setBrand(String brand) {
        this.brand = brand;
    }

    /**
     * group
     * (Required)
     * 
     */
    public String getGroup() {
        return group;
    }

    /**
     * group
     * (Required)
     * 
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * city
     * 
     */
    public String getCity() {
        return city;
    }

    /**
     * city
     * 
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * version
     * 
     */
    public String getVersion() {
        return version;
    }

    /**
     * version
     * 
     */
    public void setVersion(String version) {
        this.version = version;
    }

}
