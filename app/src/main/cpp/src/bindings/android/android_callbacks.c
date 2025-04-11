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

INLINE JNIEnv* ndkGetJniEnv()
{
	JNIEnv* ndkEnv;
	/* Callbacks should be called on the JVM threads only */
	(*ndkJvm)->GetEnv(ndkJvm, (void**)&ndkEnv, JNI_VERSION_1_6);
	return ndkEnv;
}

void qspInitCallBacks()
{
	int i;
	qspIsInCallBack = QSP_FALSE;
	qspIsDisableCodeExec = QSP_FALSE;
	qspIsExitOnError = QSP_FALSE;
	for (i = 0; i < QSP_CALL_DUMMY; ++i)
		qspCallBacks[i] = 0;
}

void qspSetCallBack(int type, QSP_CALLBACK func)
{
	qspCallBacks[type] = func;
}

void qspCallDebug(QSP_CHAR* str)
{
	if (str == NULL) return;
	if (qspCallBacks[QSP_CALL_DEBUG])
	{
		QSPCallState state;
		JNIEnv *javaEnv = ndkGetJniEnv();
		jstring qspText = ndkToJavaString(javaEnv, str);

		qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_DEBUG], qspText);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
	}
}

void qspCallSetTimer(int msecs)
{
	/* Set timer interval */
	if (qspCallBacks[QSP_CALL_SETTIMER])
	{
		QSPCallState state;
		JNIEnv *javaEnv = ndkGetJniEnv();

		qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_SETTIMER], msecs);
		qspRestoreCallState(&state);
	}
}

void qspCallRefreshInt(QSP_BOOL isRedraw)
{
	/* Refresh UI to show the latest state */
	if (qspCallBacks[QSP_CALL_REFRESHINT])
	{
		QSPCallState state;
		JNIEnv *javaEnv = ndkGetJniEnv();

		qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_REFRESHINT], isRedraw);
		qspRestoreCallState(&state);
	}
}

void qspCallSetInputStrText(QSP_CHAR* text)
{
	if (text == NULL) return;
	if (qspCallBacks[QSP_CALL_SETINPUTSTRTEXT])
	{
		QSPCallState state;
		JNIEnv *javaEnv = ndkGetJniEnv();
		jstring qspText = ndkToJavaString(javaEnv, text);

		qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_SETINPUTSTRTEXT], qspText);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
	}
}

void qspCallAddMenuItem(QSP_CHAR* name, QSP_CHAR* imgPath)
{
	if (name == NULL || imgPath == NULL) return;
	if (qspCallBacks[QSP_CALL_ADDMENUITEM]) {
		QSPCallState state;
		JNIEnv *javaEnv = ndkGetJniEnv();
		jstring menuItemName = ndkToJavaString(javaEnv, name);
		jstring menuItemImg = ndkToJavaString(javaEnv, imgPath);

		qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_ADDMENUITEM], menuItemName, menuItemImg);
		(*javaEnv)->DeleteLocalRef(javaEnv, menuItemName);
		(*javaEnv)->DeleteLocalRef(javaEnv, menuItemImg);
		qspRestoreCallState(&state);
	}
}

void qspCallSystem(QSP_CHAR* cmd)
{
	if (cmd == NULL) return;
	if (qspCallBacks[QSP_CALL_SYSTEM])
	{
		QSPCallState state;
		JNIEnv *javaEnv = ndkGetJniEnv();
		jstring qspText = ndkToJavaString(javaEnv, cmd);

		qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_SYSTEM], qspText);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
	}
}

void qspCallOpenGame(QSP_CHAR* file)
{
	if (file == NULL) return;
	if (qspCallBacks[QSP_CALL_OPENGAMESTATUS]) {
		QSPCallState state;
		JNIEnv *javaEnv = ndkGetJniEnv();
		jstring qspText = ndkToJavaString(javaEnv, file);

		qspSaveCallState(&state, QSP_FALSE, QSP_TRUE);
		(*javaEnv)->CallVoidMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_OPENGAMESTATUS], qspText);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
	}
}

void qspCallSaveGame(QSP_CHAR* file)
{
	if (file == NULL) return;
	if (qspCallBacks[QSP_CALL_SAVEGAMESTATUS]) {
		QSPCallState state;
		JNIEnv *javaEnv = ndkGetJniEnv();
		jstring qspText = ndkToJavaString(javaEnv, file);

		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_SAVEGAMESTATUS], qspText);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
	}
}

void qspCallShowMessage(QSP_CHAR* text)
{
	if (text == NULL) return;
	if (qspCallBacks[QSP_CALL_SHOWMSGSTR]) {
		QSPCallState state;
		JNIEnv *javaEnv = ndkGetJniEnv();
		jstring qspText = ndkToJavaString(javaEnv, text);

		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_SHOWMSGSTR], qspText);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
	}
}

void qspCallShowMenu()
{
	if (qspCallBacks[QSP_CALL_SHOWMENU]) {
		QSPCallState state;
		JNIEnv *javaEnv = ndkGetJniEnv();

		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_SHOWMENU]);
		qspRestoreCallState(&state);
	}
}

void qspCallShowPicture(QSP_CHAR* file)
{
	if (file == NULL) return;
	if (qspCallBacks[QSP_CALL_SHOWIMAGE]) {
		QSPCallState state;
		JNIEnv *javaEnv = ndkGetJniEnv();
		jstring qspText = ndkToJavaString(javaEnv, file);

		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_SHOWIMAGE], qspText);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
	}
}

