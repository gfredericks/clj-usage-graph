(ns codeq
  (:require [datomic.api :refer [q db] :as d]
            [clojure.string :as str]))

(def connection (d/connect "datomic:free://localhost:4334/git"))

(defn codeqs
  [db]
  (map (fn [[x]] (->> x (d/entity db)))
       (q '[:find ?codeq
            :where
            [?codeq :codeq/file _]]
          db)))

(defn position-info
  [codeq]
  (-> codeq
      :codeq/loc
      (str/split #"\s+")))

(def line-number (comp #(Integer/parseInt %) first position-info))
(def col-number (comp #(Integer/parseInt %) second position-info))
(def endline (comp #(Integer/parseInt %) #(nth % 2) position-info))
(def endcol (comp #(Integer/parseInt %) #(nth % 3) position-info))

(def schema
  [{:db/ident ::line-number
    :db/id #db/id[:db.part/db]
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}
   {:db/ident ::col-number
    :db/id #db/id[:db.part/db]
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}
   {:db/ident ::endline
    :db/id #db/id[:db.part/db]
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}
   {:db/ident ::endcol
    :db/id #db/id[:db.part/db]
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}])

(defn add-schema!
  []
  (d/transact connection schema)

 (defn position-info-transaction
   "Given a database conncetion and a seq of codeqs, return a transaction
   data structure which will add ::line-number ::col-number ::endline
   and ::endcol attributes to each codeq."
   [codeqs]
   (->> codeqs
        (mapcat #(vector [:db/add (:db/id %)
                          ::line-number (line-number %)]
                         [:db/add (:db/id %)
                          ::col-number (col-number %)]
                         [:db/add (:db/id %)
                          ::endline (endline %)]
                         [:db/add (:db/id %)
                          ::endcol (endcol %)]))))

 (defn transact-position-info!
   []
   (d/transact connection (position-info-transaction (codeqs (db connection))))))
