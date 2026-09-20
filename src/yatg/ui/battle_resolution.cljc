(ns yatg.ui.battle-resolution 
  (:require
   [yatg.schemas :refer [BattleResolution GameState]]
   [yatg.ui.schemas :refer [Hiccup]]
   [yatg.utils :refer [get-by-id]]))

(defn render-battle-resolution
  {:malli/schema [:-> BattleResolution GameState Hiccup]}
  [battle-resolution
   {:keys [locations]
    {:keys [location-id battle]} :current-scene
    :as   game-state}]
  [:div
   [:h1 (if (:victory? battle-resolution) "Victory!" "Defeat.")]
   [:div "Insert loot here."]
   [:button {:on {:click [[:actions/view-location location-id]]}}
    (str "Back to " (:display-name (get-by-id locations location-id)))]])
