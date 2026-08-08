(ns yatg.event-handling.actions
  (:require [yatg.event-handling.infra :refer [rsa! ra!]]
            [yatg.battle :refer [generate-battle]]
            [yatg.character :refer [prep-for-combat]]
            [yatg.schemas :refer [GameState]]
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
(rsa! :actions/select-tile
      (fn [game-state tile]
        (sp/setval [:current-scene
                    :battle
                    :hexgrid
                    sp/ALL
                    #(= (:id tile) (:id %))
                    :selected?]
                   true
                   game-state)))
(rsa! :actions/deselect-tile
      (fn [game-state tile]
        (sp/setval [:current-scene
                    :battle
                    :hexgrid
                    sp/ALL
                    #(= (:id tile) (:id %))
                    :selected?]
                   false
                   game-state)))

(ra! :actions/advance-timeline-one-tick
     (fn [game-state]
       (let [timeline      (get-in game-state [:current-scene :battle :timeline])
             new-tick      (inc (:current-tick timeline))]
         (concat 
           ; Tick our timeline forward.
           [:effects/swap #(assoc-in % [:current-scene :battle :timeline :current-tick] new-tick)]
           ; Then do all the actions at this new tick.
           (get (:actions timeline) new-tick [])))))

; Move along the timeline until we hit a tick with something on it.
(ra!
  :actions/advance-timeline
  (fn [game-state]
    (let [timeline (get-in game-state [:current-scene :battle :timeline])
          ; TODO change this to "next tick with actions that cannot be
          ; automatically done without human interaction"
          next-tick-with-actions (->> timeline
                                      (:actions)
                                      (keys)
                                      (filter #(> % (:current-tick timeline)))
                                      (apply min))]
      [:actions/execute-sequential
       (repeat (- next-tick-with-actions (:current-tick timeline))
               {:action [:actions/advance-timeline-one-tick] :delay-ms 200})])))
