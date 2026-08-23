(ns yatg.ui.core
  (:require [yatg.ui.overworld :refer [render-overworld render-location]]
            [yatg.ui.battle :refer [render-battle]]
            [yatg.utils :refer [get-by-id]]))
   
(defn render-game
  [{:keys [locations] {:keys [location-id battle]} :current-scene :as game-state}]
  [:div
   (if (nil? location-id)
     (render-overworld game-state)
     (if (nil? battle)
       (render-location (get-by-id locations location-id))
       (render-battle battle game-state)))])
