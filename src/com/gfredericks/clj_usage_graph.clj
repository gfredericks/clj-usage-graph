(ns com.gfredericks.clj-usage-graph
  "Two entry points: annotate-usages, and usage-graph."
  (:require [clojure.set :refer [intersection]]
            [com.gfredericks.clj-usage-graph.codeq :as codeq]
            [com.gfredericks.clj-usage-graph.graphviz :as graphviz]
            [com.gfredericks.clj-usage-graph.usages :as usages]
            [datomic.api :as d]))

(def schema
  [{:db/ident              :clj/use
    :db/id                 #db/id[:db.part/db]
    :db/valueType          :db.type/ref
    :db/cardinality        :db.cardinality/many
    :db.install/_attribute :db.part/db}])

(defn annotate-usages
  "Requires this code to be run as a library of the project on the
  relevant commit."
  [db-uri commit-sha]
  (let [c (d/connect db-uri)
        db (d/db c)

        datoms
        (for [[ns codeqs] (codeq/commit->codeqs db commit-sha)
              :let [_ (require ns)] ; gotta make sure the code around
              codeq codeqs
              used-sym (usages/usages ns (-> codeq
                                             :codeq/code
                                             :code/text))
              :let [code-name-id (d/tempid :db.part/user)]]
          [[:db/add (:db/id codeq) :clj/use code-name-id]
           [:db/add code-name-id :code/name (str used-sym)]])]
    (d/transact c schema)
    (d/transact c (apply concat datoms)))
  (d/shutdown true)
  (System/exit 0))

(defn ^:private internalize
  "Given a map from symbols to sets of symbols, filters the sets so
  that they only contain elements that are keys in the top level
  map."
  [m]
  (let [pred (-> m keys set)]
    (into {}
          (for [[k v] m] [k (intersection pred v)]))))

(defn ^:private usage-graph-data
  "Returns a map from symbols to sets of symbols."
  [db commit-sha]
  (->> (for [[ns codeqs] (codeq/commit->codeqs db commit-sha)
             codeq codeqs
             :let [def'd-sym (-> codeq :clj/def :code/name read-string)
                   used-syms (-> codeq
                                 :clj/use
                                 (->> (map (comp read-string :code/name)))
                                 (set))]]
         [def'd-sym used-syms])
       (into {})
       (internalize)))

(defn usage-graph
  "Prints a DOT representation of the usage graph for the given commit.

  Must run annotate-usages first."
  [db-uri commit-sha]
  (-> db-uri
      d/connect
      d/db
      (usage-graph-data commit-sha)
      (graphviz/generate))
  (d/shutdown true)
  (System/exit 0))
