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

void qspCallDebug(QSP_CHAR *str)
{
	QSPCallState state;
	if (qspCallBacks[QSP_CALL_DEBUG])
	{
		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		qspCallBacks[QSP_CALL_DEBUG](str);
		qspRestoreCallState(&state);
	}
}

void qspCallSetTimer(int msecs)
{
	QSPCallState state;
	qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		
	jclass cls = (*qspCallbackEnv)->GetObjectClass(qspCallbackEnv, qspCallbackObject);
	jmethodID mid = 
		 (*qspCallbackEnv)->GetMethodID(qspCallbackEnv, cls, "SetTimer", "(I)V");
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, cls );
	if (mid == NULL)
		return; /* method not found */

	(*qspCallbackEnv)->CallVoidMethod(qspCallbackEnv, qspCallbackObject, mid, msecs);
	
	qspRestoreCallState(&state);
}

void qspCallRefreshInt(QSP_BOOL isRedraw)
{
	QSPCallState state;

	qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
	
	
	jclass cls = (*qspCallbackEnv)->GetObjectClass(qspCallbackEnv, qspCallbackObject);
	jmethodID mid = 
		 (*qspCallbackEnv)->GetMethodID(qspCallbackEnv, cls, "RefreshInt", "()V");
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, cls );
	if (mid == NULL)
		return; /* method not found */

	(*qspCallbackEnv)->CallVoidMethod(qspCallbackEnv, qspCallbackObject, mid);
	
	
	qspRestoreCallState(&state);
}

void qspCallSetInputStrText(QSP_CHAR *text)
{
	QSPCallState state;
	if (qspCallBacks[QSP_CALL_SETINPUTSTRTEXT])
	{
		qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		qspCallBacks[QSP_CALL_SETINPUTSTRTEXT](text);
		qspRestoreCallState(&state);
	}
}

void qspCallAddMenuItem(QSP_CHAR *name, QSP_CHAR *imgPath)
{
	QSPCallState state;
	qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		
	jclass cls = (*qspCallbackEnv)->GetObjectClass(qspCallbackEnv, qspCallbackObject);
	jmethodID mid = 
		 (*qspCallbackEnv)->GetMethodID(qspCallbackEnv, cls, "AddMenuItem", "(Ljava/lang/String;Ljava/lang/String;)V");
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, cls );
	if (mid == NULL)
		return; /* method not found */

	char * sz = qspW2C(name);
	jstring menuItemName = (*qspCallbackEnv)->NewStringUTF(qspCallbackEnv, sz);
	if (sz!=NULL)
		free(sz);
		
	sz = qspW2C(imgPath);
	jstring menuItemImg = (*qspCallbackEnv)->NewStringUTF(qspCallbackEnv, sz);
	if (sz!=NULL)
		free(sz);

	(*qspCallbackEnv)->CallVoidMethod(qspCallbackEnv, qspCallbackObject, mid, menuItemName, menuItemImg);
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, menuItemName );
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, menuItemImg );
	
	qspRestoreCallState(&state);
}

void qspCallSystem(QSP_CHAR *cmd)
{
	QSPCallState state;
	if (qspCallBacks[QSP_CALL_SYSTEM])
	{
		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		qspCallBacks[QSP_CALL_SYSTEM](cmd);
		qspRestoreCallState(&state);
	}
}

void qspCallOpenGame(QSP_CHAR *file)
{
	QSPCallState state;

	qspSaveCallState(&state, QSP_FALSE, QSP_TRUE);
	jclass cls = (*qspCallbackEnv)->GetObjectClass(qspCallbackEnv, qspCallbackObject);
		jmethodID mid =
			 (*qspCallbackEnv)->GetMethodID(qspCallbackEnv, cls, "OpenGame", "(Ljava/lang/String;)V");
		(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, cls );
		if (mid == NULL)
			return; /* method not found */

		char * sz = qspW2C(file);
		jstring fileName = (*qspCallbackEnv)->NewStringUTF(qspCallbackEnv, sz);
		if (sz!=NULL)
			free(sz);

		(*qspCallbackEnv)->CallVoidMethod(qspCallbackEnv, qspCallbackObject, mid, fileName);
		(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, fileName );

		qspRestoreCallState(&state);
	qspRestoreCallState(&state);

}


