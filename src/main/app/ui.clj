(ns app.ui
  (:refer-clojure :exclude [time])
  (:import
   (java.text DecimalFormat NumberFormat)
   (java.util Locale))
  (:require
   [app.render :as render]
   [app.util :as util]
   [clojure.string :as str]
   [ctmx.render :as ctmx.render]
   [hiccup2.core :refer [html]]
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

(def button-priority-classes {:secondary
                              "border-transparent bg-indigo-100 px-4 py-2  text-indigo-700 hover:bg-indigo-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2"
                              :white
                              "border-gray-300 bg-white px-4 py-2 text-sm  text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2 focus:ring-offset-gray-100"
                              :white-destructive
                              "border-red-300 bg-white px-4 py-2 text-sm  text-red-600 shadow-sm hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2 focus:ring-offset-gray-100"
                              :red
                              "border-transparent bg-red-600 text-white shadow-sm hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 focus:ring-offset-gray-100"
                              :primary
                              "border-transparent bg-indigo-600 text-white shadow-sm hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 focus:ring-offset-gray-100"
                              :white-rounded "rounded-full border border-gray-300 bg-white text-gray-700 shadow-sm hover:bg-gray-50"})

(def button-sizes-classes {:2xsmall "px-1.5 py-0.5 text-xs"
                           :xsmall "px-2.5 py-1.5 text-xs"
                           :small "px-3 py-2 text-sm leading-4"
                           :normal "px-4 py-2 text-sm"
                           :large "px-4 py-2 text-base"
                           :xlarge "px-6 py-3 text-base"})

(def button-icon-sizes-classes {:xsmall "h-2 w-2"
                                :small "h-3 w-3"
                                :normal "h-5 w-5"
                                :large "h-5 w-5"
                                :xlarge "h-5 w-5"})

(defn button [& {:keys [tag label disabled? class attr icon priority centered? size hx-target hx-get hx-put hx-post hx-delete hx-vals form id]
                 :or   {class ""
                        priority :white
                        size  :normal
                        disabled? false
                        tag :button}}]

  [tag (merge
        (util/remove-nils {:hx-target hx-target :hx-get hx-get :hx-post hx-post :hx-put hx-put :hx-delete hx-delete :hx-vals hx-vals :form form})
        {:id id :class
         (cs
          "inline-flex items-center rounded-md border font-medium"
          ;; "inline-flex items-center border font-medium"
          ;; "inline-flex items-center rounded-md border"
          (size button-sizes-classes)
          (priority button-priority-classes)
          (when centered? "items-center justify-center")
          class)
         :disabled disabled?}
        attr)
   (when icon (icon  {:class (cs (size button-icon-sizes-classes)  (when label "-ml-1 mr-2"))}))
   label])

(defn link-button [& opts]
  (apply button (conj opts :a :tag)))

(def hx-trigger-types
  {:hx-trigger "HX-Trigger"
   :hx-trigger-after-settle "HX-Trigger-After-Settle"
   :hx-trigger-after-swap "HX-Trigger-After-Swap"})
