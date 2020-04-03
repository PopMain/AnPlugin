package com.paomian.myplugin.plugin;

import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;

public class Util {

   public static class V_14 {
        protected static Field sPathListField;
        private static Constructor sDexElementConstructor;
        private static Class sDexElementClass;
        private static Field sDexElementsField;

       public static boolean expandDexPathList(ClassLoader cl,
                                               String[] dexPaths, DexFile[] dexFiles) {
           try {
               int N = dexPaths.length;
               Object[] elements = new Object[N];
               for (int i = 0; i < N; i++) {
                   String dexPath = dexPaths[i];
                   File pkg = new File(dexPath);
                   DexFile dexFile = dexFiles[i];
                   elements[i] = makeDexElement(pkg, dexFile, false);
               }

               fillDexPathList(cl, elements);
           } catch (Exception e) {
               e.printStackTrace();
               return false;
           }
           return true;
       }

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
               case 3 :
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

   }
}
