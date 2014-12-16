package com.taobao.tao.update.alipay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

/**
 * Created by guanjie on 14-7-24.
 */
public class NetWorkStateChangeReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

            if(AlipaySilentDownloader.getInstance(context.getApplicationContext()).canDownload()){
                AlipaySilentDownloader.getInstance(context.getApplicationContext()).checkDownload();
            }else{
                AlipaySilentDownloader.getInstance(context.getApplicationContext()).stopDownload();
            }
        }
    }

}
