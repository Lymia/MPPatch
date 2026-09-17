> # Maintance Warning
> 
> The current version of MPPatch has not been properly maintained since early 2020, as I moved into a different living arrangement and lost access to my desktop. I currently (as of September 2026) have access to a second computer again, so I can continue work on this project. Expect a properly working version again within one to two months.
>
> Note that I still lack access to a Mac, and will likely drop Mac support for the foreseeable future. If you have access to a mac, and have the proper skills to work on a project like this, please email me! I'd appreciate the help.

MPPatch
=======

MPPatch is a patch for Civilization V that allows mods to be used in multiplayer without any special preparation.
It supports the Steam versions of Civilization V on Windows and Linux. macOS is supported, but only if you run
the Windows version of Civilization V through Proton.

You can download the [latest version of MPPatch here](https://github.com/Lymia/MPPatch/releases).

For usage instructions, [read the user guide](https://github.com/Lymia/MPPatch/wiki/User-Manual).

Compiling
---------

(TODO: These instructions are heavily outdated.)

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
