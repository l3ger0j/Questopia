#include <string.h>
#include <stdlib.h>
#include "android/log.h"
#include "jni.h"
#include "qsp/bindings/android/android.h"

JNIEnv *qspJniEnv;
jobject qspCallbacksObj;

void Java_org_qp_android_model_libQP_NativeMethods_QSPInit(JNIEnv *env,
                                                           jobject this) {
    qspJniEnv = env;

    jclass cls = (*env)->GetObjectClass(env, this);
    jfieldID fid = (*env)->GetFieldID(env, cls, "callbacks",
                                      "Lorg/qp/android/model/libQP/LibQpCallbacks;");
    jobject callbacks = (*env)->GetObjectField(env, this, fid);
    qspCallbacksObj = (*env)->NewGlobalRef(env, callbacks);

    QSPInit();
}

void Java_org_qp_android_model_libQP_NativeMethods_QSPDeInit(JNIEnv *env,
                                                             jobject this) {
    QSPDeInit();
    (*env)->DeleteGlobalRef(env, qspCallbacksObj);
    qspCallbacksObj = NULL;
}

jboolean Java_org_qp_android_model_libQP_NativeMethods_QSPIsInCallBack(JNIEnv *env,
                                                                       jobject this) {
    return QSPIsInCallBack();
}

void Java_org_qp_android_model_libQP_NativeMethods_QSPEnableDebugMode(JNIEnv *env,
                                                                      jobject this,
                                                                      jboolean isDebug) {
    QSPEnableDebugMode((QSP_BOOL) isDebug);
}

jobject Java_org_qp_android_model_libQP_NativeMethods_QSPGetCurStateData(JNIEnv *env,
                                                                         jobject this) {
    //!!!STUB
    //QSPGetCurStateData(jstring *loc, (int *)actIndex, (int *)line);
    return NULL;
}

jstring Java_org_qp_android_model_libQP_NativeMethods_QSPGetVersion(JNIEnv *env,
                                                                    jobject this) {
    char *sz = qspW2C(QSPGetVersion());
    jstring result = (*env)->NewStringUTF(env, sz);
    if (sz != NULL)
        free(sz);
    return result;
}

jstring Java_org_qp_android_model_libQP_NativeMethods_QSPGetCompiledDateTime(JNIEnv *env,
                                                                             jobject this) {
    char *sz = qspW2C(QSPGetCompiledDateTime());
    jstring result = (*env)->NewStringUTF(env, sz);
    if (sz != NULL)
        free(sz);
    return result;
}

///* Number of complete location updates */
jint Java_org_qp_android_model_libQP_NativeMethods_QSPGetFullRefreshCount(JNIEnv *env,
                                                                          jobject this) {
    return QSPGetFullRefreshCount();
}

///* ------------------------------------------------------------ */
///* Full path to the downloaded game file */
jstring Java_org_qp_android_model_libQP_NativeMethods_QSPGetQstFullPath(JNIEnv *env,
                                                                        jobject this) {
    char *sz = qspW2C(QSPGetQstFullPath());
    jstring result = (*env)->NewStringUTF(env, sz);
    if (sz != NULL)
        free(sz);
    return result;
}

///* ------------------------------------------------------------ */
///* Name of the current location */
jstring Java_org_qp_android_model_libQP_NativeMethods_QSPGetCurLoc(JNIEnv *env,
                                                                   jobject this) {
    char *sz = qspW2C(QSPGetCurLoc());
    jstring result = (*env)->NewStringUTF(env, sz);
    if (sz != NULL)
        free(sz);
    return result;
}

///* ------------------------------------------------------------ */
///* Basic description of the location */
//
///* Text of the main location description window */
jstring Java_org_qp_android_model_libQP_NativeMethods_QSPGetMainDesc(JNIEnv *env,
                                                                     jobject this) {
    char *sz = qspW2C(QSPGetMainDesc());
    jstring result = (*env)->NewStringUTF(env, sz);
    if (sz != NULL)
        free(sz);
    return result;
}

