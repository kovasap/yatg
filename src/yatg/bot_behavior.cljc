(ns yatg.bot-behavior
  (:require
   [yatg.abilities.common :refer [get-possible-abilities]]
   [yatg.hex-grid.core :refer [get-empty-tiles-adjacent-to-enemies
                               get-in-range-tiles is-adjacent-to-enemy?]]
   [yatg.hex-grid.pathfinding :refer [get-first-step-to-closest-tile]]
   [yatg.schemas
             :refer
             [Ability GameState get-acting-character get-acting-character-tile]]))

; ------------- Ability Priming Functions -----------------------------
; These functions prime abilities with arguments so that they can be used.

(defn- update-single-target
  {:malli/schema [:-> :keyword Ability GameState [:maybe Ability]]}
  [ability target-tile-id]
  (if (nil? target-tile-id)
    nil
    (assoc-in ability [:primed-args :target-tile-id] target-tile-id)))
  
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

(defn try-to-prime-all-and-get-first-success
  [abilities filter-fn priming-fn]
  (->> abilities
       (filter filter-fn)
       (map priming-fn)
       (remove nil?)
       (first)))

; This is a description of how to prime a set of abilities.
(def AbilityPrimer
  [:map
   ; describes which abilities can be primed in this way
   [:filter-fn [:-> Ability :boolean]]
   ; describes how to prime
   [:priming-fn [:-> Ability [:maybe Ability]]]])

(defn get-priorities
  "An ordered list of how to prime abilities.  The entries ealier in the list
  have priority; the first successful prime from this list will be chosen for
  the bot to perform."
  {:malli/schema [:-> GameState [:vector AbilityPrimer]]}
  [game-state]
  [{:filter-fn  #(contains? (:tags %) :attack)
    :priming-fn #(arbitrary-in-range % game-state)}
   {:filter-fn  #(= :move (:id %))
    :priming-fn #(first-step-to-closest-target % game-state)}
   {:filter-fn #(= :wait (:id %)) :priming-fn #(assoc % :primed-args {})}])

(defn select-and-autoprime-ability
  {:malli/schema [:-> GameState Ability]}
  [game-state]
  (let [candidate-abilties (get-possible-abilities (get-acting-character
                                                     game-state))]
    (loop [priorities (get-priorities game-state)]
      (let [{:keys [filter-fn priming-fn]} (first priorities)]
        (if-let [primed-ability (try-to-prime-all-and-get-first-success
                                  candidate-abilties
                                  filter-fn
                                  priming-fn)]
          primed-ability
          (recur (rest priorities)))))))
