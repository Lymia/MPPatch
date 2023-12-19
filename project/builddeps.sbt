addSbtPlugin("com.typesafe.sbt" % "sbt-git"          % "1.0.0")
addSbtPlugin("com.eed3si9n"     % "sbt-assembly"     % "2.1.5")
addSbtPlugin("org.scalameta"    % "sbt-native-image" % "0.3.2")

// Dependencies for the util package
resolvers += "SpringSource" at "https://repository.springsource.com/maven/bundles/external"

libraryDependencies += "com.github.rjeschke" % "txtmark" % "0.13"
