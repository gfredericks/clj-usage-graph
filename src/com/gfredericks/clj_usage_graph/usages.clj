(ns com.gfredericks.clj-usage-graph.usages
  "Functions for determining usages."
  (:require [clojure.set :refer [intersection]]
            [clojure.tools.analyzer.jvm :as a]))

(def nodes
  (partial tree-seq
           #(and (map? %) (contains? % :children))
           ;; some children are collections of nodes
           #(for [child-name (:children %)
                  :let [x (get % child-name)]
                  y (if (map? x) [x] x)]
              y)))

(defn ^:private var->sym
  "Is it really this hard?"
  [^clojure.lang.Var v]
  (symbol (name (.getName (.ns v)))
          (name (.sym v))))

(defn usages
  "Returns a set of fully-qualified symbols."
  [ns form-str]
  (-> form-str
      (read-string)
      (a/analyze (assoc (a/empty-env) :ns ns))
      (nodes)
      (->> (filter #(= :var (:op %)))
           (map (comp var->sym :var))
           (set))))
