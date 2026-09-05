(ns yatg.abilities.common
  (:require
   [yatg.abilities.consequences :refer [replace-consequence-ability-arg-placeholders apply-consequences change-stamina]]
   [yatg.hex-grid.core :refer [in-range?]]
   [yatg.schemas
     :refer
     [Ability Character collect-effects-for-trigger GameState
      get-acting-character get-character-tile HexGrid HexTile
      path-to-character-abilities]]
   [yatg.specter-with-better-errors :as sp]
   [yatg.timeline :refer [place-next-move]]))

; ------------------ Abilities -----------------------------

(def attack
  {:id :attack
   :display-name "atk"
   :animation-id :attack
   :stamina-cost 10
   :time-cost 5
   :consequences [[:change-stamina {:target-tile-id :ability-arg-placeholder/target-tile-id
                                    :amount -20}]]
   :targetable-tiles {:min-range 1 :max-range 1 :requires-character :enemy}})

(def move
  {:id :move
   :display-name "mv"
   :stamina-cost 5
   :time-cost 5
   :consequences [[:move-character {:destination :ability-arg-placeholder/target-tile-id
                                    :traveller :active-character}]]
   :restrictions [:unengaged]
   :targetable-tiles {:min-range 1 :max-range 1}})

(def wait
  {:id :wait
   :display-name "wt"
   :stamina-cost 0
   :time-cost 5
   :consequences []
   :targetable-tiles {:min-range 0 :max-range 0}})

; ----------------- Functionality -------------------------

; When it is a character's turn, hovering over any tile should show a menu on
; that tile for what abilities can be used targetting it.
;
; We should also grey out all tiles that NO abilities can be used on.


(defn find-primed-ability
  {:malli/schema [:-> GameState Ability]}
  [game-state]
  (->> game-state
       (:characters)
       (map :abilities)
       (flatten)
       (sp/select-one [sp/ALL #(not (nil? (:primed-args %)))])))

(defn unprime-abilities
  {:malli/schema [:-> Character GameState GameState]}
  [character game-state]
  (sp/transform (path-to-character-abilities (:id character))
                #(dissoc % :primed-args)
                game-state))

(declare clear-all-targetable-abilities)

(defn use-ability
  {:malli/schema [:-> GameState Ability GameState]}
  [game-state {:keys [id stamina-cost time-cost primed-args consequences]}]
  (let [character (get-acting-character game-state)
        effects   (collect-effects-for-trigger character :after-ability-use)
        consequences-without-placeholders
        (map #(replace-consequence-ability-arg-placeholders primed-args %)
          consequences)]
    (prn "Executing ability " id " for character " (:id character))
    (as-> game-state gs
      (apply-consequences consequences-without-placeholders gs)
      (change-stamina {:target-id (:id character) :amount (- stamina-cost)} gs)
      (apply-consequences (map :consequences effects) gs)
      (unprime-abilities character gs)
      (update-in gs
                 [:current-scene :battle :timeline]
                 #(place-next-move % character time-cost))
      (update-in gs
                 [:current-scene :battle :hexgrid]
                 clear-all-targetable-abilities)
      (assoc-in gs [:current-scene :battle :acting-character-id] nil))))

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