///* The ability to change the text of the main description */
jboolean Java_org_qp_android_model_libQP_NativeMethods_QSPIsMainDescChanged(JNIEnv *env,
                                                                            jobject this) {
    return QSPIsMainDescChanged();
}

///* ------------------------------------------------------------ */
///* Additional description of the location */
//
///* Text of the additional location description window */
jstring Java_org_qp_android_model_libQP_NativeMethods_QSPGetVarsDesc(JNIEnv *env,
                                                                     jobject this) {
    char *sz = qspW2C(QSPGetVarsDesc());
    jstring result = (*env)->NewStringUTF(env, sz);
    if (sz != NULL)
        free(sz);
    return result;
}

///* The ability to change the text of the additional description */
//QSP_BOOL QSPIsVarsDescChanged()
jboolean Java_org_qp_android_model_libQP_NativeMethods_QSPIsVarsDescChanged(JNIEnv *env,
                                                                            jobject this) {
    return QSPIsVarsDescChanged();
}

///* ------------------------------------------------------------ */
///* Get the value of the specified expression */
//(const QSP_CHAR *expr, QSP_BOOL *isString, int *numVal, QSP_CHAR *strVal, int strValBufSize)
jobject Java_org_qp_android_model_libQP_NativeMethods_QSPGetExprValue(JNIEnv *env,
                                                                      jobject this) {
    //!!!STUB
    //{
    //	QSPVariant v;
    //	if (qspIsExitOnError && qspErrorNum) return QSP_FALSE;
    //	qspResetError();
    //	if (qspIsDisableCodeExec) return QSP_FALSE;
    //	v = qspExprValue((QSP_CHAR *)expr);
    //	if (qspErrorNum) return QSP_FALSE;
    //	*isString = v.IsStr;
    //	if (v.IsStr)
    //	{
    //		qspStrNCopy(strVal, QSP_STR(v), strValBufSize - 1);
    //		free(QSP_STR(v));
    //		strVal[strValBufSize - 1] = 0;
    //	}
    //	else
    //		*numVal = QSP_NUM(v);
    //	return QSP_TRUE;
    //}
    return NULL;
}

///* ------------------------------------------------------------ */
///* Text of the input line */
void Java_org_qp_android_model_libQP_NativeMethods_QSPSetInputStrText(JNIEnv *env,
                                                                      jobject this,
                                                                      jstring val) {
    const char *str = (*env)->GetStringUTFChars(env, val, NULL);
    if (str == NULL)
        return;
    QSP_CHAR *strConverted = qspC2W(str);

    QSPSetInputStrText(strConverted);

    (*env)->ReleaseStringUTFChars(env, val, str);
}

///* ------------------------------------------------------------ */
///* List of actions */
//
///* Number of actions */
jint Java_org_qp_android_model_libQP_NativeMethods_QSPGetActionsCount(JNIEnv *env,
                                                                      jobject this) {
    return QSPGetActionsCount();
}

///* These actions with the specified index */
//void QSPGetActionData(int ind, QSP_CHAR **image, QSP_CHAR **desc)
jobject Java_org_qp_android_model_libQP_NativeMethods_QSPGetActionData(JNIEnv *env,
                                                                       jobject this,
                                                                       jint ind) {
    char *qspImgFileName;
    char *qspActName;
    QSPGetActionData(ind, &qspImgFileName, &qspActName);

    char *sz = qspW2C(qspActName);
    char *isz = qspW2C(qspImgFileName);
    jstring actName = (*env)->NewStringUTF(env, sz);
    jstring actImg = (*env)->NewStringUTF(env, isz);
    if (sz != NULL)
        free(sz);
    if (isz != NULL)
        free(isz);

    jclass clazz = (*env)->FindClass(env, "org/qp/android/dto/libQP/ActionData");
    if (clazz == 0)
        return 0;
    jobject obj = (*env)->AllocObject(env, clazz);
    jfieldID fid = (*env)->GetFieldID(env, clazz, "name", "Ljava/lang/String;");
    jfieldID fid2 = (*env)->GetFieldID(env, clazz, "image", "Ljava/lang/String;");
    (*env)->DeleteLocalRef(env, clazz);
    if (fid == 0 || fid2 == 0)
        return 0;
    (*env)->SetObjectField(env, obj, fid, actName);
    (*env)->SetObjectField(env, obj, fid2, actImg);

    return obj;
}

