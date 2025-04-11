cmake_minimum_required(VERSION 3.15 FATAL_ERROR)
project(ndkqsp VERSION 5.7.0)

set(QSP_GAMEMIN_VER "5.7.0")

include(CMakePackageConfigHelpers)
include(GenerateExportHeader)
include(GNUInstallDirs)

set(TARGET ndkqsp PROPERTY CMAKE_C_STANDARD 90)
set(TARGET ndkqsp PROPERTY CMAKE_C_STANDARD_REQUIRED ON)

set(CMAKE_INCLUDE_CURRENT_DIR ON)
set(CMAKE_INSTALL_RPATH_USE_LINK_PATH ON)

list(APPEND CMAKE_MODULE_PATH ${CMAKE_SOURCE_DIR}/cmake/Modules)

if ("${CMAKE_C_COMPILER_ID}" STREQUAL "GNU")
    set(PROJECT_COMPILER_FLAGS "-Wall")
    set(PROJECT_LINKER_FLAGS "")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Wno-char-subscripts -Wno-unused-variable")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra")
    set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} -g -D_DEBUG -DDEBUG")
    set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g -D_DEBUG -DDEBUG")
    set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -Ofast -DNDEBUG")
    set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -Ofast -DNDEBUG")
elseif ("${CMAKE_C_COMPILER_ID}" STREQUAL "MSVC")
    set(PROJECT_COMPILER_FLAGS "/W4")
    set(PROJECT_LINKER_FLAGS "")
    set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} /D_DEBUG /DDEBUG")
    set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} /D_DEBUG /DDEBUG")
    set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} /O2 /DNDEBUG")
    set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} /O2 /DNDEBUG")
    set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
elseif ("${CMAKE_C_COMPILER_ID}" STREQUAL "Clang" OR "${CMAKE_C_COMPILER_ID}" STREQUAL "AppleClang")
    set(PROJECT_COMPILER_FLAGS "")
    set(PROJECT_LINKER_FLAGS "")
    set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} -g -D_DEBUG -DDEBUG")
    set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g -D_DEBUG -DDEBUG")
    set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -Ofast -DNDEBUG")
    set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -Ofast -DNDEBUG")
else()
    message(FATAL_ERROR "Compiler is not supported")
endif()

set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_VISIBILITY_INLINES_HIDDEN ON)

option(USE_INSTALLED_ONIGURUMA "Use system-wide oniguruma library instead of bundling it" OFF)

if (USE_INSTALLED_ONIGURUMA)
	find_package(oniguruma REQUIRED)
else()
	# Download and unpack oniguruma at configure time
	configure_file(CMakeLists-oniguruma.txt.in "${CMAKE_BINARY_DIR}/oniguruma-download/CMakeLists.txt")
	execute_process(COMMAND "${CMAKE_COMMAND}" -G "${CMAKE_GENERATOR}" "-DCMAKE_MAKE_PROGRAM:FILEPATH=${CMAKE_MAKE_PROGRAM}" .
			WORKING_DIRECTORY "${CMAKE_BINARY_DIR}/oniguruma-download"
	)
	execute_process(COMMAND "${CMAKE_COMMAND}" --build .
			WORKING_DIRECTORY "${CMAKE_BINARY_DIR}/oniguruma-download"
	)

	# Disable oniguruma features we don't need
	set(INSTALL_DOCUMENTATION OFF)
	set(INSTALL_EXAMPLES OFF)
	set(BUILD_TEST OFF)
	set(BUILD_SHARED_LIBS OFF)
	set(MSVC_STATIC_RUNTIME ON)

	# For Windows: Prevent overriding the parent project's compiler/linker settings
	set(oniguruma_force_shared_crt ON CACHE BOOL "" FORCE)

	# Configure the CRNL handling
	add_compile_definitions(USE_CRNL_AS_LINE_TERMINATOR=1)

	# Add oniguruma directly to our build
	set(CMAKE_POLICY_DEFAULT_CMP0063 NEW)
	set(CMAKE_POLICY_DEFAULT_CMP0077 NEW)
	add_subdirectory("${CMAKE_BINARY_DIR}/oniguruma-src"
			"${CMAKE_BINARY_DIR}/oniguruma-build"
	)
	add_library(oniguruma::onig ALIAS onig)
