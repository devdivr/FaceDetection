APP_PLATFORM := android-14
APP_CPPFLAGS := -frtti -fexceptions -O3 -Wall -Wextra -pedantic -Wno-long-long -Wno-unused-parameter -Wno-unknown-pragmas -Wstrict-aliasing
LOCAL_CFLAGS :=  -march=i686 -mtune=atom -mstackrealign -msse3 -mfpmath=sse -m32
APP_STL := gnustl_static
APP_ABI := all32
APP_OPTIM := release
NDK_TOOLCHAIN_VERSION := 4.9
