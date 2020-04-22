import org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings

lazy val commonSettings = Seq(
  scalaVersion := "2.13.1",
  organization := "hu.ksisu",
  scalafmtOnCompile := true,
  version := "0.1.0"
)

lazy val ItTest         = config("it") extend Test
lazy val itTestSettings = Defaults.itSettings ++ scalafmtConfigSettings

lazy val root = project
  .in(file("."))
  .aggregate(core)

lazy val core = (project in file("."))
  .configs(ItTest)
  .settings(inConfig(ItTest)(itTestSettings): _*)
  .settings(commonSettings: _*)
  .settings(buildInfoSettings: _*)
  .settings(
    name := "imazsak",
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-unchecked",
      "-feature",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:postfixOps",
      "-Ywarn-dead-code",
      "-Xlint"
    ),
    libraryDependencies ++= {
      Seq(
        "org.typelevel"        %% "cats-core"                % "2.1.1",
        "org.typelevel"        %% "cats-effect"              % "2.1.3",
        "com.typesafe.akka"    %% "akka-http"                % "10.1.10",
        "com.typesafe.akka"    %% "akka-http-spray-json"     % "10.1.10",
        "com.typesafe.akka"    %% "akka-http-testkit"        % "10.1.10" % "it,test",
        "com.typesafe.akka"    %% "akka-actor"               % "2.6.1",
        "com.typesafe.akka"    %% "akka-stream"              % "2.6.1",
        "com.typesafe.akka"    %% "akka-slf4j"               % "2.6.1",
        "com.typesafe.akka"    %% "akka-testkit"             % "2.6.1" % "it,test",
        "ch.qos.logback"       % "logback-classic"           % "1.2.3",
        "net.logstash.logback" % "logstash-logback-encoder"  % "6.3",
        "org.slf4j"            % "jul-to-slf4j"              % "1.7.30",
        "com.pauldijou"        %% "jwt-core"                 % "4.3.0",
        "com.pauldijou"        %% "jwt-spray-json"           % "4.3.0",
        "commons-codec"        % "commons-codec"             % "1.14",
        "ch.megard"            %% "akka-http-cors"           % "0.4.3",
        "io.opentracing"       % "opentracing-api"           % "0.33.0",
        "io.opentracing"       % "opentracing-util"          % "0.33.0",
        "io.opentracing"       % "opentracing-noop"          % "0.33.0",
        "io.jaegertracing"     % "jaeger-client"             % "1.2.0",
        "org.reactivemongo"    %% "reactivemongo"            % "0.20.3",
        "com.lightbend.akka"   %% "akka-stream-alpakka-amqp" % "1.1.2",
        "com.lightbend.akka"   %% "akka-stream-alpakka-s3"   % "1.1.2",
        "org.bouncycastle"     % "bcprov-jdk15on"            % "1.65",
        "nl.martijndwars"      % "web-push"                  % "5.1.0",
        "org.codehaus.janino"  % "janino"                    % "3.1.2",
        "com.github.etaty"     %% "rediscala"                % "1.9.0",
        "org.scalatest"        %% "scalatest"                % "3.1.1" % "it,test",
        "org.mockito"          % "mockito-core"              % "3.3.3" % "it,test",
        "org.mockito"          %% "mockito-scala"            % "1.13.9" % "it,test"
      )
    }
  )

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt it:scalafmt")
addCommandAlias("testAll", "test it:test")

enablePlugins(JavaAppPackaging)
enablePlugins(BuildInfoPlugin)

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
addCompilerPlugin("io.tryp"       % "splain"          % "0.5.3" cross CrossVersion.patch)

cancelable in Global := true

lazy val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    scalaVersion,
    sbtVersion,
    BuildInfoKey.action("commitHash") {
      git.gitHeadCommit.value
    }
  ),
  buildInfoOptions := Seq(BuildInfoOption.BuildTime),
  buildInfoPackage := "hu.ksisu.imazsak"
)
