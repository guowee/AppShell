jarsigner -verbose -keystore 你的签名文件 -storepass 密码  -keypass alias的密码 -sigfile CERT -digestalg SHA1 -sigalg MD5withRSA  -signedjar 签名后的文件 签名前的apk 签名文件的alias名称

eg:

jarsigner -verbose -keystore secure.jks -storepass androidsecure -keypass androidsecure -sigfile CERT -digestalg SHA1 -sigalg MD5withRSA -signedjar payload-release.apk payload.apk secure

签名文件的密码：androidsecure
alais的密码：androidsecure