void qspCallSaveGame(QSP_CHAR *file)
{
	QSPCallState state;
	qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);

	jclass cls = (*qspCallbackEnv)->GetObjectClass(qspCallbackEnv, qspCallbackObject);
	jmethodID mid =
		 (*qspCallbackEnv)->GetMethodID(qspCallbackEnv, cls, "SaveGame", "(Ljava/lang/String;)V");
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, cls );
	if (mid == NULL)
		return; /* method not found */

	char * sz = qspW2C(file);
	jstring fileName = (*qspCallbackEnv)->NewStringUTF(qspCallbackEnv, sz);
	if (sz!=NULL)
		free(sz);

	(*qspCallbackEnv)->CallVoidMethod(qspCallbackEnv, qspCallbackObject, mid, fileName);
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, fileName );

	qspRestoreCallState(&state);

}


void qspCallShowMessage(QSP_CHAR *text)
{
	QSPCallState state;
	qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);

	jclass cls = (*qspCallbackEnv)->GetObjectClass(qspCallbackEnv, qspCallbackObject);
	jmethodID mid = 
		 (*qspCallbackEnv)->GetMethodID(qspCallbackEnv, cls, "ShowMessage", "(Ljava/lang/String;)V");
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, cls );
	if (mid == NULL)
		return; /* method not found */
	
	char * sz = qspW2C(text);
	jstring message = (*qspCallbackEnv)->NewStringUTF(qspCallbackEnv, sz);
	if (sz!=NULL)
		free(sz);

	(*qspCallbackEnv)->CallVoidMethod(qspCallbackEnv, qspCallbackObject, mid, message);
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, message );
	
	qspRestoreCallState(&state);
}

void qspCallShowMenu()
{
	QSPCallState state;

	qspSaveCallState(&state, QSP_FALSE, QSP_TRUE);
	
	jclass cls = (*qspCallbackEnv)->GetObjectClass(qspCallbackEnv, qspCallbackObject);
	jmethodID mid = 
		 (*qspCallbackEnv)->GetMethodID(qspCallbackEnv, cls, "ShowMenu", "()V");
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, cls );
	if (mid == NULL)
		return; /* method not found */

	(*qspCallbackEnv)->CallVoidMethod(qspCallbackEnv, qspCallbackObject, mid);
	
	
	qspRestoreCallState(&state);
}

void qspCallShowPicture(QSP_CHAR *file)
{
	QSPCallState state;
	qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);

	jclass cls = (*qspCallbackEnv)->GetObjectClass(qspCallbackEnv, qspCallbackObject);
	jmethodID mid = 
		 (*qspCallbackEnv)->GetMethodID(qspCallbackEnv, cls, "ShowPicture", "(Ljava/lang/String;)V");
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, cls );
	if (mid == NULL)
		return; /* method not found */
	
	char * sz = qspW2C(file);
	jstring fileName = (*qspCallbackEnv)->NewStringUTF(qspCallbackEnv, sz);
	if (sz!=NULL)
		free(sz);

	(*qspCallbackEnv)->CallVoidMethod(qspCallbackEnv, qspCallbackObject, mid, fileName);
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, fileName );
	
	qspRestoreCallState(&state);
}

void qspCallShowWindow(int type, QSP_BOOL isShow)
{
	QSPCallState state;
	qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);

	jclass cls = (*qspCallbackEnv)->GetObjectClass(qspCallbackEnv, qspCallbackObject);
	jmethodID mid = (*qspCallbackEnv)->GetMethodID(qspCallbackEnv, cls, "ShowWindow", "(IZ)V");
	(*qspCallbackEnv)->DeleteLocalRef(qspCallbackEnv, cls);

	if (mid == NULL) return;

	(*qspCallbackEnv)->CallVoidMethod(qspCallbackEnv, qspCallbackObject, mid, type, isShow);

	qspRestoreCallState(&state);
}