///* Executing the code of the selected action */
jboolean Java_org_qp_android_model_libQP_NativeMethods_QSPExecuteSelActionCode(JNIEnv *env,
                                                                               jobject this,
                                                                               jboolean isRefresh) {
    return QSPExecuteSelActionCode((QSP_BOOL) isRefresh);
}

///* Set the index of the selected action */
jboolean Java_org_qp_android_model_libQP_NativeMethods_QSPSetSelActionIndex(JNIEnv *env,
                                                                            jobject this,
                                                                            jint ind,
                                                                            jboolean isRefresh) {
    return QSPSetSelActionIndex(ind, (QSP_BOOL) isRefresh);
}

///* Get the index of the selected action */
jint Java_org_qp_android_model_libQP_NativeMethods_QSPGetSelActionIndex(JNIEnv *env,
                                                                        jobject this) {
    return QSPGetSelActionIndex();
}

///* Ability to change the list of actions */
jboolean Java_org_qp_android_model_libQP_NativeMethods_QSPIsActionsChanged(JNIEnv *env,
                                                                           jobject this) {
    return QSPIsActionsChanged();
}

///* ------------------------------------------------------------ */
///* List of objects */
//
///* Number of objects */
jint Java_org_qp_android_model_libQP_NativeMethods_QSPGetObjectsCount(JNIEnv *env,
                                                                      jobject this) {
    return QSPGetObjectsCount();
}

///* Object data with the specified index */
//void QSPGetObjectData(int ind, QSP_CHAR **image, QSP_CHAR **desc)
jobject Java_org_qp_android_model_libQP_NativeMethods_QSPGetObjectData(JNIEnv *env,
                                                                       jobject this,
                                                                       jint ind) {
    char *qspImgFileName;
    char *qspObjName;
    QSPGetObjectData(ind, &qspImgFileName, &qspObjName);

    char *sz = qspW2C(qspObjName);
    jstring objName = (*env)->NewStringUTF(env, sz);
    if (sz != NULL)
        free(sz);

    char *isz = qspW2C(qspImgFileName);
    jstring objImg = (*env)->NewStringUTF(env, isz);
    if (isz != NULL)
        free(isz);

    jclass clazz = (*env)->FindClass(env, "org/qp/android/dto/libQP/ObjectData");
    if (clazz == 0)
        return 0;
    jobject obj = (*env)->AllocObject(env, clazz);
    jfieldID fid = (*env)->GetFieldID(env, clazz, "name", "Ljava/lang/String;");
    jfieldID fid2 = (*env)->GetFieldID(env, clazz, "image", "Ljava/lang/String;");
    (*env)->DeleteLocalRef(env, clazz);
    if (fid == 0 || fid2 == 0)
        return 0;
    // Set the major field to the operating system's major version.
    (*env)->SetObjectField(env, obj, fid, objName);
    (*env)->SetObjectField(env, obj, fid2, objImg);

    return obj;
}

///* Set the index of the selected object */
jboolean Java_org_qp_android_model_libQP_NativeMethods_QSPSetSelObjectIndex(JNIEnv *env,
                                                                            jobject this,
                                                                            jint ind,
                                                                            jboolean isRefresh) {
    return QSPSetSelObjectIndex(ind, (QSP_BOOL) isRefresh);
}

