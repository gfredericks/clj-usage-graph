(ns com.gfredericks.clj-usage-graph
  "Entry point."
  (:refer-clojure :exclude [munge])
  (:require [clojure.string :as s]
            [com.gfredericks.clj-usage-graph.usages :refer [all-internal-usages]]))

(defn munge
  "Transform characters that graphviz won't want in node identifiers."
  [s]
  (s/replace s #"[^A-Za-z0-9_]"
             (fn [s']
               (-> s'
                   first
                   int
                   (->> (format "_%d_"))))))

(defn generate
  "Given files to analyze, prints a graph file for DOT."
  [& filenames]

  ;; I don't understand tools.analyzer well enough to know if loading
  ;; the files here is necessary, but the current code doesn't work
  ;; otherwise
  (doseq [f filenames] (load-file f))

  (let [m (all-internal-usages filenames)
        namespace #(str (.ns %))
        name #(str (.sym %))
        id (fn [v]
             (munge (str (namespace v) \. (name v))))
        namespaces (->> (keys m)
                        (map namespace)
                        (distinct))
        in-ns #(->> (keys m)
                    (filter (comp #{%} namespace)))]
    (println "digraph G {")
    (println "rankdir=\"LR\";")
    (println "fontname=\"monospace\";")
    (println "node[fontname=\"monospace\"];")
    (doseq [ns namespaces]
      (printf "subgraph cluster_%s {\nshape=\"rect\";\nlabel=\"%s\";\n" (munge ns) ns)
      (doseq [v (in-ns ns)]
        (printf "%s[label=\"%s\"];\n" (id v) (name v)))
      (println "}"))
    (doseq [[v vs] m
            v' vs]
      (printf "%s -> %s;\n" (id v) (id v')))
    (println "}")))
