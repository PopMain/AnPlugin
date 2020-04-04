package com.paomian.myplugin.plugin;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.FileUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.paomian.myplugin.MainApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;

public class ReflectUtil {

    public static class V_14 {
        protected static Field sPathListField;
        private static Constructor sDexElementConstructor;
        private static Class sDexElementClass;
        private static Field sDexElementsField;

        /**
         *添加dex 目录
         * @param cl
         * @param dexPaths
         * @param dexFiles
         * @param isDirectory
         * @return
         */
        public static boolean expandDexPathList(ClassLoader cl,
                                                String[] dexPaths, DexFile[] dexFiles, boolean isDirectory) {
            try {
                int N = dexPaths.length;
                Object[] elements = new Object[N];
                for (int i = 0; i < N; i++) {
                    String dexPath = dexPaths[i];
                    File pkg = new File(dexPath);
                    DexFile dexFile = dexFiles[i];
                    elements[i] = makeDexElement(pkg, dexFile, isDirectory);
                }

                fillDexPathList(cl, elements);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        /**
         * 构造 DexElement
         *
         * @param pkg
         * @param dexFile
         * @param isDirectory
         * @return
         * @throws Exception
         */
        private static Object makeDexElement(File pkg, DexFile dexFile, boolean isDirectory) throws Exception {
            if (sDexElementClass == null) {
                sDexElementClass = Class.forName("dalvik.system.DexPathList$Element");
            }
            if (sDexElementConstructor == null) {
                if (Build.VERSION.SDK_INT >= 26) {
                    sDexElementConstructor = sDexElementClass.getConstructor(new Class[]{DexFile.class, File.class});
                } else {
                    sDexElementConstructor = sDexElementClass.getConstructors()[0];
                }
            }
            Class<?>[] types = sDexElementConstructor.getParameterTypes();
            switch (types.length) {
                case 3:
                    if (types[1].equals(ZipFile.class)) {
                        ZipFile zipFile = null;
                        try {
                            zipFile = new ZipFile(pkg);
                            return sDexElementConstructor.newInstance(pkg, zipFile, dexFile);
                        } catch (Exception e) {
                            if (zipFile != null) {
                                zipFile.close();
                            }
                            throw e;
                        }
                    } else {
                        return sDexElementConstructor.newInstance(pkg, pkg, dexFile);
                    }

                case 2:
                    if (Build.VERSION.SDK_INT >= 26) {
                        //Only SDK >= 26
                        return sDexElementConstructor.newInstance(dexFile, pkg);
                    }
                    break;

                case 4:
                default:
                    if (isDirectory) {
                        return sDexElementConstructor.newInstance(pkg, true, null, null);
                    } else {
                        return sDexElementConstructor.newInstance(pkg, false, pkg, dexFile);
                    }
            }
            return null;
        }


        /**
         * 把dexelements拼接到DexClassLoader.pathList的dexElements的field里
         *
         * @param cl
         * @param elements
         * @throws NoSuchFieldException
         * @throws IllegalAccessException
         */
        private static void fillDexPathList(ClassLoader cl, Object[] elements)
                throws NoSuchFieldException, IllegalAccessException {
            if (sPathListField == null) {
                sPathListField = getDeclaredField(DexClassLoader.class.getSuperclass(), "pathList");
            }
            Object pathList = sPathListField.get(cl);
            if (sDexElementsField == null) {
                sDexElementsField = getDeclaredField(pathList.getClass(), "dexElements");
            }
            expandArray(pathList, sDexElementsField, elements, true);
        }

        private static Field getDeclaredField(Class cls, String fieldName) {
            try {
                Field field = cls.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                return null;
            }
        }

        private static void expandArray(Object target, Field arrField,
                                        Object[] extraElements, boolean push)
                throws IllegalAccessException {
            Object[] original = (Object[]) arrField.get(target);
            Object[] combined = (Object[]) Array.newInstance(
                    original.getClass().getComponentType(), original.length + extraElements.length);
            if (push) {
                System.arraycopy(extraElements, 0, combined, 0, extraElements.length);
                System.arraycopy(original, 0, combined, extraElements.length, original.length);
            } else {
                System.arraycopy(original, 0, combined, 0, original.length);
                System.arraycopy(extraElements, 0, combined, original.length, extraElements.length);
            }
            arrField.set(target, combined);
        }


        private static Field sDexPathList_nativeLibraryDirectories_field;
        private static Field sDexPathList_nativeLibraryPathElements_field;
        private static Class sNativeLibraryElementClass;
        private static Constructor sNativeLibraryElementConstructor;

        /**
         * 添加lib目录
         * @param classLoader
         * @param libPaths
         */
        public static void expandNativeLibraryDirectories(ClassLoader classLoader,
                                                          List<File> libPaths) {
            if (sPathListField == null) {
                sPathListField = getDeclaredField(DexClassLoader.class.getSuperclass(), "pathList");
            }

            Object pathList = getValue(sPathListField, classLoader);
            if (pathList == null) return;

            if (sDexPathList_nativeLibraryDirectories_field == null) {
                sDexPathList_nativeLibraryDirectories_field = getDeclaredField(
                        pathList.getClass(), "nativeLibraryDirectories");
                if (sDexPathList_nativeLibraryDirectories_field == null) return;
            }

            try {
                // List<File> nativeLibraryDirectories
                List<File> paths = getValue(sDexPathList_nativeLibraryDirectories_field, pathList);
                if (paths == null) return;
                paths.addAll(libPaths);

                // NativeLibraryElement[] nativeLibraryPathElements
                if (sDexPathList_nativeLibraryPathElements_field == null) {
                    sDexPathList_nativeLibraryPathElements_field = getDeclaredField(
                            pathList.getClass(), "nativeLibraryPathElements");
                }
                if (sDexPathList_nativeLibraryPathElements_field == null) return;

                int N = libPaths.size();
                Object[] elements = new Object[N];
                for (int i = 0; i < N; i++) {
                    Object dexElement = makeNativeLibraryElement(libPaths.get(i));
                    elements[i] = dexElement;
                }

                expandArray(pathList, sDexPathList_nativeLibraryPathElements_field, elements, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private static Object makeNativeLibraryElement(File libraryDir) throws Exception {
            if (sNativeLibraryElementClass == null) {
                sNativeLibraryElementClass = Class.forName("dalvik.system.DexPathList$NativeLibraryElement");
            }
            if (sNativeLibraryElementConstructor == null) {
                sNativeLibraryElementConstructor = sNativeLibraryElementClass.getConstructors()[0];
                sNativeLibraryElementConstructor.setAccessible(true);
            }
            Class<?>[] types = sNativeLibraryElementConstructor.getParameterTypes();
            switch (types.length) {
                case 1:
                    return sNativeLibraryElementConstructor.newInstance(libraryDir);
                case 2:
                default:
                    return sNativeLibraryElementConstructor.newInstance(libraryDir, null);
            }
        }

        private static <T> T getValue(Field field, Object target) {
            if (field == null) {
                return null;
            }

            try {
                return (T) field.get(target);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }
        }

    }

    private static Method getMethod(Class cls, String methodName, Class[] types) {
        Log.e("wzx", "method " + methodName);
        try {
            Method method = cls.getMethod(methodName, types);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            Log.e("wzx", Arrays.toString(e.getStackTrace()));
            return null;
        }
    }

    private static <T> T invoke(Method method, Object target, Object... args) {
        try {
            return (T) method.invoke(target, args);
        } catch (Exception e) {
            // Ignored
            e.printStackTrace();
            return null;
        }
    }

    private static ArrayMap<Object, WeakReference<Object>> sResourceImpls;
    private static Object/*ResourcesImpl*/ sMergedResourcesImpl;
    private static Method sAssetManager_addAssetPaths_method;
    private static Method sAssetManager_addAssetPath_method;

    public static void mergeResources(Application app, Object activityThread, String[] assetPaths) {
        AssetManager newAssetManager;
        if (Build.VERSION.SDK_INT < 24) {
            newAssetManager = newAssetManager();
        } else {
            // On Android 7.0+, this should contains a WebView asset as base. #347
            newAssetManager = app.getAssets();
        }
        addAssetPaths(newAssetManager, assetPaths);

        try {
            if (Build.VERSION.SDK_INT < 28) {
                Method mEnsureStringBlocks = AssetManager.class.getDeclaredMethod("ensureStringBlocks", new Class[0]);
                mEnsureStringBlocks.setAccessible(true);
                mEnsureStringBlocks.invoke(newAssetManager, new Object[0]);
            } else {
                // `AssetManager#ensureStringBlocks` becomes unavailable since android 9.0
            }

            Collection<WeakReference<Resources>> references;

            if (Build.VERSION.SDK_INT >= 19) {
                Class<?> resourcesManagerClass = Class.forName("android.app.ResourcesManager");
                Method mGetInstance = resourcesManagerClass.getDeclaredMethod("getInstance", new Class[0]);
                mGetInstance.setAccessible(true);
                Object resourcesManager = mGetInstance.invoke(null, new Object[0]);
                try {
                    Field fMActiveResources = resourcesManagerClass.getDeclaredField("mActiveResources");
                    fMActiveResources.setAccessible(true);

                    ArrayMap<?, WeakReference<Resources>> arrayMap = (ArrayMap)fMActiveResources.get(resourcesManager);

                    references = arrayMap.values();
                } catch (NoSuchFieldException ignore) {
                    Field mResourceReferences = resourcesManagerClass.getDeclaredField("mResourceReferences");
                    mResourceReferences.setAccessible(true);

                    references = (Collection) mResourceReferences.get(resourcesManager);
                }

                if (Build.VERSION.SDK_INT >= 24) {
                    Field fMResourceImpls = resourcesManagerClass.getDeclaredField("mResourceImpls");
                    fMResourceImpls.setAccessible(true);
                    sResourceImpls = (ArrayMap)fMResourceImpls.get(resourcesManager);
                }
            } else {
                Field fMActiveResources = activityThread.getClass().getDeclaredField("mActiveResources");
                fMActiveResources.setAccessible(true);

                HashMap<?, WeakReference<Resources>> map = (HashMap)fMActiveResources.get(activityThread);

                references = map.values();
            }

            //to array
            WeakReference[] referenceArrays = new WeakReference[references.size()];
            references.toArray(referenceArrays);

            for (int i = 0; i < referenceArrays.length; i++) {
                Resources resources = (Resources) referenceArrays[i].get();
                if (resources == null) continue;

                try {
                    Field mAssets = Resources.class.getDeclaredField("mAssets");
                    mAssets.setAccessible(true);
                    mAssets.set(resources, newAssetManager);
                } catch (Throwable ignore) {
                    Field mResourcesImpl = Resources.class.getDeclaredField("mResourcesImpl");
                    mResourcesImpl.setAccessible(true);
                    Object resourceImpl = mResourcesImpl.get(resources);
                    Field implAssets;
                    try {
                        implAssets = resourceImpl.getClass().getDeclaredField("mAssets");
                    } catch (NoSuchFieldException e) {
                        // Compat for MiUI 8+
                        implAssets = resourceImpl.getClass().getSuperclass().getDeclaredField("mAssets");
                    }
                    implAssets.setAccessible(true);
                    implAssets.set(resourceImpl, newAssetManager);

                    if (Build.VERSION.SDK_INT >= 24) {
                        if (resources == app.getResources()) {
                            sMergedResourcesImpl = resourceImpl;
                        }
                    }
                }

                resources.updateConfiguration(resources.getConfiguration(), resources.getDisplayMetrics());
            }

            if (Build.VERSION.SDK_INT >= 21) {
                for (int i = 0; i < referenceArrays.length; i++) {
                    Resources resources = (Resources) referenceArrays[i].get();
                    if (resources == null) continue;

                    // android.util.Pools$SynchronizedPool<TypedArray>
                    Field mTypedArrayPool = Resources.class.getDeclaredField("mTypedArrayPool");
                    mTypedArrayPool.setAccessible(true);
                    Object typedArrayPool = mTypedArrayPool.get(resources);
                    // Clear all the pools
                    Method acquire = typedArrayPool.getClass().getMethod("acquire");
                    acquire.setAccessible(true);
                    while (acquire.invoke(typedArrayPool) != null) ;
                }
            }
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    public static AssetManager newAssetManager() {
        AssetManager assets;
        try {
            assets = AssetManager.class.newInstance();
        } catch (InstantiationException e1) {
            e1.printStackTrace();
            return null;
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
            return null;
        }
        return assets;
    }

    public static int[] addAssetPaths(AssetManager assets, String[] paths) {
        if (Build.VERSION.SDK_INT < 28) {
            if (sAssetManager_addAssetPaths_method == null) {
                sAssetManager_addAssetPaths_method = getMethod(AssetManager.class,
                        "addAssetPaths", new Class[]{String[].class});
            }
            if (sAssetManager_addAssetPaths_method == null) return null;
            return invoke(sAssetManager_addAssetPaths_method, assets, new Object[]{paths});
        } else {
            // `AssetManager#addAssetPaths` becomes unavailable since android 9.0,
            // use recursively `addAssetPath` instead.
            int N = paths.length;
            int[] ids = new int[N];
            for (int i = 0; i < N; i++) {
                ids[i] = addAssetPath(assets, paths[i]);
            }
            return ids;
        }
    }

    public static int addAssetPath(AssetManager assets, String path) {
        if (sAssetManager_addAssetPath_method == null) {
            sAssetManager_addAssetPath_method = getMethod(AssetManager.class,
                    "addAssetPath", new Class[]{String.class});
        }
        if (sAssetManager_addAssetPath_method == null) return 0;
        Integer ret = invoke(sAssetManager_addAssetPath_method, assets, path);
        if (ret == null) return 0;
        return ret;
    }


}
