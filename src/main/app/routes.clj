(ns app.routes
  (:require
   [ctmx.core :as ctmx]
   [app.view :as view]
   [app.layout :as layout]
   [app.interceptors :as interceptors]
   [app.routes.pedestal-reitit]
   [reitit.ring :as ring]))

(defn index-page []
  (ctmx/make-routes
   "/"
   (fn [req]
     (layout/app-shell req (view/index-page req)))))

(defn routes [system]
  ["" {:coercion     interceptors/default-coercion
       :muuntaja     interceptors/formats-instance
       :interceptors (conj  (interceptors/default-interceptors system)
                            (interceptors/system-interceptor system)
                            (interceptors/datomic-interceptor system))}

   (index-page)

                                        ;["/index.html" (index-route frontend-index-adapter index-csp)]
   ])

(defn default-handler [{:keys [] :as system}]
  (ring/routes
   (ring/create-resource-handler {:path "/"})
   (ring/create-default-handler)))
