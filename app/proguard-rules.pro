# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in Android SDK tools.
# For more details, see
#   https://developer.android.com/build/shrink-code

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}