(ns com.gfredericks.clj-usage-graph
  (:refer-clojure :exclude [munge])
  (:require [clojure.java.io :as io]
            [clojure.set :refer [intersection]]
            [clojure.string :as s]
            [clojure.tools.analyzer :refer [analyze-form]]))

(defn ops-of
  [op-name ast]
  (->> ast
       (tree-seq coll? #(if (map? %) (vals %) %))
       (filter (comp #{op-name} :op))))

(defn usages
  [filename]
  (let [is (-> filename io/reader java.io.PushbackReader.)
        forms (->> #(read is false nil)
                   (repeatedly)
                   (take-while identity))

        defs (->> forms
                  (map clojure.tools.analyzer/analyze-form)
                  (ops-of :def))]
    ;; TODO: this should silently fail on
    ;; (let [x (foo)] (def bar x))
    ;; but this should be easily fixable
    (into {}
          (for [def defs
                :let [refs (->> def
                                (ops-of :var)
                                (map :var)
                                (set))]]
            [(:var def) refs]))))

(defn all-usages
  [filenames]
  (->> filenames
       (map usages)
       (apply merge)))

(defn remove-external-vars
  "Given a map from vars to sets of vars, removes from the sets any
   vars not in the top-level map."
  [usage-map]
  (let [top-level-set (-> usage-map keys set)]
    (into {}
          (for [[v v-set] usage-map]
            [v (intersection v-set top-level-set)]))))

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
  (let [m (->> filenames
               (all-usages)
               (remove-external-vars))
        namespace (comp str :ns meta)
        name (comp str :name meta)
        id (fn [v]
             (munge (str (namespace v) \. (name v))))
        namespaces (->> m
                        keys
                        (map namespace)
                        (distinct))
        in-ns #(->> m
                    keys
                    (filter (comp #{%} namespace)))]
    (println "digraph G {")
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
