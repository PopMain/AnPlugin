package com.paomian.myplugin;

import androidx.appcompat.app.AppCompatActivity;
import dalvik.system.DexFile;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.paomian.myplugin.plugin.ReflectUtil;
import com.paomian.myplugin.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final TextView tv = findViewById(R.id.main_text);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        /**
                         * asset中的app2.apk复制到应用缓存目录 （data/user/0/com.paomian.myplugin/app_plugin/app2.apk）
                         */
                        File file = MainApplication.getApplication().getDir("plugin", Context.MODE_PRIVATE);
                        if (!file.exists()) {
                            file.mkdirs();
                        }
                        String fileName = "app2.apk";
                        File apkFile = new File(file, fileName);
                        InputStream inputStream = null;
                        try {
                            inputStream = MainApplication.getApplication().getAssets().open(fileName);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        FileOutputStream outputStream;
                        if (apkFile.exists()) {
                            apkFile.delete();
                        }
                        try {
                            apkFile.createNewFile();
                            outputStream = new FileOutputStream(apkFile);
                            byte[] buffer = new byte[1024];
                            int read = 0;
                            while ((read = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, read);
                            }
                            outputStream.flush();
                            outputStream.close();
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        String[] dexPath = new String[1];
                        DexFile[] dexFiles = new DexFile[1];
                        dexPath[0] = file.getAbsolutePath();
                        try {
                            /**
                             * opt dex文件保存路径 data/user/0/com.paomian.myplugin/app_plugin/app.dex
                             */
                            File outPutDexFile = new File(file, "app2.dex");
                            // loaddex: param1 Android/data/com.paomian.myplugin/plugin/app2.apk; param2 Android/data/com.paomian.myplugin/plugin/app.dex
                            Log.d("wzx", "pluginPath = " + file.getAbsolutePath() + " \n " +
                                    " apkFilePath " + apkFile.getAbsolutePath() + " \n " +
                                    " dexOutputPath " + outPutDexFile.getAbsolutePath());
                            DexFile dexFile = DexFile.loadDex(apkFile.getAbsolutePath(), outPutDexFile.getAbsolutePath(), 0);
                            dexFiles[0] = dexFile;
                            ClassLoader classLoader = MainApplication.getApplication().getClassLoader();
                            /**
                             * 把/data/user/0/com.paomian.myplugin/app_plugin/目录、DexFile路径加到 DexClassLoader.pathList.dexElements的数组中去
                             * pathList:dalvik.system.DexPathList
                             * dexElement: dalvik.system.DexPathList$Element
                             * 这一步是为了访问apk2的class
                             */
                            ReflectUtil.V_14.expandDexPathList(classLoader, dexPath, dexFiles, false);

                            /**
                             * 解压apk, 并把lib目录下的so目录加的DexClassLoader#pathList#nativeLibraryPathElements
                             * pathList:dalvik.system.DexPathList
                             * nativeLibraryPathElement:dalvik.system.DexPathList$NativeLibraryElement
                             * 这一步的目的是为了访问apk2 lib中的so
                             */
                           String[] libPaths =  FileUtil.unZipApk(apkFile.getAbsolutePath(),"zip");
                           if (libPaths != null) {
                               List<File> libFiles = new ArrayList<>();
                               for (int i =0 ; i < libPaths.length; i++) {
                                   File libFile = new File(libPaths[i]);
                                   libFiles.add(libFile);
                               }
                               ReflectUtil.V_14.expandNativeLibraryDirectories(classLoader, libFiles);
                           }
                            try {
                                Class clz = Class.forName("com.paomian.app2.util.NativeCaculator");
                                Method method = clz.getMethod("add", null);
                                final int result = (int) method.invoke(null, null);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tv.setText(String.valueOf(result));
                                    }
                                });
                                // TODO 资源处理
                                Class clzRu = Class.forName("com.paomian.app2.util.ResourceUtil");
                                Method method1 = clzRu.getMethod("getString", new Class[]{Context.class});
                                final String app2Str = (String) method1.invoke(null, MainApplication.getApplication());
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, app2Str, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            } catch (NoSuchMethodException e) {
                                e.printStackTrace();
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

            }
        });

    }
}
