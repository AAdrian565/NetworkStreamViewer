cmake_minimum_required(VERSION 3.22.1)

project(network_stream_viewer LANGUAGES CXX)

if(NOT DEFINED NDI_SDK_DIR)
    message(FATAL_ERROR "NDI_SDK_DIR was not supplied by Gradle")
endif()

add_library(ndi SHARED IMPORTED)
set_target_properties(ndi PROPERTIES
    IMPORTED_LOCATION "${NDI_SDK_DIR}/lib/${ANDROID_ABI}/libndi.so"
)

add_library(ndi_bridge SHARED ndi_bridge.cpp)
target_include_directories(ndi_bridge PRIVATE "${NDI_SDK_DIR}/include")
target_compile_features(ndi_bridge PRIVATE cxx_std_17)
target_compile_options(ndi_bridge PRIVATE -Wall -Wextra -Werror)

find_library(android-lib android REQUIRED)
find_library(log-lib log REQUIRED)

target_link_libraries(ndi_bridge PRIVATE ndi ${android-lib} ${log-lib})
