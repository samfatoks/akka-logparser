lazy val commonSettings = Seq(
  version       := "1.0",
  scalaVersion  := "2.12.8",
  organization := "com.fibbometrix",
  crossVersion := CrossVersion.binary,
  test in assembly := {},
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature", "-language:higherKinds", "-language:implicitConversions", "-Ydelambdafy:method", "-target:jvm-1.8", "-Ypartial-unification"),
  resolvers     ++= Seq(
    "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    Resolver.sonatypeRepo("snapshots"),
    Resolver.sonatypeRepo("releases"),
  ),
  libraryDependencies ++= commonDependencies,
)

lazy val revolverSettings = Seq(
  javaOptions in reStart += "-Xmx2g",
  envVars in reStart := Map("PORT" -> "8080")
)

lazy val assemblySettings = Seq(
  mainClass in assembly := Some("com.fibbometrix.logparser.Boot"),
  assemblyJarName in assembly := s"logparser-${version.value}.jar",
)

val akkaVersion = "2.5.30"
val circeVersion = "0.12.3"
val logbackVersion   = "1.2.3"
val scalaLoggingVersion = "3.9.0"


val circe = Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-literal" % circeVersion,
)

val akkaStack = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-kafka" % "1.0-RC1",
)

val loggingStack = Seq(
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "net.logstash.logback" % "logstash-logback-encoder" % "4.11",
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
)

val utilStack = Seq(
  "net.codingwell" %% "scala-guice" % "4.2.1",
)
val commonDependencies = circe ++ loggingStack

lazy val main = (project in file("."))
  .settings(commonSettings: _*)
  .settings(revolverSettings: _*)
  .settings(assemblySettings: _*)
  .settings(
    name := "akka-logparser",
    libraryDependencies ++= akkaStack ++ utilStack,
    inThisBuild(List(
      version := "0.0.1",
      organizationName := "Fibbometrix Technologies",
      startYear := Some(2019),
      licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    )),
  )
  .enablePlugins(RevolverPlugin)


