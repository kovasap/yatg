(ns yatg.ui
  (:require [yatg.schemas :as schemas]))

(def Hiccup
  :any)

(defn render-tactical-map
  "A hex grid to fight upon."
  [store])

(defn render-location
  "Show a zoomed in view of the location, with UI elements to interact with it."
  [store])

(defn render-overworld
  {:malli/schema [:-> schemas/GameState Hiccup]}
  [{:keys [overworld locations]}]
  (into [:div
         [:img
          {:src (:path-to-svg overworld) :alt "Overworld Image" :height 1000}]]
        (for [{:keys         [display-name path-to-img id]
               {:keys [x y]} :screen-coordinates}
              locations]
          [:div {:style {:position "absolute" :left x :top y}
                 :on    {:click [:view-location id]}}
           [:img {:src path-to-img}]
           [:span display-name]])))
   

(defn render-game [{:keys [current-scene] :as store}]
  [:div
   (case current-scene
     :overworld (render-overworld store)
     :location (render-location store)
     :tactical-map (render-tactical-map store))])
