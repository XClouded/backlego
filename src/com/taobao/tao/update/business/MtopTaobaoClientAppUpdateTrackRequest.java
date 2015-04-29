
package com.taobao.tao.update.business;

import mtopsdk.mtop.domain.IMTOPDataObject;


/**
 * app更新的反馈接口
 * 
 */
public class MtopTaobaoClientAppUpdateTrackRequest
    implements IMTOPDataObject
{

    /**
     * API的名称
     * (Required)
     * 
     */
    private String API_NAME = "mtop.taobao.client.appUpdateTrack";
    /**
     * API的版本号
     * (Required)
     * 
     */
    private String VERSION = "1.0";
    /**
     * API的签名方式
     * (Required)
     * 
     */
    private boolean NEED_ECODE = false;
    /**
     * 淘宝无线用户会话ID
     * (Required)
     * 
     */
    private boolean NEED_SESSION = false;
    /**
     * appGroup
     * (Required)
     * 
     */
    private String appGroup = null;
    /**
     * curVersion
     * (Required)
     * 
     */
    private String curVersion = null;
    /**
     * newVersion
     * (Required)
     * 
     */
    private String newVersion = null;
    /**
     * updateStep
     * (Required)
     * 
     */
    private long updateStep = 0L;
    /**
     * updateModel
     * 
     */
    private long updateModel = 0L;

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
     * appGroup
     * (Required)
     * 
     */
    public String getAppGroup() {
        return appGroup;
    }

    /**
     * appGroup
     * (Required)
     * 
     */
    public void setAppGroup(String appGroup) {
        this.appGroup = appGroup;
    }

    /**
     * curVersion
     * (Required)
     * 
     */
    public String getCurVersion() {
        return curVersion;
    }

    /**
     * curVersion
     * (Required)
     * 
     */
    public void setCurVersion(String curVersion) {
        this.curVersion = curVersion;
    }

    /**
     * newVersion
     * (Required)
     * 
     */
    public String getNewVersion() {
        return newVersion;
    }

    /**
     * newVersion
     * (Required)
     * 
     */
    public void setNewVersion(String newVersion) {
        this.newVersion = newVersion;
    }

    /**
     * updateStep
     * (Required)
     * 
     */
    public long getUpdateStep() {
        return updateStep;
    }

    /**
     * updateStep
     * (Required)
     * 
     */
    public void setUpdateStep(long updateStep) {
        this.updateStep = updateStep;
    }

    /**
     * updateModel
     * 
     */
    public long getUpdateModel() {
        return updateModel;
    }

    /**
     * updateModel
     * 
     */
    public void setUpdateModel(long updateModel) {
        this.updateModel = updateModel;
    }

}
