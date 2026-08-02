(ns yatg.game
  (:require [yatg.schemas :as schemas]
            [yatg.battle :refer [generate-battle]]
            [yatg.hex-grid :refer [HexTile]]
            [com.rpl.specter :as s]))

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

(defn view-location
  "Zoom in to a location."
  {:malli/schema [:-> schemas/GameState :keyword schemas/GameState]}
  [store location-id]
  (assoc store :current-scene {:location-id location-id}))

(defn view-overworld
  "Go back to the overworld."
  {:malli/schema [:-> schemas/GameState schemas/GameState]}
  [store]
  (assoc store :current-scene {:location-id nil}))

(defn start-battle
  "Start a battle"
  {:malli/schema [:-> schemas/GameState schemas/GameState]}
  [store]
  (assoc-in store [:current-scene :battle] (generate-battle 10 10)))

(defn select-tile
  "Select a tile for further action."
  {:malli/schema [:-> schemas/GameState HexTile schemas/GameState]}
  [store tile]
  (s/setval [:current-scene :battle :hexgrid s/ALL #(= tile %) :selected?]
            true
            store))

(defn deselect-tile
  "Deselect a tile."
  {:malli/schema [:-> schemas/GameState HexTile schemas/GameState]}
  [store tile]
  (s/setval [:current-scene :battle :hexgrid s/ALL #(= tile %) :selected?]
            false
            store))
