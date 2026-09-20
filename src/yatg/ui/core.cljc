(ns yatg.ui.core
  (:require
   [yatg.schemas :refer [GameState]]
   [yatg.ui.battle :refer [render-battle]]
   [yatg.ui.battle-resolution :refer [render-battle-resolution]]
   [yatg.ui.overworld :refer [render-location render-overworld]]
   [yatg.ui.schemas :refer [Hiccup]]
   [yatg.utils :refer [get-by-id]]))
   
(defn render-game
  {:malli/schema [:-> GameState Hiccup]}
  [{:keys [locations] {:keys [location-id battle battle-resolution]} :current-scene :as game-state}]
  [:div
   (if (nil? location-id)
     (render-overworld game-state)
     (if (nil? battle)
       (render-location (get-by-id locations location-id))
       (if (nil? battle-resolution)
         (render-battle battle game-state)
         (render-battle-resolution battle-resolution game-state))))])
