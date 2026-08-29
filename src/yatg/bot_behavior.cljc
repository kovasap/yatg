(ns yatg.bot-behavior
  (:require
   [yatg.hex-grid.core :refer [get-character-tile get-in-range-tiles]]
   [yatg.hex-grid.pathfinding :refer [get-paths-to-closest-tiles]]
   [yatg.schemas :refer [Ability GameState get-acting-character get-hexgrid]]
   [yatg.utils :refer [get-by-id]]))

(def ability-priorities
  [:attack :move :wait])


(defn determine-ability-to-use
  {:malli/schema [:-> GameState Ability]}
  [game-state]
  (let [acting-character (get-acting-character game-state)
        hexgrid (get-hexgrid game-state)
        acting-character-tile (get-character-tile hexgrid acting-character)]
    (loop [ability-ids ability-priorities]
      (let [ability-id (first ability-ids)
            ability (get-by-id (:abilities acting-character) ability-id)]
        (if (nil? ability)
          ; Skip the ability if the character doesn't have it.
          (recur (rest ability-ids))
          (let [in-range-tiles (get-in-range-tiles acting-character-tile
                                                   (:targetable-tiles ability)
                                                   game-state)]
            (if (seq in-range-tiles)
              ; If this ability has any in range tiles, then we can do it, and
              ; now will!
              (assoc ability
                :pending-args (case (:id ability)
                                :attack {:target-tile-id (first in-range-tiles)}
                                :move   {:target-tile-id
                                         (first (first (get-paths-to-closest-tiles
                                                         acting-character-tile
                                                         (:targetable-tiles
                                                           ability)
                                                         game-state)))}
                                ability))
              ; Otherwise, move on to the next ability
              (recur (rest ability-ids)))))))))
