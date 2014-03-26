(ns com.gfredericks.clj-usage-graph
  "Entry point."
  (:refer-clojure :exclude [munge])
  (:require [com.gfredericks.clj-usage-graph.graphviz :as graphviz]
            [com.gfredericks.clj-usage-graph.usages :refer [all-internal-usages]]))

(defn generate
  "Given files to analyze, prints a graph file for DOT."
  [& filenames]

  ;; I don't understand tools.analyzer well enough to know if loading
  ;; the files here is necessary, but the current code doesn't work
  ;; otherwise
  (doseq [f filenames] (load-file f))
  (graphviz/generate (all-internal-usages filenames)))
