-ignorewarnings

########################
# Obfusication options #
########################

-keeppackagenames **
-keepattributes SourceFile,SourceDir,LineNumberTable,RuntimeVisible*Annotations*

-keepnames class ** {
    <methods>;
}

#########################
# Optimization settings #
#########################

-allowaccessmodification
-optimizationpasses 5

# Disable optimizations that can make stack traces harder to read.
-optimizations !method/inlining/unique,!method/inlining/short,!class/merging/*,*

#####################
# Shrinking options #
#####################

-keep public class moe.lymia.mppatch.MPPatchInstaller {
    public static void main(java.lang.String[]);
}
