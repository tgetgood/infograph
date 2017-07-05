(defproject infograph "0.1.0-SNAPSHOT"
  :description "Graphical Interactive Interactive Infographic Development"

  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [org.clojure/clojurescript "1.9.660"]
                 [org.clojure/core.async "0.3.443"
                  :exclusions [org.clojure/tools.reader]]
                 [devcards "0.2.3"]
                 [reagent "0.6.2"]
                 [re-frame "0.9.4"]]

  :plugins [[lein-cljsbuild "1.1.6" :exclusions [[org.clojure/clojure]]]]

  :min-lein-version "2.7.1"

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.4"]
                   [figwheel-sidecar "0.5.10"]
                   [com.cemerick/piggieback "0.2.1"]]
    :plugins      [[lein-figwheel "0.5.10"]]}} 

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src"]
     :figwheel     {:on-jsload "infograph.core/mount-root"}
     :compiler     {:main                 infograph.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :parallel-build       true
                    :preloads             [devtools.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}}}

    {:id           "devcards"
     :source-paths ["src"]
     :figwheel     {:devcards true}
     :compiler     {:main                 infograph.devcards 
                    :asset-path           "js/compiled/devcards_out"
                    :output-to            "resources/public/js/compiled/devcards.js"
                    :output-dir           "resources/public/js/compiled/devcards_out"
                    :source-map-timestamp true
                    :parallel-build       true
                    :preloads             [devtools.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}}}

    {:id           "min"
     :source-paths ["src"]
     :compiler     {:main            infograph.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :parallel-build  true
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}]})
