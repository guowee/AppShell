# AppShell
利用动态加载技术实现APK加壳

## Apk加壳原理
[Apk加壳原理及用法](https://blog.csdn.net/jiangwei0910410003/article/details/48415225) 参考这篇博客

## 说明

### AndroidShell 是一个Android Studio项目工程，其中包含了app，secure，secure-jni三个module.
app -- 源APP，即待加壳的apk (demo.apk)
secure -- 壳APP, 使用java实现加壳的过程.(shell.apk)
secure-jni -- 壳APP，使用JNI实现加壳的过程.(shell.apk)

### AndroidShellTools 一个对apk加密并将apk与dex合并的工具
主要功能是加密demo.apk,然后将加密后的demo.apk数据与shell.dex数据合并为classes.dex，修改classes.dex文件的头部，并添加apk文件的大小至尾部。

### Tools 用于对apk进行签名的工具
主要功能是对payload.apk签名，生成签名后的payload-release.apk

#### apk签名指令介绍

jarsigner -verbose -keystore 你的签名文件 -storepass 密码  -keypass alias的密码 -sigfile CERT -digestalg SHA1 -sigalg MD5withRSA  -signedjar 签名后的文件 签名前的apk 签名文件的alias名称

eg:

jarsigner -verbose -keystore secure.jks -storepass androidsecure -keypass androidsecure -sigfile CERT -digestalg SHA1 -sigalg MD5withRSA -signedjar payload-release.apk payload.apk secure

签名文件的密码：androidsecure
alais的密码：androidsecure