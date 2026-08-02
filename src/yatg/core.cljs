(ns yatg.core
  (:require [replicant.dom :as r]
            [yatg.game :as game]
            [yatg.ui.core :refer [render-game]]))

(defn start-new-game [store]
  (reset! store (game/create-initial-store)))

(defn main [store]
  (let [el (js/document.getElementById "app")]

    ;; Globally handle DOM events
    (r/set-dispatch!
     (fn [_ [action & args]]
       (case action
         :view-location (apply swap! store game/view-location args)
         :view-overworld (swap! store game/view-overworld)
         :start-battle (swap! store game/start-battle)
         :select-tile (apply swap! store game/select-tile args)
         :deselect-tile (apply swap! store game/deselect-tile args)
         :reset (start-new-game store))))

    ;; Render on every change
    (add-watch store ::render
               (fn [_ _ _ store]
                 (->> (render-game store)
                      (r/render el))))))
