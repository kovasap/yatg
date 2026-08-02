(ns yatg.ui.core
  (:require [yatg.ui.overworld :refer [render-overworld render-location]]
            [yatg.ui.tactical :refer [render-tactical-map]]
            [com.rpl.specter :as s]))
   
(defn render-game
  [{:keys [locations] {:keys [location-id battle]} :current-scene :as store}]
  [:div
   (if (nil? location-id)
     (render-overworld store)
     (if (nil? battle)
       (render-location (s/select-one [s/ALL #(= (:id %) location-id)]
                                      locations))
       (render-tactical-map battle)))])
