(ns yatg.event-handling.actions
  (:require
   [yatg.abilities.common
             :refer
             [find-primed-ability set-all-targetable-abilities
              unprime-abilities use-primed-ability]]
   [yatg.abilities.consequences :refer [apply-consequences]]
   [yatg.battle :refer [start-battle]]
   [yatg.bot-behavior :refer [select-and-autoprime-ability]]
   [yatg.event-handling.infra :refer [interleave-delay ra! rsa!]]
   [yatg.graphics.sprite :refer [set-frame]]
   [yatg.schemas
             :refer
             [Ability Action BattleSpec CharacterId GameState
              get-acting-character HexTile path-to-ability path-to-character
              path-to-tile Sprite]]
   [yatg.specter-with-better-errors :as sp]
   [yatg.timeline :refer [get-next-tick-with-actions]]
   [yatg.utils :refer [get-by-id]]))

; ------------------- Overworld and Menu Navigation -----------------------

; Zoom in on a location.
(rsa! :actions/view-location
      [:-> GameState :keyword GameState]
      (fn [game-state location-id]
        (assoc game-state :current-scene {:location-id location-id})))

; Go back to the overworld.
(rsa! :actions/view-overworld
   [:-> GameState GameState]
   (fn [game-state]
     (assoc game-state :current-scene {:location-id nil})))

; Create and then start a battle.
(rsa! :actions/start-battle [:-> GameState BattleSpec GameState] start-battle)

(rsa! :actions/toggle-auto-advance-timeline
      [:-> GameState GameState]
      (fn [game-state]
        (update-in game-state [:settings :auto-advance-timeline] not)))

; ------------------- Overall Turn Flow -----------------------

; Give the player a chance to command their character.
(ra! :actions/start-player-turn
     [:-> GameState CharacterId [:sequential Action]]
     (fn [game-state character-id]
       (prn "Starting turn for " character-id)
       [[:actions/set-acting-character character-id]
        [:actions/set-all-targetable-abilities character-id]]))

; Use an ability, then move to the next turn on the timeline.  This should be
; used over raw :actions/use-ability most of the time.
(ra! :actions/use-primed-ability-and-advance-timeline
     [:-> GameState [:sequential Action]]
     (fn [game-state]
       [[:effects/execute-actions-with-delay
         [[:actions/play-primed-ability-animation]
          [:actions/ms-delay 50]
          [:actions/use-primed-ability]
          [:actions/advance-timeline]]]]))

(ra! :actions/perform-turn
     [:-> GameState :keyword [:sequential Action]]
     (fn [game-state character-id]
       [[:actions/set-acting-character character-id]
        [:actions/select-and-autoprime-ability]
        [:actions/use-primed-ability-and-advance-timeline]]))

; ------------------- Timeline Manipulation -----------------------

(ra! :actions/advance-timeline-one-tick
     [:-> GameState [:sequential Action]]
     (fn [game-state]
       (let [{:keys [actions current-tick]}
             (get-in game-state [:current-scene :battle :timeline])
             new-tick (inc current-tick)]
         (concat
           ; Tick our timeline forward.
           [[:effects/swap
             :tick-forward
             #(assoc-in %
                [:current-scene :battle :timeline :current-tick]
                new-tick)]
            [:actions/regen-all-character-stamina]]
           ; Then do all the actions at this new tick.
           (get actions new-tick [])))))

(rsa! :actions/regen-all-character-stamina
      [:-> GameState GameState]
      (fn [game-state]
        (apply-consequences
          (map (fn [{:keys [id attributes]}]
                 [:change-stamina {:target-id id
                                   :amount (:stamina-regen attributes)}])
            (:characters game-state))
          game-state)))

(defn get-ticks-to-advance
  {:malli/schema [:-> GameState :int]}
  [game-state]
  (let [{:keys [current-tick] :as timeline}
        (get-in game-state [:current-scene :battle :timeline])
        next-tick-with-actions (get-next-tick-with-actions timeline)]
    (if (or (nil? next-tick-with-actions)
            (not (:auto-advance-timeline (:settings game-state))))
      1
      (- next-tick-with-actions current-tick))))

