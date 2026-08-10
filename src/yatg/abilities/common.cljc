(ns yatg.abilities.common
  (:require
    [yatg.schemas :refer [HexTile HexGrid Ability path-to-ability Character]]
    [yatg.hex-grid :refer [in-range?]]
    [yatg.utils :refer [get-by-id only]]))

; ------------------ Abilities -----------------------------

(def move
  {:id :move
   :display-name "Move"
   :stamina-cost 5
   :targetable-tiles {:min-range 1 :max-range 1}})

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


; ----------------- Setting Tile Abilities -------------------------

(defn set-targetable-abilties
  {:malli/schema [:-> HexTile Character HexTile]}
  [tile acting-character acting-characters-tile]
  (assoc tile
    :abilities-that-can-target
    (filterv #(in-range? acting-characters-tile tile (:targetable-tiles %))
      (:abilties acting-character))))

(defn set-all-targetable-abilities
  "Attach abilities that can target tiles to those tiles for selection in the UI."
  {:malli/schema [:-> HexGrid Character HexGrid]}
  [hexgrid character]
  (let [characters-tile (only (filter #(= (:id character) (:character-id %))
                                hexgrid))]
    (mapv #(set-targetable-abilties % character characters-tile) hexgrid)))

(defn clear-all-targetable-abilities
  "Remove abilities that can target tiles from those tiles for selection in the UI."
  {:malli/schema [:-> HexGrid HexGrid]}
  [hexgrid]
  (mapv #(assoc % :abilities-that-can-target nil) hexgrid))
