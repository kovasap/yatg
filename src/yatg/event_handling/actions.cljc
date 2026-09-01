(ns yatg.event-handling.actions
  (:require
   [com.rpl.specter :as sp]
   [yatg.abilities.common
             :refer
             [find-primed-ability set-all-targetable-abilities
              use-primed-ability]]
   [yatg.battle :refer [start-battle]]
   [yatg.bot-behavior :refer [select-and-autoprime-ability]]
   [yatg.event-handling.infra :refer [ra! rsa!]]
   [yatg.schemas
             :refer
             [get-acting-character path-to-ability path-to-character-abilities
              path-to-tile]]
   [yatg.timeline :refer [get-next-tick-with-actions]]
   [yatg.utils :refer [get-by-id]]))

; Zoom in on a location.
(rsa! :actions/view-location
      (fn [game-state location-id]
        (assoc game-state :current-scene {:location-id location-id})))

; Go back to the overworld.
(rsa! :actions/view-overworld
   (fn [game-state]
     (assoc game-state :current-scene {:location-id nil})))

; Create and then start a battle.
(rsa! :actions/start-battle
      (fn [game-state battle-spec] (start-battle game-state battle-spec)))

; Select and deselect tiles.
(rsa! :actions/hover-tile
      (fn [game-state tile]
        (sp/transform (path-to-tile (:id tile))
                      #(assoc % :hovered? true)
                      game-state)))
(rsa! :actions/unhover-tile
      (fn [game-state tile]
        (sp/transform (path-to-tile (:id tile))
                      #(assoc % :hovered? false)
                      game-state)))

; Give the player a chance to command their character.
(rsa! :actions/start-player-turn
      (fn [{:keys [characters] :as game-state} character-id]
        (-> game-state
            (assoc-in [:current-scene :battle :acting-character-id]
                      character-id)
            (update-in [:current-scene :battle :hexgrid]
                       #(set-all-targetable-abilities
                          %
                          (get-by-id characters character-id)
                          game-state)))))

; Prime an ability manually (likely because of player input).
(rsa! :actions/prime-ability
      (fn [game-state ability target-tile-id]
        (sp/transform (path-to-ability (:id (get-acting-character game-state))
                                       (:id ability))
                      #(assoc % :pending-args {:target-tile-id target-tile-id})
                      game-state)))
(rsa! :actions/unprime-ability
      (fn [game-state]
        (sp/transform (path-to-character-abilities (:id (get-acting-character
                                                          game-state)))
                      #(dissoc % :pending-args)
                      game-state)))

; Select and prime the ability that the bot will use.
(rsa! :actions/select-and-autoprime-ability
      (fn [game-state]
        (let [primed-ability (select-and-autoprime-ability game-state)]
          (sp/setval (path-to-ability (:id (get-acting-character game-state))
                                      (:id primed-ability))
                     primed-ability
                     game-state))))

(rsa! :actions/use-primed-ability use-primed-ability)

(ra! :actions/play-primed-ability-animation
     (fn [game-state]
       (let [primed-ability (find-primed-ability game-state)
             acting-character (get-acting-character game-state)]
         [[:actions/set-animation-frame]])))

; Use an ability, then move to the next turn on the timeline.  This should be
; used over raw :actions/use-ability most of the time.
(ra! :actions/use-primed-ability-and-advance-timeline
     (fn [game-state]
       [[:actions/play-primed-ability-animation]
        [:actions/use-primed-ability]
        [:actions/advance-timeline]]))

(ra!
  :actions/perform-turn
  (fn [game-state]
    [[:actions/select-and-autoprime-ability]
     [:actions/use-primed-ability-and-advance-timeline]]))

(ra! :actions/advance-timeline-one-tick
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
                new-tick)]]
           ; Then do all the actions at this new tick.
           (get actions new-tick [])))))

; Move along the timeline until we hit a tick with something on it.
(ra! :actions/advance-timeline
     (fn [game-state]
       (let [{:keys [current-tick] :as timeline}
             (get-in game-state [:current-scene :battle :timeline])
             next-tick-with-actions (get-next-tick-with-actions timeline)]
         [[:actions/execute-sequential
           (repeat (if (nil? next-tick-with-actions)
                     1 ; if there is no more stuff in the timeline, just
                       ; advance one tick at a time.
                     (- next-tick-with-actions current-tick))
                   {:action   [:actions/advance-timeline-one-tick]
                    :delay-ms 200})]])))
