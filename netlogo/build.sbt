name := "NetLogo Minecraft Plugin"

version := "0.1"

organization := "jcdc.pluginfactory"

scalaVersion := "2.10.0-RC5"

resolvers += Resolver.sonatypeRepo("snapshots")

resolvers ++= Seq(
  "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype Releases"  at "http://oss.sonatype.org/content/repositories/releases",
  "Bukkit"             at "http://repo.bukkit.org/content/repositories/releases"
)

libraryDependencies ++= Seq(
  "jcdc.pluginfactory" % "jcdc-plugin-factory_2.10" % "0.1",
  "org.nlogo"          % "NetLogo"                  % "5.0.3" from "http://ccl.northwestern.edu/netlogo/5.0.3/NetLogo.jar",
  "javax.servlet"      % "servlet-api"              % "2.5" % "provided->default",
  "org.bukkit"         % "craftbukkit"              % "1.4.5-R0.2",
  "ch.spacebase"       % "NPCCreatures"             % "1.4" from "http://dev.bukkit.org/media/files/584/232/NPCCreatures.jar"
)