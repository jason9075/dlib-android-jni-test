cmake_minimum_required(VERSION 3.4.1)

set(TARGET_NAME native-lib)

set(INSTALL_DIR ${CMAKE_SOURCE_DIR}/../lib-distribution/)
# Configure import libs.
set(LIB_DIR ${CMAKE_SOURCE_DIR}/src/main/cppLibs)

add_library(dlib SHARED IMPORTED)
set_target_properties(dlib PROPERTIES IMPORTED_LOCATION
        ${LIB_DIR}/dlib/lib/${ANDROID_ABI}/libdlib.so)

# Link to libprotobuf-lite.so.
add_library(protobuf SHARED IMPORTED)
set_target_properties(protobuf PROPERTIES IMPORTED_LOCATION
        ${LIB_DIR}/protobuf/lib/${ANDROID_ABI}/libprotobuf-lite.so)


add_library(${TARGET_NAME}
        SHARED
        src/main/cpp/native-lib.cpp
        src/main/cpp/include/my/dlib/data/messages.pb.cc)
target_include_directories(${TARGET_NAME} PRIVATE
        ${CMAKE_SOURCE_DIR}/main/cpp/include
        ${LIB_DIR}/protobuf/include
        ${LIB_DIR}/dlib/include)
#set_target_properties(native-lib PROPERTIES
#        LIBRARY_OUTPUT_DIRECTORY
#        ${distribution_DIR}/libs/${ANDROID_ABI})

find_library(
        log-lib
        log)

target_link_libraries(
        ${TARGET_NAME}
        android
        dlib
        protobuf
        jnigraphics
        ${log-lib})

# Make install directory.
file(MAKE_DIRECTORY ${INSTALL_DIR}/${TARGET_NAME})
file(MAKE_DIRECTORY ${INSTALL_DIR}/${TARGET_NAME}/include)
file(MAKE_DIRECTORY ${INSTALL_DIR}/${TARGET_NAME}/include/data)
file(MAKE_DIRECTORY ${INSTALL_DIR}/${TARGET_NAME}/lib)

# Copy out lib header file.
file(COPY "${LIB_DIR}/dlib/include/"
        DESTINATION "${INSTALL_DIR}/${TARGET_NAME}/include/")

# Copy generated protobuf message header file.
file(COPY "${CMAKE_SOURCE_DIR}/src/main/cpp/include/my/dlib/data/messages.pb.h"
        DESTINATION "${INSTALL_DIR}/${TARGET_NAME}/include/my/dlib/data/")

# Copy out the project binary.
add_custom_command(TARGET ${TARGET_NAME} POST_BUILD

        # Copy pre-built *.so files.
        COMMAND "${CMAKE_COMMAND}" -E
        copy_directory "${LIB_DIR}/dlib/lib/${ANDROID_ABI}"
        "${INSTALL_DIR}/${TARGET_NAME}/lib/${ANDROID_ABI}"

        # Copy generated *.so files.
        COMMAND "${CMAKE_COMMAND}" -E
        copy_directory "${CMAKE_SOURCE_DIR}/build/intermediates/cmake/${CMAKE_BUILD_TYPE}/obj/${ANDROID_ABI}"
        "${INSTALL_DIR}/${TARGET_NAME}/lib/${ANDROID_ABI}"

        COMMENT "Copying ${TARGET_NAME} shared library to output directory")
