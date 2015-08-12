package com.taobao.storagespace;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.taobao.atlas.framework.Atlas;
import android.view.*;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.Toast;
import com.taobao.lightapk.LightActivityManager;
import com.taobao.tao.Globals;
import com.taobao.tao.util.TBDialog;
import com.taobao.tao.util.ActivityHelper;
import com.taobao.tao.util.CacheFileUtil;
import com.taobao.lego.R;
import org.osgi.framework.BundleException;

import java.util.List;

public class SpaceActivity extends ActionBarActivity implements OnClickListener, OnMenuItemClickListener{

    private BundleListAdapter mAdapter;
    
    private ListView mListView;
    
    private boolean isCleaned = false;
    
    private boolean isAllClean = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_space);
        mListView = (ListView)findViewById(R.id.activity_space_bundle_list);
        mAdapter = new BundleListAdapter(this);
        if(mAdapter.getCount() == 0){
            onNoDelableBundle();
        }
        mListView.setAdapter(mAdapter);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("空间管理");
        actionBar.setDisplayHomeAsUpEnabled(true);
        supportDisablePublicMenu();
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.space_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.activity_space_btn_cancel){
            clean();
        }else if(v.getId() == R.id.activity_space_btn_clear){
            v.setEnabled(false);
            org.osgi.framework.Bundle[] delBundleList = mAdapter.getCheckedBundles();
            String sizeString = mAdapter.getCheckedBundleSizeString();
            for (org.osgi.framework.Bundle bundle : delBundleList) {
                try {
                    Atlas.getInstance().uninstallBundle(bundle.getLocation());
                } catch (BundleException e) {
                    e.printStackTrace();
                }
            }
            isCleaned = true;
            mAdapter.rebuild();
            mAdapter.notifyDataSetChanged();
            v.setEnabled(true);
            if(mAdapter.getCount() == 0){
                onNoDelableBundle();
            }
            String toastString;
            
            if(sizeString == null){
                toastString = "亲，请先选择要清理的插件";
            }else{
                toastString = "插件清理完毕，提升空间" + sizeString;
            }
            Toast toast= Toast.makeText(getApplicationContext(), toastString, Toast.LENGTH_SHORT);
            
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            clean();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public boolean onMenuItemClick(MenuItem arg0) {
        if(arg0.getItemId() == R.id.action_clear){
            final TBDialog dialog = new TBDialog(this, "清除数据", "清除后数据不可恢复，确定清除数据？");
            dialog.setTitle("清除数据");
            dialog.setNegativeButtonText("取消");
            dialog.setPositiveButtonText("确定");
            dialog.setPositiveButton(new OnClickListener() {
                
                @Override
                public void onClick(View arg0) {
                    isCleaned = true;
                    isAllClean = true;
                    clean();
                }
            });
            dialog.setNegativeButton(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    dialog.dismiss();
                }
            });
            dialog.show();
            return true;
        }
        return false;
    }
    
    private void onNoDelableBundle(){
        findViewById(R.id.activity_space_btn_clear).setEnabled(false);
        mListView.setVisibility(View.GONE);
        findViewById(R.id.activity_space_no_delable_bundle_note).setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK) {
            clean();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!isFinishing()) {
            clean();
        }
    }

    private void clean(){
        finish();
        LightActivityManager.finishAll();
        if(isAllClean || isCleaned){
            if(isAllClean){
                CacheFileUtil.clear(getApplicationContext(), false);
            }
            ActivityHelper.killTaoBaoRemoteProcess();
            try {
                ActivityManager am = (ActivityManager) Globals.getApplication().getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> a = am.getRunningAppProcesses();
                for (int i = 0; i < a.size(); i++) {
                    ActivityManager.RunningAppProcessInfo b = a.get(i);
                    if (b.processName.equalsIgnoreCase("com.taobao.taobao")){
                        android.os.Process.killProcess(b.pid);
                        continue;
                    }
                }
            } catch (Exception e) {

            }
      }

    }
}
