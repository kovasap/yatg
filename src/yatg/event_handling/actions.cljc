(ns yatg.event-handling.actions
  (:require [yatg.event-handling.infra :refer [rsa! ra!]]
            [yatg.battle :refer [start-battle]]
            [yatg.schemas
             :refer
             [get-acting-character
              path-to-tile
              path-to-ability
              path-to-character-abilities]]
            [yatg.utils :refer [get-by-id]]
            [yatg.timeline :refer [get-next-tick-with-actions]]
            [yatg.abilities.common
             :refer
             [use-pending-ability set-all-targetable-abilities]]
            [com.rpl.specter :as sp]))

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
                          (get-by-id characters character-id))))))

; Show what the ability usage would do.  Useful when hovering an ability
(rsa! :actions/preview-ability
      (fn [game-state ability target-tile-id]
        (sp/transform (path-to-ability (:id (get-acting-character game-state))
                                       (:id ability))
                      #(assoc % :pending-args {:target-tile-id target-tile-id})
                      game-state)))
(rsa! :actions/unpreview-ability
      (fn [game-state]
        (sp/transform (path-to-character-abilities (:id (get-acting-character
                                                          game-state)))
                      #(dissoc % :pending-args)
                      game-state)))

; Use an ability.  Useful when clicking an ability.
#_(rsa! :actions/use-ability
        (fn [game-state ability target-tile]
          (-> game-state
              (update-in [:current-scene :battle :hexgrid]
                         clear-all-targetable-abilities))))

(rsa! :actions/use-pending-ability use-pending-ability)

; Use an ability, then move to the next turn on the timeline.  This should be
; used over raw :actions/use-ability most of the time.
(ra! :actions/use-pending-ability-and-advance-timeline
     (fn [game-state]
       [[:actions/use-pending-ability]
        [:actions/advance-timeline]]))

; Automatically perform a turn for a non-player-controlled character.
(rsa! :actions/perform-turn
      (fn [game-state character-id]
        (assoc-in game-state
          [:current-scene :battle :acting-character-id]
          character-id)))

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
