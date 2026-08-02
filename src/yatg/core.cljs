(ns yatg.core
  (:require [replicant.dom :as r]
            [yatg.init :refer [initialize-store]]
            [yatg.ui.core :refer [render-game]]
            [yatg.asset-manifest :refer [load-asset-manifest!]]
            [yatg.sprite :refer [load-sprite-templates]]
            [nexus.registry :as nxr]
            [yatg.events])) ; Make sure nxr registry code is executed.
 
(defn main
  [store]
  ;; Globally handle DOM events
  (r/set-dispatch! #(nxr/dispatch store %1 %2))
  (load-asset-manifest!
    (fn [manifest]
      (swap! store assoc :asset-manifest manifest)
      (swap! store assoc :sprite-templates (load-sprite-templates manifest))))
  (swap! store initialize-store)
  ;; Render on every change
  (add-watch store
             ::render
             (fn [_ _ _ store]
               (->> (render-game store)
                    (r/render (js/document.getElementById "app"))))))
