; i use this to uberjar up a no op plugin, which is then used as a
; project dependency for all my other plugins,
; so that scala is on the classpath for all my plugins.

(defproject scala-library-plugin "2.10.0-RC5"
  ; puts plugin.xml, and all jcdc.pluginfactory classes in the jar.
  :resource-paths ["target/scala-2.10/classes/"]
  ; put the jar file in the target dir.
  :target-path    "target/"
  :dependencies   [ [org.scala-lang/scala-library "2.10.0-RC5"] ]
)