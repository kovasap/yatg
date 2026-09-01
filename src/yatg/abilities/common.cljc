(ns yatg.abilities.common
  (:require
   [com.rpl.specter :as sp]
   [yatg.hex-grid.core :refer [in-range?]]
   [yatg.schemas
             :refer
             [Ability AbilityArgs Character CharacterId GameState
              get-acting-character get-character-tile HexGrid HexTile
              path-to-character path-to-characters-tile path-to-tile]]
   [yatg.timeline :refer [place-next-move]]))

; ------------------ Abilities -----------------------------

(def AbilityFn
  [:-> GameState AbilityArgs CharacterId GameState])

(def attack
  {:id :attack
   :display-name "atk"
   :animation-id :attack
   :stamina-cost 10
   :targetable-tiles {:min-range 1 :max-range 1 :requires-character :enemy}})

(defn drain-stamina
  {:malli/schema [:-> GameState CharacterId :int GameState]}
  [game-state character-id drain-amount]
  (sp/transform (concat (path-to-character character-id) [:resources :stamina])
                #(- % drain-amount)
                game-state))
  

(defn do-attack
  {:malli/schema AbilityFn}
  [game-state args character-id]
  ; TODO add more data to abilities for attack damage and implement this
  (let [target-character-id (sp/select-one (concat (path-to-tile
                                                     (:target-tile-id args))
                                                   [:character-id])
                                           game-state)]
    ; TODO stop hardcoding the drain-amount here
    (drain-stamina game-state target-character-id 20)))

(def move
  {:id :move
   :display-name "mv"
   :stamina-cost 5
   :targetable-tiles {:min-range 1 :max-range 1}})

(defn do-move
  {:malli/schema AbilityFn}
  [game-state {:keys [target-tile-id]} character-id]
  (->>
    game-state
    (sp/setval (concat (path-to-characters-tile character-id) [:character-id])
               sp/NONE)
    (sp/setval (concat (path-to-tile target-tile-id) [:character-id])
               character-id)))

(def wait
  {:id :wait
   :display-name "wt"
   :stamina-cost 0
   :targetable-tiles {:min-range 0 :max-range 0}})

(defn do-wait
  {:malli/schema AbilityFn}
  [game-state args character-id]
  game-state)

(def ability-fns
  {:attack do-attack
   :move do-move
   :wait do-wait})

; ----------------- Functionality -------------------------

; When it is a character's turn, hovering over any tile should show a menu on
; that tile for what abilities can be used targetting it.
;
; We should also grey out all tiles that NO abilities can be used on.

(declare clear-all-targetable-abilities)

(defn use-ability
  {:malli/schema [:-> GameState Ability GameState]}
  [game-state {:keys [id pending-args stamina-cost]}]
  (let [character (get-acting-character game-state)]
    (as-> game-state gs
      ((id ability-fns) gs pending-args (:id character))
      (drain-stamina gs (:id character) stamina-cost)
      (update-in gs
                 [:current-scene :battle :timeline]
                 #(place-next-move % character))
      (update-in gs
                 [:current-scene :battle :hexgrid]
                 clear-all-targetable-abilities)
      (assoc-in gs [:current-scene :battle :acting-character-id] nil))))
  

(defn find-primed-ability
  {:malli/schema [:-> GameState Ability]}
  [game-state]
  (->> game-state
       (:characters)
       (map :abilities)
       (flatten)
       (sp/select-one [sp/ALL #(not (nil? (:pending-args %)))])))

(defn use-primed-ability
  {:malli/schema [:-> GameState GameState]}
  [game-state]
  (use-ability game-state (find-primed-ability game-state)))

(defn get-possible-abilities
  {:malli/schema [:-> Character [:vector Ability]]}
  [{:keys [resources abilities]}]
  (filterv #(> (:stamina resources) (:stamina-cost %)) abilities))

; ----------------- Setting Tile Abilities -------------------------

(defn set-targetable-abilties
  {:malli/schema [:-> HexTile Character HexTile GameState HexTile]}
  [tile character acting-characters-tile game-state]
  (assoc tile
    :abilities-that-can-target
    (filterv
      #(in-range? acting-characters-tile tile (:targetable-tiles %) game-state)
      (get-possible-abilities character))))

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
