Multiverse Mod Manager
======================

[![Build Status](https://lymia.moe/jenkins/job/MultiverseModManager/badge/icon)]
(https://lymia.moe/jenkins/job/MultiverseModManager/)

Multiverse Mod Manager allows Civilization V mods to be used in multiplayer by patching out some of the code in the
Civilization V binary that deactivates mods when a multiplayer game is started, and reactivating the dummied out
modded multiplayer menus in its GUI.

Currently, Multiverse Mod Manager supports Windows and Linux. Mac support is planned "eventually", but, as I don't
have a Mac, this will take a while. 

This project is obviously pending a rename, as it is no longer a mod manager of any kind.

Compiling
---------

Multiverse Mod Manager can only be built on Unix systems. The build scripts have only been tested on the distributions
listed below, and have only been extensively tested on Arch Linux. You are on your own for other distributions.

On Ubuntu, you will need the following packages: `openjdk-8-jdk sbt mingw-w64 nasm`. In addition, on 64-bit systems you
will need `libc6-dev-i386`. Note that sbt is not in the default repositories, so, you will need to add the repository
manually. See [here](http://www.scala-sbt.org/0.13/tutorial/Installing-sbt-on-Linux.html) for details.

On Arch Linux, you will need the following packages: `base-devel jdk8-openjdk sbt mingw-w64-gcc nasm`. In addition, on
64-bit systems, you will need `gcc-multilib`.

To build a release, use `sbt clean dist`. You can also use `sbt run` to test your local version without building a full
release.