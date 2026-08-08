(ns yatg.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [replicant.dom :as r]
            [cljs.core.async :refer [<!]]
            [yatg.init :refer [initialize-store]]
            [yatg.ui.core :refer [render-game]]
            [yatg.asset-manifest :refer [load-asset-manifest!]]
            [yatg.sprite :refer [load-sprite-templates]]
            [nexus.registry :as nxr]
            ; Make sure nxr registry code is executed.
            [yatg.event-handling.infra]
            [yatg.event-handling.actions]))
 
(defn main
  "Takes in an atom storing game state and sets up the rendering loop."
  [store]
  ;; Globally handle DOM events
  (r/set-dispatch! #(nxr/dispatch store %1 %2))
  ;; Render on every change
  (add-watch store
             ::render
             (fn [_ _ _ store]
               (->> (render-game store)
                    (r/render (js/document.getElementById "app")))))
  ; This is async code, so it goes last so we can let it take as long as
  ; it needs without depending on it
  (go (<! (load-asset-manifest!
            (fn [manifest]
              (reset! store 
                      (-> {:asset-manifest manifest}
                          (assoc :sprite-templates (load-sprite-templates manifest))
                          (initialize-store))))))))
