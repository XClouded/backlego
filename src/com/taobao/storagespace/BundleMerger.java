package com.taobao.storagespace;

import android.support.v7.taobao.util.Globals;
import android.taobao.util.FileManagerUtils;
import android.util.Log;
import com.taobao.bspatch.BSPatch;
import com.taobao.update.UpdateUtils;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by guanjie on 15/6/24.
 */
public class BundleMerger {

    public static final String TAG = "BundleMerger";
    public static final String MERGE_TMP_DIR = Globals.getApplication().getFilesDir() + File.separator + "patch_tmp";
    public static final String PATCH_DEX_NAME = "patch.dex";
    public static final String DEX_NAME = "classes.dex";
    public static final int BUFFER_SIZE = 0x4000;
    public static final int MAX_EXTRACT_ATTEMPTS = 3;

    /**
     * be not call in main thread
     */
    public static void merge(File oldBundle, File patchBundle, File targetBundle,String dexMd5) throws IOException {
        if (oldBundle.exists() && patchBundle.exists()) {
            ZipFile patchZip = new ZipFile(patchBundle);
            ZipFile sourceZip = new ZipFile(oldBundle);
            String patch_dex = Globals.getApplication().getFilesDir() + File.separator + "patch.dex";
            String source_dex= Globals.getApplication().getFilesDir() + File.separator + "source.dex";
            File newDex = null;
            try {
                ZipEntry entry = patchZip.getEntry(PATCH_DEX_NAME);
                if (entry!=null){
                    performEntryExtractions(patchZip,entry,patch_dex);
                }
                entry =sourceZip.getEntry(DEX_NAME);
                if(entry!=null){
                    performEntryExtractions(sourceZip,entry,source_dex);
                }
                newDex = new File(MERGE_TMP_DIR,"classes.dex");
                //dex merge
                getNewBundleDex(source_dex,patch_dex,newDex.getAbsolutePath(),dexMd5);

                File tempFile = File.createTempFile(patchBundle.getName(), null,patchBundle.getParentFile());
                createNewBundleInternal(sourceZip,patchZip,newDex,tempFile);
                if(tempFile.exists()){
                    tempFile.renameTo(targetBundle);
                }
            }catch(IOException e){
                throw new IOException(e);
            }finally{
                try {
                    patchZip.close();
                    sourceZip.close();
                    File file = new File(patch_dex);
                    if (file.exists()) {
                        file.delete();
                    }
                    file = new File(source_dex);
                    if (file.exists()) {
                        file.delete();
                    }
                    if (newDex.exists()) {
                        newDex.delete();
                    }
                }catch(Throwable e){}
            }
        }
    }

    private static void createNewBundleInternal(ZipFile source,ZipFile patch,File newDex,File target) throws IOException{
        // get a temp file
        byte[] buffer = new byte[BUFFER_SIZE];
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(target)));
        //先写入classes.dex
        out.putNextEntry(new ZipEntry("classes.dex"));
        InputStream in = new FileInputStream(newDex);
        write(in,out,buffer);
        //接着写入source中未变的文件
        java.util.Enumeration e = source.entries();
        while (e.hasMoreElements()) {
            ZipEntry zipEnt = (ZipEntry) e.nextElement();
            if(shouldSkip(zipEnt)){
                continue;
            }
            String name = zipEnt.getName();
            boolean toBeDeleted = false;
            if (patch.getEntry(name)!=null) {
                toBeDeleted = true;
            }
            if(!toBeDeleted){
                // Add ZIP entry to output stream.
                out.putNextEntry(new ZipEntry(name));
                // Transfer bytes from the ZIP file to the output file
                in = source.getInputStream(zipEnt);
                write(in,out,buffer);
            }
        }
        //最后写入patch中除dex以外的内容
        e = patch.entries();
        while (e.hasMoreElements()) {
            ZipEntry zipEnt = (ZipEntry) e.nextElement();
            if(shouldSkip(zipEnt)){
                continue;
            }
            String name = zipEnt.getName();
            // Add ZIP entry to output stream.
            out.putNextEntry(new ZipEntry(name));
            // Transfer bytes from the ZIP file to the output file
            in = source.getInputStream(zipEnt);
            write(in,out,buffer);
        }
        out.closeEntry();
        out.close();
    }

    private static boolean shouldSkip(ZipEntry entry){
       return entry.getName().endsWith(".dex") || entry.getName().startsWith("META-INF");
    }

    private static void write(InputStream in,OutputStream out,byte[] buffer) throws IOException{
        int length = in.read(buffer);
        while (length != -1) {
            out.write(buffer, 0, length);
            length = in.read(buffer);
        }
        closeQuitely(in);
    }

    private static void closeQuitely(InputStream in){
        if(in!=null){
            try{
                in.close();
            }catch(Exception e){}
        }
    }

    private static void performEntryExtractions(ZipFile bundleFile, ZipEntry entry, String targetPath) throws IOException {
        if (entry != null) {
            int numAttempts = 0;
            BufferedOutputStream bos = null;
            while (numAttempts < MAX_EXTRACT_ATTEMPTS) {
                try {
                    numAttempts++;
                    String fileDir = targetPath.substring(0, targetPath.lastIndexOf("/"));
                    File fileDirFile = new File(fileDir);
                    if (!fileDirFile.exists()) {
                        fileDirFile.mkdirs();
                    }
                    File file = new File(targetPath);
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    bos = new BufferedOutputStream(
                            new FileOutputStream(targetPath));

                    BufferedInputStream bi = new BufferedInputStream(bundleFile.getInputStream(entry));
                    byte[] readContent = new byte[BUFFER_SIZE];
                    int readCount = bi.read(readContent);
                    while (readCount != -1) {
                        bos.write(readContent, 0, readCount);
                        readCount = bi.read(readContent);
                    }
                    break;
                } catch (Throwable e) {
                } finally {
                    try {
                        if (bos != null)
                            bos.close();
                    } catch (Throwable e) {
                    }
                }
            }
        }
    }


    private static void getNewBundleDex(String oldDexPath, String patchPath, String targetPath, String md5) throws IOException {
        //合并
        int ret = 0;
        try {
            ret = BSPatch.bspatch(oldDexPath, targetPath, patchPath);
        } catch (Error e) {
            throw new IOException(e);
        }
        if (ret == 1) {
            //合并成功则校验MD5
            String newApkMD5 = UpdateUtils.getMD5(targetPath);
            if (newApkMD5 != null && newApkMD5.equals(md5)) {
                return;
            } else {
                throw new IOException("patch dex fail");
            }
        }
    }

}
