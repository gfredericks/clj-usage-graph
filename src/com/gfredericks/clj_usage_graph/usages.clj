(ns com.gfredericks.clj-usage-graph.usages
  "Functions for determining usages."
  (:require [clojure.java.io :as io]
            [clojure.set :refer [intersection]]
            [clojure.tools.analyzer.jvm :as a]
            [clojure.tools.analyzer.ast :refer [nodes]]))

(defn forms
  "Reads all the forms in a file."
  [filename]
  (with-open [r (io/reader filename)
              pbr (java.io.PushbackReader. r)]
    (->> (repeatedly #(read pbr false nil))
         (take-while identity)
         (doall))))

(defn usages
  "Returns a map from vars to sets of vars"
  [filename]
  (let [[ns-form & ns-forms] (forms filename)
        _ (and (assert (= 'ns (first ns-form)))
               (eval ns-form)) ;; ensure the ns is created
        the-ns-sym (second ns-form)
        env (assoc (a/empty-env)
              :ns the-ns-sym)]
    (into {}
          (for [form ns-forms
                :let [tree (a/analyze+eval form env)]
                {:keys [op var] :as def} (nodes tree)
                :when (= :def op)]
            [var (-> (nodes def)
                     (->> (keep :var))
                     (set)
                     (disj var))]))))

(defn remove-external-vars
  "Given a map from vars to sets of vars, removes from the sets any
   vars not in the top-level map."
  [usage-map]
  (let [top-level-set (-> usage-map keys set)]
    (into {}
          (for [[v v-set] usage-map]
            [v (intersection v-set top-level-set)]))))

(defn all-usages
  [filenames]
  (->> filenames
       (map usages)
       (apply merge)))

(defn all-internal-usages
  "Returns a map from vars defined in the given files to sets of vars
  that they reference, where the sets only contain vars also defined
  in those files (i.e., omitting any core clojure vars or libraries,
  etc.)"
  [filenames]
  (remove-external-vars (all-usages filenames)))
