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

Multiverse Mod Manager can only be built on Unix systems, and furthermore, the build scripts have only been tested
for Arch Linux. You will need the following packages:

 * TODO: package list goes here
 
To build, simply type in `sbt proguard:proguard`.
