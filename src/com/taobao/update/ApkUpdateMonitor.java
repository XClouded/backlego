package com.taobao.update;

import android.text.TextUtils;

import com.alibaba.mtl.appmonitor.AppMonitor;

/**
 * Created by wuzhong on 15/9/24.
 */
public class ApkUpdateMonitor {

    public static final String NO_UPDATE = "no_update";
    public static final String HAS_UPDATE = "has_update";
    public static final String CANCEL_DOWNLOAD = "cancel_download";
    public static final String DOWNLOAD_ERROR = "download_error";
    public static final String CANCEL_INSTALL = "cancel_install";
    public static final String SUCCESS = "success";
    public static final String SUCCESS_FORCE = "success_force";

    public static final String MODULE = "update";

    public static void count(String step, String args) {
        if (TextUtils.isEmpty(args)) {
            AppMonitor.Counter.commit(MODULE, step, 1);
        } else {
            AppMonitor.Counter.commit(MODULE, step, args, 1);
        }
    }

}
