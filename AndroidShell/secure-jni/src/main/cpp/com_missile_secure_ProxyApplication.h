/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_missile_secure_ProxyApplication */

#ifndef _Included_com_missile_secure_ProxyApplication
#define _Included_com_missile_secure_ProxyApplication
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_missile_secure_ProxyApplication
 * Method:    decrypt
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_missile_secure_ProxyApplication_decrypt___3B
  (JNIEnv *, jobject, jbyteArray);

/*
 * Class:     com_missile_secure_ProxyApplication
 * Method:    decrypt
 * Signature: ([BLjava/lang/String;)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_missile_secure_ProxyApplication_decrypt___3BLjava_lang_String_2
  (JNIEnv *, jobject, jbyteArray, jstring);

/*
 * Class:     com_missile_secure_ProxyApplication
 * Method:    replaceDefaultClassLoader
 * Signature: (Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_com_missile_secure_ProxyApplication_replaceDefaultClassLoader
  (JNIEnv *, jobject, jstring, jint);

/*
 * Class:     com_missile_secure_ProxyApplication
 * Method:    originalAppCreate
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_missile_secure_ProxyApplication_originalAppCreate
  (JNIEnv *, jobject, jint);

#ifdef __cplusplus
}
#endif
#endif
