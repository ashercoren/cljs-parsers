(defproject parsers "0.1.0"
  :description "Clojrescript parsers for various file types"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2755"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/core.match "0.3.0-alpha3"]
                 [cljsjs/csv "1.1.1-0"]
                 [im.chit/purnam "0.5.1"]]

  :plugins [[lein-cljsbuild "1.0.4"]]

  :source-paths ["src" "target/classes"]

  :clean-targets ["out" "out-adv"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src"]
              :compiler {
                :main parsers.core
                :output-to "out/parsers.js"
                :output-dir "out"
                :optimizations :none
                :cache-analysis true
                :source-map true}}
             {:id "release"
              :source-paths ["src"]
              :compiler {
                :main parsers.core
                :output-to "out-adv/parsers.min.js"
                :output-dir "out-adv"
                :optimizations :advanced
                :pretty-print false}}]})
