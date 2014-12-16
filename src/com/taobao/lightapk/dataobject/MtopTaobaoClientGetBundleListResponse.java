
package com.taobao.lightapk.dataobject;

import mtopsdk.mtop.domain.BaseOutDo;


/**
 * CLIENT ATLAS
 * 
 */
public class MtopTaobaoClientGetBundleListResponse
    extends BaseOutDo
{

    /**
     * API业务对象
     * 
     */
    private MtopTaobaoClientGetBundleListResponseData data;

    /**
     * API业务对象
     * 
     */
    public MtopTaobaoClientGetBundleListResponseData getData() {
        return data;
    }

    /**
     * API业务对象
     * 
     */
    public void setData(MtopTaobaoClientGetBundleListResponseData data) {
        this.data = data;
    }

}
