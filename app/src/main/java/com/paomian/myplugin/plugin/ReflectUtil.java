package com.paomian.myplugin.plugin;

import android.content.Context;
import android.os.Build;
import android.os.FileUtils;
import android.util.Log;

import com.paomian.myplugin.MainApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
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


}
