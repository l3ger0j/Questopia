LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_DISABLE_FATAL_LINKER_WARNINGS := true
LOCAL_CFLAGS := -fshort-wchar -D_ANDROID -D_UNICODE -DNOT_RUBY -DEXPORT -w
LOCAL_LDLIBS := -llog
LOCAL_LDFLAGS := -fPIC
LOCAL_MODULE    := ndkqsp
LOCAL_SRC_FILES := androidqspwrapper.c \
qsp/actions.c \
qsp/callbacks.c \
qsp/codetools.c \
qsp/coding.c \
qsp/common.c \
qsp/errors.c \
qsp/game.c \
qsp/locations.c \
qsp/mathops.c \
qsp/memwatch.c \
qsp/menu.c \
qsp/objects.c \
qsp/playlist.c \
qsp/statements.c \
qsp/text.c \
qsp/time.c \
qsp/towlower.c \
qsp/towupper.c \
qsp/variables.c \
qsp/variant.c \
qsp/onig/regcomp.c \
qsp/onig/regenc.c \
qsp/onig/regerror.c \
qsp/onig/regexec.c \
qsp/onig/regparse.c \
qsp/onig/regsyntax.c \
qsp/onig/regtrav.c \
qsp/onig/regversion.c \
qsp/onig/st.c \
qsp/onig/enc/ascii.c \
qsp/onig/enc/cp1251.c \
qsp/onig/enc/koi8_r.c \
qsp/onig/enc/unicode.c \
qsp/onig/enc/utf16_le.c \
qsp/onig/enc/utf32_le.c \
qsp/bindings/android/android_callbacks.c \
qsp/bindings/android/android_coding.c \
qsp/bindings/android/android_control.c

include $(BUILD_SHARED_LIBRARY)