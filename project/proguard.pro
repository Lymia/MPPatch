-dontobfuscate
-ignorewarnings
-optimizationpasses 5
-allowaccessmodification

-keepattributes *
-optimizations !code/allocation/variable

-keep public class moe.lymia.multiverse.MultiverseModManager {
    public static void main(java.lang.String[]);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
