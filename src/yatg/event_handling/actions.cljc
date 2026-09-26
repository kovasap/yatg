(ns yatg.event-handling.actions
  (:require
   [clojure.set :refer [intersection]]
   [yatg.abilities.common
             :refer
             [get-dead-character-ids get-primed-ability
              prime-acting-character-ability set-all-targetable-abilities
              unprime-abilities use-ability]]
   [yatg.abilities.consequences :refer [apply-consequences]]
   [yatg.battle :refer [start-battle]]
   [yatg.battle-log :refer [log log-str]]
   [yatg.bot-behavior :refer [select-and-autoprime-ability]]
   [yatg.character
             :refer
             [grant-experience-for-kills update-character-engagements]]
   [yatg.event-handling.infra :refer [interleave-delay ra! rsa!]]
   [yatg.graphics.sprite :refer [set-frame]]
   [yatg.schemas
             :refer
             [Ability Action BattleSpec CharacterId
              collect-effects-for-trigger EffectTrigger GameState
              get-acting-character get-characters get-modified-attributes
              HexTile Message path-to-acting-character path-to-character
              path-to-tile Sprite]]
   [yatg.specter-with-better-errors :as sp]
   [yatg.timeline :refer [get-next-tick-with-actions]]
   [yatg.utils :refer [get-by-id]]))

(rsa! :actions/log [:-> GameState Message GameState] log)

; ------------------- Overworld and Menu Navigation -----------------------

; Zoom in on a location.
(rsa! :actions/view-location
      [:-> GameState :keyword GameState]
      (fn [game-state location-id]
        (-> game-state
          (assoc :current-scene {:location-id location-id})
          (dissoc :current-scene :battle :battle-resolution))))

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
       [[:actions/prepare-turn character-id]
        [:actions/set-all-targetable-abilities character-id]]))

; Automatically perform a turn for a computer-controlled player.
(ra! :actions/perform-turn
     [:-> GameState :keyword [:sequential Action]]
     (fn [game-state character-id]
       [[:actions/prepare-turn character-id]
        [:actions/select-and-autoprime-ability]
        [:actions/use-primed-ability-and-complete-turn]]))

(ra! :actions/use-primed-ability-and-complete-turn
     [:-> GameState [:sequential Action]]
     (fn [game-state]
       [[:effects/execute-actions-with-delay
         [[:actions/play-primed-ability-animation]
          [:actions/ms-delay 50]
          [:actions/use-primed-ability]
          [:actions/complete-turn]]]]))

(ra! :actions/prepare-turn
     [:-> GameState CharacterId [:sequential Action]]
     (fn [game-state character-id]
       [[:actions/set-acting-character character-id]
        [:actions/trigger-effects :prepare-turn]]))

(ra! :actions/complete-turn
     [:-> GameState [:sequential Action]]
     (fn [game-state]
       [[:actions/trigger-effects :complete-turn]
        [:actions/grant-experience]
        [:actions/recompute-engagements]
        [:actions/set-acting-character nil]
        [:actions/clean-dead-characters]
        [:actions/check-battle-completion]
        [:actions/advance-timeline]]))

(rsa! :actions/grant-experience
      [:-> GameState GameState]
      (fn [{:keys [newly-dead-character-ids] :as game-state}]
        (let [acting-character (get-acting-character game-state)]
          (as-> game-state gs
            (sp/transform (path-to-acting-character game-state)
                          #(grant-experience-for-kills
                             %
                             (get-characters newly-dead-character-ids
                                             game-state))
                          gs)
            (log-str gs
                     (if (not (= (:level (get-acting-character gs))
                                 (:level acting-character)))
                       (str (:id acting-character)
                            " leveled up from " (:level acting-character)
                            " to " (:level (get-acting-character gs)))
                       nil))))))

(rsa! :actions/recompute-engagements
      [:-> GameState GameState]
      (fn [game-state]
        (sp/transform [:characters sp/ALL]
                      #(update-character-engagements % game-state)
                      game-state)))

(rsa! :actions/clean-dead-characters
      [:-> GameState GameState]
      (fn [game-state]
        (let [dead-character-ids (get-dead-character-ids game-state)]
          (as-> game-state gs
            (dissoc game-state :newly-dead-character-ids)
            (sp/transform [:current-scene :battle :hexgrid sp/ALL]
                          #(if (contains? dead-character-ids (:character-id %))
                             (dissoc % :character-id)
                             %)
                          gs)
            (sp/transform
              [:current-scene :battle :timeline :actions sp/MAP-VALS]
              (fn [actions]
                (into []
                      (remove #(not (empty? (intersection (set %)
                                                          dead-character-ids)))
                        actions)))
              gs)))))

(rsa!
  :actions/check-battle-completion
  [:-> GameState GameState]
  (fn [game-state]
    (let [remaining-characters
          (get-characters
            (remove nil?
              (sp/select [:current-scene :battle :hexgrid sp/ALL :character-id]
                         game-state))
            game-state)]
      (cond (empty? (filter #(= :with-player (:team %)) remaining-characters))
            (assoc-in game-state
              [:current-scene :battle-resolution :victory?]
              false)
            (empty? (remove #(= :with-player (:team %)) remaining-characters))
            (assoc-in game-state
              [:current-scene :battle-resolution :victory?]
              true)
            :else game-state))))

; ------------------- Effect Triggering -----------------------

(rsa! :actions/trigger-effects
      [:-> GameState EffectTrigger GameState
            (fn [game-state trigger]
              (apply-consequences (map :consequences
                                    (collect-effects-for-trigger
                                      (get-acting-character game-state)
                                      trigger))
                                  game-state))])

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
          (map (fn [{:keys [id] :as character}]
                 (let [attributes (get-modified-attributes character)]
                   [:change-stamina {:target-id id
                                     :amount    (:stamina-regen attributes)}]))
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
(rsa! :actions/prime-ability
      [:-> GameState Ability :keyword GameState]
      (fn [game-state ability target-tile-id]
        (prime-acting-character-ability
          (assoc ability :primed-args {:target-tile-id target-tile-id})
          game-state)))
(rsa! :actions/unprime-abilities [:-> GameState GameState] unprime-abilities)

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

; Select and prime the ability that the computer will use.
(rsa! :actions/select-and-autoprime-ability
      [:-> GameState GameState]
      (fn [game-state]
        (prime-acting-character-ability
          (select-and-autoprime-ability game-state)
          game-state)))

; ----------------- Ability Use and Animations ----------------------

(rsa! :actions/use-primed-ability
      [:-> GameState GameState]
      (fn [game-state]
        (use-ability game-state (get-primed-ability game-state))))

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
       (let [{:keys [animation-id]} (get-primed-ability game-state)]
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
