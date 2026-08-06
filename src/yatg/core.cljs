(ns yatg.core
  (:require [replicant.dom :as r]
            [yatg.events :refer [handle-event]]
            [yatg.init :refer [create-initial-store]]
            [yatg.ui.core :refer [render-game]]
            [yatg.asset-manifest :refer [load-asset-manifest!]]
            [yatg.sprite :refer [load-sprite-templates]]))

(defn start-new-game [store]
  (reset! store (create-initial-store)))

(defn main
  [store]
  ;; Globally handle DOM events
  (r/set-dispatch! (fn [_ [action & args]] (handle-event store action args)))
  (load-asset-manifest!
    (fn [manifest]
      (swap! store assoc :asset-manifest manifest)
      (swap! store assoc :sprite-templates (load-sprite-templates manifest))))
  ;; Render on every change
  (add-watch store
             ::render
             (fn [_ _ _ store]
               (->> (render-game store)
                    (r/render (js/document.getElementById "app"))))))
