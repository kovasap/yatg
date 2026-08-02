(ns yatg.init
  (:require [yatg.schemas :as schemas]))

(defn create-initial-store
  "Sets up the initial game state."
  {:malli/schema [:-> schemas/GameState]}
  []
  {:locations [{:id :capitol
                :display-name "Capitol City"
                :path-to-img "castle.png"
                :screen-coordinates {:x 400 :y 400}}]
   :overworld {:path-to-svg "overworld.svg"}
   :current-scene {:location-id nil
                   :battle nil}})
