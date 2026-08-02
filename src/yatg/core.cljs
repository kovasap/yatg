(ns yatg.core
  (:require [replicant.dom :as r]
            [yatg.game :as game]
            [yatg.ui :as ui]))

(defn start-new-game [store]
  (reset! store (game/create-initial-store)))

(defn main [store]
  (let [el (js/document.getElementById "app")]

    ;; Globally handle DOM events
    (r/set-dispatch!
     (fn [_ [action & args]]
       (case action
         :view-location (apply swap! store game/view-location args)
         :reset (start-new-game store))))

    ;; Render on every change
    (add-watch store ::render
               (fn [_ _ _ store]
                 (->> (ui/render-game store)
                      (r/render el))))))
