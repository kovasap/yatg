(ns yatg.scenes
  (:require [portfolio.replicant :refer-macros [defscene]]
            [portfolio.ui :as portfolio]
            [yatg.ui.overworld :refer [render-overworld]]
            [yatg.ui.battle :refer [render-battle]]
            [yatg.battle :refer [start-battle]]
            [yatg.character :refer [generate-character]]
            [nexus.action-log :as action-log]
            [yatg.event-handling.infra]
            [yatg.event-handling.actions]
            [nexus.registry :as nxr]
            [replicant.dom :as r]
            [dataspex.core :as dataspex]
            [malli.dev.cljs :as malli-dev]
            [yatg.malli-utils :refer [custom-reporter]]))

(defscene overworld
          (render-overworld {:locations [{:id :capitol
                                          :display-name "Capitol City"
                                          :path-to-img "castle.png"
                                          :screen-coordinates {:x 400 :y 400}}]
                             :overworld {:path-to-svg "overworld.svg"}}))

(def base-game-state
  {:asset-manifest {:image-filepaths []}
   :locations []
   :overworld {:path-to-svg "dummy"}
   :current-scene {:location-id :here}
   :sprite-templates
    ; this code renders under the /portfolio/ path, so we use ../ to get at the
    ; images relative to root.
    [{:animations [{:frame-img-paths ["../class-images/assassin/attack/1.png"
                                      "../class-images/assassin/attack/2.png"]
                    :id :attack}
                   {:frame-img-paths ["../class-images/assassin/idle.png"]
                    :id :idle}]
      :id         :assassin}]})

(defscene battle
          :params
          (atom (-> base-game-state
                    (assoc :characters [(generate-character
                                          :they :assassin
                                          true  (:sprite-templates
                                                  base-game-state))])
                    (start-battle {:display-name "test" :rows 3 :cols 3
                                   :num-enemies 1})))
          [store]
          (dataspex/inspect "Game state"
                            store
                            {:track-changes? true :history-limit 25})
          (r/set-dispatch! #(nxr/dispatch store %1 %2))
          (render-battle (:battle (:current-scene @store)) @store))

(defn main []
  (malli-dev/start! {:report custom-reporter})
  (action-log/inspect {:max-age {:hours 3}})
  (portfolio/start!
   {:config
    {:css-paths ["/styles.css"]
     :viewport/defaults
     {:background/background-color "#fdeddd"}}}))
