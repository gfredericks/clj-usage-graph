(defproject com.gfredericks/clj-usage-graph "0.2.0-SNAPSHOT"
  :description "Haxy usage-graph generation code for Clojure projects"
  :url "https://github.com/fredericksgary/clj-usage-graph"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.datomic/datomic-free "0.9.4699"
                  :exclusions [[org.slf4j/log4j-over-slf4j]
                               ;; datomic lists an earlier version of
                               ;; this lib than clj-http does, and clj-http
                               ;; will fail to load on the earlier version
                               org.apache.httpcomponents/httpclient]]
                 [org.clojure/tools.analyzer.jvm "0.1.0-beta8"]]

  :jvm-opts ["-Xmx1g"]

  :profiles {:dev {:source-paths ["dev"]}})
