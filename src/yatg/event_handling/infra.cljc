(ns yatg.event-handling.infra
  "Contains core event handling infrastructure to be used to trigger more specific actions."
  (:require [yatg.schemas :refer [GameStateAtom GameState]]
            [nexus.registry :as nxr]))

; Shorthand to make writing these registrations more "defn"-like.
(def ra! nxr/register-action!)
(def re! nxr/register-effect!)
(defn register-swap-action!
  [id swap-fn]
  (ra! id (fn [_store & args]
           [[:effects/swap id #(apply swap-fn % args)]])))
(def rsa! register-swap-action!)

; See https://github.com/cjohansen/nexus#nexus-at-a-glance for useful details
; about nexus.

(defn swap-store!
  {:malli/schema [:-> :any GameStateAtom [:-> GameState GameState] :nil]}
  ; The action-id exists here just to get a more clear description in dataspex
  [_ctx store _action-id action-fn]
  (swap! store action-fn))

(re! :effects/swap swap-store!)

; Do a swap, but wait ms first.
(re! :effects/swap-with-delay
     (fn [ctx store action-id action-fn ms]
       #?(:clj (swap-store! ctx store action-id action-fn) ; don't do any
                                                           ; delay in clj
          :cljs (js/setTimeout #(swap-store! ctx store action-id action-fn)
                               ms))))

(re! :effects/log (fn [_ctx _store message] (prn message)))

(re! :effects/execute-sequential
     (fn [{:keys [dispatch]} _store {:keys [ms actions]}]
       #?(:clj (future (Thread/sleep ms)
                       (dispatch [[:actions/execute-sequential actions]]))
          :cljs (js/setTimeout #(dispatch [[:actions/execute-sequential
                                            actions]])
                               ms))))

; See https://clojurians.slack.com/archives/C06JZ4X334N/p1786140926937659
; for more context and a better way to do this.
;
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
              [:effects/execute-sequential {:ms      delay-ms
                                            :actions remaining-actions}]]
             ; If we just have one action (left), we just execute it
             ; without any delay.
             [action])))))

(nxr/register-system->state! deref)
