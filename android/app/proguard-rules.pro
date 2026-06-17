# Add project specific ProGuard rules here.

# Jsoup
-keep class org.jsoup.** { *; }

# OkHttp (uses reflection in places)
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
