name := "kokel-utils-core"
libraryDependencies ++= Seq(
	"org.scala-lang.modules" %% "scala-xml" % "1.0.5",
	"com.github.tototoshi" %% "scala-csv" % "1.3.3"
) map (_.exclude("xml-apis", "xml-apis"))
assemblyJarName in assembly := "kokel-utils-core.jar"