; Move along the timeline until we hit a tick with something actionable for the
; player on it.
(ra! :actions/advance-timeline
     [:-> GameState [:sequential Action]]
     (fn [game-state]
       [(interleave-delay (repeat (get-ticks-to-advance game-state)
                                  [:actions/advance-timeline-one-tick])
                          100)]))

(defn set-acting-character
  {:malli/schema [:-> GameState :keyword GameState]}
  [game-state character-id]
  (assoc-in game-state
    [:current-scene :battle :acting-character-id]
    character-id))

(rsa! :actions/set-acting-character
      [:-> GameState :keyword GameState]
      set-acting-character)

; ------------------- Player Turn Interactions -----------------------

; Select and deselect tiles.
(rsa! :actions/hover-tile
      [:-> GameState HexTile GameState]
      (fn [game-state tile]
        (sp/transform (path-to-tile (:id tile))
                      #(assoc % :hovered? true)
                      game-state)))
(rsa! :actions/unhover-tile
      [:-> GameState HexTile GameState]
      (fn [game-state tile]
        (sp/transform (path-to-tile (:id tile))
                      #(assoc % :hovered? false)
                      game-state)))

; Prime an ability manually (likely because of player input).
(rsa!
  :actions/prime-ability
  [:-> GameState Ability :keyword GameState]
  (fn [game-state ability target-tile-id]
    (sp/transform
      (path-to-ability (:id (get-acting-character game-state)) (:id ability))
      #(assoc % :primed-args {:target-tile-id target-tile-id})
      game-state)))
(rsa! :actions/unprime-abilities
      [:-> GameState GameState]
      (fn [game-state]
        (unprime-abilities (get-acting-character game-state) game-state)))

; Set up all tiles in the UI so that they show what abilities can target them.
(rsa! :actions/set-all-targetable-abilities
      [:-> GameState CharacterId GameState]
      (fn [{:keys [characters] :as game-state} character-id]
        (update-in game-state
                   [:current-scene :battle :hexgrid]
                   #(set-all-targetable-abilities %
                                                  (get-by-id characters
                                                             character-id)
                                                  game-state))))

; ----------------- Automatic Turn Selection (Bot) ----------------------

; Select and prime the ability that the bot will use.
(rsa! :actions/select-and-autoprime-ability
      [:-> GameState GameState]
      (fn [game-state]
        (let [primed-ability (select-and-autoprime-ability game-state)]
          (sp/setval (path-to-ability (:id (get-acting-character game-state))
                                      (:id primed-ability))
                     primed-ability
                     game-state))))

; ----------------- Ability Use and Animations ----------------------

(rsa! :actions/use-primed-ability [:-> GameState GameState] use-primed-ability)

(rsa! :actions/set-sprite
      [:-> GameState :keyword Sprite GameState]
      (fn [game-state character-id new-sprite]
        (sp/setval (conj (path-to-character character-id) :sprite)
                   new-sprite
                   game-state)))

(def frame-time-ms 150)

(ra! :actions/play-primed-ability-animation
     [:-> GameState [:sequential Action]]
     (fn [game-state]
       (let [{:keys [animation-id]} (find-primed-ability game-state)]
         (if (nil? animation-id)
           []
           (let [{:keys [sprite id]} (get-acting-character game-state)
                 num-frames (count (:frame-img-paths (get-by-id
                                                       (:animations sprite)
                                                       animation-id)))]
             [(interleave-delay
                (conj (mapv (fn [frame-idx]
                              [:actions/set-sprite
                               id
                               (set-frame sprite animation-id frame-idx)])
                        (range num-frames))
                      [:actions/set-sprite id (set-frame sprite :idle 0)])
                frame-time-ms)])))))
