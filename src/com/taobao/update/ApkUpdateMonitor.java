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
//    public static final String CANCEL_INSTALL = "cancel_install";
    public static final String SUCCESS = "success";
    public static final String SUCCESS_FORCE = "success_force";
    //TODO
    public static final String POP_UPDATE_NOTIFY = "pop_update_notify";
    public static final String CONFIRM_DOWNLOAD = "confirm_download";
    public static final String POP_INSTALL_NOTIFY = "pop_install_notify";
//    public static final String CONFIRM_INSTALL_NOTIFY = "confirm_install";

    public static final String MODULE = "update";

    public static final String POINT = "apkupdate";

    public static void count(String step, String args) {
        if (TextUtils.isEmpty(args)) {
            AppMonitor.Counter.commit(MODULE, POINT, step, 1);
        } else {
            AppMonitor.Counter.commit(MODULE, POINT, step + ":" + args, 1);
        }
    }

}
