package com.taobao.storagespace;


import android.content.Context;
import android.taobao.atlas.framework.Atlas;
import android.taobao.utconfig.ConfigCenterLifecycleObserver;
import android.text.TextUtils;
import android.util.Log;
import com.alibaba.fastjson.JSON;
import com.taobao.android.task.Coordinator;
import com.taobao.android.task.Coordinator.TaggedRunnable;
import com.taobao.tao.Globals;
import com.taobao.wswitch.api.business.ConfigContainerAdapter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class StorageManager {
    
    private static final String TAG = "StorageManager";
    
    private static final long MB_IN_BYTES = 1024 * 1024;
    private static final int THRESHOLD_PERCENTAGE = 10;
    private static final long THRESHOLD_MAX_BYTES = 500 * MB_IN_BYTES;
    private static final double THRESHOLD_LOW_CUSTOM = 1.2;
    
    private static final long BUNDLE_SAFE_TIME = 1000l * 60l * 60l * 24l * 14l;//两周
    
    private static StorageManager sInstance = null;
    
    private Context mContext;
    
    static final String[] sBundleWriteList = new String[]{
        "com.taobao.android.scancode",
        "com.taobao.android.trade",
        "com.taobao.libs",
        "com.taobao.login4android",
        "com.taobao.mytaobao",
        "com.taobao.search",
        "com.taobao.tao.purchase",
        "com.taobao.taobao.alipay",
        "com.taobao.taobao.cashdesk",
        "com.taobao.taobao.home",
        "com.taobao.taobao.zxing",
        "com.taobao.wangxin",
        "com.taobao.weapp",
        "com.taobao.allspark",
        "com.ut.share",
        "com.taobao.ruleenginebundle",
        "com.taobao.trade.order",
            "com.taobao.trade.rate",
            "com.taobao.dynamic",
            "com.taobao.browser",
            "com.taobao.calendar",
        "com.taobao.passivelocation",
        "com.taobao.taobao.map"};
    
    //上次启动的bundle名称
    private String mLastVisitBundle = null;
    
    //通过LinkedHashSet实现简单的LRU
    private Object mLRULock = new Object();
    private TaoLRUSet<String> mLRUSet = new TaoLRUSet<String>();
    
    //FIXME:此处应该放在lru中统一管理
    private HashMap<String, Long> mBundleVisitTime = new HashMap<String, Long>();
    
    private HashSet<String> mBundleWriteSet = new HashSet<String>();
    
    private boolean initing = false;
    public static synchronized StorageManager getInstance(Context context){
        if(sInstance == null){
            sInstance = new StorageManager(context);
        }
        return sInstance;
    }
    
    private StorageManager(Context context){
        mContext = context.getApplicationContext();
        
        for (String bundleName : sBundleWriteList) {
        	mBundleWriteSet.add(bundleName);
        }
        if(!Globals.isMiniPackage()){
            return;
        }
        //建立lru
        Coordinator.postTask(new TaggedRunnable("restoreLRU") { @Override public void run() {
            initing = true;
            restoreLRU();
            Log.d(TAG, "LRU created");
            printLRU();
            tryFreeSpaceSync();
            initing = false;
        }});
        
    }
    
    public void freeSpace(){
        if(!Globals.isMiniPackage()){
            return;
        }
        if(initing){
            return;
        }
        Coordinator.postTask(new TaggedRunnable("freeSpace") { @Override public void run() {
            tryFreeSpaceSync();
        }});
    }
    
    public Bundle[] getDeletableBundles(){
        /**
         * 删除无用的bundle
         */
        String value = ConfigContainerAdapter.getInstance().getConfig(
                ConfigCenterLifecycleObserver.CONFIG_GROUP_SYSTEM, "bundles_cannot_remove", "");
        Log.d(TAG,value);
        if(!TextUtils.isEmpty(value)){
            List<String> cannotRemoveBundles = JSON.parseArray(value, String.class);
            if(cannotRemoveBundles!=null && cannotRemoveBundles.size()>0){
                for(String name : cannotRemoveBundles){
                    if(!mBundleWriteSet.contains(name)){
                        mBundleWriteSet.add(name);
                        Log.d(TAG,"add "+value);
                    }
                }
            }
        }
        List<Bundle> bundles = Atlas.getInstance().getBundles();
        List<Bundle> delBundles = new ArrayList<Bundle>();
        for (Bundle bundle : bundles) {
            if(bundle.getState() != Bundle.UNINSTALLED && !mBundleWriteSet.contains(bundle.getLocation())){
                delBundles.add(bundle);
            }
        }
//        
//        
//        for (String bundleName : mDeletableBundleList) {
//            for (Bundle bundle : bundles) {
//                if(bundle.getState() != Bundle.UNINSTALLED && bundle.getLocation().equals(bundleName)){
//                    delBundles.add(bundle);
//                    break;
//                }
//            }
//        }
        return delBundles.toArray(new Bundle[0]);
    }
    
//    int mIndex = 0;
    public void onBundleStarted(String bundleName){
        if(!Globals.isMiniPackage()){
            return;
        }
        if(bundleName == null){
            return;
        }
        
        if(mBundleWriteSet.contains(bundleName)){
            return;
        }
        Log.d(TAG, "onBundleStarted:" + bundleName);
        //lru中记录下
        flushLRU(bundleName);
    }
    
    private void flushLRU(String bundleName){
        synchronized (mLRULock) {
            if(mLastVisitBundle != null && mLastVisitBundle.equals(bundleName)){
                Log.d(TAG, "visit last");
                return;
            }
            Log.d(TAG, "visit new, contins:" + mLRUSet.contains(bundleName));
            mLastVisitBundle = bundleName;
            mLRUSet.add(bundleName);
            mBundleVisitTime.put(bundleName, System.currentTimeMillis());
            printLRU();
        }
        Coordinator.postTask(new TaggedRunnable("writeLRU2File") { @Override public void run() {
            saveLRU();
            Log.d(TAG, "tryFreeSpaceSync");
            tryFreeSpaceSync();
        }});
    }
    private void restoreLRU(){
        TaoLRUSet<String> set = new TaoLRUSet<String>();
        try {
            for(Bundle bundle : getDeletableBundles()){
                set.add(bundle.getLocation());
            }
//            for (String bundle : mDeletableBundleList) {
//                set.add(bundle);
//            }
            
            FileReader fr = new FileReader(new File(mContext.getFilesDir(), "BundleLRU"));
            BufferedReader reader = new BufferedReader(fr);
            String line;
            
            while ((line = reader.readLine()) != null) {
                String[] lines = line.split(" ");
                if(lines.length != 2){
                    continue;
                }
                set.add(lines[0]);
                try {
                    mBundleVisitTime.put(lines[0], Long.parseLong(lines[1]));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
            }
            reader.close();
            fr.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        synchronized (mLRULock) {
            set.addAll(mLRUSet);
            mLRUSet = set;
        }
    }
    
    private synchronized void saveLRU(){
        try {
            String[] bundleNames = mLRUSet.toArray(new String[0]);
            FileWriter writer = new FileWriter(new File(mContext.getFilesDir(), "BundleLRU"));
            writer.write("");
            for (String name : bundleNames) {
                Long visitTime = mBundleVisitTime.get(name);
                if(visitTime == null){
                    visitTime = 0l;
                }
                writer.append(name + " " + visitTime +"\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void tryFreeSpaceSync(){
        if(needDelBundle()){
            long nowTime = System.currentTimeMillis();
            Log.d(TAG, "need free");
            printLRU();
            String[] bundles = mLRUSet.toArray(new String[0]);
            for (String bundleName : bundles) {
                if(needDelBundle()){
                    synchronized (mLRULock) {
                        long visitTime = mBundleVisitTime.get(bundleName);
                        if(nowTime - visitTime > BUNDLE_SAFE_TIME){
                            Log.d(TAG, "try free bundle:" + bundleName);
                            mLRUSet.remove(bundleName);
                            //删除bundle
                            try {
                                Atlas.getInstance().uninstallBundle(bundleName);
                            } catch (BundleException e) {
                                e.printStackTrace();
                            }
                            saveLRU();
                        }
                    }
                }
            }
        }
    }
    
    private long mLowBytes = -1;
    private long mTotalBytes = -1;
    private boolean needDelBundle(){
//        if(mIndex > 10){
//            return true;
//        }
        File tmpFile = mContext.getFilesDir();
        if(mTotalBytes < 0){
            mTotalBytes = tmpFile.getTotalSpace();
        }
        if(mLowBytes < 0){
            try {
                Method method = android.os.storage.StorageManager.class.getDeclaredMethod("getStorageLowBytes",
                        new Class[]{File.class});
                mLowBytes = (Long) method.invoke(mContext.getSystemService(Context.STORAGE_SERVICE), new Object[]{tmpFile});
            } catch (Exception e) {
                e.printStackTrace();
                mLowBytes = (mTotalBytes * THRESHOLD_PERCENTAGE) / 100;
                mLowBytes = Math.min(mLowBytes, THRESHOLD_MAX_BYTES);
            }
            mLowBytes = (long) (mLowBytes * THRESHOLD_LOW_CUSTOM);
        }
        
        if(tmpFile.getFreeSpace() > mLowBytes){
            return false;
        }
        return true;
    }
    
    private void printLRU(){
        Log.d(TAG, "---PrintLRU--");
        int index = 0;
        for (String bundleName : mLRUSet) {
            Log.d(TAG, "LRU" + (index++) + ": " + bundleName);
        }
        Log.d(TAG, "---PrintLRU end--");
    }
    
}
