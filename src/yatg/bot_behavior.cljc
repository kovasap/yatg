(ns yatg.bot-behavior
  (:require
   [yatg.hex-grid.core :refer [get-in-range-tiles]]
   [yatg.hex-grid.pathfinding :refer [get-first-step-to-closest-tile]]
   [yatg.schemas :refer [Ability GameState get-acting-character get-hexgrid]]
   [yatg.utils :refer [get-by-id]]))

(def ability-priorities
  [:attack :move :wait])

(defmacro defn-ability-primer
  [fn-name & body]
  (let [fn-symbol-with-meta
        (with-meta fn-name
          (merge (meta fn-name)
                 {:malli/schema [:-> Ability GameState [:maybe Ability]]}))]
    `(defn ~fn-symbol-with-meta ~@body)))

(defn-ability-primer arbitrary-in-range
  "Pick an arbitrary in range tile and update the ability to target it.
  If there are no in range tiles, then return nil instead of the ability."
  {:malli/schema [:-> Ability GameState [:maybe Ability]]}
  [{:keys [targetable-tiles] :as ability} game-state]
  (let [target-tile-id (first (get-in-range-tiles (get-acting-character-tile
                                                    game-state)
                                                  targetable-tiles
                                                  game-state))]
    (if (nil? target-tile-id)
      nil
      (assoc-in ability [:pending-args :target-tile-id] target-tile-id))))

(defn update-single-target-ability-args
  "Pick an arbitrary in range tile and update the ability to target it.
  If there are no in range tiles, then return nil instead of the ability."
  {:malli/schema [:-> Ability GameState [:maybe Ability]]}
  [{:keys [targetable-tiles] :as ability} game-state]
  (let [target-tile-id (first (get-in-range-tiles (get-acting-character-tile
                                                    game-state)
                                                  targetable-tiles
                                                  game-state))]
    (if (nil? target-tile-id)
      nil
      (assoc-in ability [:pending-args :target-tile-id] target-tile-id))))

(defn determine-ability-to-use
  {:malli/schema [:-> GameState Ability]}
  [game-state]
  (let [acting-character (get-acting-character game-state)
        hexgrid (get-hexgrid game-state)
        acting-character-tile (get-character-tile hexgrid acting-character)]
    (loop [ability-ids ability-priorities]
      (let [ability-id (first ability-ids)
            {:keys [targetable-tiles id] :as ability}
            (get-by-id (:abilities acting-character) ability-id)]
        (if (nil? ability)
          ; Skip the ability if the character doesn't have it.
          (recur (rest ability-ids))
          (case id
            :attack (let [args {:target-tile-id (first (get-in-range-tiles
                                                         acting-character-tile
                                                         targetable-tiles
                                                         game-state))}]
                      (if (nil? (:target-tile-id args))
                        (recur (rest ability-ids))
                        (assoc ability :pending-args args)))
            :move   (let [args {:target-tile-id (get-first-step-to-closest-tile
                                                  acting-character-tile
                                                  targetable-tiles
                                                  game-state)}]
                      (if (nil? (:target-tile-id args))
                        (recur (rest ability-ids))
                        (assoc ability :pending-args args)))
            ; Always wait if we try everything else and they all can't be
            ; done.
            :wait   ability))))))
