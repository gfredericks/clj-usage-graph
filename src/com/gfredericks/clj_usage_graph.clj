(ns com.gfredericks.clj-usage-graph
  "Entry point."
  (:refer-clojure :exclude [munge])
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.tools.namespace.parse :as ctnp]
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

(defn var-graph
  "Given files to analyze, prints a graph file for DOT."
  [& filenames]
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

(defn namespace-graph
  [& filenames]
  (let [names&deps (for [filename filenames
                         :let [decl (-> filename
                                        io/reader
                                        java.io.PushbackReader.
                                        ctnp/read-ns-decl)
                               deps (ctnp/deps-from-ns-decl decl)]]
                     [(second decl) deps])
        node-id (memoize (fn [_] (gensym "node")))
        included? (set (map first names&deps))]
    (println "digraph G {")
    (println "ranksep=2")
    (println "concentrate=true;")
    (println "fontname=\"monospace\";")
    (println "node[fontname=\"monospace\"];")
    (doseq [[ns-name deps] names&deps]
      (printf "%s[label=\"%s\"];\n" (node-id ns-name) ns-name)
      (doseq [dep deps
              :when (included? dep)]
        (printf "%s -> %s;\n" (node-id ns-name) (node-id dep))))
    (println "}")))
