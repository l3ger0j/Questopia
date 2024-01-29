/* Copyright (C) 2005-2010 Valeriy Argunov (nporep AT mail DOT ru) */
/*
* This library is free software; you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation; either version 2.1 of the License, or
* (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

#include "../../declarations.h"

#ifdef _ANDROID

#include "../../callbacks.h"
#include "../../actions.h"
#include "../../coding.h"
#include "../../common.h"
#include "../../errors.h"
#include "../../objects.h"
#include "../../text.h"

void qspInitCallBacks() {
  int i;
  qspIsInCallBack = QSP_FALSE;
  qspIsDisableCodeExec = QSP_FALSE;
  qspIsExitOnError = QSP_FALSE;
  for (i = 0; i < QSP_CALL_DUMMY; ++i)
	qspCallBacks[i] = 0;
}

void qspSetCallBack(int type, QSP_CALLBACK func) {
  qspCallBacks[type] = func;
}

void qspCallDebug(QSP_CHAR *str) {
  QSPCallState state;
  if (qspCallBacks[QSP_CALL_DEBUG]) {
	qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
	qspCallBacks[QSP_CALL_DEBUG](str);
	qspRestoreCallState(&state);
  }
}

void qspCallSetTimer(int msecs) {
  QSPCallState state;
  qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);

  jclass cls = (*qspJniEnv)->GetObjectClass(qspJniEnv, qspCallbacksObj);
  jmethodID mid =
	  (*qspJniEnv)->GetMethodID(qspJniEnv, cls, "SetTimer", "(I)V");
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, cls);
  if (mid == NULL)
	return; /* method not found */

  (*qspJniEnv)->CallVoidMethod(qspJniEnv, qspCallbacksObj, mid, msecs);

  qspRestoreCallState(&state);
}

void qspCallRefreshInt(QSP_BOOL isRedraw) {
  QSPCallState state;

  qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);

  jclass cls = (*qspJniEnv)->GetObjectClass(qspJniEnv, qspCallbacksObj);
  jmethodID mid =
	  (*qspJniEnv)->GetMethodID(qspJniEnv, cls, "RefreshInt", "()V");
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, cls);
  if (mid == NULL)
	return; /* method not found */

  (*qspJniEnv)->CallVoidMethod(qspJniEnv, qspCallbacksObj, mid);

  qspRestoreCallState(&state);
}

void qspCallSetInputStrText(QSP_CHAR *text) {
  QSPCallState state;
  if (qspCallBacks[QSP_CALL_SETINPUTSTRTEXT]) {
	qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
	qspCallBacks[QSP_CALL_SETINPUTSTRTEXT](text);
	qspRestoreCallState(&state);
  }
}

void qspCallAddMenuItem(QSP_CHAR *name, QSP_CHAR *imgPath) {
  QSPCallState state;
  qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);

  jclass cls = (*qspJniEnv)->GetObjectClass(qspJniEnv, qspCallbacksObj);
  jmethodID mid =
	  (*qspJniEnv)->GetMethodID(qspJniEnv, cls, "AddMenuItem",
								"(Ljava/lang/String;Ljava/lang/String;)V");
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, cls);
  if (mid == NULL)
	return; /* method not found */

  char *sz = qspW2C(name);
  jstring menuItemName = (*qspJniEnv)->NewStringUTF(qspJniEnv, sz);
  if (sz != NULL)
	free(sz);

  sz = qspW2C(imgPath);
  jstring menuItemImg = (*qspJniEnv)->NewStringUTF(qspJniEnv, sz);
  if (sz != NULL)
	free(sz);

  (*qspJniEnv)->CallVoidMethod(qspJniEnv, qspCallbacksObj, mid, menuItemName, menuItemImg);
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, menuItemName);
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, menuItemImg);

  qspRestoreCallState(&state);
}

void qspCallSystem(QSP_CHAR *cmd) {
  QSPCallState state;
  if (qspCallBacks[QSP_CALL_SYSTEM]) {
	qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
	qspCallBacks[QSP_CALL_SYSTEM](cmd);
	qspRestoreCallState(&state);
  }
}

