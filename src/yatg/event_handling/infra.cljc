(ns yatg.event-handling.infra
  "Contains core event handling infrastructure to be used to trigger more specific actions."
  (:require [yatg.battle :refer [generate-battle]]
            [yatg.hex-grid :refer [is-same-tile?]]
            [yatg.character :refer [prep-for-combat]]
            [yatg.schemas :refer [GameState]]
            [nexus.registry :as nxr :refer [register-action! register-effect!]]
            [com.rpl.specter :as s]))

; Shorthand to make writing these registrations more "defn"-like.
(def ra! nxr/register-action!)
(def re! nxr/register-effect!)
(defn rsa!
  [id swap-fn]
  (ra! id (fn [_store & args]
           [[:effects/swap #(apply swap-fn % args)]])))

; See https://github.com/cjohansen/nexus#nexus-at-a-glance for useful details
; about nexus.

(defn swap-store!
  {:malli/schema [:-> :any [:atom GameState] [:-> GameState GameState] nil]}
  [_ctx store action-fn]
  (swap! store action-fn))

(re! :effects/swap swap-store!)

; Do a swap, but wait ms first.
(re! :effects/swap-with-delay
     (fn [ctx store action-fn ms]
       #?(:clj (swap-store! ctx store action-fn) ; don't do any delay in clj
          :cljs (js/setTimeout #(swap-store! ctx store action-fn) ms))))

(re! :effects/log (fn [_ctx _store message] (prn message)))

(re! :effects/execute-sequential
     (fn [{:keys [dispatch]} {:keys [ms actions]}]
       #?(:clj (future (Thread/sleep ms)
                       (dispatch [:action/execute-sequential actions]))
          :cljs (js/setTimeout #(dispatch [:action/execute-sequential actions])
                               ms))))

; This action executes a list of actions sequentially with delay after each
; one like this:
;
; (def action-sequence
;   [{:action [:effects/log "Starting step 1..."] :delay-ms 1000}
;    {:action [:effects/log "Step 2 running after 1s..."] :delay-ms 2500}
;    {:action [:effects/log "Step 3 running after 2.5s!"] :delay-ms 0})
; 
; ;; Kick off the sequential chain
; (nexus/dispatch system [:actions/execute-sequential action-sequence])
(ra! :actions/execute-sequential
     (fn [_ [current-action & remaining-actions]]
       (when current-action
         (let [{:keys [action delay-ms]} current-action]
           (if (seq remaining-actions)
             [action
              [:effect/execute-sequential {:ms      delay-ms
                                           :actions remaining-actions}]]
             ; If we just have one action (left), we just execute it without
             ; any delay.
             [action])))))

(nxr/register-system->state! deref)

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
