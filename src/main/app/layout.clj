(ns app.layout
  (:require
   [app.render :as render]))

(defn app-shell [req body]
  (render/html5-response
   [:div {:class "min-h-full"}
    body]))
