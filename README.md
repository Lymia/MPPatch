MPPatch
=======

[![Build Status](https://lymia.moe/jenkins/job/MPPatch/badge/icon)](https://lymia.moe/jenkins/job/MPPatch/)

MPPatch is a patch for Civilization V that allows mods to be used in multiplayer without any special preparation.
It currently supports Windows and Linux. I will eventually add Mac support. I don't currently have access to a Mac,
so this could take some time.

You can download the [latest version of MPPatch here](https://github.com/Lymia/MPPatch/releases).

For usage instructions, [read the user guide](https://github.com/Lymia/MPPatch/wiki/User-Manual).

Compiling
---------

MPPatch can only be built on Linux systems. The build scripts have only been tested on the distributions listed below,
and have only been extensively tested on Arch Linux. You are on your own for other distributions.

On Ubuntu, you will need the following packages: `openjdk-8-jdk sbt mingw-w64 nasm`. In addition, on 64-bit systems you
will need `libc6-dev-i386`. Note that sbt is not in the default repositories, so, you will need to add the repository
manually. See [here](http://www.scala-sbt.org/0.13/tutorial/Installing-sbt-on-Linux.html) for details.

On Arch Linux, you will need the following packages: `base-devel jdk8-openjdk sbt mingw-w64-gcc nasm gcc-multilib`.

The first time you build a release, you must initialize submodules used by MPPatch. To do this, run
`git submodule update --init`.

To build a release, use `sbt clean dist`. You can also use `sbt run` to test your local version without building a full
release.

Contributing
------------

Pull requests welcome. :)