///* Get the index of the selected object */
jint Java_org_qp_android_model_libQP_NativeMethods_QSPGetSelObjectIndex(JNIEnv *env,
                                                                        jobject this) {
    return QSPGetSelObjectIndex();
}

///* Ability to change the list of objects */
jboolean Java_org_qp_android_model_libQP_NativeMethods_QSPIsObjectsChanged(JNIEnv *env,
                                                                           jobject this) {
    return QSPIsObjectsChanged();
}

///* ------------------------------------------------------------ */
///* Variables */
//
///* Get the number of array elements */
//QSP_BOOL QSPGetVarValuesCount(const QSP_CHAR *name, int *count)
jobject Java_org_qp_android_model_libQP_NativeMethods_QSPGetVarValuesCount(JNIEnv *env,
                                                                           jobject this,
                                                                           jstring name) {
    //!!!STUB
    //{
    //	QSPVar *var;
    //	if (qspIsExitOnError && qspErrorNum) return QSP_FALSE;
    //	qspResetError();
    //	var = qspVarReference((QSP_CHAR *)name, QSP_FALSE);
    //	if (qspErrorNum) return QSP_FALSE;
    //	*count = var->ValsCount;
    //	return QSP_TRUE;
    //}
    return NULL;
}

///* Get the values of the specified array element */
//QSP_BOOL QSPGetVarValues(const QSP_CHAR *name, int ind, int *numVal, QSP_CHAR **strVal)
jobject Java_org_qp_android_model_libQP_NativeMethods_QSPGetVarValues(JNIEnv *env,
                                                                      jobject this,
                                                                      jstring name,
                                                                      jint ind) {
    //Convert array name to QSP string
    const char *str = (*env)->GetStringUTFChars(env, name, NULL);
    if (str == NULL)
        return NULL;
    QSP_CHAR *strConverted = qspC2W(str);

    //Call QSP function
    int numVal = 0;
    char *strVal;
    QSP_BOOL result = QSPGetVarValues(strConverted, (int) ind, &numVal, &strVal);

    // Attempt to find the GetVarValuesResponse class.
    jclass clazz = (*env)->FindClass(env, "org/qp/android/dto/libQP/GetVarValuesResponse");
    // If this class does not exist then return null.
    if (clazz == 0)
        return NULL;
    jobject obj = (*env)->AllocObject(env, clazz);

    jfieldID successFid = (*env)->GetFieldID(env, clazz, "isSuccess", "Z");
    if (successFid == 0)
        return NULL;
    if (result == QSP_TRUE) {
        (*env)->SetBooleanField(env, obj, successFid, JNI_TRUE);

        char *sz = qspW2C(strVal);
        jstring jstringVal = (*env)->NewStringUTF(env, sz);
        if (sz != NULL)
            free(sz);

        jfieldID stringValueFid = (*env)->GetFieldID(env, clazz, "stringValue",
                                                     "Ljava/lang/String;");
        if (stringValueFid == 0)
            return NULL;
        (*env)->SetObjectField(env, obj, stringValueFid, jstringVal);

        jfieldID intValueFid = (*env)->GetFieldID(env, clazz, "intValue", "I");
        if (intValueFid == 0)
            return NULL;
        (*env)->SetIntField(env, obj, intValueFid, numVal);
    } else {
        (*env)->SetBooleanField(env, obj, successFid, JNI_FALSE);
    }

    (*env)->DeleteLocalRef(env, clazz);
    (*env)->ReleaseStringUTFChars(env, name, str);
    return obj;
}

///* Get the maximum number of variables */
jint Java_org_qp_android_model_libQP_NativeMethods_QSPGetMaxVarsCount(JNIEnv *env,
                                                                      jobject this) {
    return QSPGetMaxVarsCount();
}

