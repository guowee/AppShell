package com.missile.secure;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.sql.Ref;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import dalvik.system.DexClassLoader;


public class ProxyApplication extends Application {

    private static final String appkey = "APPLICATION_CLASS_NAME";
    private String apkFileName;
    private String odexPath;
    private String libPath;


    /**
     * 思路：可以将我们使用的DexClassLoader加载器绑定到系统加载Activity的类加载器上，替换LoadedApk中的mClassLoader
     * 如何获取LoadedApk? 需要从ActivityThread入手，在ActivityThread中有一个ArrayMap存放Apk包名和LoadedApk映射关系的数据结构，
     * 就从这里入手，使用反射获取LoadedApk中的mClassLoader，然后将它替换成源apk使用的DexClassLoader加载器
     *
     * @param base
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            File odex = this.getDir("payload_odex", MODE_PRIVATE);
            File libs = this.getDir("payload_lib", MODE_PRIVATE);
            odexPath = odex.getAbsolutePath();
            libPath = libs.getAbsolutePath();

            apkFileName = odex.getAbsolutePath() + "/payload.apk";
            File dexFile = new File(apkFileName);

            if (!dexFile.exists()) {
                dexFile.createNewFile();  //在payload_odex文件夹内，创建payload.apk
                // 读取程序classes.dex文件
                byte[] dexdata = readDexFileFromApk();

                // 分离出解壳后的apk文件已用于动态加载
                splitPayLoadFromDex(dexdata);
            }

            Object currentActivityThread = RefInvoke.invokeStaticMethod(
                    "android.app.ActivityThread", "currentActivityThread",
                    new Class[]{}, new Object[]{});

            String packageName = getPackageName();

            ArrayMap mPackages = (ArrayMap) RefInvoke.getFieldObject(
                    "android.app.ActivityThread", currentActivityThread,
                    "mPackages");
            WeakReference wr = (WeakReference) mPackages.get(packageName);  //获取当前的LoadedApk

            //这里，需要注意的是，DexClassLoader的最后一个参数，是DexClassLoader的parent，这里需要设置成PathClassLoader类，
            // 因为我们上面虽然说是替换PathClassLoader为DexClassLoader,
            // 但是PathClassLoader是系统本身默认的类加载器(也就是mClassLoader变量的值，我们如果单独的将DexClassLoader设置为mClassLoader的值的话，就会出错的)，
            // 所以一定要讲DexClassLoader的父加载器设置成PathClassLoader，因为类加载器是符合双亲委派机制的。


            DexClassLoader dLoader = new DexClassLoader(apkFileName, odexPath, libPath,/*getClassLoader()*/
                    (ClassLoader) RefInvoke.getFieldObject("android.app.LoadedApk", wr.get(), "mClassLoader"));

            //替换ClassLoader
            RefInvoke.setFieldObject("android.app.LoadedApk", "mClassLoader", wr.get(), dLoader);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onCreate() {

        String appClassName = null;

        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);

            Bundle bundle = ai.metaData;

            if (bundle != null && bundle.containsKey(appkey)) {
                appClassName = bundle.getString(appkey);
            } else {
                return;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        Object currentActivityThread = RefInvoke.invokeStaticMethod("android.app.ActivityThread", "currentActivityThread",
                new Class[]{}, new Object[]{});

        Object mBoundApplication = RefInvoke.getFieldObject("android.app.ActivityThread", currentActivityThread,
                "mBoundApplication");

        Object loadedApkInfo = RefInvoke.getFieldObject("android.app.ActivityThread$AppBindData", mBoundApplication,
                "info");
        //把当前进程的mApplication 设置成了null
        RefInvoke.setFieldObject("android.app.LoadedApk", "mApplication", loadedApkInfo, null);
        Object oldApplication = RefInvoke.getFieldObject(
                "android.app.ActivityThread", currentActivityThread,
                "mInitialApplication");

        ArrayList<Application> mAllApplications = (ArrayList<Application>) RefInvoke
                .getFieldObject("android.app.ActivityThread",
                        currentActivityThread, "mAllApplications");
        mAllApplications.remove(oldApplication);//删除oldApplication

        ApplicationInfo appinfo_In_LoadedApk = (ApplicationInfo) RefInvoke
                .getFieldObject("android.app.LoadedApk", loadedApkInfo,
                        "mApplicationInfo");
        ApplicationInfo appinfo_In_AppBindData = (ApplicationInfo) RefInvoke
                .getFieldObject("android.app.ActivityThread$AppBindData",
                        mBoundApplication, "appInfo");
        appinfo_In_LoadedApk.className = appClassName;
        appinfo_In_AppBindData.className = appClassName;
        Application app = (Application) RefInvoke.invokeMethod(
                "android.app.LoadedApk", "makeApplication", loadedApkInfo,
                new Class[]{boolean.class, Instrumentation.class},
                new Object[]{false, null});//执行 makeApplication（false,null）
        RefInvoke.setFieldObject("android.app.ActivityThread",
                "mInitialApplication", currentActivityThread, app);


        ArrayMap mProviderMap = (ArrayMap) RefInvoke.getFieldObject(
                "android.app.ActivityThread", currentActivityThread,
                "mProviderMap");
        Iterator it = mProviderMap.values().iterator();
        while (it.hasNext()) {
            Object providerClientRecord = it.next();
            Object localProvider = RefInvoke.getFieldObject(
                    "android.app.ActivityThread$ProviderClientRecord",
                    providerClientRecord, "mLocalProvider");
            RefInvoke.setFieldObject("android.content.ContentProvider",
                    "mContext", localProvider, app);
        }

        app.onCreate();

    }

    /**
     * 从apk包里面获取dex文件内容（byte）
     *
     * @return
     * @throws IOException
     */
    private byte[] readDexFileFromApk() throws IOException {
        ByteArrayOutputStream dexByteArrayOutputStream = new ByteArrayOutputStream();
        ZipInputStream localZipInputStream = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(
                        this.getApplicationInfo().sourceDir)));
        while (true) {
            ZipEntry localZipEntry = localZipInputStream.getNextEntry();
            if (localZipEntry == null) {
                localZipInputStream.close();
                break;
            }
            if (localZipEntry.getName().equals("classes.dex")) {
                byte[] arrayOfByte = new byte[1024];
                while (true) {
                    int i = localZipInputStream.read(arrayOfByte);
                    if (i == -1)
                        break;
                    dexByteArrayOutputStream.write(arrayOfByte, 0, i);
                }
            }
            localZipInputStream.closeEntry();
        }
        localZipInputStream.close();
        return dexByteArrayOutputStream.toByteArray();
    }

    /**
     * 释放被加壳的apk文件，so文件
     *
     * @param
     * @throws IOException
     */
    private void splitPayLoadFromDex(byte[] apkdata) throws IOException {
        int ablen = apkdata.length;
        //取被加壳apk的长度   这里的长度取值，对应加壳时长度的赋值都可以做些简化
        byte[] dexlen = new byte[4];
        System.arraycopy(apkdata, ablen - 4, dexlen, 0, 4);
        ByteArrayInputStream bais = new ByteArrayInputStream(dexlen);
        DataInputStream in = new DataInputStream(bais);
        int readInt = in.readInt();
        System.out.println(Integer.toHexString(readInt));
        byte[] newdex = new byte[readInt];
        //把被加壳apk内容拷贝到newdex中
        System.arraycopy(apkdata, ablen - 4 - readInt, newdex, 0, readInt);
        //这里应该加上对于apk的解密操作，若加壳是加密处理的话
        //?

        //对源程序Apk进行解密
        newdex = decrypt(newdex);

        //写入apk文件
        File file = new File(apkFileName);
        try {
            FileOutputStream localFileOutputStream = new FileOutputStream(file);
            localFileOutputStream.write(newdex);
            localFileOutputStream.close();
        } catch (IOException localIOException) {
            throw new RuntimeException(localIOException);
        }

        //分析被加壳的apk文件
        ZipInputStream localZipInputStream = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(file)));
        while (true) {
            ZipEntry localZipEntry = localZipInputStream.getNextEntry();//不了解这个是否也遍历子目录，看样子应该是遍历的
            if (localZipEntry == null) {
                localZipInputStream.close();
                break;
            }
            //取出被加壳apk用到的so文件，放到 libPath中（data/data/包名/payload_lib)
            String name = localZipEntry.getName();
            if (name.startsWith("lib/") && name.endsWith(".so")) {
                File storeFile = new File(libPath + "/"
                        + name.substring(name.lastIndexOf('/')));
                storeFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(storeFile);
                byte[] arrayOfByte = new byte[1024];
                while (true) {
                    int i = localZipInputStream.read(arrayOfByte);
                    if (i == -1)
                        break;
                    fos.write(arrayOfByte, 0, i);
                }
                fos.flush();
                fos.close();
            }
            localZipInputStream.closeEntry();
        }
        localZipInputStream.close();


    }

   /* private byte[] decrypt(byte[] srcdata) {
        for (int i = 0; i < srcdata.length; i++) {
            srcdata[i] = (byte) (0xFF ^ srcdata[i]);
        }
        return srcdata;
    }*/


    private static byte[] decrypt(byte[] src) {
        try {
            String key = "8A1ED465A5942AD6";
            // DES算法要求有一个可信任的随机数源
            SecureRandom random = new SecureRandom();
            // 创建一个DESKeySpec对象
            DESKeySpec desKeySpec = new DESKeySpec(key.getBytes());
            // 创建一个密匙工厂
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            // 将DESKeySpec对象转换成SecretKey对象
            SecretKey secretkey = keyFactory.generateSecret(desKeySpec);
            // Cipher对象实际完成解密操作
            Cipher cipher = Cipher.getInstance("DES");
            // 用密匙初始化Cipher对象
            cipher.init(Cipher.DECRYPT_MODE, secretkey, random);
            // 真正开始解密操作
            return cipher.doFinal(src);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
