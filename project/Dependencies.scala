import sbt._

object Dependencies {
  val DAML_SDK_VERSION = "100.13.12"
  val scalapbVersion = "0.9.2"
  val yamlVersion = "1.23"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"
  lazy val damlJavaBinding = "com.daml.ledger" % "bindings-java" % DAML_SDK_VERSION
  lazy val damlScalaBinding = "com.daml.scala" %% "bindings" % DAML_SDK_VERSION
  lazy val damlLedgerClient = "com.daml.ledger" % "bindings-rxjava" % DAML_SDK_VERSION
  lazy val grizzledLogger = "org.clapper" %% "grizzled-slf4j" % "1.3.4"
  lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"
  lazy val commonsIO = "commons-io" % "commons-io" % "2.6"
  lazy val damlLFArchive = "com.digitalasset" %% "daml-lf-archive-scala" % DAML_SDK_VERSION
  lazy val damlLFArchivePlain = "com.digitalasset" % "daml-lf-archive" % DAML_SDK_VERSION
  lazy val scalapbJson4s =   "com.thesamet.scalapb" %% "scalapb-json4s" % scalapbVersion
  lazy val snakeYaml =  "org.yaml" % "snakeyaml" % yamlVersion
  lazy val hamcrestOptional = "com.spotify" % "hamcrest-optional" % "1.1.4"

  lazy val junit4 = "junit" % "junit" % "4.12"
  lazy val junitInterface = "com.novocode" % "junit-interface" % "0.11" % Test exclude("junit", "junit-dep")
}
