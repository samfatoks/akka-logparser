
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
    //    Resolver.jcenterRepo
  ),
  libraryDependencies ++= commonDependencies,
)

lazy val revolverSettings = Seq(
  javaOptions in reStart += "-Xmx2g",
  envVars in reStart := Map("PORT" -> "8080")
)

lazy val assemblySettings = Seq(
  mainClass in assembly := Some("com.fibbo.logparser.Boot"),
  assemblyJarName in assembly := s"logparser-${version.value}.jar",
  //  assemblyExcludedJars in assembly := {
  //    val cp = (fullClasspath in assembly).value
  //    cp filter {_.data.getName == "compile-0.1.0.jar"}
  //  }
)

val akkaVersion = "2.5.19"
val circeVersion = "0.9.3"
val logbackVersion   = "1.2.3"
val scalaTestV = "3.0.5"
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
  "com.lightbend.akka" %% "akka-stream-alpakka-elasticsearch" % "1.0-M2",
)

val testStack = Seq(
  "org.scalatest" %% "scalatest" % scalaTestV % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
)

val loggingStack = Seq(
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "net.logstash.logback" % "logstash-logback-encoder" % "4.11",
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
)

val utilStack = Seq(
  "net.codingwell" %% "scala-guice" % "4.2.1",
//  "com.google.inject.extensions" % "guice-assistedinject" % "4.2.2",
  "cool.graph" % "cuid-java" % "0.1.1",
//  "com.lihaoyi" %% "upickle" % "0.7.1",
//  "com.thesamet.scalapb" %% "compilerplugin" % "0.8.2",
  "com.sksamuel.avro4s" %% "avro4s-core" % "2.0.3",
  //"com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
)

val commonDependencies = testStack ++ circe ++ loggingStack

lazy val main = (project in file("."))
//  .aggregate(backend)

//lazy val backend: Project = (project in file("backend"))
  .settings(commonSettings: _*)
  .settings(revolverSettings: _*)
  .settings(assemblySettings: _*)
  .settings(
    name := "AkkaLogParser",
    libraryDependencies ++= akkaStack ++ utilStack,
    inThisBuild(List(
      version := "0.0.1",
      organizationName := "Fibbometrix Technologies",
      startYear := Some(2019),
      licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    )),
  )
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(RevolverPlugin)


