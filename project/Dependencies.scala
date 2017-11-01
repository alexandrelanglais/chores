import sbt._

object Dependencies {
  // Versions
  lazy val akkaVersion            = "2.3.8"
  lazy val playJsonV              = "2.6.7" // For Mongo.
  lazy val pureConfigV            = "0.8.0"
  lazy val reactiveMongoV         = "0.12.6"
  lazy val reactiveMongoPlayJsonV = "0.12.7-play26" // For Mongo.

  // Libraries
  val akkaHttp          = "com.typesafe.akka"          %% "akka-http"            % "10.0.10"
  val akkaStream        = "com.typesafe.akka"          %% "akka-stream"          % "2.5.4"
  val akkaActor         = "com.typesafe.akka"          %% "akka-actor"           % "2.5.4"
  val akkaHttpSprayJson = "com.typesafe.akka"          %% "akka-http-spray-json" % "10.0.10"
  val logBack           = "ch.qos.logback"             % "logback-classic"       % "1.2.3"
  val scalaLogging      = "com.typesafe.scala-logging" %% "scala-logging"        % "3.7.2"
  val reactiveMongo     = "org.reactivemongo"          %% "reactivemongo"        % reactiveMongoV
  val cors              = "ch.megard"                  %% "akka-http-cors"       % "0.2.2"

  // Projects
  val backendDeps =
    Seq(akkaHttp, akkaStream, akkaActor, akkaHttpSprayJson, logBack, scalaLogging, reactiveMongo, cors)

}
