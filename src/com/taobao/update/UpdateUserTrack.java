package com.taobao.update;

import android.util.Log;

import com.alibaba.mtl.appmonitor.AppMonitor;

public class UpdateUserTrack {
    private final static String matoTrace = "MTAO_UPDATE_USERTRACK_ID";
    private final static String bundleTrace = "BUNDLE_UPDATE_USERTRACK_ID";

    private UpdateUserTrack() {
    }

    public static final String MODULE = "update";

    /**
     * 手淘更新usertrack
     *
     * @param contextMessage
     * @param detailMessage
     */
    public static void mTaoUpdateTrack(String contextMessage, String detailMessage) {
        Log.i(contextMessage, detailMessage);
        AppMonitor.Counter.commit(MODULE, matoTrace, contextMessage + "|" + detailMessage, 1);
//        Properties mTao = new Properties();
//        mTao.setProperty(contextMessage, detailMessage);
//        TBS.Ext.commitEvent(matoTrace,mTao);
    }

    /**
     * bundle更新usertrack
     *
     * @param contextMessage
     * @param detailMessage
     */
    public static void bundleUpdateTrack(String contextMessage, String detailMessage) {
        Log.i(contextMessage, detailMessage);
        AppMonitor.Counter.commit(MODULE, bundleTrace, contextMessage + "|" + detailMessage, 1);
//        Properties bundle = new Properties();
//        bundle.setProperty(contextMessage, detailMessage);
//        TBS.Ext.commitEvent(bundleTrace,bundle);
    }

}
