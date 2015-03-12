(ns com.gfredericks.clj-usage-graph.usages
  "Functions for determining usages."
  (:require [clojure.java.io :as io]
            [clojure.set :refer [intersection]]
            [clojure.tools.analyzer.jvm :as a]
            [clojure.tools.analyzer.ast :refer [nodes]]))

(defn usages
  "Returns a map from vars to sets of vars"
  [filename]
  (binding [*ns* *ns*]
    (with-open [r (io/reader filename)
                pbr (java.io.PushbackReader. r)]
      (let [ns-form (read pbr false ::empty)]
        (if (= ::empty ns-form)
          {}
          (if (not (and (seq? ns-form)
                        (= 'ns (first ns-form))
                        (symbol? (second ns-form))))
            (throw (ex-info "Bad ns-form!" {:form ns-form}))
            (let [the-ns-sym (second ns-form)
                  env (assoc (a/empty-env)
                        :ns the-ns-sym)]
              (eval ns-form)
              (loop [out {}]
                (let [next-form (read pbr false ::empty)]
                  (if (= ::empty next-form)
                    out
                    (let [tree (a/analyze+eval next-form env)
                          usages
                          (->> (nodes tree)
                               (filter #(= :def (:op %)))
                               (map (fn [{:keys [op var] :as def}]
                                      [var (-> (nodes def)
                                               (->> (keep :var))
                                               (set)
                                               (disj var))])))]
                      (recur (into out usages)))))))))))))

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
