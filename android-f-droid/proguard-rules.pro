-assumenosideeffects class **$$Lambda$* { *; }
 -assumenosideeffects class android.util.Log { *; }
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**
-keep class tv.danmaku.ijk.media.player.** { *; }
-assumenosideeffects class kotlinx.coroutines.DebugStrings {
    public static *** toString(...);
}
# 告诉 D8/R8，如果碰到了重写失败的 Kotlin 元数据，直接闭嘴
-dontwarn kotlin.Metadata
-dontwarn kotlin.jvm.**
-dontwarn me.voltual.pyrolysis.**