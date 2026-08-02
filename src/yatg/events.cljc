(ns yatg.events
  (:require [yatg.battle :refer [generate-battle]]
            [yatg.hex-grid :refer [is-same-tile?]]
            [com.rpl.specter :as s]))

(def handler-fns
  {; Zoom in on a location.
   :view-location  (fn [store location-id]
                     (assoc store :current-scene {:location-id location-id}))
   ; Go back to the overworld.
   :view-overworld (fn [store] (assoc store :current-scene {:location-id nil}))
   :start-battle   (fn [store]
                     (assoc-in store
                       [:current-scene :battle]
                       (generate-battle 10 10)))
   ; Select and deselect tiles.
   :select-tile    (fn [store tile]
                     (s/setval [:current-scene
                                :battle
                                :hexgrid
                                s/ALL
                                #(is-same-tile? tile %)
                                :selected?]
                               true
                               store))
   :deselect-tile  (fn [store tile]
                     (s/setval [:current-scene
                                :battle
                                :hexgrid
                                s/ALL
                                #(is-same-tile? tile %)
                                :selected?]
                               false
                               store))})

(defn get-handler-fn
  [action]
  (get handler-fns
       action
       ; Log the action and do nothing if the action isn't recognized
       #(do (prn "no such action " action) %)))
  
   
(defn handle-event
  "Mutate the store based on the event."
  {:malli/schema [:-> [:atom schemas/GameState] :keyword ...]}
  [store action & args]
  ; Kinda ugly but we have two layers of lists of args for some reason
  (apply apply swap! store (get-handler-fn action) args))
