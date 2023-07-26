import sbt._

object Dependencies {
  val DAML_SDK_VERSION = "2.6.5"
  val cucumberVersion = "7.13.0"

  lazy val damlJavaBinding = "com.daml" % "bindings-java" % DAML_SDK_VERSION
  lazy val damlLedgerClient = "com.daml" % "bindings-rxjava" % DAML_SDK_VERSION
  lazy val damlLFArchive = "com.daml" % "daml-lf-dev-archive-java-proto" % DAML_SDK_VERSION

  lazy val grizzledLogger = "org.clapper" %% "grizzled-slf4j" % "1.3.4"
  lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.4.8"
  lazy val commonsIO = "commons-io" % "commons-io" % "2.13.0"
  lazy val hamcrestOptional = "com.spotify" % "hamcrest-optional" % "1.3.2"

  lazy val scalaz = "org.scalaz" %% "scalaz-core" % "7.3.7"
  lazy val guava = "com.google.guava" % "guava" % "32.1.1-jre"

  lazy val junit5 = "org.junit.jupiter" % "junit-jupiter-api" % "5.10.0"
  lazy val junitInterface = "net.aichler" % "jupiter-interface" % "0.11.1" % Test

  lazy val cucumberJ8 = "io.cucumber" % "cucumber-java8" % cucumberVersion
  lazy val cucumberJunit = "io.cucumber" % "cucumber-junit" % cucumberVersion
  lazy val cucumberPicoContainer = "io.cucumber" % "cucumber-picocontainer" % cucumberVersion

}