void qspCallOpenGame(QSP_CHAR *file) {
  QSPCallState state;

  qspSaveCallState(&state, QSP_FALSE, QSP_TRUE);
  jclass cls = (*qspJniEnv)->GetObjectClass(qspJniEnv, qspCallbacksObj);
  jmethodID mid =
	  (*qspJniEnv)->GetMethodID(qspJniEnv, cls, "OpenGame", "(Ljava/lang/String;)V");
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, cls);
  if (mid == NULL)
	return; /* method not found */

  char *sz = qspW2C(file);
  jstring fileName = (*qspJniEnv)->NewStringUTF(qspJniEnv, sz);
  if (sz != NULL)
	free(sz);

  (*qspJniEnv)->CallVoidMethod(qspJniEnv, qspCallbacksObj, mid, fileName);
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, fileName);

  qspRestoreCallState(&state);
  qspRestoreCallState(&state);

}

void qspCallSaveGame(QSP_CHAR *file) {
  QSPCallState state;
  qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);

  jclass cls = (*qspJniEnv)->GetObjectClass(qspJniEnv, qspCallbacksObj);
  jmethodID mid =
	  (*qspJniEnv)->GetMethodID(qspJniEnv, cls, "SaveGame", "(Ljava/lang/String;)V");
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, cls);
  if (mid == NULL)
	return; /* method not found */

  char *sz = qspW2C(file);
  jstring fileName = (*qspJniEnv)->NewStringUTF(qspJniEnv, sz);
  if (sz != NULL)
	free(sz);

  (*qspJniEnv)->CallVoidMethod(qspJniEnv, qspCallbacksObj, mid, fileName);
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, fileName);

  qspRestoreCallState(&state);

}

void qspCallShowMessage(QSP_CHAR *text) {
  QSPCallState state;
  qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);

  jclass cls = (*qspJniEnv)->GetObjectClass(qspJniEnv, qspCallbacksObj);
  jmethodID mid =
	  (*qspJniEnv)->GetMethodID(qspJniEnv, cls, "ShowMessage", "(Ljava/lang/String;)V");
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, cls);
  if (mid == NULL)
	return; /* method not found */

  char *sz = qspW2C(text);
  jstring message = (*qspJniEnv)->NewStringUTF(qspJniEnv, sz);
  if (sz != NULL)
	free(sz);

  (*qspJniEnv)->CallVoidMethod(qspJniEnv, qspCallbacksObj, mid, message);
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, message);

  qspRestoreCallState(&state);
}

void qspCallShowMenu() {
  QSPCallState state;

  qspSaveCallState(&state, QSP_FALSE, QSP_TRUE);

  jclass cls = (*qspJniEnv)->GetObjectClass(qspJniEnv, qspCallbacksObj);
  jmethodID mid =
	  (*qspJniEnv)->GetMethodID(qspJniEnv, cls, "ShowMenu", "()V");
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, cls);
  if (mid == NULL)
	return; /* method not found */

  (*qspJniEnv)->CallVoidMethod(qspJniEnv, qspCallbacksObj, mid);

  qspRestoreCallState(&state);
}

void qspCallShowPicture(QSP_CHAR *file) {
  QSPCallState state;
  qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);

  jclass cls = (*qspJniEnv)->GetObjectClass(qspJniEnv, qspCallbacksObj);
  jmethodID mid =
	  (*qspJniEnv)->GetMethodID(qspJniEnv, cls, "ShowPicture", "(Ljava/lang/String;)V");
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, cls);
  if (mid == NULL)
	return; /* method not found */

  char *sz = qspW2C(file);
  jstring fileName = (*qspJniEnv)->NewStringUTF(qspJniEnv, sz);
  if (sz != NULL)
	free(sz);

  (*qspJniEnv)->CallVoidMethod(qspJniEnv, qspCallbacksObj, mid, fileName);
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, fileName);

  qspRestoreCallState(&state);
}

void qspCallShowWindow(int type, QSP_BOOL isShow) {
  QSPCallState state;
  qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);

  jclass cls = (*qspJniEnv)->GetObjectClass(qspJniEnv, qspCallbacksObj);
  jmethodID mid = (*qspJniEnv)->GetMethodID(qspJniEnv, cls, "ShowWindow", "(IZ)V");
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, cls);

  if (mid == NULL) return;

  (*qspJniEnv)->CallVoidMethod(qspJniEnv, qspCallbacksObj, mid, type, isShow);

  qspRestoreCallState(&state);
}