///* Get the variable name with the specified index */
//QSP_BOOL QSPGetVarNameByIndex(int index, QSP_CHAR **name)
jobject Java_org_qp_android_model_libQP_NativeMethods_QSPGetVarNameByIndex(JNIEnv *env,
                                                                           jobject this,
                                                                           jint index) {
    //!!!STUB
//{
//	if (index < 0 || index >= QSP_VARSCOUNT || !qspVars[index].Name) return QSP_FALSE;
//	*name = qspVars[index].Name;
//	return QSP_TRUE;
//}
    return NULL;
}

///* ------------------------------------------------------------ */
///* Code Execution */
//
///* Executing a line of code */
jboolean Java_org_qp_android_model_libQP_NativeMethods_QSPExecString(JNIEnv *env,
                                                                     jobject this,
                                                                     jstring s,
                                                                     jboolean isRefresh) {
    const char *str = (*env)->GetStringUTFChars(env, s, NULL);
    if (str == NULL)
        return JNI_FALSE;
    QSP_CHAR *strConverted = qspC2W(str);

    jboolean result = QSPExecString(strConverted, (QSP_BOOL) isRefresh);

    (*env)->ReleaseStringUTFChars(env, s, str);
    return result;
}

///* Executing the code of the specified location */
jboolean Java_org_qp_android_model_libQP_NativeMethods_QSPExecLocationCode(JNIEnv *env,
                                                                           jobject this,
                                                                           jstring name,
                                                                           jboolean isRefresh) {
    const char *str = (*env)->GetStringUTFChars(env, name, NULL);
    if (str == NULL)
        return JNI_FALSE;
    QSP_CHAR *strConverted = qspC2W(str);

    jboolean result = QSPExecLocationCode(strConverted, (QSP_BOOL) isRefresh);

    (*env)->ReleaseStringUTFChars(env, name, str);
    return result;
}

///* Execution of the location-counter code */
jboolean Java_org_qp_android_model_libQP_NativeMethods_QSPExecCounter(JNIEnv *env,
                                                                      jobject this,
                                                                      jboolean isRefresh) {
    return QSPExecCounter((QSP_BOOL) isRefresh);
}

///* Execution of the location-handler code of the input string */
jboolean Java_org_qp_android_model_libQP_NativeMethods_QSPExecUserInput(JNIEnv *env,
                                                                        jobject this,
                                                                        jboolean isRefresh) {
    return QSPExecUserInput((QSP_BOOL) isRefresh);
}

///* ------------------------------------------------------------ */
///* Error */
//
///* Get information about the latest error */
jobject Java_org_qp_android_model_libQP_NativeMethods_QSPGetLastErrorData(JNIEnv *env,
                                                                          jobject this) {
    jclass clazz = (*env)->FindClass(env, "org/qp/android/dto/libQP/ErrorData");
    if (clazz == 0)
        return NULL;
    jfieldID fid = (*env)->GetFieldID(env, clazz, "locName", "Ljava/lang/String;");
    jfieldID fid2 = (*env)->GetFieldID(env, clazz, "errorNum", "I");
    jfieldID fid3 = (*env)->GetFieldID(env, clazz, "index", "I");
    jfieldID fid4 = (*env)->GetFieldID(env, clazz, "line", "I");
    if (fid == 0 || fid2 == 0 || fid3 == 0 || fid4 == 0)
        return NULL;
    jobject obj = (*env)->AllocObject(env, clazz);
    (*env)->DeleteLocalRef(env, clazz);

    int errorNum;
    char *locName;
    int index;
    int line;

    QSPGetLastErrorData(&errorNum, &locName, &index, &line);

    char *sz = qspW2C(locName);
    jstring jLocName = (*env)->NewStringUTF(env, sz);
    if (sz != NULL)
        free(sz);

    (*env)->SetObjectField(env, obj, fid, jLocName);
    (*env)->SetIntField(env, obj, fid2, errorNum);
    (*env)->SetIntField(env, obj, fid3, index);
    (*env)->SetIntField(env, obj, fid4, line);
    return obj;
}

