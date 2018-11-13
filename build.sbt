import sbt.Keys._
import sbtrelease.ReleaseStateTransformations._

name := "freesourcing"
val gitlabProjectName = "freesourcing"

val scalaV = "2.12.7"
scalaVersion in ThisBuild := scalaV
organization in ThisBuild := "stephenzoio"

// ---- shell prompt ----
shellPrompt in ThisBuild := { state =>
  scala.Console.GREEN + Project.extract(state).currentRef.project + "> " + scala.Console.RESET
}

// ---- limit test runners ----

parallelExecution in ThisBuild := false
concurrentRestrictions in ThisBuild += Tags.limit(Tags.Test, 1)
cancelable in Global := true

// ---- resolvers to search through ----

resolvers in Global ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)


scalacOptions in ThisBuild := Seq(
  // following two lines must be "together"
  "-encoding",
  "UTF-8",
  "-Xlint",
  "-Xlint:missing-interpolator",
  //"-Xlog-implicits", // enable when trying to debug implicits
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Yno-adapted-args",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Ywarn-value-discard",
  // "-Ywarn-unused-import", // seems to be broken for some imports [2.11]
  //"-Ypartial-unification", // enable once we go scala 2.12, fixes si-2712
  // "-Ywarn-unused", // broken in frontned [2.11]
  "-Ywarn-numeric-widen"
)

// ---- support of better repl via test:console -> ammonite repl ----

val ammoniteConsole = Seq(
//  libraryDependencies += "com.lihaoyi" % "ammonite" % "1.1.2" cross CrossVersion.full,
//  initialCommands in (Test, console) := """ammonite.Main().run()"""
)

// ---- kind projector to have cleaner type lambdas ----

val kindProjectorPlugin = Seq(
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7")
)

// ---- support unified way to declare new typeclasses ----

val simulacrumPlugin = Seq(
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  libraryDependencies += "com.github.mpilquist" %% "simulacrum" % "0.13.0"
)

// ---- enable build info ----

def withBuildInfo(p: Project): Project =
  p.enablePlugins(BuildInfoPlugin)
    .settings(
      Seq(
        // is set to package name so we can't use -
        buildInfoPackage := ("stephenzoio.freesourcing." + p.id.replace('-', '.')),
        buildInfoOptions += BuildInfoOption.ToMap,
        buildInfoKeys := Seq[BuildInfoKey](
          name,
          version,
          scalaVersion,
          sbtVersion,
          BuildInfoKey.action("buildTime") { System.currentTimeMillis },
          git.baseVersion,
          git.gitHeadCommit,
          git.formattedShaVersion
        )
      ))

// ---- CI related settings ----
val ignoreApplicationConfig = Seq(
  mappings in (Compile, packageBin) ~= {
    _.filter(!_._1.getName.equals("application.conf"))
  }
)

val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

val packageSettings = Seq(
  // remove one level of nesting from artifact (so when it's unzipped we don't have unnecessary folders)
  topLevelDirectory := None,
  // remove version postfix from artifact (so it's not $ARTIFACT-$VERSION)
  packageName in Universal := packageName.value
  // include addJava command in bash script to run server
)

// ----- Docker related settings -----

val dockerFileJdk8Settings = Seq(
  dockerfile in docker := {
    val appDir: File = stage.value
    val targetDir    = "/app"

    new Dockerfile {
      from("openjdk:8-jre")
      entryPoint(s"$targetDir/bin/${executableScriptName.value}")
      copy(appDir, targetDir)
    }
  }
)

// ---- dependencies ----
val catsV          = "1.1.0"
val catsEffectV    = "0.10.1"
val circeV         = "0.9.3"
val doobieV        = "0.5.3"
val Http4sVersion  = "0.18.16"
val monocleVersion = "1.4.0"
val fs2Version     = "0.10.5"
val kafkaVersion   = "1.1.0"

lazy val sharedDeps = Seq(
  libraryDependencies ++= Seq(
    "org.typelevel"  %% "cats-core"       % catsV,
    "org.typelevel"  %% "cats-free"       % catsV,
    "org.typelevel"  %% "cats-effect"     % catsEffectV,
    "co.fs2"         %% "fs2-core"        % fs2Version,
    "io.circe"       %% "circe-core"      % circeV,
    "io.circe"       %% "circe-generic"   % circeV,
    "io.circe"       %% "circe-parser"    % circeV,
    "io.circe"       %% "circe-java8"     % circeV,
    "org.tpolecat"   %% "doobie-core"     % doobieV,
    "org.tpolecat"   %% "doobie-postgres" % doobieV,
    "org.tpolecat"   %% "doobie-hikari"   % doobieV,
    "org.flywaydb"   % "flyway-core"      % "4.0.3",
    "com.typesafe"   % "config"           % "1.3.1",
    "org.scalatest"  %% "scalatest"       % "3.0.1",
    "org.scalacheck" %% "scalacheck"      % "1.13.4"
  ))

