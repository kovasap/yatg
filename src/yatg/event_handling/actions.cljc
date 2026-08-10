(ns yatg.event-handling.actions
  (:require [yatg.event-handling.infra :refer [rsa! ra!]]
            [yatg.battle :refer [generate-battle]]
            [yatg.character :refer [prep-for-combat]]
            [yatg.schemas
             :refer
             [get-acting-character-id path-to-tile path-to-ability]]
            [yatg.utils :refer [get-by-id]]
            [yatg.abilities.common
             :refer
             [clear-all-targetable-abilities set-all-targetable-abilities]]
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
      (fn [game-state]
        (let [prepped-characters (mapv prep-for-combat (:characters game-state))]
          (-> game-state
              (assoc :characters prepped-characters)
              (assoc-in [:current-scene :battle]
                        ; TODO select a subset of characters somehow
                        (generate-battle 10 10 prepped-characters))))))

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
      (fn [game-state character-id]
        (->
          game-state
          (assoc-in [:current-scene :battle :acting-character-id] character-id)
          (update-in [:current-scene :battle :hexgrid]
                     #(set-all-targetable-abilities
                        %
                        (get-by-id (:characters game-state) character-id))))))

; Show what the ability usage would do.  Useful when hovering an ability
(rsa! :actions/preview-ability
      (fn [game-state ability]
        (sp/transform (path-to-ability
                        (get-acting-character-id game-state)
                        (:id ability))
                      #(assoc % :previewed? true)
                      game-state)))
(rsa! :actions/unpreview-ability
      (fn [game-state ability]
        (sp/transform (path-to-ability
                        (get-acting-character-id game-state)
                        (:id ability))
                      #(assoc % :previewed? true)
                      game-state)))

; Use an ability.  Useful when clicking an ability.
(rsa! :actions/use-ability
      (fn [game-state ability]
        (-> game-state
            (update-in [:current-scene :battle :hexgrid]
                       clear-all-targetable-abilities))))

; Automatically perform a turn for a non-player-controlled character.
(rsa! :actions/perform-turn
      (fn [game-state character-id]
        (assoc-in game-state
          [:current-scene :battle :acting-character-id]
          character-id)))

(ra! :actions/advance-timeline-one-tick
     (fn [game-state]
       (let [timeline (get-in game-state [:current-scene :battle :timeline])
             new-tick (inc (:current-tick timeline))]
         (concat
           ; Tick our timeline forward.
           [[:effects/swap
             :tick-forward
             #(assoc-in %
                [:current-scene :battle :timeline :current-tick]
                new-tick)]]
           ; Then do all the actions at this new tick.
           (get (:actions timeline) new-tick [])))))

; Move along the timeline until we hit a tick with something on it.
(ra! :actions/advance-timeline
     (fn [game-state]
       (let [{:keys [actions current-tick]}
             (get-in game-state [:current-scene :battle :timeline])
             next-tick-with-actions
             (->> actions
                  (filter (fn [[k v]]
                            (and (> k current-tick)
                                 ; allow all actions to automatically
                                 ; process except when a character the
                                 ; player controls starts their turn
                                 (contains? (set (map first v))
                                            :actions/start-player-turn))))
                  (keys)
                  (apply min))]
         [[:actions/execute-sequential
           (repeat (if (nil? next-tick-with-actions)
                     1 ; if there is no more stuff in the timeline, just
                       ; advance one tick at a time.
                     (- next-tick-with-actions current-tick))
                   {:action   [:actions/advance-timeline-one-tick]
                    :delay-ms 200})]])))
