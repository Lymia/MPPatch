-dontobfuscate
-ignorewarnings
-optimizationpasses 5
-allowaccessmodification

-keepattributes *

-keep public class moe.lymia.mppatch.MPPatchInstaller {
    public static void main(java.lang.String[]);
}