///* Get a description of the error by its number */
jstring Java_org_qp_android_model_libQP_NativeMethods_QSPGetErrorDesc(JNIEnv *env,
                                                                      jobject this,
                                                                      jint errorNum) {
    char *sz = qspW2C(QSPGetErrorDesc(errorNum));
    jstring result = (*env)->NewStringUTF(env, sz);
    if (sz != NULL)
        free(sz);
    return result;
}

///* ------------------------------------------------------------ */
///* Game Management */
//
///* Loading a new game from memory */
jboolean Java_org_qp_android_model_libQP_NativeMethods_QSPLoadGameWorldFromData(JNIEnv *env,
                                                                                jobject this,
                                                                                jbyteArray data,
                                                                                jint dataSize,
                                                                                jstring fileName) {
    //converting data
    jbyte *jbuf = malloc(dataSize * sizeof(jbyte));
    if (jbuf == NULL)
        return JNI_FALSE;

    (*env)->GetByteArrayRegion(env, data, 0, dataSize, jbuf);
    int size = dataSize;
    char *mydata = (char *) jbuf;

    /* assume the prompt string and user input has less than 128
        characters */
    int fileNameLen = (*env)->GetStringLength(env, fileName) + 1;
    char buf[fileNameLen];
    const jbyte *str;
    str = (*env)->GetStringUTFChars(env, fileName, NULL);
    if (str == NULL) {
        free(jbuf);
        return JNI_FALSE; /* OutOfMemoryError already thrown */
    }

    QSP_CHAR *wcs = qspC2W(str);
    jboolean result = QSPLoadGameWorldFromData(mydata, size, wcs);
    (*env)->ReleaseStringUTFChars(env, fileName, str);
    free(wcs);

    free(jbuf);
    return result;
}

///* Saving the state to memory */
jbyteArray Java_org_qp_android_model_libQP_NativeMethods_QSPSaveGameAsData(JNIEnv *env,
                                                                           jobject this,
                                                                           jboolean isRefresh) {
    void *buffer = NULL;
    int bufferSize = 0;
    if (QSPSaveGameAsData(&buffer, &bufferSize, (QSP_BOOL) isRefresh) == QSP_FALSE)
        return NULL;

    jbyteArray result;
    result = (*env)->NewByteArray(env, bufferSize);
    if (result == NULL)
        return NULL;

    (*env)->SetByteArrayRegion(env, result, 0, bufferSize, buffer);

    return result;
}

///* Loading state from memory */
jboolean Java_org_qp_android_model_libQP_NativeMethods_QSPOpenSavedGameFromData(JNIEnv *env,
                                                                                jobject this,
                                                                                jbyteArray data,
                                                                                jint dataSize,
                                                                                jboolean isRefresh) {
    //converting data
    jbyte *jbuf = malloc(dataSize * sizeof(jbyte));
    if (jbuf == NULL)
        return JNI_FALSE;

    (*env)->GetByteArrayRegion(env, data, 0, dataSize, jbuf);
    int size = dataSize;
    void *mydata = (void *) jbuf;

    jboolean result = QSPOpenSavedGameFromData(mydata, size, (QSP_BOOL) isRefresh) == QSP_TRUE;

    free(jbuf);
    return result;
}

///* Restarting the game */
jboolean Java_org_qp_android_model_libQP_NativeMethods_QSPRestartGame(JNIEnv *env,
                                                                      jobject this,
                                                                      jboolean isRefresh) {
    return QSPRestartGame((QSP_BOOL) isRefresh);
}

///* ------------------------------------------------------------ */
///* Menu */
void Java_org_qp_android_model_libQP_NativeMethods_QSPSelectMenuItem(JNIEnv *env,
                                                                     jobject this,
                                                                     jint ind) {
    QSPSelectMenuItem(ind);
}
///* ------------------------------------------------------------ */
///* Installing CALLBACKS */
//void QSPSetCallBack(int type, QSP_CALLBACK func)
//{
//	qspSetCallBack(type, func);
//}
