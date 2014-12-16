package com.taobao.lightapk;

import android.app.Activity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by guanjie on 14/11/18.
 */
public class LightActivityManager {

    private static ArrayList<WeakReference<Activity>> mActivitys = new ArrayList<WeakReference<Activity>>();

    public static void onActivityCreate(Activity activity){
        WeakReference<Activity> ref = new WeakReference<Activity>(activity);
        mActivitys.add(ref);
    }

    public static void onActivityDestroy(Activity activity){
        synchronized (LightActivityManager.class) {
            try {
                for (WeakReference<Activity> ref : mActivitys) {
                    if (ref != null && ref.get() != null && ref.get() == activity) {
                        mActivitys.remove(ref);
                        break;
                    }
                }
            } catch (Throwable e) {

            }
        }
    }

     public static  void finishAll(){
         synchronized (LightActivityManager.class) {
             for (WeakReference<Activity> ref : mActivitys) {
                 if (ref != null && ref.get() != null && !ref.get().isFinishing()) {
                     try {
                         ref.get().finish();
                     }catch(Throwable e){

                     }
                 }
             }
             mActivitys.clear();
         }
    }
}
