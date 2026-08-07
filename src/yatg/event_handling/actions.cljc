(ns yatg.event-handling.actions
  (:require [yatg.event-handling.infra :refer [rsa! ra!]]
            [yatg.battle :refer [generate-battle]]
            [yatg.hex-grid :refer [is-same-tile?]]
            [yatg.character :refer [prep-for-combat]]
            [yatg.schemas :refer [GameState]]
            [com.rpl.specter :as s]))

; Zoom in on a location.
(rsa! :actions/view-location
      (fn [store location-id]
        (assoc store :current-scene {:location-id location-id})))

; Go back to the overworld.
(rsa! :actions/view-overworld
   (fn [store]
     (assoc store :current-scene {:location-id nil})))

; Create and then start a battle.
(rsa! :actions/start-battle
      (fn [store]
        (-> store
             (update :characters #(mapv prep-for-combat %))
             (assoc-in [:current-scene :battle]
                      (generate-battle 10 10 [:rando :adam])))))

; Select and deselect tiles.
(rsa!
  :actions/select-tile
  (fn [store tile]
    (s/setval
      [:current-scene :battle :hexgrid s/ALL #(is-same-tile? tile %) :selected?]
      true
      store)))
(rsa!
  :actions/deselect-tile
  (fn [store tile]
    (s/setval
      [:current-scene :battle :hexgrid s/ALL #(is-same-tile? tile %) :selected?]
      false
      store)))

(ra! :actions/advance-timeline-one-tick
     (fn [store]
       (let [timeline      (get-in store [:current-scene :battle :timeline])
             new-tick      (inc (:current-tick timeline))]
         (concat 
           ; Tick our timeline forward.
           [:effects/swap #(assoc-in % [:current-scene :battle :timeline :current-tick] new-tick)]
           ; Then do all the actions at this new tick.
           (get (:actions timeline) new-tick [])))))

; Move along the timeline until we hit a tick with something on it.
(ra!
  :actions/advance-timeline
  (fn [store]
    (let [timeline (get-in store [:current-scene :battle :timeline])
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
