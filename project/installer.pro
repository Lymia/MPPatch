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
-optimizationpasses 2

# Disable optimizations that can make stack traces harder to read.
-optimizations !method/inlining/unique,!method/inlining/short,!class/merging/*,*
