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

#include <jni.h>

#ifndef QSP_ANDROIDDEFINES
	#define QSP_ANDROIDDEFINES

	#ifdef _UNICODE
		#ifdef _WIN32
			typedef wchar_t QSP_CHAR;
			#define QSP_FMT2(x) L##x
			#define QSP_FMT(x) QSP_FMT2(x)
		#else
			typedef unsigned short QSP_CHAR;
			#define QSP_FMT2(x) u##x
			#define QSP_FMT(x) QSP_FMT2(x)
		#endif

		#define QSP_STRCOLL qspStrsComp
		#define QSP_CHRLWR qspToWLower
		#define QSP_CHRUPR qspToWUpper
		#define QSP_WCSTOMBSLEN(a) (int)wcstombs(0, a, 0)
		#define QSP_WCSTOMBS wcstombs
		#define QSP_MBTOSB(a) ((a) % 256)
		#define QSP_ONIG_ENC (sizeof(QSP_CHAR) == 2 ? ONIG_ENCODING_UTF16_LE : ONIG_ENCODING_UTF32_LE)
		#define QSP_FROM_OS_CHAR(a) qspReverseConvertUC(a, qspCP1251ToUnicodeTable)
		#define QSP_TO_OS_CHAR(a) qspDirectConvertUC(a, qspCP1251ToUnicodeTable)
		#define QSP_WCTOB
		#define QSP_BTOWC

		#ifdef _MSC_VER
			#define QSP_FOPEN _wfopen
		#else
			#define QSP_FOPEN qspFileOpen
		#endif
	#else
		#error "Non-Unicode build using Android binding is not supported"
	#endif

	#ifdef _MSC_VER
		#define QSP_TIME _time64
	#else
		#define QSP_TIME time
	#endif

	typedef jmethodID QSP_CALLBACK;

	#include "../qsp.h"
	#include "org_libndkqsp_jni_NDKLib.h"

	extern JavaVM *ndkJvm;
	extern jclass ndkApiClass;
	extern jobject ndkApiObject;

	extern jclass ndkListItemClass;
	extern jclass ndkExecutionStateClass;
	extern jclass ndkErrorInfoClass;
	extern jclass ndkVarValResp;

	typedef struct
	{
		jstring Image;
		jstring Name;
		jobject ListItem;
	} JNIListItem;

	jstring ndkToJavaString(JNIEnv *env, QSP_CHAR *str);
	QSP_CHAR *ndkFromJavaString(JNIEnv *env, jstring str);
	JNIListItem ndkToJavaListItem(JNIEnv *env, QSP_CHAR *image, QSP_CHAR *name);
	void ndkReleaseJavaListItem(JNIEnv *env, JNIListItem *listItem);

#endif
