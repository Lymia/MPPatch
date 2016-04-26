-ignorewarnings
-optimizationpasses 5
-allowaccessmodification

-keeppackagenames moe.lymia.**
-keepclasseswithmembernames class moe.lymia.**
-keepclassmembernames class **

-flattenpackagehierarchy moe.lymia.multiverse.libraries
-keepattributes SourceFile,LineNumberTable

-keep public class moe.lymia.multiverse.MultiverseModManager {
    public static void main(java.lang.String[]);
}