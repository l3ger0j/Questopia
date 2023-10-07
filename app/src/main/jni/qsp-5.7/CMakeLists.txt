cmake_minimum_required(VERSION 3.14)

project(ndkqsp)

include_directories( ../onig )

# -fPIC
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

set(QSP_SOURCES
        androidqspwrapper.c 
        qsp/actions.c
        qsp/callbacks.c
        qsp/codetools.c
        qsp/coding.c
        qsp/common.c
        qsp/errors.c
        qsp/game.c
        qsp/locations.c
        qsp/mathops.c
        qsp/memwatch.c
        qsp/menu.c
        qsp/objects.c
        qsp/playlist.c
        qsp/statements.c
        qsp/text.c
        qsp/time.c
        qsp/towlower.c
        qsp/towupper.c
        qsp/variables.c
        qsp/variant.c
        qsp/bindings/android/android_callbacks.c
        qsp/bindings/android/android_coding.c
        qsp/bindings/android/android_control.c
)

add_definitions( -D_ANDROID -D_UNICODE -DNOT_RUBY -DEXPORT -w )

add_library( ndkqsp SHARED ${QSP_SOURCES} )

find_library( log-lib log )

target_link_libraries( ndkqsp
                       onig
                       ${log-lib} )
