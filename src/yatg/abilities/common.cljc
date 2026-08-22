(ns yatg.abilities.common
  (:require [yatg.schemas
             :refer
             [GameState
              HexTile
              HexGrid
              Ability
              path-to-ability
              path-to-tile
              path-to-characters-tile
              get-acting-character
              Character]]
            [yatg.timeline :refer [place-next-move]]
            [yatg.hex-grid :refer [in-range? get-character-tile]]
            [com.rpl.specter :as sp]))

; ------------------ Abilities -----------------------------

(def move
  {:id :move
   :display-name "Move"
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
   :display-name "Wait"
   :stamina-cost 0
   :targetable-tiles {:min-range 0 :max-range 0}})

; ----------------- Functionality -------------------------

; When it is a character's turn, hovering over any tile should show a menu on
; that tile for what abilities can be used targetting it.
;
; We should also grey out all tiles that NO abilities can be used on.

(declare clear-all-targetable-abilities)

(defn use-ability
  {:malli/schema [:-> GameState Ability Character GameState]}
  [game-state {:keys [id pending-args]} character]
  (-> game-state
      ((case id
        :move (fn [game-state]
               (move-character game-state
                               (:target-tile-id pending-args)
                               (:id character)))
        ; Do nothing
        :wait (fn [game-state] game-state)))
      (update-in [:current-scene :battle :timeline]
                 #(place-next-move % character))
      (update-in [:current-scene :battle :hexgrid]
                 clear-all-targetable-abilities)))
  

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
  (use-ability game-state (find-pending-ability game-state)
               (get-acting-character game-state)))

; ----------------- Setting Tile Abilities -------------------------

(defn set-targetable-abilties
  {:malli/schema [:-> HexTile Character HexTile HexTile]}
  [tile {:keys [abilities]} acting-characters-tile]
  (assoc tile
   :abilities-that-can-target
   (filterv #(in-range? acting-characters-tile tile (:targetable-tiles %))
     abilities)))

(defn set-all-targetable-abilities
  "Attach abilities that can target tiles to those tiles for selection in the UI."
  {:malli/schema [:-> HexGrid Character HexGrid]}
  [hexgrid character]
  (let [char-tile (get-character-tile hexgrid character)]
    (mapv #(set-targetable-abilties % character char-tile) hexgrid)))

(defn clear-all-targetable-abilities
  "Remove abilities that can target tiles from those tiles for selection in the UI."
  {:malli/schema [:-> HexGrid HexGrid]}
  [hexgrid]
  (mapv #(assoc % :abilities-that-can-target nil) hexgrid))
