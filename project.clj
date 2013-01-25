(defproject mogul "1.0.0-SNAPSHOT"
  :description "A multiplayer turn-based economic game"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [noir "1.2.2"]
                 [hiccup "0.3.8"]
                 [clj-stacktrace "0.2.4"]]
  :plugins [[lein-cljsbuild "0.2.1"]]
  :source-path "src/clj"
 ; Enable the lein hooks for: clean, compile, test, and jar.
  :hooks [leiningen.cljsbuild]
  :run-aliases {:server mogul.server}
  :cljsbuild {:repl-listen-port 9000
              :crossovers [mogul.math mogul.star-map mogul.utils mogul.game-gen]
              :crossover-path "src/cljs-x"
              :crossover-jar false
              :builds {:dev {:source-path "src/cljs"
                             :jar true
                             :compiler {:output-to "resources/public/js/main-debug.js"
                                        :optimizations :whitespace
                                        :pretty-print true}}
                       :prod {:source-path "src/cljs"
                              :compiler {:output-to "resources/public/js/main.js"
                                         :optimizations :simple
                                         :pretty-print false}}}})
