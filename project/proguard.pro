-dontobfuscate
-ignorewarnings
-optimizationpasses 5
-allowaccessmodification

-keepattributes *

-keep public class moe.lymia.mppatch.MPPatch {
    public static void main(java.lang.String[]);
}