void qspCallPlayFile(QSP_CHAR *file, int volume)
{
	QSPCallState state;
	qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);

	jclass cls = (*qspCallbackEnv)->GetObjectClass(qspCallbackEnv, qspCallbackObject);
	jmethodID mid = 
		 (*qspCallbackEnv)->GetMethodID(qspCallbackEnv, cls, "PlayFile", "(Ljava/lang/String;I)V");
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, cls );
	if (mid == NULL)
		return; /* method not found */
		
	char * sz = qspW2C(file);
	jstring fileName = (*qspCallbackEnv)->NewStringUTF(qspCallbackEnv, sz);
	if (sz!=NULL)
		free(sz);

	(*qspCallbackEnv)->CallVoidMethod(qspCallbackEnv, qspCallbackObject, mid, fileName, volume);
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, fileName );
	
	qspRestoreCallState(&state);
}

QSP_BOOL qspCallIsPlayingFile(QSP_CHAR *file)
{
	QSPCallState state;
	qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		
	char * sz = qspW2C(file);
	jstring fileName = (*qspCallbackEnv)->NewStringUTF(qspCallbackEnv, sz);
	if (sz!=NULL)
		free(sz);

	jclass cls = (*qspCallbackEnv)->GetObjectClass(qspCallbackEnv, qspCallbackObject);
	jmethodID mid = 
		 (*qspCallbackEnv)->GetMethodID(qspCallbackEnv, cls, "IsPlayingFile", "(Ljava/lang/String;)Z");
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, cls );
		 
	QSP_BOOL result = QSP_FALSE;
	if (mid != NULL)
	{
		jboolean result_jni = (*qspCallbackEnv)->CallBooleanMethod(qspCallbackEnv, qspCallbackObject, mid, fileName);
		if (result_jni==JNI_TRUE)
			result = QSP_TRUE;
	}
	else
	{
		result = QSP_FALSE; /* method not found */
	}
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, fileName );

	qspRestoreCallState(&state);
	return result;
}

void qspCallSleep(int msecs)
{
	QSPCallState state;
	qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
	
	jclass cls = (*qspCallbackEnv)->GetObjectClass(qspCallbackEnv, qspCallbackObject);
	jmethodID mid = 
		 (*qspCallbackEnv)->GetMethodID(qspCallbackEnv, cls, "Wait", "(I)V");
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, cls );
	if (mid == NULL)
		return; /* method not found */

	(*qspCallbackEnv)->CallVoidMethod(qspCallbackEnv, qspCallbackObject, mid, msecs);
	
	qspRestoreCallState(&state);
}

int qspCallGetMSCount()
{
	QSPCallState state;
	int count = 0;
	qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);	
	
	jclass cls = (*qspCallbackEnv)->GetObjectClass(qspCallbackEnv, qspCallbackObject);
	jmethodID mid = 
		 (*qspCallbackEnv)->GetMethodID(qspCallbackEnv, cls, "GetMSCount", "()I");
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, cls );
	if (mid != NULL)
	{
		count = (int)(*qspCallbackEnv)->CallIntMethod(qspCallbackEnv, qspCallbackObject, mid);
	}
	
	qspRestoreCallState(&state);	
	return count;
}

void qspCallCloseFile(QSP_CHAR *file)
{
	QSPCallState state;
	qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		
	jclass cls = (*qspCallbackEnv)->GetObjectClass(qspCallbackEnv, qspCallbackObject);
	jmethodID mid = 
		 (*qspCallbackEnv)->GetMethodID(qspCallbackEnv, cls, "CloseFile", "(Ljava/lang/String;)V");
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, cls );
	if (mid == NULL)
		return; /* method not found */

	char * sz = qspW2C(file);
	jstring fileName = (*qspCallbackEnv)->NewStringUTF(qspCallbackEnv, sz);
	if (sz!=NULL)
		free(sz);

	(*qspCallbackEnv)->CallVoidMethod(qspCallbackEnv, qspCallbackObject, mid, fileName);
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, fileName );
	
	qspRestoreCallState(&state);
}

