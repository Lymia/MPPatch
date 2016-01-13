Multiverse Mod Manager
======================

Multiverse Mod Manager allows Civilization V mods to be used in multiplayer by rewriting mods so that the game
treats them as official DLCs. To allow mods that use .sql scripts to work, Multiverse Mod Manager installs a patch
that modifies some of Civilization's V code to allow .xml files to directly run SQL code.

In short, this program is an attempt to combine the simplicity of using JdH's CiV MP Mod Manager, and MPMPM's ability
to actually support the majority of mods out there.

Currently, Multiverse Mod Manager supports Windows and Linux. Mac support is planned "eventually", but, as I don't
have a Mac, this will take a while. 

Compiling
---------

Multiverse Mod Manager can only be built on Unix systems. The build scripts have only been tested on the distributions
listed below, and have only been extensively tested on Arch Linux. You are on your own for other distributions.

On Ubuntu, you will need the following packages: `openjdk-8-jdk sbt mingw-w64 nasm`. In addition, on 64-bit systems you
will need `libc6-dev-i386`. Note that sbt is not in the default repositories, so, you will need to add the repository
manually. See [here](http://www.scala-sbt.org/0.13/tutorial/Installing-sbt-on-Linux.html) for details.

On Arch Linux, you will need the following packages: `base-devel jdk8-openjdk sbt mingw-w64-gcc nasm`. In addition, on
64-bit systems, you will need `gcc-multilib`.

To build, simply type in `sbt proguard:proguard`.
