addSbtPlugin("com.typesafe.sbt" % "sbt-proguard" % "0.2.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")

// Dependencies for the util package
libraryDependencies += "org.tukaani" % "xz" % "1.5"
libraryDependencies += "org.whispersystems" % "curve25519-java" % "0.3.0"

// Include common source files
unmanagedSourceDirectories in Compile +=
  baseDirectory.value / ".." / "src" / "main" / "scala" / "moe" / "lymia" / "mppatch" / "util" / "common"