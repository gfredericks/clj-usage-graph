(defproject com.gfredericks/clj-usage-graph "0.3.1-SNAPSHOT"
  :description "Haxy usage-graph generation code for Clojure projects"
  :url "https://github.com/fredericksgary/clj-usage-graph"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.analyzer.jvm "0.6.6"]
                 [org.clojure/tools.namespace "0.2.10"]]
  :deploy-repositories [["releases" :clojars]])
