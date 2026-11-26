> # Maintance Warning
> 
> The current stable version of MPPatch has not been worked on since 2020, as I moved into a different living arrangement and lost access to my desktop. I have access to a desktop again, and am now continuing work on MPPatch.
> 
> **Update (2026-Nov-26):** I've released an updated alpha version of MPPatch that should support current versions of Steam and Civilzation V, as well as add compatibility with the new hotfix released for Windows. You can download it here: https://github.com/Lymia/MPPatch/releases/tag/v0.2.0-alpha.3
> 
> From here, I intend to fix any remaining bugs in the native patch or Lua patch, and work on a new installer written in Rust (like the current native patch) for easier maintenance.

MPPatch
=======

MPPatch is a patch for Civilization V that allows mods to be used in multiplayer without any special preparation. It supports the Steam versions of Civilization V on Windows and Linux. macOS is supported, but only if you run  the Windows version of Civilization V through Proton.

You can download the [latest version of MPPatch here](https://github.com/Lymia/MPPatch/releases/latest).

For usage instructions, [read the user guide](https://github.com/Lymia/MPPatch/wiki/User-Manual).

Compiling
---------

MPPatch can only be built on Linux systems. The build scripts has only been extensively tested on Fedora Linux. You are on your own for other distributions.

On Fedora, you will need the following packages: `gcc libgcc.i686 glibc-devel.i686 clang mingw32-binutils mingw32-gcc`. You will also need to install Rust via `rustup` and `sbt` separately.

The first time you build a release, you must initialize submodules used by MPPatch. To do this, run `git submodule update --init`.

Use the `sbt buildNative` command to build the native binaries. To build a release, use `sbt clean dist`. You can also use `sbt run` to test your local version without building a full  release.

Contributing
------------

Pull requests welcome. :)