void qspCallPlayFile(QSP_CHAR *file, int volume) {
  QSPCallState state;
  qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);

  jclass cls = (*qspJniEnv)->GetObjectClass(qspJniEnv, qspCallbacksObj);
  jmethodID mid =
	  (*qspJniEnv)->GetMethodID(qspJniEnv, cls, "PlayFile", "(Ljava/lang/String;I)V");
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, cls);
  if (mid == NULL)
	return; /* method not found */

  char *sz = qspW2C(file);
  jstring fileName = (*qspJniEnv)->NewStringUTF(qspJniEnv, sz);
  if (sz != NULL)
	free(sz);

  (*qspJniEnv)->CallVoidMethod(qspJniEnv, qspCallbacksObj, mid, fileName, volume);
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, fileName);

  qspRestoreCallState(&state);
}

QSP_BOOL qspCallIsPlayingFile(QSP_CHAR *file) {
  QSPCallState state;
  qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);

  char *sz = qspW2C(file);
  jstring fileName = (*qspJniEnv)->NewStringUTF(qspJniEnv, sz);
  if (sz != NULL)
	free(sz);

  jclass cls = (*qspJniEnv)->GetObjectClass(qspJniEnv, qspCallbacksObj);
  jmethodID mid =
	  (*qspJniEnv)->GetMethodID(qspJniEnv, cls, "IsPlayingFile", "(Ljava/lang/String;)Z");
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, cls);

  QSP_BOOL result = QSP_FALSE;
  if (mid != NULL) {
	jboolean result_jni = (*qspJniEnv)
		->CallBooleanMethod(qspJniEnv, qspCallbacksObj, mid, fileName);
	if (result_jni == JNI_TRUE)
	  result = QSP_TRUE;
  } else {
	result = QSP_FALSE; /* method not found */
  }
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, fileName);

  qspRestoreCallState(&state);
  return result;
}

void qspCallSleep(int msecs) {
  QSPCallState state;
  qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);

  jclass cls = (*qspJniEnv)->GetObjectClass(qspJniEnv, qspCallbacksObj);
  jmethodID mid =
	  (*qspJniEnv)->GetMethodID(qspJniEnv, cls, "Wait", "(I)V");
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, cls);
  if (mid == NULL)
	return; /* method not found */

  (*qspJniEnv)->CallVoidMethod(qspJniEnv, qspCallbacksObj, mid, msecs);

  qspRestoreCallState(&state);
}

int qspCallGetMSCount() {
  QSPCallState state;
  int count = 0;
  qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);

  jclass cls = (*qspJniEnv)->GetObjectClass(qspJniEnv, qspCallbacksObj);
  jmethodID mid =
	  (*qspJniEnv)->GetMethodID(qspJniEnv, cls, "GetMSCount", "()I");
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, cls);
  if (mid != NULL) {
	count = (int)(*qspJniEnv)->CallIntMethod(qspJniEnv, qspCallbacksObj, mid);
  }

  qspRestoreCallState(&state);
  return count;
}

void qspCallCloseFile(QSP_CHAR *file) {
  QSPCallState state;
  qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);

  jclass cls = (*qspJniEnv)->GetObjectClass(qspJniEnv, qspCallbacksObj);
  jmethodID mid =
	  (*qspJniEnv)->GetMethodID(qspJniEnv, cls, "CloseFile", "(Ljava/lang/String;)V");
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, cls);
  if (mid == NULL)
	return; /* method not found */

  char *sz = qspW2C(file);
  jstring fileName = (*qspJniEnv)->NewStringUTF(qspJniEnv, sz);
  if (sz != NULL)
	free(sz);

  (*qspJniEnv)->CallVoidMethod(qspJniEnv, qspCallbacksObj, mid, fileName);
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, fileName);

  qspRestoreCallState(&state);
}

