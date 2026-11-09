# androidx.room 2.2.5 arrives transitively through play-services-ads ->
# androidx.work:work-runtime. Room creates the generated WorkDatabase_Impl via
# Class.forName(...).newInstance(), so the no-arg constructor is only used
# reflectively. R8 full mode cannot see that use and strips the constructor,
# which crashes WorkManager initialization in androidx.startup before the
# activity starts ("Failed to create an instance of ...WorkDatabase").
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}

# WindowCompat.enableEdgeToEdge() deliberately calls the platform
# setStatusBarColor/setNavigationBarColor APIs on older Android versions to
# provide backward-compatible edge-to-edge. Without this rule, R8 inlines the
# helper into MainActivity, so Play's Android 15 edge-to-edge scanner attributes
# those legacy library calls to the app. Keep the helper as its own AndroidX
# class so the app no longer owns any deprecated edge-to-edge call site.
-keep class androidx.core.view.WindowCompat {
    public static void enableEdgeToEdge(android.view.Window);
}
