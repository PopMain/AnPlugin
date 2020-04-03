package com.paomian.myplugin.util;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtil {

    /**
     * 解压APK, 并且返回lib下的所有目录
     * @param zipPath
     * @param outPutFileName
     * @return
     * @throws Exception
     */
    public static String[] unZipApk(String zipPath, String outPutFileName) throws Exception {
        ZipInputStream inZip = new ZipInputStream(new FileInputStream(zipPath));
        File apkFile = new File(zipPath);
        File parent = apkFile.getParentFile();
        String outPutFilePath = parent + File.separator + outPutFileName;
        ZipEntry zipEntry;
        String szName = "";
        while ((zipEntry = inZip.getNextEntry()) != null) {
            szName = zipEntry.getName();
            if (zipEntry.isDirectory()) {
                //获取部件的文件夹名
                szName = szName.substring(0, szName.length() - 1);
                File folder = new File(outPutFilePath + File.separator + szName);
                folder.mkdirs();
            } else {
                Log.e("wzx", outPutFilePath + File.separator + szName);
                File file = new File(outPutFilePath + File.separator + szName);
                if (!file.exists()) {
                    Log.e("wzx", "Create the file:" + outPutFilePath + File.separator + szName);
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                // 获取文件的输出流
                FileOutputStream out = new FileOutputStream(file);
                int len;
                byte[] buffer = new byte[1024];
                // 读取（字节）字节到缓冲区
                while ((len = inZip.read(buffer)) != -1) {
                    // 从缓冲区（0）位置写入（字节）字节
                    out.write(buffer, 0, len);
                    out.flush();
                }
                out.close();
            }
        }
        inZip.close();

        File libPath = new File(outPutFilePath + File.separator +"lib"+File.separator);
        if (libPath.exists()) {
            Log.e("wxz", "lib path " + libPath);
            String[] libPaths = libPath.list();
            if (libPaths != null) {
                String[] result = new String[libPaths.length];
                for (int i = 0; i < libPaths.length; i++) {
                    result[i] = libPath + File.separator + libPaths[i];
                    Log.e("wzx", "lib " + result[i]);
                }
                return result;
            }
        }
        return null;
    }
}
