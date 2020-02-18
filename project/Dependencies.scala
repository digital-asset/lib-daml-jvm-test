import sbt._

object Dependencies {
  val DAML_SDK_VERSION = "100.13.52"
  val scalapbVersion = "0.9.2"
  val yamlVersion = "1.23"
  val cucumberVersion = "4.3.1"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"
  lazy val damlJavaBinding = "com.daml.ledger" % "bindings-java" % DAML_SDK_VERSION
  lazy val damlLedgerClient = "com.daml.ledger" % "bindings-rxjava" % DAML_SDK_VERSION
  lazy val damlLFArchive = "com.digitalasset" % "daml-lf-dev-archive-java-proto" % DAML_SDK_VERSION

  lazy val grizzledLogger = "org.clapper" %% "grizzled-slf4j" % "1.3.4"
  lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"
  lazy val commonsIO = "commons-io" % "commons-io" % "2.6"
  lazy val snakeYaml = "org.yaml" % "snakeyaml" % yamlVersion
  lazy val hamcrestOptional = "com.spotify" % "hamcrest-optional" % "1.1.4"

  lazy val scalaz = "org.scalaz" %% "scalaz-core" % "7.2.24"
  lazy val guava = "com.google.guava" % "guava" % "28.0-jre"

  lazy val junit4 = "junit" % "junit" % "4.12"
  lazy val junitInterface = "com.novocode" % "junit-interface" % "0.11" % Test exclude ("junit", "junit-dep")

  lazy val cucumberJ8 = "io.cucumber" % "cucumber-java8" % cucumberVersion
  lazy val cucumberJunit = "io.cucumber" % "cucumber-junit" % cucumberVersion
  lazy val cucumberPicoContainer = "io.cucumber" % "cucumber-picocontainer" % cucumberVersion

}
