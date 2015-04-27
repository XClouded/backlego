
package com.taobao.tao.update.business;

import mtopsdk.mtop.domain.BaseOutDo;


/**
 * app更新的反馈接口
 * 
 */
public class MtopTaobaoClientAppUpdateTrackResponse
    extends BaseOutDo
{

    /**
     * API业务对象
     * 
     */
    private MtopTaobaoClientAppUpdateTrackResponseData data;

    /**
     * API业务对象
     * 
     */
    public MtopTaobaoClientAppUpdateTrackResponseData getData() {
        return data;
    }

    /**
     * API业务对象
     * 
     */
    public void setData(MtopTaobaoClientAppUpdateTrackResponseData data) {
        this.data = data;
    }

}
