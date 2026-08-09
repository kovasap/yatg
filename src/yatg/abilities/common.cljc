(ns yatg.abilities.common
  (:require [yatg.schemas :refer [HexTile Character]]))

(def move
  {:id :move
   :display-name "Move"
   :stamina-cost 5
   :targetable-tiles {:min-range 1 :max-range 1}})

; When it is a character's turn, hovering over any tile should show a menu on
; that tile for what abilities can be used targetting it.
;
; We should also grey out all tiles that NO abilities can be used on.

(defn can-target?)

(defn set-targetable-abilties
  "Attach abilities that can target tiles to those tiles for selection in the UI."
  {:malli/schema [:-> HexTile Character HexTile]}
  [tile acting-character]
  (assoc tile
    :abilities-that-can-target (filterv #(can-target? % tile)
                                 (:abilties acting-character))))


; This should be called when hovering over an ability on a specific tile, and
; should show what effect it will have if used.
(defn preview-ability
  [game-state])
  
 
; 1. Unset abilities-that-can-target on all tiles
; 2. Apply effects of ability to game state.
(defn use-ability
  [using-character-id
   ability
   game-state])
