package com.taobao.storagespace;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.taobao.lightapk.BundleInfoManager;
import com.taobao.lightapk.BundleListing.BundleInfo;
import com.taobao.lego.R;
import org.osgi.framework.Bundle;

import java.util.ArrayList;
import java.util.List;

public class BundleListAdapter extends BaseAdapter {

    Context mContext;
    
    Bundle[] mBundles;
    
    private LayoutInflater mInflater;
    
    boolean[] mCheckedList;
    
    OnCheckBoxChecked mCheckListener = new OnCheckBoxChecked();
    
    TextView mTotalTv;
    
    Button mDelBtn;
    
    public BundleListAdapter(Context context){
        mContext = context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mTotalTv = (TextView)((Activity)context).findViewById(R.id.activity_space_tv_total);
        mDelBtn = (Button)((Activity)context).findViewById(R.id.activity_space_btn_clear);
        rebuild();
    }
    
    @Override
    public int getCount() {
        if(mBundles == null){
            return 0;
        }
        return mBundles.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if(convertView == null){
            convertView = mInflater.inflate(R.layout.list_item_bundle_list, null);  
            holder = new ViewHolder();  
            holder.bundleNameTv = (TextView)convertView.findViewById(R.id.list_item_tv_bundle_name);
            holder.bundleSizeTv = (TextView)convertView.findViewById(R.id.list_item_tv_bundle_size);
            holder.delCb = (CheckBox)convertView.findViewById(R.id.list_item_cb_check);
            holder.delCb.setOnCheckedChangeListener(mCheckListener);
            convertView.setTag(holder);  
        }else{
            holder = (ViewHolder)convertView.getTag();
        }
        if(mBundles != null && mBundles.length > position){
            BundleInfo info = BundleInfoManager.instance().getBundleInfoByPkg(mBundles[position].getLocation());
            if(info != null){
                if(info.getPkgName().equals("com.taobao.taoguide")){
                    info.setName("导购栏目");
                    info.setDesc("千万淘宝达人给你最专业的购物建议");
                }else if(info.getPkgName().equals("com.taobao.android.big")){
                    info.setName("big");
                    info.setDesc("人人都是生活家");
                }
                if(TextUtils.isEmpty(info.getName())){
                    info.setName("动态模块");
                }
                holder.bundleNameTv.setText("名称:" + info.getName());
                holder.bundleSizeTv.setText("大小:" + formatSize(info.getSize()) + "");
                holder.delCb.setTag(position);
                holder.delCb.setChecked(mCheckedList[position]);
            }
        }
        
        return convertView;
    }
    
    public void rebuild(){
        mBundles = StorageManager.getInstance(mContext.getApplicationContext()).getDeletableBundles();
        if(mBundles == null){
            mCheckedList = new boolean[0];
            return;
        }
        mCheckedList = new boolean[mBundles.length];
        for(int i = 0; i < mCheckedList.length; i++){
            mCheckedList[i] = false;
        }
    }
    
    public Bundle[] getCheckedBundles(){
        List<Bundle> bundleList = new ArrayList<Bundle>();
        if(mBundles == null){
            return new Bundle[0];
        }
        
        for (int i = 0; i < mCheckedList.length; i++) {
            if(mCheckedList[i]){
                bundleList.add(mBundles[i]);
            }
        }
        return bundleList.toArray(new Bundle[0]);
    }
    
    
    
    @Override
    public void notifyDataSetChanged() {
        checkBoxChecked();
        super.notifyDataSetChanged();
    }

    public long getCheckedBundleSize(){
        long totalSize = 0;
        for(int i = 0; i < mCheckedList.length; i++){
            if(mCheckedList[i]){
                totalSize += BundleInfoManager.instance().getBundleInfoByPkg(mBundles[i].getLocation()).getSize();
            }
        }
        return totalSize;
    }
    
    public String getCheckedBundleSizeString(){
        long size = getCheckedBundleSize();
        if(size <= 0){
            return null;
        }else{
            return formatSize(size);
        }
        
    }
    
    private void checkBoxChecked(){
        
        long size = getCheckedBundleSize();
        if(size > 0){
            mDelBtn.setTextColor(0xfff45050);
            mTotalTv.setText("选中文件大小:" + formatSize(size));
        }else{
            mDelBtn.setTextColor(0xff999999);
            mTotalTv.setText("");
        }
    }
    
    private String formatSize(long size){
        java.text.DecimalFormat df=new   java.text.DecimalFormat("#0.00"); 
        if(size < 1024){
            return size + "B";
        }else if(size < 1024 * 1024){
            return df.format(size / 1024.0) + "K";
        }else{
            return df.format(size / 1024.0 / 1024) + "M";
        }
    }

    class ViewHolder{  
        TextView bundleNameTv;  
        TextView bundleSizeTv;
        CheckBox delCb;
    }
    
    class OnCheckBoxChecked implements OnCheckedChangeListener{

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {
            try {
                int postion = (Integer) buttonView.getTag();
                mCheckedList[postion] = isChecked;
            } catch (Exception e) {
            }
            checkBoxChecked();
        }
    }
}
