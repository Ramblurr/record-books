(ns app.view
  (:require [app.ui :as ui]
            [app.icons :as icons]))

(defn index-page [req]
  [:div {:class ""}
   [:div {:class "sticky top-0 shadow-xl z-100 p-5 bg-blue-900 w-full"}
    [:div {:class "flex flex-col items-center w-full"}
     (ui/link-button :class "hidden" :id "download" :label "Download" :icon icons/download)
     [:div {:class "recording"}
      [:div {:class "record-icon-container flex space-x-4"}
       (ui/button :priority :red :class "hidden shadow shadow-black stop-recording" :label [:span {:class "recording-timer"} "00:00"] :icon icons/circle-stop)
       (ui/button :class "hidden shadow shadow-black pause-recording" :label "Pause" :icon icons/circle-pause)
       (ui/button :class "hidden shadow shadow-black resume-recording" :label "Resume" :icon icons/microphone)
       (ui/button :priority :red :class "shadow shadow-black start-recording"  :label
                  [:span {:class "record-label"} "Record"] :icon icons/microphone)]]
     [:div {:class "playback hidden"}
      [:audio {:class "player" :preload "metadata"}]
      [:input {:type :file :accept "audio/*" :capture true :id "recorder" :class "hidden"}]
      [:div {:id "audio-player-container" :class "mt-4 flex flex-col items-center justify-center"}
       [:div {:id "play-icon-container"}
        (ui/button :class "hidden pause" :label "Pause" :icon icons/circle-pause)
        (ui/button :class "play" :label "Play" :icon icons/circle-play)]
       [:div
        [:span {:class "current-time text-white mr-2 text-lg"} "0:00"]
        [:input {:type "range" :class "seek-slider", :max "100", :value "0"}]
        [:span {:class "duration text-white ml-2 text-lg"} "0:00"]]
       ;; [:input {:type "range", :id "volume-slider", :max "100", :value "100"}]
       ]]]]
   [:div {:class ""}
    [:div {:id "adobe-dc-view"}]]])
