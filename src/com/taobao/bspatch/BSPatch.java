package com.taobao.bspatch;

import com.taobao.tao.util.SoInstallMgr;


public class BSPatch {
    
    static boolean mIsSupportBSPatch = false;
    
	static {
        try {
            
            mIsSupportBSPatch = SoInstallMgr.initSo("BSPatch", 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
    public static boolean isSupportBSPatch()
    {
        return mIsSupportBSPatch;
    } 
	
	/**
	 * bsdiff算法合并
	 * @param old_file		基础文件路径
	 * @param new_file		新生成文件路径
	 * @param patch_file	补丁文件路径
	 * @return
	 */
	public static native int bspatch(String old_file, String new_file, String patch_file);
}
