(ns yatg.abilities.common
  (:require
   [clojure.set :refer [difference intersection]]
   [clojure.string :as st]
   [yatg.abilities.consequences :refer [apply-consequences change-stamina
                                        replace-consequence-ability-arg-placeholders]]
   [yatg.battle-log :refer [log-str]]
   [yatg.character :refer [grant-experience-for-kills]]
   [yatg.hex-grid.core :refer [get-adjacent-enemy-ids in-range?]]
   [yatg.schemas
     :refer
     [Ability Character CharacterId collect-effects-for-trigger GameState
      get-abilities get-acting-character get-character-tile
      get-modified-attributes HexGrid HexTile path-to-acting-character
      Restriction]]
   [yatg.specter-with-better-errors :as sp]
   [yatg.timeline :refer [place-next-move]]
   [yatg.utils :refer [get-by-id]]))

; ----------------- Functionality -------------------------

; When it is a character's turn, hovering over any tile should show a menu on
; that tile for what abilities can be used targetting it.
;
; We should also grey out all tiles that NO abilities can be used on.


(defn get-primed-ability
  {:malli/schema [:-> GameState [:maybe Ability]]}
  [game-state]
  (:primed-ability (sp/select-one (path-to-acting-character game-state)
                                  game-state)))

(defn prime-acting-character-ability
  {:malli/schema [:-> Ability GameState GameState]}
  [primed-ability game-state]
  (sp/transform (path-to-acting-character game-state)
                #(assoc % :primed-ability primed-ability)
                game-state))

(defn unprime-abilities
  {:malli/schema [:-> GameState GameState]}
  [game-state]
  (sp/transform [:characters sp/ALL]
                #(dissoc % :primed-ability)
                game-state))

(defn get-dead-character-ids
  {:malli/schema [:-> GameState [:set CharacterId]]}
  [game-state]
  (set (map :id (filter :dead? (:characters game-state)))))

(declare clear-all-targetable-abilities)

(defn use-ability
  {:malli/schema [:-> GameState Ability GameState]}
  [game-state
   {:keys [stamina-cost time-cost primed-args display-name consequences]}]
  (assert (not (nil? primed-args)) "Ability must be primed to be used!")
  (let [character (get-acting-character game-state)
        already-dead-character-ids (get-dead-character-ids game-state)
        consequences-without-placeholders
        (map #(replace-consequence-ability-arg-placeholders primed-args %)
          consequences)]
    (as-> game-state gs
      (apply-consequences consequences-without-placeholders gs)
      (change-stamina {:target-id (:id character) :amount (- stamina-cost)} gs)
      (log-str gs
               (str (:display-name character) " uses ability " display-name))
      (unprime-abilities gs)
      (update-in gs
                 [:current-scene :battle :timeline]
                 #(place-next-move % character time-cost))
      (update-in gs
                 [:current-scene :battle :hexgrid]
                 clear-all-targetable-abilities)
      (assoc gs
        :newly-dead-character-ids (difference (get-dead-character-ids gs)
                                              already-dead-character-ids))
      (log-str gs
               (if (empty? (:newly-dead-character-ids gs))
                 nil
                 (str (st/join ", " (:newly-dead-character-ids gs))
                      " died!"))))))

(defn is-restriction-active?
  {:malli/schema [:-> Restriction Character :boolean]}
  [[restriction-name] character]
  (case restriction-name
    :unengaged (empty? (:engaged-character-ids (:resources character)))
    :engaged   (not (empty? (:engaged-character-ids (:resources character))))))

(defn get-possible-abilities
  {:malli/schema [:-> Character [:vector Ability]]}
  [character]
  (->> (get-abilities character)
       (filterv (fn [{:keys [restrictions]}]
                  (every? true?
                          (map #(is-restriction-active? % character)
                            restrictions))))))
  ; This prevents us from going below our stamina using an ability.  We
  ; actually want the player to be able to overexert, and therefore wound, their
  ; characters by using abilties, so we removed this!
  ; TODO add some UI showing that using an ability would wound their character
  ; by showing it in red.
  ; (filterv #(> (:stamina resources) (:stamina-cost %))))

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
