(ns yatg.events
  (:require [yatg.battle :refer [generate-battle]]
            [yatg.hex-grid :refer [is-same-tile?]]
            [yatg.character :refer [prep-for-combat]]
            [com.rpl.specter :as s]))

(declare handle-actions)
(def handler-fns
  {; Zoom in on a location.
   :view-location    (fn [store location-id]
                       (assoc store :current-scene {:location-id location-id}))
   ; Go back to the overworld.
   :view-overworld   (fn [store]
                       (assoc store :current-scene {:location-id nil}))
   :start-battle     (fn [store]
                       (-> store
                           (update :characters #(mapv prep-for-combat %))
                           (assoc-in [:current-scene :battle]
                                     (generate-battle 10 10 [:rando :adam]))))
   ; Select and deselect tiles.
   :select-tile      (fn [store tile]
                       (s/setval [:current-scene
                                  :battle
                                  :hexgrid
                                  s/ALL
                                  #(is-same-tile? tile %)
                                  :selected?]
                                 true
                                 store))
   :deselect-tile    (fn [store tile]
                       (s/setval [:current-scene
                                  :battle
                                  :hexgrid
                                  s/ALL
                                  #(is-same-tile? tile %)
                                  :selected?]
                                 false
                                 store))
   :advance-timeline (fn [store]
                       (let [timeline (get-in store [:current-scene :battle :timeline])
                             new-tick (inc (:current-tick timeline))
                             actions-to-do (get (:actions timeline) new-tick [])]
                         (handle-actions store actions-to-do)
                         (assoc-in
                           store
                           [:current-scene :battle :timeline :current-tick]
                           new-tick)))
   :print (fn [store message]
            (print message)
            store)
   :delay            (fn [store ms actions]
                       #?(:clj (handle-actions store actions) ; don't do any
                                                              ; delay in clj
                          :cljs (js/setTimeout #(handle-actions store actions)
                                               ms)))})


; We can't recursively handle actions here because when we are in the handler
; functions we are not dealing with an atom anymore that we can mutate, but
; instead an actual store value.
;
; I think what I need to do here is instead switch to nexus and use
; `nxr/register-expansion!` to dynamically create actions from other actions.
;
; To get around the main issue i had with nexus before I think I can just be
; liberal with creating a bunch of effects (e.g. for all the handler functions
; above) and not worry about having a small set of them that are generalized.
            

(defn get-handler-fn
  [action]
  (get handler-fns
       action
       ; Log the action and do nothing if the action isn't recognized
       #(do (prn "no such action " action) %)))
   
(defn handle-action
  "Mutate the store based on the event."
  {:malli/schema [:-> [:atom schemas/GameState] :keyword ...]}
  [store action & args]
  ; Kinda ugly but we have two layers of lists of args for some reason
  (apply apply swap! store (get-handler-fn action) args))

(defn handle-actions
  [store actions-with-args]
  (doseq [[action & args] actions-with-args]
    (handle-action store action args)))
