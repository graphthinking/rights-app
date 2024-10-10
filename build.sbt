name := "rights-app"

version := "0.1"

Universal / packageName := "rights-app-dist"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

routesGenerator := InjectedRoutesGenerator

libraryDependencies += "org.apache.jena" % "apache-jena-libs" % "2.13.0"
libraryDependencies += "com.github.jknack" % "handlebars" % "2.2.2"
libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "4.0.1.201506240215-r"
libraryDependencies += "commons-io" % "commons-io" % "2.4"
libraryDependencies += "com.google.inject" % "guice" % "3+"
libraryDependencies += "co.elastic.apm" % "elastic-apm-agent" % "1.34.1"
libraryDependencies += guice

Test / javaOptions += "-Dconfig.file=conf/test.conf"
Universal / javaOptions ++= Seq("-Dpidfile.path=/dev/null","-Dconfig.file=conf/environment.conf")
scalaVersion := "2.13.14"

enablePlugins(JavaAppPackaging,DockerPlugin)
dockerExposedPorts := Seq(9000)
dockerBaseImage := "eclipse-temurin:17-jdk"
dockerRepository := Option("docker.io/europeana")
