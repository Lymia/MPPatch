MPPatch
=======

[![Build Status](https://lymia.moe/jenkins/job/MPPatch/badge/icon)](https://lymia.moe/jenkins/job/MPPatch/)

MPPatch is a patch for Civilization V that allows mods to be used in multiplayer without any special preparation.
It supports Windows, macOS and Linux.

You can download the [latest version of MPPatch here](https://github.com/Lymia/MPPatch/releases).

For usage instructions, [read the user guide](https://github.com/Lymia/MPPatch/wiki/User-Manual).

Compiling
---------

MPPatch can only be built on Linux systems. The build scripts has only been extensively tested on Arch Linux. You are
on your own for other distributions.

On Arch Linux, you will need the following packages: `base-devel jdk8-openjdk sbt mingw-w64-gcc nasm gcc-multilib
clang llvm`.

The first time you build a release, you must initialize submodules used by MPPatch. To do this, run
`git submodule update --init`.

You will also need to install [osxcross](https://github.com/tpoechtrager/osxcross). After cloning osxcross into a
directory and setting up the xcode tarballs, execute `./build.sh`, then add `osxcross/target/bin` to your `PATH`.

To build a release, use `sbt clean dist`. You can also use `sbt run` to test your local version without building a full
release.

Contributing
------------

Pull requests welcome. :)