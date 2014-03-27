(ns com.gfredericks.clj-usage-graph.graphviz
  "Generating graphviz output from a usage graph."
  (:refer-clojure :exclude [munge])
  (:require [clojure.string :as s]))

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
  "Given a map from fully-qualified symbols to sets of fully qualified
  symbols, prints the graphviz output."
  [m]
  (let [id (fn [v]
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
