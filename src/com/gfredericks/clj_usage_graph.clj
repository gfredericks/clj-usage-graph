(ns com.gfredericks.clj-usage-graph
  (:refer-clojure :exclude [munge])
  (:require [clojure.java.io :as io]
            [clojure.set :refer [intersection]]
            [clojure.string :as s]
            [clojure.tools.analyzer :as a]))

(defn filtered-tree-seq
  "Like clojure.core/tree-seq but adds a filter function and
   is not lazy."
  [branch? children keep? root]
  ;; This seems to be the bottleneck of the whole program so
  ;; why not use an ArrayList to give it an extra boost
  (let [res (java.util.ArrayList.)
        walk (fn walk [node]
               (if (keep? node) (.add res node))
               (when (branch? node)
                 (doseq [x (children node)] (walk x))))]
    (walk root)
    (seq res)))

(defn ops-of
  [op-name ast]
  (filtered-tree-seq coll?
                     #(if (map? %) (vals %) %)
                     #(= op-name (:op %))
                     ast))

(defn usages
  [filename]
  (let [is (-> filename io/reader java.io.PushbackReader.)
        forms (->> #(read is false nil)
                   (repeatedly)
                   (take-while identity))

        defs (->> forms
                  (map a/analyze-form)
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
