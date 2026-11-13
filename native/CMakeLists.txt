cmake_minimum_required(VERSION 3.22)
project(netormium_chromium_components C)

include(FetchContent)

# Brotli is shipped in Chromium's third_party tree and is independently
# buildable. Pin the upstream release for reproducible native builds.
set(BROTLI_DISABLE_TESTS ON CACHE BOOL "" FORCE)
set(BROTLI_BUNDLED_MODE ON CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)

FetchContent_Declare(
  brotli
  GIT_REPOSITORY https://github.com/google/brotli.git
  GIT_TAG v1.1.0
  GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(brotli)

# Expose only the decoder to the first Netormium integration milestone.
# The Android companion can link this target without bundling the encoder
# or command-line tool in its runtime.
add_library(netormium_brotli_decoder INTERFACE)
target_link_libraries(netormium_brotli_decoder INTERFACE brotlidec)
target_include_directories(netormium_brotli_decoder INTERFACE
  "${brotli_SOURCE_DIR}/c/include"
)

message(STATUS "Netormium native component: upstream Google Brotli v1.1.0")
message(STATUS "Brotli decoder target: netormium_brotli_decoder")
