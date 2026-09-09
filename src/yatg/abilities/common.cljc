(ns yatg.abilities.common
  (:require
   [yatg.abilities.consequences :refer [apply-consequences change-stamina
                                        replace-consequence-ability-arg-placeholders]]
   [yatg.battle-log :refer [log-diff]]
   [yatg.hex-grid.core :refer [get-adjacent-enemy-ids in-range?]]
   [yatg.schemas
     :refer
     [Ability Character collect-effects-for-trigger GameState get-abilities
      get-acting-character get-character-tile get-modified-attributes HexGrid
      HexTile path-to-character-abilities Restriction]]
   [yatg.specter-with-better-errors :as sp]
   [yatg.timeline :refer [place-next-move]]))

; ----------------- Functionality -------------------------

; When it is a character's turn, hovering over any tile should show a menu on
; that tile for what abilities can be used targetting it.
;
; We should also grey out all tiles that NO abilities can be used on.


(defn get-primed-ability
  {:malli/schema [:-> GameState [:maybe Ability]]}
  [game-state]
  (->> game-state
       (:characters)
       (map get-abilities)
       (flatten)
       (sp/select-one [sp/ALL #(not (nil? (:primed-args %)))])))

(defn unprime-abilities
  {:malli/schema [:-> Character GameState GameState]}
  [character game-state]
  (sp/transform (path-to-character-abilities (:id character))
                #(dissoc % :primed-args)
                game-state))

(defn update-character-engagements
  {:malli/schema [:-> Character GameState Character]}
  [character game-state]
  (let [max-engagements (:max-engagements (get-modified-attributes character))
        current-adjacent-enemy-ids (set (get-adjacent-enemy-ids character
                                                                game-state))]
    (update-in
      character
      [:resources :engaged-character-ids]
      ; Prefer keeping engagements the character already has
      (fn [engaged-character-ids]
        (let [persistent-engaged-ids (remove #(not (contains?
                                                     current-adjacent-enemy-ids
                                                     %))
                                       engaged-character-ids)
              new-engaged-ids        (filter #(not (contains?
                                                     (set engaged-character-ids)
                                                     %))
                                       current-adjacent-enemy-ids)]
          (vec (take max-engagements
                    (concat persistent-engaged-ids new-engaged-ids))))))))

(defn recompute-engagements
  {:malli/schema [:-> GameState GameState]}
  [game-state]
  (sp/transform [:characters sp/ALL]
                #(update-character-engagements % game-state)
                game-state))

(declare clear-all-targetable-abilities)


(defn use-ability
  {:malli/schema [:-> GameState Ability GameState]}
  [game-state
   {:keys [id stamina-cost time-cost primed-args display-name consequences]}]
  (let [character (get-acting-character game-state)
        consequences-without-placeholders
        (map #(replace-consequence-ability-arg-placeholders primed-args %)
          consequences)]
    (as-> game-state gs
      (apply-consequences consequences-without-placeholders gs)
      (change-stamina {:target-id (:id character) :amount (- stamina-cost)} gs)
      (log-diff (str (:display-name character) " uses ability " display-name)
                game-state
                gs)
      (apply-consequences (map :consequences
                            (collect-effects-for-trigger character
                                                         :after-ability-use))
                          gs)
      (recompute-engagements gs)
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
  (use-ability game-state (get-primed-ability game-state)))

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
