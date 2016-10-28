addSbtPlugin("com.typesafe.sbt" % "sbt-proguard" % "0.2.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")

// Dependencies for the util package
libraryDependencies += "org.tukaani" % "xz" % "1.5"

// should probably not go in plugins.sbt, but... easiest way.
unmanagedSourceDirectories in Compile +=
  baseDirectory.value / ".." / "src" / "main" / "scala" / "moe" / "lymia" / "mppatch" / "util" / "common"