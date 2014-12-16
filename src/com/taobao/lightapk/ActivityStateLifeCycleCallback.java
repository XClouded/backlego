package com.taobao.lightapk;

import android.app.Activity;
import android.os.Bundle;
import com.taobao.android.compat.ApplicationCompat;

/**
 * Created by guanjie on 14/11/18.
 */
public class ActivityStateLifeCycleCallback extends ApplicationCompat.AbstractActivityLifecycleCallbacks {

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        LightActivityManager.onActivityCreate(activity);
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        LightActivityManager.onActivityDestroy(activity);
    }
}
