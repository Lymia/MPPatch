MPPatch
=======

[![Build Status](https://lymia.moe/jenkins/job/MPPatch/badge/icon)](https://lymia.moe/jenkins/job/MPPatch/)

MPPatch allows Civilization V mods to be used in multiplayer by patching out some of the code in the Civilization V
binary that deactivates mods when a multiplayer game is started, and reactivating the dummied out modded multiplayer
menus in its GUI.

Currently, MPPatch supports Windows and Linux. Mac support is planned "eventually", but, as I don't have a Mac, this
will take a while. 

Compiling
---------

MPPatch can only be built on Linux systems. The build scripts have only been tested on the distributions listed below,
and have only been extensively tested on Arch Linux. You are on your own for other distributions.

On Ubuntu, you will need the following packages: `openjdk-8-jdk sbt mingw-w64 nasm`. In addition, on 64-bit systems you
will need `libc6-dev-i386`. Note that sbt is not in the default repositories, so, you will need to add the repository
manually. See [here](http://www.scala-sbt.org/0.13/tutorial/Installing-sbt-on-Linux.html) for details.

On Arch Linux, you will need the following packages: `base-devel jdk8-openjdk sbt mingw-w64-gcc nasm`. In addition, on
64-bit systems, you will need `gcc-multilib`.

The first time you build a release, you must initialize submodules used by MPPatch. To do this, run
`git submodule update --init`.

To build a release, use `sbt clean dist`. You can also use `sbt run` to test your local version without building a full
release.