lazy val coreDeps = Seq(
  libraryDependencies ++= Seq(
    // Event log dependencies
    "org.tpolecat"     %% "doobie-core"      % doobieV,
    "org.tpolecat"     %% "doobie-postgres"  % doobieV,
    "org.tpolecat"     %% "doobie-hikari"    % doobieV,
    "ch.qos.logback"   % "logback-classic"   % "1.2.1",
    "org.tpolecat"     %% "doobie-scalatest" % doobieV % Test,
    "com.ironcorelabs" %% "cats-scalatest"   % "2.3.1" % Test,
    "org.tpolecat"     %% "doobie-scalatest" % doobieV % Test,
    "org.tpolecat"     %% "doobie-specs2"    % doobieV % Test
  ))

lazy val backendDeps = Seq(
  libraryDependencies ++= Seq(
    // Event log dependencies
    "co.fs2"           %% "fs2-io"              % fs2Version,
    "org.http4s"       %% "http4s-blaze-server" % Http4sVersion,
    "org.http4s"       %% "http4s-circe"        % Http4sVersion,
    "org.http4s"       %% "http4s-dsl"          % Http4sVersion,
    "ch.qos.logback"   % "logback-classic"      % "1.2.1",
    "com.ironcorelabs" %% "cats-scalatest"      % "2.3.1" % Test
  ))

// ---- wart remover ----
import wartremover.Wart._
lazy val wartRemover = Seq(
  wartremoverWarnings in (Compile, compile) := Seq(
    // Any, // integration with libs is easier
    // AsInstanceOf, // bugged in "1.2.0"
    // DefaultArguments, // sometimes useful
    EitherProjectionPartial,
    Enumeration,
    //Equals,
    //ExplicitImplicitTypes, // problems with scala.js binding
    FinalCaseClass,
    ImplicitConversion,
    IsInstanceOf,
    // JavaConversions, // broken in "1.1.1"
    LeakingSealed,
    ListOps,
    // MutableDataStructures, // broken in "1.1.1"
    NoNeedForMonad,
    //NonUnitStatements, // not with Rx
    // Nothing, // sometimes useful
    Null,
    Option2Iterable,
    OptionPartial,
    // Overloading, // sometimes useful
    Product,
    Return,
    Serializable,
    StringPlusAny,
    // ToString,
    TryPartial,
    Var,
    While
  )
)

// ---- common settings for all projects ----

val commonSettings = ammoniteConsole ++ kindProjectorPlugin ++ simulacrumPlugin ++ wartRemover

// ---- modules ----
noPublishSettings

// shared module is compiled for both Jvm and Js platforms
lazy val shared = Project(id = "shared", base = file("modules/shared"))
  .settings(commonSettings, noPublishSettings)
  .settings(exportJars := true)
  .settings(sharedDeps)

lazy val core =
  Project(id = "core", base = file("modules/core"))
    .settings(commonSettings, noPublishSettings)
    .settings(exportJars := true)
    .settings(coreDeps)
    .dependsOn(shared, model)

lazy val model =
  Project(id = "model", base = file("modules/model"))
    .settings(commonSettings, noPublishSettings)
    .settings(exportJars := true)
    .dependsOn(shared)

lazy val backend =
  Project(id = "backend", base = file("modules/backend"))
    .enablePlugins(BuildInfoPlugin, JavaAppPackaging, sbtdocker.DockerPlugin)
    .settings(
      commonSettings,
      noPublishSettings,
      packageSettings,
      ignoreApplicationConfig,
      dockerFileJdk8Settings,
      backendDeps,
      // configure flyway:
      flywayDriver := "org.postgresql.Driver",
      flywayLocations := Seq("db.migration", "db.import"),
      flywayPlaceholders := Map(
        "initialDataLocation" -> ((baseDirectory in ThisBuild).value / "src/main/resources/db/import").getPath
      ),
      mappings in Universal += {
        ((resourceDirectory in Compile).value) / "application.conf" -> "conf/application.conf"
      }
    )
    .dependsOn(shared, core, model)
    .configure(withBuildInfo)

releaseTagComment := s"release ${(version in ThisBuild).value}"
releaseCommitMessage := s"set version to ${(version in ThisBuild).value}"

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion,
  pushChanges
)
