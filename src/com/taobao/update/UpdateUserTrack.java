package com.taobao.update;

import android.util.Log;
import com.taobao.statistic.TBS;
import com.taobao.tao.util.StringUtil;

import java.util.Properties;

public class UpdateUserTrack {
    private final static String  matoTrace = "MTAO_UPDATE_USERTRACK_ID";
    private final static String  bundleTrace = "BUNDLE_UPDATE_USERTRACK_ID";
    private UpdateUserTrack(){}
    
    /**
     * 手淘更新usertrack
     * @param contextMessage
     * @param detailMessage
     */
    public static void mTaoUpdateTrack(String contextMessage,String detailMessage){
        Log.i(contextMessage, detailMessage);
        Properties mTao = new Properties();
        mTao.setProperty(contextMessage, detailMessage);
        TBS.Ext.commitEvent(matoTrace,mTao);
    }
    /**
     * bundle更新usertrack
     * @param contextMessage
     * @param detailMessage
     */
    public static void bundleUpdateTrack(String contextMessage,String detailMessage){
        Log.i(contextMessage, detailMessage);
        Properties bundle = new Properties();
        bundle.setProperty(contextMessage, detailMessage);
        TBS.Ext.commitEvent(bundleTrace,bundle);
    }

}