void qspCallShowWindow(int type, QSP_BOOL isShow)
{
	if (qspCallBacks[QSP_CALL_SHOWWINDOW]) {
		QSPCallState state;
		JNIEnv *javaEnv = ndkGetJniEnv();

		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_SHOWWINDOW], type, isShow);
		qspRestoreCallState(&state);
	}
}

void qspCallPlayFile(QSP_CHAR* file, int volume)
{
	if (file == NULL) return;
	if (qspCallBacks[QSP_CALL_PLAYFILE]) {
		QSPCallState state;
		JNIEnv *javaEnv = ndkGetJniEnv();
		jstring qspText = ndkToJavaString(javaEnv, file);

		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_PLAYFILE], qspText, volume);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
	}
}

QSP_BOOL qspCallIsPlayingFile(QSP_CHAR* file)
{
	if (file == NULL) return JNI_FALSE;
	if (qspCallBacks[QSP_CALL_ISPLAYINGFILE]) {
		QSPCallState state;
		QSP_BOOL isPlaying;
		JNIEnv *javaEnv = ndkGetJniEnv();
		jstring qspText = ndkToJavaString(javaEnv, file);

		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		isPlaying = (QSP_BOOL)(*javaEnv)->CallBooleanMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_ISPLAYINGFILE], qspText);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);

		return isPlaying;
	}
	return JNI_FALSE;
}

void qspCallSleep(int msecs)
{
	if (qspCallBacks[QSP_CALL_SLEEP]) {
		QSPCallState state;
		JNIEnv *javaEnv = ndkGetJniEnv();

		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_SLEEP], msecs);
		qspRestoreCallState(&state);
	}
}

int qspCallGetMSCount(void)
{
	if (qspCallBacks[QSP_CALL_GETMSCOUNT]) {
		QSPCallState state;
		int count;
		JNIEnv *javaEnv = ndkGetJniEnv();

		qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		count = (*javaEnv)->CallIntMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_GETMSCOUNT]);
		qspRestoreCallState(&state);
		return count;
	}
	return 0;
}

void qspCallCloseFile(QSP_CHAR* file)
{
	if (file == NULL) return;
	if (qspCallBacks[QSP_CALL_CLOSEFILE]) {
		QSPCallState state;
		JNIEnv *javaEnv = ndkGetJniEnv();
		jstring qspText = ndkToJavaString(javaEnv, file);

		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_CLOSEFILE], qspText);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
	}
}

void qspCallDeleteMenu(void)
{
	if (qspCallBacks[QSP_CALL_DELETEMENU]) {
		QSPCallState state;
		JNIEnv *javaEnv = ndkGetJniEnv();

		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_DELETEMENU]);
		qspRestoreCallState(&state);
	}
}

QSP_CHAR* qspCallInputBox(QSP_CHAR* text)
{
	if (text == NULL) return qspGetNewText(QSP_FMT(""), 0);
	if (qspCallBacks[QSP_CALL_INPUTBOX]) {
		QSPCallState state;
		QSP_CHAR* buffer;
		JNIEnv *javaEnv = ndkGetJniEnv();
		jstring qspText = ndkToJavaString(javaEnv, text);

		qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		jstring jResult = (jstring)((*javaEnv)->CallObjectMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_INPUTBOX], qspText));
		const char* str = (*javaEnv)->GetStringUTFChars(javaEnv, jResult, NULL);
		if (str != NULL)
			buffer = ndkFromJavaString(javaEnv, jResult);
		else
			buffer = qspGetNewText(QSP_FMT(""), 0);
		(*javaEnv)->ReleaseStringUTFChars(javaEnv, jResult, str);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
		return buffer;
	}
	return qspGetNewText(QSP_FMT(""), 0);
}

char* qspCallGetFileContents(QSP_CHAR* fileName, int* filesize)
{
	if (fileName == NULL) return (char*)qspGetNewText(QSP_FMT(""), 0);
	if (qspCallBacks[QSP_CALL_GETFILECONTENT]) {
		QSPCallState state;
		JNIEnv *javaEnv = ndkGetJniEnv();
		// Convert QSP file name to Java
		jstring javaFileName = ndkToJavaString(javaEnv, fileName);

		qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		// Call GetFileContents
		jbyteArray byteArray = (jbyteArray)(*javaEnv)->CallObjectMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_GETFILECONTENT], javaFileName);
		(*javaEnv)->DeleteLocalRef(javaEnv, javaFileName);
		if (!byteArray) return NULL;

		// Copy file contents into a new buffer
		jboolean isCopy;
		jbyte* data = (*javaEnv)->GetByteArrayElements(javaEnv, byteArray, &isCopy);
		jsize byteArrayLen = (*javaEnv)->GetArrayLength(javaEnv, byteArray);
		char* result = (char*)malloc(byteArrayLen);
		memcpy(result, data, byteArrayLen);
		(*javaEnv)->ReleaseByteArrayElements(javaEnv, byteArray, data, JNI_ABORT);

		// Set file size
		if (filesize)
		{
			*filesize = byteArrayLen;
		}
		qspRestoreCallState(&state);
		return result;
	}
	return (char*)qspGetNewText(QSP_FMT(""), 0);
}

void qspCallChangeQuestPath(QSP_CHAR* path)
{
	if (path == NULL) return;
	if (qspCallBacks[QSP_CALL_CHANGEQUESTPATH]) {
		QSPCallState state;
		JNIEnv *javaEnv = ndkGetJniEnv();
		// Convert QSP path to Java
		jstring qspText = ndkToJavaString(javaEnv, path);

		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		// Call ChangeQuestPath
		(*javaEnv)->CallVoidMethod(javaEnv, ndkApiObject, qspCallBacks[QSP_CALL_CHANGEQUESTPATH], qspText);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
	}
}

#endif
