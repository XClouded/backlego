
package com.taobao.tao.update.business;

import mtopsdk.mtop.domain.BaseOutDo;


/**
 * 静默消息接口，手淘下载支付宝钱包成功，调用此接口发送消息给淘宝会员
 * 
 */
public class MtopTaobaoFerrariNoviceSendsilentMessageResponse
    extends BaseOutDo
{

    /**
     * API业务对象
     * 
     */
    private MtopTaobaoFerrariNoviceSendsilentMessageResponseData data;

    /**
     * API业务对象
     * 
     */
    public MtopTaobaoFerrariNoviceSendsilentMessageResponseData getData() {
        return data;
    }

    /**
     * API业务对象
     * 
     */
    public void setData(MtopTaobaoFerrariNoviceSendsilentMessageResponseData data) {
        this.data = data;
    }

}
