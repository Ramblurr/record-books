(ns app.ui
  (:refer-clojure :exclude [time])
  (:import
   (java.text DecimalFormat NumberFormat)
   (java.util Locale))
  (:require
   [hiccup2.core :refer [html]]
   [app.render :as render]
   [clojure.string :as str]
   [ctmx.render :as ctmx.render]
   [jsonista.core :as j]
   [medley.core :as m]
   [tick.core :as t]))

(defn cs [& names]
  (clojure.string/join " " (filter identity names)))

(defn unknown-error-body [req]
  (let [human-id (:human-id req)]
    [:body {:class "h-full"}
     [:div {:class "min-h-full bg-white px-4 py-16 sm:px-6 sm:py-24 md:grid md:place-items-center lg:px-8"}
      [:div {:class "mx-auto max-w-max"}
       [:main {:class "sm:flex"}

        [:div {:class "sm:ml-6"}
         [:div {:class "sm:border-l sm:border-gray-200 sm:pl-6"}
          [:h1 {:class "text-4xl font-bold tracking-tight text-gray-900 sm:text-5xl"}
           "Unknown Error"]
          [:p {:class "mt-1 text-base text-gray-500"}
           "Sorry something went wrong"]

          [:p {:class "mt-1 text-base text-gray-500"}
           "Error Code: "
           [:span {:class "mt-1 text-base text-red-500 font-mono bg-red-100"}
            human-id]]]]]]
      [:div {:class "flex items-center justify-center mt-10"}
       [:img {:src "/img/tuba-robot-boat-1000.jpg" :class "rounded-md w-full sm:w-1/2"}]]
      [:div {:class "mx-auto max-w-max"}
       [:main {:class "sm:flex"}
        [:div {:class "sm:ml-6"}
         [:div {:class "mt-10 flex space-x-3 sm:border-l sm:border-transparent sm:pl-6"}
          [:a {:href "/", :class "inline-flex items-center rounded-md border border-transparent bg-indigo-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2"}
           "Go Home"]
          [:a {:href "#", :class "inline-flex items-center rounded-md border border-transparent bg-indigo-100 px-4 py-2 text-sm font-medium text-indigo-700 hover:bg-indigo-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2"}
           "Notify Casey"]]]]]]
     (render/body-end)]))

(defn error-page-response-fragment [cause req status]
  (render/html-status-response (or status 404)
                               {"HX-Retarget" "body"}
                               (str (html (unknown-error-body req)))))

(defn error-page-response [cause req status]
  (render/html-status-response (or status 404)
                               (render/html5-safe
                                [:head
                                 [:meta {:charset "utf-8"}]
                                 [:meta {:name "viewport"
                                         :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
                                 [:link {:rel "stylesheet" :href "/css/compiled/main.css"}]]
                                (ctmx.render/walk-attrs
                                 (unknown-error-body req)))))

(def hx-trigger-types
  {:hx-trigger "HX-Trigger"
   :hx-trigger-after-settle "HX-Trigger-After-Settle"
   :hx-trigger-after-swap "HX-Trigger-After-Swap"})
