(ns yatg.game
  (:require [yatg.schemas :as schemas]))

(defn create-initial-store
  "Sets up the initial game state."
  {:malli/schema [:-> schemas/GameState]}
  []
  {:locations [{:id :capitol
                :display-name "Capitol City"
                :path-to-img "castle.png"
                :screen-coordinates {:x 400 :y 400}}]
   :overworld "overworld.svg"
   :current-scene {:type :overworld
                   :id nil}})

(defn view-location
  "Zoom in to a location."
  {:malli/schema [:-> schemas/GameState :keyword schemas/GameState]}
  [store location-id]
  (assoc store :current-scene {:type :location
                               :id location-id}))
