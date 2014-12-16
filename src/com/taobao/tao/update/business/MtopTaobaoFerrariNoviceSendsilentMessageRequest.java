
package com.taobao.tao.update.business;

import mtopsdk.mtop.domain.IMTOPDataObject;


/**
 * 静默消息接口，手淘下载支付宝钱包成功，调用此接口发送消息给淘宝会员
 * 
 */
public class MtopTaobaoFerrariNoviceSendsilentMessageRequest
    implements IMTOPDataObject
{

    /**
     * API的名称
     * (Required)
     * 
     */
    private String API_NAME = "mtop.taobao.ferrari.novice.sendsilentMessage";
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
    private boolean NEED_ECODE = true;
    /**
     * 淘宝无线用户会话ID
     * (Required)
     * 
     */
    private boolean NEED_SESSION = true;

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

}
