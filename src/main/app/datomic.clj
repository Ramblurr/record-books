(ns app.datomic
  (:refer-clojure :exclude [ref])
  (:require [datomic.client.api :as d]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.yetanalytics.squuid :as sq]
            [medley.core :as m]
            [clojure.tools.logging :as log]
            [integrant.repl.state :as state]))

(defn ident-has-attr?
  [db ident attr]
  (contains? (d/pull db {:eid ident :selector '[*]}) attr))

(defn uuid-for-seed [tx]
  (if-let [uuid-key (:uuid-key tx)]
    (-> tx
        (dissoc :uuid-key)
        (assoc uuid-key (sq/generate-squuid)))
    tx))

(defn transact [conn opts]
  (try
    (d/transact conn opts)
    (catch clojure.lang.ExceptionInfo e
      {:error (ex-data e)
       :exception e
       :msg (ex-message e)})))

(defn unique-error? [error]
  (= :db.error/unique-conflict (-> error :error :db/error)))

(defn db-error? [result]
  (contains? result :error))

(defn db-ok? [result]
  (let [ok? (not (db-error? result))]
    (if ok? true
        (do
          (tap> result)
          false))))

(defn pull-many
  "Warning: can result in many requests if the peer is remote"
  [db pattern eids]
  (map (partial d/pull db pattern) eids))

(defn find-all
  "Returns a list of all entities having attr"
  [db attr pattern]
  (d/q '[:find (pull ?e pattern)
         :in $ ?attr pattern
         :where
         [?e ?attr ?v]]
       db attr pattern))

(defn count-all
  "Returns the number of entities having attr"
  [db attr]
  (let [result
        (ffirst (d/q '[:find (count ?e)
                       :in $ ?attr
                       :where [?e ?attr ?v]]
                     db attr))]
    (if (nil? result)
      0
      result)))

(defn find-all-by
  "Returns the entities having attr and val"
  [db attr attr-val pattern]
  (d/q '[:find (pull ?e pattern)
         :in $ ?attr ?val pattern
         :where [?e ?attr ?val]]
       db attr attr-val pattern))

(defn find-by
  "Returns the unique entity identified by attr and val."
  [db attr attr-val pattern]
  (ffirst (find-all-by db attr attr-val pattern)))

(defn match-by
  "Returns the unique entity identified by attr and matching val-pattern"
  [db attr val-pattern pattern]
  (ffirst
   (d/q '[:find (pull ?e pattern)
          :in $ ?attr ?match pattern
          :where
          [(str "(?i)" ?match) ?matcher]
          [(re-pattern ?matcher) ?regex]
          [(re-find ?regex ?aname)]
          [?e ?attr ?aname]]
        db attr val-pattern pattern)))

(def entity-ids [])

(defn ref
  "Given a map and a key returns a tuple of [key value]. Useful for building datomic ref tuples from a pull result"
  ([m]
   (if-let [k (m/find-first #(contains? m %) entity-ids)]
     (ref m k)
     (throw (ex-info "entity map does not contain a known id key" {:entity-map m
                                                                   :possible-id-keys entity-ids}))))

  ([m k]
   [k (k m)]))

(def q d/q)

(defn load-dataset
  [env conn]
  (let [db (d/db conn)
        tx #(d/transact conn {:tx-data %})]
    (when-not (ident-has-attr? db :member/name :db/ident)
      (log/info "Loading db schema")
      (tx (-> (io/resource "schema.edn") slurp edn/read-string))
      ;; (tx (map uuid-for-seed (-> (io/resource "seeds.edn") slurp edn/read-string)))
      (tx (-> (io/resource "seeds.edn") slurp edn/read-string)))
    ;; (when-not (ident-has-attr? db :account/account-id :db.attr/preds)
    ;;   (tx validation/attr-pred))
    ;; (when-not (ident-has-attr? db :account/validate :db.entity/attrs)
    ;;   (tx validation/entity-attrs))
    )
  :seeded)

(when nil :cool)

(comment
  (do
    (require '[integrant.repl.state :as state])
    (require  '[datomic.client.api :as datomic])
    (def conn (-> state/system :app.ig/datomic-db :conn))
    (def db (datomic/db conn))) ;; rcf

  (d/transact conn {:tx-data (-> (io/resource "seeds.edn") slurp edn/read-string)})

;;
  )
