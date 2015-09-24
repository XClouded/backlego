package com.taobao.tao;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.taobao.atlas.bundleInfo.AtlasBundleInfoManager;
import android.taobao.atlas.bundleInfo.BundleListing;
import android.taobao.atlas.framework.Atlas;
import android.taobao.atlas.runtime.ClassNotFoundInterceptorCallback;
import android.text.TextUtils;
import android.util.Log;
import com.taobao.android.nav.Nav;
import com.taobao.lightapk.BundleInfoManager;

import java.util.ArrayList;
import java.util.List;

public class ClassNotFoundInterceptor implements ClassNotFoundInterceptorCallback{

    public final String TAG = "ClassNotFundInterceptor";
    public static final List<String> GO_H5_BUNDLES_IF_NOT_EXISTS = new ArrayList<String>();

    /**
     * 进过一次h5，则主客不退出时一直走h5
     * @param pkgName
     */
    public static void addGoH5BundlesIfNotExists(String pkgName){
        if(!GO_H5_BUNDLES_IF_NOT_EXISTS.contains(pkgName)) {
            GO_H5_BUNDLES_IF_NOT_EXISTS.add(pkgName);
        }
    }

    /**
     * 清除缓存的强制走H5的bundle
     */
    public static void resetGoH5BundlesIfNotExists(){
        GO_H5_BUNDLES_IF_NOT_EXISTS.clear();
    }
	@Override
	public Intent returnIntent(final Intent intent) {
        final String className = intent.getComponent().getClassName();
        final String url = intent.getDataString();
        if(className!=null && className.equals("com.taobao.tao.welcome.Welcome")){
            return intent;
        }
        String bundleName = AtlasBundleInfoManager.instance().getBundleForComponet(className);
        if(BundleInfoManager.sInternalBundles==null){
            BundleInfoManager.instance().resolveInternalBundles();
        }
        boolean downloadGuide = false;
        if(BundleInfoManager.sInternalBundles==null){
            downloadGuide = Globals.isMiniPackage() || bundleName.equalsIgnoreCase("com.duanqu.qupai.recorder")
            || bundleName.equalsIgnoreCase("com.taobao.android.big");
        }else{
            downloadGuide = !BundleInfoManager.sInternalBundles.contains(bundleName) && Atlas.getInstance().getBundle(bundleName)==null;
        }
        if(downloadGuide) {
            Log.d(TAG, "bundle not found");
            final BundleListing.BundleInfo info = BundleInfoManager.instance().findBundleByActivity(className);
            if (info != null) {
                if (Atlas.getInstance().getBundle(info.getPkgName()) == null && !GO_H5_BUNDLES_IF_NOT_EXISTS.contains(info.getPkgName())) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Intent i = new Intent();
                            if(intent.getExtras()!=null) {
                                i.putExtras(intent.getExtras());
                            }
                            i.putExtra(BundleNotFoundActivity.KEY_ACTIVITY, className);
                            i.putExtra(BundleNotFoundActivity.KEY_BUNDLE_PKG, info.getPkgName());
                            i.setData(intent.getData());
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            i.setClass(Globals.getApplication(), BundleNotFoundActivity.class);
                            Globals.getApplication().startActivity(i);
                        }
                    });

                    return intent;
                }
            }
        }
        /**
         * 强制走h5
         */
        if(!TextUtils.isEmpty(url)) {
            if(intent.getComponent()==null || !intent.getComponent().getClassName().equals("com.taobao.browser.BrowserActivity")){
                Nav.from(Globals.getApplication()).withCategory("com.taobao.intent.category.HYBRID_UI").withExtras(intent.getExtras()).toUri(intent.getData());
            }else {
                intent.setComponent(new ComponentName(intent.getComponent().getPackageName(), "com.taobao.android.SimpleBrowserActivity"));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Globals.getApplication().startActivity(intent);
            }
        }
        return intent;
	}
}
