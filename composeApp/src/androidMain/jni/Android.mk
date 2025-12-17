LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := stockfish
LOCAL_SRC_FILES := $(wildcard $(LOCAL_PATH)/../../../../../Stockfish/src/*.cpp)
LOCAL_C_INCLUDES := $(LOCAL_PATH)/../../../../../Stockfish/src
LOCAL_CPPFLAGS := -std=c++17 -Wall -Wcast-qual -fno-exceptions -fno-rtti -pedantic -Wextra -Wshadow -DUSE_POPCNT -DUSE_PEXT -DNDEBUG -O3 -flto
LOCAL_LDFLAGS := -flto

include $(BUILD_SHARED_LIBRARY)
