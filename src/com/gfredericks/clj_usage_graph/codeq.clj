(ns com.gfredericks.clj-usage-graph.codeq
  (:require [datomic.api :as d]))

(defn commit->blobs
  "Given a commit entity, returns a collection of blob entities."
  [commit]
  (->> (:node/object (:commit/tree commit))
       (tree-seq #(= :tree (:git/type %))
                 #(map :node/object (:tree/nodes %)))
       (filter #(= :blob (:git/type %)))))

(defn commit->codeqs
  "Returns a map from ns symbols to codeq entities."
  [db commit-sha]
  (let [ent-id (ffirst (d/q '[:find ?x
                              :in $ ?commit
                              :where [?x :git/sha ?commit]]
                            db commit-sha))
        ent (d/entity db ent-id)]
    (into {}
          (for [blob (commit->blobs ent)
                :let [codeqs (:codeq/_file blob)
                      [ns] (filter :clj/ns codeqs)]
                :when ns
                :let [ns-sym (-> ns :clj/ns :code/name read-string)]]
            [ns-sym (filter :clj/def codeqs)]))))
