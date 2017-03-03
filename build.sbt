name := "KinesisClientTut"

version := "1.0"

scalaVersion := "2.11.8"

mainClass in run := Some("Consumer")

lazy val kinesis_client = RootProject(file("../core-kinesis-client"))
val main = Project(id = "application", base = file(".")).dependsOn(kinesis_client)


libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-actor_2.11" % "2.4.17"
)
    