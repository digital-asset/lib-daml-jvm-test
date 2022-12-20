import sbt._

object Dependencies {
  val DAML_SDK_VERSION = "2.2.1"
  val scalapbVersion = "0.9.2"
  val yamlVersion = "1.33"
  val cucumberVersion = "5.7.0"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.14"
  lazy val damlJavaBinding = "com.daml" % "bindings-java" % DAML_SDK_VERSION
  lazy val damlLedgerClient = "com.daml" % "bindings-rxjava" % DAML_SDK_VERSION
  lazy val damlLFArchive = "com.daml" % "daml-lf-dev-archive-java-proto" % DAML_SDK_VERSION

  lazy val grizzledLogger = "org.clapper" %% "grizzled-slf4j" % "1.3.4"
  lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.4.5"
  lazy val commonsIO = "commons-io" % "commons-io" % "2.11.0"
  lazy val snakeYaml = "org.yaml" % "snakeyaml" % yamlVersion
  lazy val hamcrestOptional = "com.spotify" % "hamcrest-optional" % "1.3.1"

  lazy val scalaz = "org.scalaz" %% "scalaz-core" % "7.3.7"
  lazy val guava = "com.google.guava" % "guava" % "31.1-jre"

  lazy val junit5 = "org.junit.jupiter" % "junit-jupiter-api" % "5.9.1"
  lazy val junitInterface = "net.aichler" % "jupiter-interface" % "0.11.1" % Test

  lazy val cucumberJ8 = "io.cucumber" % "cucumber-java8" % cucumberVersion
  lazy val cucumberJunit = "io.cucumber" % "cucumber-junit" % cucumberVersion
  lazy val cucumberPicoContainer = "io.cucumber" % "cucumber-picocontainer" % cucumberVersion

}
