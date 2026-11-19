# Add project specific ProGuard / R8 rules here.
# For more details, see:
#   https://developer.android.com/build/shrink-code

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Room database entities and DAO interfaces
-keep class com.example.data.*Entity { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep game model enums whose constant names are serialized to Room / SharedPreferences via Enum.name / Enum.valueOf
-keepclassmembers enum com.example.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
