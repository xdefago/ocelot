name := "Ocelot"

version := "0.1.1-SNAPSHOT"

scalaVersion := "2.12.1"

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
  "org.scala-graph" %% "graph-core" % "1.11.4",
  // testing
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  // logging
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "org.codehaus.groovy" % "groovy-all" % "2.4.7",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
)

publishTo := Some(Resolver.file("ocelot", file("sbt-repo")))

// to use:
//   resolvers += "ocelot" at "https://github.com/xdefago/ocelot/raw/master/sbt-repo/"
//   libraryDependencies += "default" %% "ocelot" % <version>
