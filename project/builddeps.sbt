addSbtPlugin("com.typesafe.sbt" % "sbt-proguard" % "0.2.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")

// Dependencies for the util package
resolvers += "SpringSource" at "http://repository.springsource.com/maven/bundles/external"

libraryDependencies += "org.tukaani" % "xz" % "1.5"
libraryDependencies += "com.github.rjeschke" % "txtmark" % "0.13"
libraryDependencies += "net.sf.launch4j" % "launch4j" % "3.9"

// Include common source files
unmanagedSourceDirectories in Compile +=
  baseDirectory.value / ".." / "src" / "main" / "scala" / "moe" / "lymia" / "mppatch" / "util" / "common"