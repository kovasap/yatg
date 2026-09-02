(ns yatg.event-handling.infra
  "Contains core event handling infrastructure to be used to trigger more specific actions."
  (:require [yatg.utils :refer [insert-at]]
            [malli.core :as m]
            [malli.dev.pretty :as pretty]
            [nexus.registry :as nxr]
            [yatg.schemas :refer [GameState GameStateAtom]]))

(defn instrument
  [id schema f]
  (m/-instrument {:schema (insert-at schema 1 {:title id})
                  :report (pretty/reporter)}
                 f))

; Shorthand to make writing these registrations more "defn"-like.
(defn ra!
  [action-k schema f]
  (nxr/register-action! action-k (instrument action-k schema f)))
(def re! nxr/register-effect!)
(defn register-swap-action!
  [id schema swap-fn]
  (nxr/register-action! id
    (fn [_store & args]
      [[:effects/swap id #(apply (instrument id schema swap-fn) % args)]])))
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

; Use like this:
;
; [:effects/execute-actions-with-delay
;  [[:actions/first]
;   [:actions/delay-ms 50]
;   [:actions/second]
;   [:actions/delay-ms 50]
;   [:actions/third arg]])
(re! :effects/execute-actions-with-delay
     (fn [{:keys [dispatch]} _store actions]
       (when (seq actions)
         (let [current-action (first actions)
               execute-rest-effect [:effects/execute-actions-with-delay
                                    (rest actions)]
               dispatch-rest       #(dispatch [execute-rest-effect])]
           (if (= :actions/ms-delay (first current-action))
             (let [ms (second current-action)]
               #?(:clj (future (Thread/sleep ms) (dispatch-rest))
                  :cljs (js/setTimeout dispatch-rest ms)))
             (dispatch [current-action execute-rest-effect]))))))

(defn interleave-delay
  "Helpful function to create input for :effects/execute-actions-with-delay."
  [actions ms-delay]
  [:effects/execute-actions-with-delay
   (drop-last (interleave actions (repeat [:actions/ms-delay ms-delay])))])

(nxr/register-system->state! deref)
