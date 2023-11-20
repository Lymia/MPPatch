addSbtPlugin("com.typesafe.sbt" % "sbt-git"      % "1.0.0")
addSbtPlugin("com.eed3si9n"     % "sbt-assembly" % "0.15.0")

// Dependencies for the util package
resolvers += "SpringSource" at "https://repository.springsource.com/maven/bundles/external"

libraryDependencies += "com.github.rjeschke" % "txtmark" % "0.13"
