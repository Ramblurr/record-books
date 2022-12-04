(ns dev
  (:require
   [app.ig]
   [app.routes.pedestal-reitit]
   [integrant.repl.state :as state]
   [malli.dev :as md]
   [datomic.client.api :as d]
   [ol.app.dev.dev-extras :as dev-extra]
   [clojure.tools.namespace.repl :as repl]
   [ol.system :as system]))

(set! *print-namespace-maps* false)

;(mr/set-default-registry! schemas/registry)
(def datomic (-> state/system :app.ig/datomic-db))
(def conn (:conn datomic))
(def app (-> state/system :app.ig.router/routes))

(defn reset []
  (dev-extra/reset))

(comment

  ;; Run go to start the system
  (dev-extra/go)
  ;; Run halt to shutdown the system
  (dev-extra/halt)
  ;; Run reset to reload all code and restart the system
  ;; if the browser is connected it will refresh the page
  (refresh)

  ;; If you have chromium  and chromium driver installed
  ;; you can get code reloading by using this version of go
  ;; see readme for more info
  (go-with-browser)

;;;; Setup Integrant Repl State
;;; Run this before running either of the seeds below
  (do
    (require '[integrant.repl.state :as state])
    (def env (:app.ig/env state/system))
    (def conn (-> state/system :app.ig/datomic-db :conn))) ;; rcf

;;;   SEEDS

  (do
    (let [tx-data []]
      (d/transact conn {:tx-data tx-data}))
    :seed-done) ;; END SEEDS

;;;; Scratch pad
  ;;  everything below is notes/scratch

  (require '[clojure.tools.namespace.repl :refer [refresh]])
  (refresh)

  (require '[datomic.dev-local :as dl])
  (dl/release-db {:system "dev" :db-name "probematic"})
  (md/start! schemas/malli-opts)
  (md/stop!)

  (set-prep! {:profile :dev})
  (keys state/system)
  (-> state/system :app.ig/pedestal)
  (-> state/system :app.ig/env)
  (-> state/system :app.ig/profile)

  (system/config {:profile :dev})

  (system/system-config {:profile :dev})

  (d/transact conn {:tx-data [{:db/ident :song/arrangement-notes
                               :db/doc "Notes for the arrangement"
                               :db/valueType :db.type/string
                               :db/cardinality :db.cardinality/one}]})

;;
  )