void qspCallDeleteMenu() {
  QSPCallState state;

  qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);

  jclass cls = (*qspJniEnv)->GetObjectClass(qspJniEnv, qspCallbacksObj);
  jmethodID mid =
	  (*qspJniEnv)->GetMethodID(qspJniEnv, cls, "DeleteMenu", "()V");
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, cls);
  if (mid == NULL)
	return; /* method not found */

  (*qspJniEnv)->CallVoidMethod(qspJniEnv, qspCallbacksObj, mid);

  qspRestoreCallState(&state);
}

QSP_CHAR *qspCallInputBox(QSP_CHAR *text) {
  QSPCallState state;
  QSP_CHAR *buffer;

  qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);

  char *sz = qspW2C(text);
  jstring jText = (*qspJniEnv)->NewStringUTF(qspJniEnv, sz);
  if (sz != NULL)
	free(sz);

  jclass cls = (*qspJniEnv)->GetObjectClass(qspJniEnv, qspCallbacksObj);
  jmethodID mid = (*qspJniEnv)
	  ->GetMethodID(qspJniEnv, cls, "InputBox", "(Ljava/lang/String;)Ljava/lang/String;");
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, cls);
  if (mid != NULL) {
	jstring jResult = (jstring)((*qspJniEnv)
		->CallObjectMethod(qspJniEnv, qspCallbacksObj, mid, jText));
	const char *str = (*qspJniEnv)->GetStringUTFChars(qspJniEnv, jResult, NULL);
	if (str != NULL)
	  buffer = qspC2W(str);
	else
	  qspGetNewText(QSP_FMT(""), 0);
	(*qspJniEnv)->ReleaseStringUTFChars(qspJniEnv, jResult, str);
  } else
	buffer = qspGetNewText(QSP_FMT(""), 0);
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, jText);
  qspRestoreCallState(&state);
  return buffer;
}

char *qspCallGetFileContents(QSP_CHAR *fileName, int *filesize) {
  // Get GetFileContents method
  jclass cls = (*qspJniEnv)->GetObjectClass(qspJniEnv, qspCallbacksObj);
  jmethodID mid = (*qspJniEnv)
	  ->GetMethodID(qspJniEnv, cls, "GetFileContents", "(Ljava/lang/String;)[B");
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, cls);

  // Convert QSP file name to Java
  char *cFileName = qspW2C(fileName);
  jstring javaFileName = (*qspJniEnv)->NewStringUTF(qspJniEnv, cFileName);
  free(cFileName);

  // Call GetFileContents
  jbyteArray byteArray = (jbyteArray)(*qspJniEnv)
	  ->CallObjectMethod(qspJniEnv, qspCallbacksObj, mid, javaFileName);
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, javaFileName);
  if (!byteArray) return NULL;

  // Copy file contents into a new buffer
  jboolean isCopy;
  jbyte *data = (*qspJniEnv)->GetByteArrayElements(qspJniEnv, byteArray, &isCopy);
  jsize byteArrayLen = (*qspJniEnv)->GetArrayLength(qspJniEnv, byteArray);
  char *result = (char *)malloc(byteArrayLen);
  memcpy(result, data, byteArrayLen);
  (*qspJniEnv)->ReleaseByteArrayElements(qspJniEnv, byteArray, data, JNI_ABORT);

  // Set file size
  if (filesize) {
	*filesize = byteArrayLen;
  }

  return result;
}

void qspCallChangeQuestPath(QSP_CHAR *path) {
  // Get ChangeQuestPath method
  jclass cls = (*qspJniEnv)->GetObjectClass(qspJniEnv, qspCallbacksObj);
  jmethodID mid = (*qspJniEnv)
	  ->GetMethodID(qspJniEnv, cls, "ChangeQuestPath", "(Ljava/lang/String;)V");
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, cls);

  // Convert QSP path to Java
  char *cPath = qspW2C(path);
  jstring javaPath = (*qspJniEnv)->NewStringUTF(qspJniEnv, cPath);
  free(cPath);

  // Call ChangeQuestPath
  (*qspJniEnv)->CallVoidMethod(qspJniEnv, qspCallbacksObj, mid, javaPath);
  (*qspJniEnv)->DeleteLocalRef(qspJniEnv, javaPath);
}

#endif
