(ns yatg.ui
  (:require [yatg.schemas :as schemas]
            [com.rpl.specter :as s]))

(def Hiccup
  :any)

(defn render-tactical-map
  "A hex grid to fight upon."
  [store])

(defn render-location
  "Show a zoomed in view of the location, with UI elements to interact with it."
  {:malli/schema [:-> schemas/Location Hiccup]}
  [location]
  [:div
   [:button 
    {:on {:click [:view-overworld]}}
    "Back to Overworld"]
   [:img {:src path-to-img
          :width 30}]])

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
           [:img {:src path-to-img
                  :width 30}]
           [:span display-name]])))
   

(defn render-game
  [{:keys [current-scene locations] :as store}]
  [:div
   (case (:type current-scene)
     :overworld    (render-overworld store)
     :location     (render-location (s/select-one
                                      [s/ALL #(= (:id %) (:id current-scene))]
                                      locations))
     :tactical-map (render-tactical-map store))])
