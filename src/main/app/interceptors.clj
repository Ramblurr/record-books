(ns app.interceptors
  (:import (org.eclipse.jetty.server HttpConfiguration))
  (:require
   [app.config :as config]
   [app.rand-human-id :as human-id]
   [app.routes.errors :as errors]
   [app.routes.pedestal-prone :as pedestal-prone]
   [datomic.client.api :as d]
   [io.pedestal.http.ring-middlewares :as middlewares]
   [io.pedestal.interceptor.error :as error-int]
   [luminus-transit.time :as time]
   [muuntaja.core :as m]
   [reitit.coercion.malli :as rcm]
   [reitit.http.coercion :as coercion]
   [reitit.http.interceptors.multipart :as multipart]
   [reitit.http.interceptors.muuntaja :as muuntaja]
   [reitit.http.interceptors.parameters :as parameters]
   [ring.middleware.keyword-params :as keyword-params]))

(def keyword-params-interceptor
  "Keywordizes request parameter keys. CTMX expects this."
  {:name ::keyword-params
   :enter (fn [ctx]
            (let [request (:request ctx)]
              (assoc ctx :request
                     (keyword-params/keyword-params-request request))))})

(defn datomic-interceptor
  "Attaches the datomic db and connection to the request map"
  [system]
  {:name ::datomic--interceptor
   :enter (fn [ctx]
            (let [conn (-> system :datomic :conn)]
              (-> ctx
                  (assoc-in  [:request :db] (d/db conn))
                  (assoc-in  [:request :datomic-conn] conn))))})

(def service-error-handler
  (error-int/error-dispatch [ctx ex]

                            [{:exception-type :java.lang.ArithmeticException :interceptor ::another-bad-one}]
                            (assoc ctx :response {:status 400 :body "Another bad one"})

                            [{:exception-type :java.lang.ArithmeticException}]
                            (assoc ctx :response {:status 400 :body "A bad one"})

                            [{:exception-type :clojure.lang.ExceptionInfo :cognitect.anomalies/category :cognitect.anomalies/incorrect}]
                            (assoc ctx :response (errors/not-found-error (:request ctx) ex))

                            [{:exception-type :clojure.lang.ExceptionInfo :app/error-type :app.error.type/validation}]
                            (assoc ctx :response (errors/validation-error (:request ctx) ex))

                            [{:exception-type :clojure.lang.ExceptionInfo :app/error-type :app.error.type/authentication-failure}]
                            (assoc ctx :response (errors/unauthorized-error (:request ctx) ex))

                            :else
                            (assoc ctx :response (errors/unknown-error (:request ctx) ex))))

(def htmx-interceptor
  "Sets :htmx? to true if the request originates from htmx"
  {:name ::htmx
   :enter (fn [ctx]
            (let [request (:request ctx)
                  headers (:headers request)]
              (if (some? (get headers "hx-request"))
                (-> ctx
                    (assoc-in [:request :htmx]
                              (->> headers
                                   (filter (fn [[key _]] (.startsWith key "hx-")))
                                   (map (fn [[key val]] [(keyword key) val]))
                                   (into {})))
                    (assoc-in [:request :htmx?] true))
                (assoc-in ctx [:request :htmx?] false))))})

(defn system-interceptor
  "Install the integrant system map into the request under the :system key"
  [system]
  {:name ::system-interceptor
   :enter (fn [ctx]
            (assoc-in ctx [:request :system] system))})

(defn dev-mode-interceptor
  "Tell the request if we are in dev mode or not"
  [system]
  {:name ::dev-mode-interceptor
   :enter (fn [ctx]
            (assoc-in ctx [:request :dev?] (config/dev-mode? (-> system :env))))})

(def human-id-interceptor
  "Add a human readable id for the request"
  {:name ::human-id-interceptor
   :enter (fn [ctx]
            (assoc-in ctx [:request :human-id] (human-id/human-id)))})

(def tap-interceptor
  {:name :tap-interceptor
   :enter (fn [req]
            (tap> (-> req :request))
            (tap> (-> req :request :params))
            (tap> (-> req :request :body-params))
            (tap> (-> req :request :form-params))
            req)})

(defn default-interceptors [system]
  (into [] (remove nil?
                   [human-id-interceptor
                    service-error-handler
                    dev-mode-interceptor
                    middlewares/cookies
                    ;; query-params & form-params
                    (parameters/parameters-interceptor)
                    ;; content-negotiation
                    (muuntaja/format-negotiate-interceptor)
                    ;; encoding response body
                    (muuntaja/format-response-interceptor)
                    ;; exception handling
                    ;; exception-interceptor
                    ;; decoding request body
                    (muuntaja/format-request-interceptor)
                    ;; coercing response bodys
                    (coercion/coerce-response-interceptor)
                    ;; coercing request parameters
                    (coercion/coerce-request-interceptor)
                    htmx-interceptor
                    ;; htmx reequires all params (query, form etc) to be keywordized
                    keyword-params-interceptor
                    ;; multipart
                    (multipart/multipart-interceptor)])))

(defn with-default-interceptors [service system]
  (update-in service [:io.pedestal.http/interceptors] conj (default-interceptors system)))

(def default-coercion
  (-> rcm/default-options

      rcm/create))

(def formats-instance
  (m/create
   (-> m/default-options
       (update-in
        [:formats "application/transit+json" :decoder-opts]
        (partial merge time/time-deserialization-handlers))
       (update-in
        [:formats "application/transit+json" :encoder-opts]
        (partial merge time/time-serialization-handlers))
       (assoc-in [:formats "application/json" :encoder-opts]
                 {:encode-key-fn name})
       (assoc-in [:formats "application/json" :decoder-opts]
                 {:decode-key-fn keyword}))))

(defn prone-exception-interceptor
  "Pretty prints exceptions in the browser"
  [service]
  (update-in service [:io.pedestal.http/interceptors] #(vec (cons (pedestal-prone/exceptions {:app-namespaces ["app"]}) %))))

(defn http-configuration
  [max-size]
  (doto (HttpConfiguration.)
    (.setRequestHeaderSize max-size)))
