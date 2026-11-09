# androidx.room 2.2.5 arrives transitively through play-services-ads ->
# androidx.work:work-runtime. Room creates the generated WorkDatabase_Impl via
# Class.forName(...).newInstance(), so the no-arg constructor is only used
# reflectively. R8 full mode cannot see that use and strips the constructor,
# which crashes WorkManager initialization in androidx.startup before the
# activity starts ("Failed to create an instance of ...WorkDatabase").
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
