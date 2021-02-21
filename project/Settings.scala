import ch.epfl.scala.sbtmissinglink.MissingLinkPlugin.autoImport._
import sbt._

object Settings {
  lazy val missingLinkSettings: List[Def.Setting[_]] = List(
    missinglinkExcludedDependencies ++= List(
      moduleFilter(organization = "ch.qos.logback", name = "logback-classic"),
      moduleFilter(organization = "ch.qos.logback", name = "logback-core"),
      moduleFilter(organization = "com.squareup.okhttp3", name = "okhttp"),
      moduleFilter(organization = "com.sun.activation", name = "jakarta.activation"),
      moduleFilter(organization = "com.sun.mail", name = "jakarta.mail"),
      moduleFilter(organization = "com.zaxxer", name = "HikariCP"),
      moduleFilter(organization = "commons-logging", name = "commons-logging"),
      moduleFilter(organization = "io.micrometer", name = "micrometer-registry-statsd"),
      moduleFilter(organization = "io.netty", name = "netty-codec"),
      moduleFilter(organization = "io.netty", name = "netty-common"),
      moduleFilter(organization = "io.netty", name = "netty-handler"),
      moduleFilter(organization = "io.sentry", name = "sentry"),
      moduleFilter(organization = "jakarta.activation", name = "jakarta.activation-api"),
      moduleFilter(organization = "org.apache.logging.log4j", name = "log4j-api"),
      moduleFilter(organization = "org.apache.logging.log4j", name = "log4j-core"),
      moduleFilter(organization = "org.apache.logging.log4j", name = "log4j-slf4j-impl"),
      moduleFilter(organization = "org.asynchttpclient", name = "async-http-client"),
      moduleFilter(organization = "org.javassist", name = "javassist"),
      moduleFilter(organization = "org.jboss.logging", name = "jboss-logging"),
      moduleFilter(organization = "org.jboss.resteasy", name = "resteasy-jaxrs"),
      moduleFilter(organization = "org.reflections", name = "reflections"),
      moduleFilter(organization = "org.slf4j", name = "slf4j-api")
    ),
    missinglinkIgnoreSourcePackages += IgnoredPackage("cz.jenda")
  )
}
