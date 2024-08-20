name := "rights-app"

version := "0.1"

packageName in Universal := "rights-app-dist"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

routesGenerator := InjectedRoutesGenerator

libraryDependencies += "org.apache.jena" % "apache-jena-libs" % "2.13.0"
libraryDependencies += "com.github.jknack" % "handlebars" % "2.2.2"
libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "4.0.1.201506240215-r"
libraryDependencies += "commons-io" % "commons-io" % "2.4"

javaOptions in Test += "-Dconfig.file=conf/test.conf"

scalaVersion := "2.11.11"

enablePlugins(JavaAppPackaging,DockerPlugin)
dockerExposedPorts := Seq(9000)
dockerBaseImage := "openjdk:8-jdk"
