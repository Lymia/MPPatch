organization := "moe.lymia"
name := "multiverse-mod-manager"
version := "1.0"
scalaVersion := "2.11.7"

resolvers += Resolver.sonatypeRepo("public")

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.3"
libraryDependencies += "com.github.scopt" %% "scopt" % "3.3.0"

