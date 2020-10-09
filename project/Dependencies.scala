import sbt._

object Dependencies {
  val DAML_SDK_VERSION = "1.0.0"
  val scalapbVersion = "0.9.2"
  val yamlVersion = "1.23"
  val cucumberVersion = "4.3.1"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"
  lazy val damlJavaBinding = "com.daml" % "bindings-java" % DAML_SDK_VERSION
  lazy val damlLedgerClient = "com.daml" % "bindings-rxjava" % DAML_SDK_VERSION
  lazy val damlLFArchive = "com.daml" % "daml-lf-dev-archive-java-proto" % DAML_SDK_VERSION

  lazy val grizzledLogger = "org.clapper" %% "grizzled-slf4j" % "1.3.4"
  lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"
  lazy val commonsIO = "commons-io" % "commons-io" % "2.6"
  lazy val snakeYaml = "org.yaml" % "snakeyaml" % yamlVersion
  lazy val hamcrestOptional = "com.spotify" % "hamcrest-optional" % "1.1.4"

  lazy val aether1 = "org.eclipse.aether" % "aether-api" % "1.1.0"
  lazy val aether2 = "org.eclipse.aether" % "aether-spi" % "1.1.0"
  lazy val aether3 = "org.eclipse.aether" % "aether-impl" % "1.1.0"
  lazy val aether4 = "org.eclipse.aether" % "aether-connector-basic" % "1.1.0"
  lazy val aether5 = "org.eclipse.aether" % "aether-transport-file" % "1.1.0"
  lazy val aether6 = "org.eclipse.aether" % "aether-transport-http" % "1.1.0"
  lazy val aether7 = "org.apache.maven" % "maven-aether-provider" % "3.3.9"


  lazy val scalaz = "org.scalaz" %% "scalaz-core" % "7.2.24"
  lazy val guava = "com.google.guava" % "guava" % "28.0-jre"

  lazy val junit4 = "junit" % "junit" % "4.12"
  lazy val junitInterface = "com.novocode" % "junit-interface" % "0.11" % Test exclude ("junit", "junit-dep")

  lazy val cucumberJ8 = "io.cucumber" % "cucumber-java8" % cucumberVersion
  lazy val cucumberJunit = "io.cucumber" % "cucumber-junit" % cucumberVersion
  lazy val cucumberPicoContainer = "io.cucumber" % "cucumber-picocontainer" % cucumberVersion

}
