-keepnames class ** { *; }
-optimizations enum/unboxing/*
-optimizations class/merging/*
-assumenosideeffects class **$$Lambda$* { *; }
 -assumenosideeffects class android.util.Log { *; }
-keep class tv.danmaku.ijk.media.player.** { *; }
-assumenosideeffects class kotlinx.coroutines.DebugStrings {
    public static *** toString(...);
}