endif()

configure_file(qsp_config.h.cmakein qsp_config.h @ONLY)

set(NDKQSP_SOURCES
		src/bindings/bindings_config.h
		src/bindings/default/default_callbacks.c
		src/bindings/default/default_control.c
		src/bindings/default/qsp_default.h
		src/bindings/android/android_callbacks.c
		src/bindings/android/android_control.c
		src/bindings/android/android_coding.c
		src/bindings/android/qsp_android.h
		src/bindings/qsp.h
		src/actions.c src/actions.h
		src/callbacks.c src/callbacks.h
		src/codetools.c src/codetools.h
		src/coding.c src/coding.h
		src/common.c src/common.h
		src/errors.c src/errors.h
		src/game.c src/game.h
		src/locations.c src/locations.h
		src/mathops.c src/mathops.h
		src/memwatch.c src/memwatch.h
		src/menu.c src/menu.h
		src/objects.c src/objects.h
		src/playlist.c src/playlist.h
		src/statements.c src/statements.h
		src/text.c src/text.h
		src/time.c src/time.h
		src/towlower.c
		src/towupper.c
		src/variables.c src/variables.h
		src/variant.c src/variant.h
)
add_library(ndkqsp SHARED ${NDKQSP_SOURCES})
target_compile_definitions(ndkqsp PUBLIC _UNICODE)
if (WIN32)
	target_compile_definitions(ndkqsp PRIVATE _CRT_SECURE_NO_WARNINGS)
endif()
target_compile_options(ndkqsp PRIVATE ${PROJECT_COMPILER_FLAGS})
target_link_options(ndkqsp PRIVATE ${PROJECT_LINKER_FLAGS})
target_link_libraries(ndkqsp PRIVATE oniguruma::onig)

target_include_directories(ndkqsp
		INTERFACE
		"$<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/src/bindings/default>"
		"$<BUILD_INTERFACE:${CMAKE_CURRENT_BINARY_DIR}>"
		"$<INSTALL_INTERFACE:${CMAKE_INSTALL_INCLUDEDIR}/src>"
		"$<INSTALL_INTERFACE:${CMAKE_INSTALL_INCLUDEDIR}/src/default>"
)

install(TARGETS ndkqsp EXPORT QspTargets)

generate_export_header(ndkqsp
		BASE_NAME Qsp
		EXPORT_MACRO_NAME QSP_EXTERN
)

write_basic_package_version_file(${CMAKE_CURRENT_BINARY_DIR}/cmake/Qsp/QspConfigVersion.cmake COMPATIBILITY AnyNewerVersion)
configure_package_config_file(QspConfig.cmake.in ${CMAKE_CURRENT_BINARY_DIR}/cmake/Qsp/QspConfig.cmake
		INSTALL_DESTINATION "${CMAKE_INSTALL_LIBDIR}/cmake/Qsp"
)

export(TARGETS ndkqsp NAMESPACE Qsp:: FILE ${CMAKE_CURRENT_BINARY_DIR}/QspConfig.cmake)
install(EXPORT QspTargets NAMESPACE Qsp:: DESTINATION "${CMAKE_INSTALL_LIBDIR}/cmake/Qsp")

install(
		DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/cmake
		DESTINATION "${CMAKE_INSTALL_LIBDIR}"
)

install(FILES
		src/bindings/qsp.h
		src/bindings/bindings_config.h
		"${CMAKE_CURRENT_BINARY_DIR}/qsp_export.h"
		DESTINATION
		"${CMAKE_INSTALL_INCLUDEDIR}/src"
)

install(FILES
		src/bindings/default/qsp_default.h
		DESTINATION
		"${CMAKE_INSTALL_INCLUDEDIR}/src/default"
)