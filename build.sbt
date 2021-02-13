val Http4sVersion = "0.21.16"
val CirceVersion = "0.13.0"
val MunitVersion = "0.7.20"
val LogbackVersion = "1.2.3"
val MunitCatsEffectVersion = "0.13.0"
val DoobieVersion = "0.9.0"

lazy val root = (project in file("."))
  .settings(
    organization := "com.zhpooer.ecommerce.order",
    name := "order-service",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.4",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"      %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "org.typelevel"   %% "cats-mtl"            % "1.1.1",
      "org.typelevel"   %% "cats-tagless-macros" % "0.12",
      "io.circe"        %% "circe-parser"        % CirceVersion,
      "io.circe"        %% "circe-generic"       % CirceVersion,
      "org.tpolecat"    %% "doobie-core"         % DoobieVersion,
      "org.tpolecat"    %% "doobie-hikari"       % DoobieVersion,
      "org.tpolecat"    %% "doobie-quill"        % DoobieVersion,
      "mysql"           % "mysql-connector-java" % "8.0.23",
      "org.scalameta"   %% "munit"               % MunitVersion           % Test,
      "org.typelevel"   %% "munit-cats-effect-2" % MunitCatsEffectVersion % Test,
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion,
      "org.scalameta"   %% "svm-subs"            % "20.2.0"
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.11.3" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
    testFrameworks += new TestFramework("munit.Framework")
  )
scalacOptions += "-Ymacro-annotations"