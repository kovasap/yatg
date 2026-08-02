(ns yatg.ui.overworld
  (:require [yatg.schemas :as schemas]
            [yatg.ui.schemas :refer [Hiccup]]))

(defn render-location
  "Show a zoomed in view of the location, with UI elements to interact with it."
  {:malli/schema [:-> schemas/Location Hiccup]}
  [{:keys [path-to-img]}]
  [:div
   [:button 
    {:on {:click [[:view-overworld]]}}
    "Back to Overworld"]
   [:button 
    {:on {:click [[:start-battle]]}}
    "Start battle"]
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
