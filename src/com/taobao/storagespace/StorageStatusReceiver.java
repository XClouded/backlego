package com.taobao.storagespace;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StorageStatusReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (intent.getAction().equals(Intent.ACTION_DEVICE_STORAGE_LOW)) {
                StorageManager.getInstance(context).freeSpace();
            }
        }catch(Exception e){

        }
    }
}
