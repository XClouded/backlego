package com.taobao.tao;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.net.ConnectivityManagerCompat;
import android.taobao.atlas.framework.Atlas;
import android.taobao.util.SafeHandler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.taobao.android.lifecycle.PanguActivity;
import com.taobao.android.nav.Nav;
import com.taobao.lightapk.BatchBundleDownloader;
import com.taobao.lightapk.BatchBundleInstaller;
import com.taobao.lightapk.BundleInfoManager;
import com.taobao.lightapk.BundleListing;
import com.taobao.statistic.TBS;
import com.taobao.tao.util.NetWorkUtils;
import com.taobao.lego.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by guanjie on 14-9-15.
 */
public class BundleNotFoundActivity extends PanguActivity implements View.OnClickListener,Handler.Callback{

    public static final int AUTO_DOWNLOAD_CANCEL = 1;
    public final String TAG = "BundleNotFoundActivity";
    public static final String KEY_BUNDLE_PKG = "lightapk_pkg";
    public static final String KEY_ACTIVITY = "lightapk_activity";
    private String mH5Url ;
    private String mActivityName;
    private BundleListing.BundleInfo mTargetBundleInfo;
    private BatchBundleDownloader mBatchBundleDownloader;
    private BatchBundleInstaller mBatchBundleInstaller;
    private boolean autoDownload = false;
    private SafeHandler mSafeHandler;
    private boolean mShowH5 = true;
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        Intent intent = getIntent();
        if(intent!=null) {
            mH5Url = intent.getDataString();
            String pkgName = intent.getStringExtra(KEY_BUNDLE_PKG);
            mActivityName = intent.getStringExtra(KEY_ACTIVITY);
            mTargetBundleInfo = BundleInfoManager.instance().findBundleByActivity(mActivityName);
            if(mTargetBundleInfo!=null && mTargetBundleInfo.getPkgName().equals("com.taobao.taoguide")){
                mTargetBundleInfo.setName("导购栏目");
                mTargetBundleInfo.setDesc("千万淘宝达人给你最专业的购物建议");
            }else if(mTargetBundleInfo!=null && mTargetBundleInfo.getPkgName().equals("com.taobao.android.big")){
                mTargetBundleInfo.setName("big");
                mTargetBundleInfo.setDesc("人人都是生活家");
            }
            if(mTargetBundleInfo!=null && (
                    mTargetBundleInfo.getPkgName().equals("com.tmall.wireless.plugin") ||
                            mTargetBundleInfo.getPkgName().equals("com.taobao.ju.android") ||
                            mTargetBundleInfo.getPkgName().equals("com.taobao.mobile.dipei") ||
                            mTargetBundleInfo.getPkgName().equals("com.taobao.taoguide") ||
                            mTargetBundleInfo.getPkgName().equals("com.taobao.taobao.pluginservice"))){
                ;
            }else{
                mShowH5 = false;
            }
            autoDownload = mTargetBundleInfo.getSize()<500*1024 && !ConnectivityManagerCompat.isActiveNetworkMetered(
                    (ConnectivityManager)Globals.getApplication().getSystemService(Context.CONNECTIVITY_SERVICE)
            );
            if(autoDownload){
                super.onCreate(savedInstanceState);
                mSafeHandler = new SafeHandler(this);
                setContentView(R.layout.activity_bundle_not_found);
                findViewById(R.id.loading_mask).setVisibility(View.VISIBLE);
                startDownloadBundleAndWait();
                mSafeHandler.sendEmptyMessageDelayed(AUTO_DOWNLOAD_CANCEL,5000);
            }else{
                setTheme(R.style.Theme_NoBackgroundAndTitle);
                super.onCreate(savedInstanceState);
                getSupportActionBar().setDisplayShowHomeEnabled(false);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(true);
                setContentView(R.layout.activity_bundle_not_found);
                findViewById(R.id.loading_mask).setVisibility(View.GONE);
                LayoutInflater.from(this).inflate(R.layout.bundle_wait,(ViewGroup)findViewById(R.id.loading_mask).getParent());
            }
            initView(autoDownload);
            if(mTargetBundleInfo!=null){
                TBS.Ext.commitEvent("PAGE_AndroidLite",2001,"View_"+mTargetBundleInfo.getPkgName());
            }
        }
    }

    /**
     * 根据数据初始化界面内容
     */
    private void initView(boolean autoDownload){
        if(!autoDownload) {
            findViewById(R.id.btn_h5).setVisibility(View.GONE);

            findViewById(R.id.btn_cancel).setOnClickListener(this);
            findViewById(R.id.btn_h5).setOnClickListener(this);
            findViewById(R.id.btn_native).setOnClickListener(this);

            if(mShowH5 && !TextUtils.isEmpty(mH5Url)) {
                findViewById(R.id.btn_h5).setVisibility(View.VISIBLE);
            }

            if (mTargetBundleInfo != null) {
                ((TextView) findViewById(R.id.tv_name)).setText(mTargetBundleInfo.getName());
                ((TextView) findViewById(R.id.tv_name2)).setText(mTargetBundleInfo.getName());
                ((TextView) findViewById(R.id.tv_desc)).setText(mTargetBundleInfo.getDesc() != null ? mTargetBundleInfo.getDesc() : "");
                if(!mShowH5){
                    ((TextView) findViewById(R.id.tv_size)).setText(
                            String.format("体验完整版(%.2fM)", ((float) mTargetBundleInfo.getSize()) / 1024 / 1024));
                }else {
                    ((TextView) findViewById(R.id.tv_size)).setText(
                            String.format("你可以继续浏览网页版或者体验更好的完整版(%.2fM)", ((float) mTargetBundleInfo.getSize()) / 1024 / 1024));
                }
            }
        }
    }

    /**
     * 启动bundle下载
     */
    private void startDownloadBundleAndWait(){
        Log.d(TAG,"startDownloadBundleAndWait");
        new AsyncTask<String, Integer, Boolean>() {
            @Override
            protected Boolean doInBackground(String... strings) {
                if(mTargetBundleInfo!=null && mTargetBundleInfo.getUrl()==null){
                    return BundleInfoManager.instance().resoveBundleUrlFromServer();
                }else if(mTargetBundleInfo!=null){
                   List<String> dependencyPkg = mTargetBundleInfo.getDependency();
                   if(dependencyPkg!=null){
                       for(String pkg : dependencyPkg){
                          BundleListing.BundleInfo info =  BundleInfoManager.instance().getBundleInfoByPkg(pkg);
                          if(info!=null && TextUtils.isEmpty(info.getUrl())){
                              return BundleInfoManager.instance().resoveBundleUrlFromServer();
                          }
                       }
                   }
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if(!result){
                    Toast.makeText(getApplicationContext(),"获取模块下载地址失败，请先使用网页版或者稍后尝试，谢谢",Toast.LENGTH_SHORT).show();
                    if(!autoDownload) {
                        findViewById(R.id.ll_download).setVisibility(View.GONE);
                        findViewById(R.id.ll_choice).setVisibility(View.VISIBLE);
                        findViewById(R.id.horizontal_divide).setVisibility(View.GONE);
                    }
                }else{
                    /**
                     * 开始下载bundle
                     */
                    if(mBatchBundleDownloader==null){
                        mBatchBundleDownloader = BatchBundleDownloader.obtainBatchBundleDownloader(getApplicationContext(),mTargetBundleInfo.getPkgName());
                    }
                    mBatchBundleDownloader.addDownloadListener(new BatchBundleDownloader.BatchDownloadListener() {
                        @Override
                        public void onDownloadProgress(int i) {
                            if(!autoDownload) {
                                ((ProgressBar) findViewById(R.id.progress)).setProgress(i);
                            }
                        }

                        @Override
                        public void onDownloadError(int i, String s) {
                            Toast.makeText(getApplicationContext(),s,Toast.LENGTH_SHORT).show();
                            if(!autoDownload) {
                                findViewById(R.id.ll_download).setVisibility(View.GONE);
                                findViewById(R.id.ll_choice).setVisibility(View.VISIBLE);
                                findViewById(R.id.horizontal_divide).setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public void onDownloadFinish(String s) {
//                            File file = new File(s);
//                            ArrayList<String> filePath = new ArrayList<String>();
//                            if(file!=null){
//                                File[] bundles = file.listFiles();
//                                for(File bundleFile :bundles){
//                                    if(bundleFile.getAbsolutePath().endsWith(".so")){
//                                        filePath.add(bundleFile.getAbsolutePath());
//                                    }
//                                }
//                            }
                            startInstallBundle(s);
                        }
                    });

                    ArrayList<String> pkgs = new ArrayList<String>();
                    pkgs.add(mTargetBundleInfo.getPkgName());
                    if(mTargetBundleInfo.getDependency()!=null){
                        pkgs.addAll(mTargetBundleInfo.getDependency());
                    }
                    if(!mBatchBundleDownloader.isRunning()) {
                        for (int x = 0; x < pkgs.size(); ) {
                            if (Atlas.getInstance().getBundle(pkgs.get(x)) != null) {
                                pkgs.remove(x);
                            } else {
                                x++;
                            }
                        }
                        mBatchBundleDownloader.startBatchDownload(
                                BundleInfoManager.instance().getBundleDownloadInfoByPkg(pkgs));
                    }
                }

            }
        }.execute();
    }

    public void goDestination(){
        if (!TextUtils.isEmpty(mH5Url)) {
            Nav.from(BundleNotFoundActivity.this).withExtras(getIntent().getExtras()).toUri(Uri.parse(mH5Url));
            finish();
        } else {
            if (!TextUtils.isEmpty(mActivityName)) {
                Intent intent = new Intent();
                intent.setClassName(BundleNotFoundActivity.this, mActivityName);
                intent.putExtras(getIntent().getExtras());
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(getApplicationContext(), "错误的Activity Class", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 启动安装bundle
     * @param  bundlePath  需要安装的bundle的本地地址
     */
    private void startInstallBundle(String bundlePath){
        if(mBatchBundleInstaller==null){
            mBatchBundleInstaller = new BatchBundleInstaller(getApplicationContext());
            mBatchBundleInstaller.setBatchBundleInstallerListener(new BatchBundleInstaller.BatchBundleInstallerListener() {
                @Override
                public void onInstallSuccess(List<String> pkgList) {
                    if(!isFinishing()) {
                        goDestination();
                    }
                }

                @Override
                public void onInstallFailed(int errorCode) {
                    if(!isFinishing()) {
                        Toast.makeText(getApplicationContext(), "ERRORCODE = " + errorCode, Toast.LENGTH_SHORT).show();
                        if (!autoDownload) {
                            findViewById(R.id.ll_choice).setVisibility(View.VISIBLE);
                            findViewById(R.id.ll_download).setVisibility(View.GONE);
                            findViewById(R.id.horizontal_divide).setVisibility(View.GONE);
                        }
                    }
                }
            });
            Log.d(TAG,bundlePath);
            mBatchBundleInstaller.installBundleAsync(bundlePath);
        }
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.btn_cancel){
            if(mBatchBundleDownloader!=null){
                mBatchBundleDownloader.cancel();
            }
            findViewById(R.id.ll_choice).setVisibility(View.VISIBLE);
            findViewById(R.id.ll_download).setVisibility(View.GONE);
            findViewById(R.id.horizontal_divide).setVisibility(View.GONE);

        }else if(view.getId() == R.id.btn_native){
            if(mTargetBundleInfo!=null && NetWorkUtils.isNetworkAvailable(getApplicationContext())){
                findViewById(R.id.ll_choice).setVisibility(View.GONE);
                findViewById(R.id.ll_download).setVisibility(View.VISIBLE);
                findViewById(R.id.horizontal_divide).setVisibility(View.VISIBLE);
                if(mTargetBundleInfo!=null){
                    TBS.Ext.commitEvent("PAGE_AndroidLite",2101,"Download_"+mTargetBundleInfo.getPkgName());
                }
                if(mTargetBundleInfo!=null && Atlas.getInstance().getBundle(mTargetBundleInfo.getPkgName())!=null) {
                    goDestination();
                }else{
                    startDownloadBundleAndWait();
                }
            }else{
                if(!NetWorkUtils.isNetworkAvailable(getApplicationContext())){
                    Toast.makeText(getApplicationContext(),"网络异常，请稍后再试",Toast.LENGTH_SHORT).show();
                }
            }
        }else if(view.getId() == R.id.btn_h5){
            ClassNotFoundInterceptor.addGoH5BundlesIfNotExists(mTargetBundleInfo.getPkgName());
            if(!TextUtils.isEmpty(mH5Url)){
                Nav.from(this).withCategory("com.taobao.intent.category.HYBRID_UI").toUri(Uri.parse(mH5Url));
                finish();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(isFinishing() && mSafeHandler!=null){
            mSafeHandler.destroy();
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        switch(message.what){
            case AUTO_DOWNLOAD_CANCEL:
                if(mH5Url!=null){
                    if(mShowH5 && !TextUtils.isEmpty(mH5Url)) {
                        Nav.from(this).withCategory("com.taobao.intent.category.HYBRID_UI").toUri(Uri.parse(mH5Url));
                    }else{
                        Toast.makeText(this,"获取模块失败，请稍后再试",Toast.LENGTH_SHORT).show();
                    }
                    if(mBatchBundleDownloader!=null){
                        mBatchBundleDownloader.cancel();
                    }
                    if(mBatchBundleInstaller!=null){
                        mBatchBundleInstaller.setBatchBundleInstallerListener(null);
                    }
                    finish();
                }
                break;
        }
        return false;
    }
}
