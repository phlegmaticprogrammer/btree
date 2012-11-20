; project file for Leiningen 1.7.1
(defproject phlegmaticprogrammer/btree "0.1.0"
  :description "Purely functional btrees"
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :dev-dependencies [[lein-autodoc "0.9.0"]]
  :test-path "src"
  :aot [phlegmaticprogrammer.btree]
  :plugins [[lein-swank "1.4.4"]])