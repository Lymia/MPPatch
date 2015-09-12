Multiverse Mod Manager Patches
==============================

This directory contains the various binary patches that Multiverse Mod Manager
uses to allow DLC files to use the functionality that mods need to run. To
build the binary patches, run `scripts/updatePatchDir.sh` under a Linux system.

On Windows, the patch creates a proxy around the CvGameDatabase .dll file,
containing both of the functions we patch.

On Linux, however, the CvGameDatabase binary is statically compiled into the
main binary, making it a lot harder for us to proxy stuff in it. Luckily,
Linux has LD_PRELOAD letting us inject into the main binary directly even with
no good .so files to proxy.

On Mac... I don't have a Mac, so, I can't develop a patch for the Mac version.
Contributions welcome.
