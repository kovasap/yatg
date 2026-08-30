(ns yatg.bot-behavior
  (:require
   [yatg.abilities.common :refer [get-possible-abilities]]
   [yatg.hex-grid.core :refer [get-empty-tiles-adjacent-to-enemies
                               get-in-range-tiles is-adjacent-to-enemy?]]
   [yatg.hex-grid.pathfinding :refer [get-first-step-to-closest-tile]]
   [yatg.schemas
             :refer
             [Ability GameState get-acting-character get-acting-character-tile]]
   [yatg.utils :refer [get-by-id]]))

(def ability-priorities
  [:attack :move :wait])

; ------------- Ability Priming Functions -----------------------------
; These functions prime abilities with arguments so that they can be used.

(defn- update-single-target
  {:malli/schema [:-> :keyword Ability GameState [:maybe Ability]]}
  [ability target-tile-id]
  (if (nil? target-tile-id)
    nil
    (assoc-in ability [:pending-args :target-tile-id] target-tile-id)))
  
(defn arbitrary-in-range
  "Pick an arbitrary in range tile and update the ability to target it.

  If there are no in range tiles, then return nil instead of the ability."
  {:malli/schema [:-> Ability GameState [:maybe Ability]]}
  [{:keys [targetable-tiles] :as ability} game-state]
  (update-single-target ability
                        (:id (first (get-in-range-tiles
                                      (get-acting-character-tile game-state)
                                      targetable-tiles
                                      game-state)))))

(defn first-step-to-closest-target
  "Update the ability to target the first tile on the path to the closest
  valid target.

  If there are no valid targets, then return nil instead of the ability."
  {:malli/schema [:-> Ability GameState [:maybe Ability]]}
  [ability game-state]
  (if (is-adjacent-to-enemy? (get-acting-character game-state) game-state)
    nil
    (update-single-target ability
                          (get-first-step-to-closest-tile
                            (get-acting-character-tile game-state)
                            (get-empty-tiles-adjacent-to-enemies
                              (get-acting-character game-state)
                              game-state)
                            game-state))))

; -------------------------------------------------------------------------

(defn determine-ability-to-use
  {:malli/schema [:-> GameState Ability]}
  [game-state]
  (loop [ability-ids ability-priorities]
    (if-let [ability (get-by-id (get-possible-abilities (get-acting-character
                                                          game-state))
                                (first ability-ids))]
      (if-let [primed-ability
               (case (:id ability)
                 :attack (arbitrary-in-range ability game-state)
                 :move   (first-step-to-closest-target ability game-state)
                 :wait   (assoc ability :pending-args {}))]
        primed-ability
        ; If we failed to prime, move on to other abilities
        (recur (rest ability-ids)))
      ; Skip the ability if the character doesn't have it.
      (recur (rest ability-ids)))))
