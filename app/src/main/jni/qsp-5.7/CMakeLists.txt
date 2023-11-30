
project(ndkqsp)

include_directories( ../onig )

# -fPIC
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

set(QSP_SOURCES
        androidqspwrapper.c
        src/actions.c
        src/callbacks.c
        src/codetools.c
        src/coding.c
        src/common.c
        src/errors.c
        src/game.c
        src/locations.c
        src/mathops.c
        src/memwatch.c
        src/menu.c
        src/objects.c
        src/playlist.c
        src/statements.c
        src/text.c
        src/time.c
        src/towlower.c
        src/towupper.c
        src/variables.c
        src/variant.c
        src/bindings/android/android_callbacks.c
        src/bindings/android/android_coding.c
        src/bindings/android/android_control.c
)

add_definitions( -D_ANDROID -D_UNICODE -DNOT_RUBY -DEXPORT -w )

add_library( ndkqsp SHARED ${QSP_SOURCES} )

find_library( log-lib log )

target_link_libraries( ndkqsp
                       onig
                       ${log-lib} )
