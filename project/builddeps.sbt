addSbtPlugin("com.github.sbt" % "sbt-git"          % "2.2.0")
addSbtPlugin("com.eed3si9n"   % "sbt-assembly"     % "2.5.0")
addSbtPlugin("org.scalameta"  % "sbt-native-image" % "0.3.2")

// Dependencies for the util package
resolvers += "SpringSource" at "https://repository.springsource.com/maven/bundles/external"

libraryDependencies += "com.github.rjeschke" % "txtmark" % "0.13"