void qspCallDeleteMenu()
{
	QSPCallState state;

	qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
	
	
	jclass cls = (*qspCallbackEnv)->GetObjectClass(qspCallbackEnv, qspCallbackObject);
	jmethodID mid = 
		 (*qspCallbackEnv)->GetMethodID(qspCallbackEnv, cls, "DeleteMenu", "()V");
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, cls );
	if (mid == NULL)
		return; /* method not found */

	(*qspCallbackEnv)->CallVoidMethod(qspCallbackEnv, qspCallbackObject, mid);
	
	
	qspRestoreCallState(&state);
}

QSP_CHAR *qspCallInputBox(QSP_CHAR *text)
{
	QSPCallState state;
	QSP_CHAR *buffer;

	qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		
	char * sz = qspW2C(text);
	jstring jText = (*qspCallbackEnv)->NewStringUTF(qspCallbackEnv, sz);
	if (sz!=NULL)
		free(sz);

	jclass cls = (*qspCallbackEnv)->GetObjectClass(qspCallbackEnv, qspCallbackObject);
	jmethodID mid = (*qspCallbackEnv)->GetMethodID(qspCallbackEnv, cls, "InputBox", "(Ljava/lang/String;)Ljava/lang/String;");
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, cls );
	if (mid != NULL)
	{
		jstring jResult = (jstring)((*qspCallbackEnv)->CallObjectMethod(qspCallbackEnv, qspCallbackObject, mid, jText));
		const char *str = (*qspCallbackEnv)->GetStringUTFChars(qspCallbackEnv, jResult, NULL);
		if (str != NULL)
			buffer = qspC2W(str);
		else
			qspGetNewText(QSP_FMT(""), 0);
		(*qspCallbackEnv)->ReleaseStringUTFChars(qspCallbackEnv, jResult, str);
	}
	else
		buffer = qspGetNewText(QSP_FMT(""), 0);
	(*qspCallbackEnv)->DeleteLocalRef( qspCallbackEnv, jText );
	qspRestoreCallState(&state);
	return buffer;
}

char *qspCallGetFileContents(QSP_CHAR *fileName, int *filesize) {
	// Get GetFileContents method
	jclass cls = (*qspCallbackEnv)->GetObjectClass(qspCallbackEnv, qspCallbackObject);
	jmethodID mid = (*qspCallbackEnv)->GetMethodID(qspCallbackEnv, cls, "GetFileContents", "(Ljava/lang/String;)[B");
	(*qspCallbackEnv)->DeleteLocalRef(qspCallbackEnv, cls);

	// Convert QSP file name to Java
	char *cFileName = qspW2C(fileName);
	jstring javaFileName = (*qspCallbackEnv)->NewStringUTF(qspCallbackEnv, cFileName);
	free(cFileName);

	// Call GetFileContents
	jbyteArray byteArray = (jbyteArray)(*qspCallbackEnv)->CallObjectMethod(qspCallbackEnv, qspCallbackObject, mid, javaFileName);
	(*qspCallbackEnv)->DeleteLocalRef(qspCallbackEnv, javaFileName);
	if (!byteArray) return NULL;

	// Copy file contents into a new buffer
	jboolean isCopy;
	jbyte *data = (*qspCallbackEnv)->GetByteArrayElements(qspCallbackEnv, byteArray, &isCopy);
	jsize byteArrayLen = (*qspCallbackEnv)->GetArrayLength(qspCallbackEnv, byteArray);
	char *result = (char *)malloc(byteArrayLen);
	memcpy(result, data, byteArrayLen);
	(*qspCallbackEnv)->ReleaseByteArrayElements(qspCallbackEnv, byteArray, data, JNI_ABORT);

	// Set file size
	if (filesize) {
		*filesize = byteArrayLen;
	}

	return result;
}

#endif
