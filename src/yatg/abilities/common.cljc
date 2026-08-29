(ns yatg.abilities.common
  (:require [yatg.schemas
             :refer
             [GameState
              HexTile
              HexGrid
              Ability
              path-to-tile
              path-to-characters-tile
              get-acting-character
              Character]]
            [yatg.timeline :refer [place-next-move]]
            [yatg.hex-grid.core :refer [in-range? get-character-tile]]
            [com.rpl.specter :as sp]))

; ------------------ Abilities -----------------------------

(def attack
  {:id :attack
   :display-name "atk"
   :stamina-cost 10
   :targetable-tiles {:min-range 1 :max-range 1 :requires-character :enemy}})
(defn attack-character
  {:malli/schema [:-> GameState :keyword :keyword GameState]}
  [game-state tile-id character-id]
  ; TODO add more data to abilities for attack damage and implement this
  game-state)

(def move
  {:id :move
   :display-name "mv"
   :stamina-cost 5
   :targetable-tiles {:min-range 1 :max-range 1}})
(defn move-character
  {:malli/schema [:-> GameState :keyword :keyword GameState]}
  [game-state tile-id character-id]
  (->>
    game-state
    (sp/setval (concat (path-to-characters-tile character-id) [:character-id])
               sp/NONE)
    (sp/setval (concat (path-to-tile tile-id) [:character-id]) character-id)))

(def wait
  {:id :wait
   :display-name "wt"
   :stamina-cost 0
   :targetable-tiles {:min-range 0 :max-range 0}})

; ----------------- Functionality -------------------------

; When it is a character's turn, hovering over any tile should show a menu on
; that tile for what abilities can be used targetting it.
;
; We should also grey out all tiles that NO abilities can be used on.

(declare clear-all-targetable-abilities)

(defn use-ability
  {:malli/schema [:-> GameState Ability GameState]}
  [game-state {:keys [id pending-args]}]
  (let [character (get-acting-character game-state)]
    (-> game-state
        ((case id
          :attack (fn [game-state]
                    (attack-character game-state 
                                      (:target-tile-id pending-args)
                                      (:id character)))
          :move (fn [game-state]
                 (move-character game-state
                                 (:target-tile-id pending-args)
                                 (:id character)))
          ; Do nothing
          :wait (fn [game-state] game-state)))
        (update-in [:current-scene :battle :timeline]
                   #(place-next-move % character))
        (update-in [:current-scene :battle :hexgrid]
                   clear-all-targetable-abilities)
        (assoc-in [:current-scene :battle :acting-character-id] nil))))
  

(defn find-pending-ability
  {:malli/schema [:-> GameState Ability]}
  [game-state]
  (->> game-state
       (:characters)
       (map :abilities)
       (flatten)
       (sp/select-one [sp/ALL #(not (nil? (:pending-args %)))])))

(defn use-pending-ability
  {:malli/schema [:-> GameState GameState]}
  [game-state]
  (use-ability game-state (find-pending-ability game-state)))

; ----------------- Setting Tile Abilities -------------------------

(defn set-targetable-abilties
  {:malli/schema [:-> HexTile Character HexTile GameState HexTile]}
  [tile
   {:keys [abilities]}
   acting-characters-tile
   game-state]
  (assoc tile
    :abilities-that-can-target (filterv #(in-range? acting-characters-tile
                                                    tile
                                                    (:targetable-tiles %)
                                                    game-state)
                                 abilities)))

(defn set-all-targetable-abilities
  "Attach abilities that can target tiles to those tiles for selection in the UI."
  {:malli/schema [:-> HexGrid Character GameState HexGrid]}
  [hexgrid acting-character game-state]
  (let [char-tile (get-character-tile hexgrid acting-character)]
    (mapv #(set-targetable-abilties % acting-character char-tile game-state)
      hexgrid)))

(defn clear-all-targetable-abilities
  "Remove abilities that can target tiles from those tiles for selection in the UI."
  {:malli/schema [:-> HexGrid HexGrid]}
  [hexgrid]
  (mapv #(assoc % :abilities-that-can-target nil) hexgrid))
