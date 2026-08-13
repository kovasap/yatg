(ns yatg.scenes
  (:require [portfolio.replicant :refer-macros [defscene]]
            [portfolio.ui :as portfolio]
            [yatg.ui.overworld :refer [render-overworld]]
            [yatg.ui.battle :refer [render-battle]]
            [yatg.battle :refer [generate-battle]]
            [yatg.character :refer [generate-character]]
            [yatg.event-handling.infra]
            [yatg.event-handling.actions]
            [nexus.registry :as nxr]
            [replicant.dom :as r]
            [dataspex.core :as dataspex]
            [malli.dev.cljs :as malli-dev]
            [malli.dev.pretty :as pretty]))

(defscene overworld
          (render-overworld {:locations [{:id :capitol
                                          :display-name "Capitol City"
                                          :path-to-img "castle.png"
                                          :screen-coordinates {:x 400 :y 400}}]
                             :overworld {:path-to-svg "overworld.svg"}}))

(def sprite-templates
  [{:animations [{:frame-img-paths ["class-images/assassin/attack/1.png"
                                    "class-images/assassin/attack/2.png"]
                  :id :attack}
                 {:frame-img-paths ["class-images/assassin/idle.png"]
                  :id :idle}]
    :id         :assassin}])

(defscene battle
          :params
          (atom
            (let [characters [(generate-character :they true sprite-templates)]
                  battle     (generate-battle 3 3 characters)]
              {:current-scene {:battle battle} :characters characters}))
          [store portfolio-opts]
          (dataspex/inspect "Game state" store)
          (r/set-dispatch! #(nxr/dispatch store %1 %2))
          (render-battle (:battle (:current-scene @store))))
                           
(defn main []
  (malli-dev/start! {:report (pretty/reporter)})
  (portfolio/start!
   {:config
    {:css-paths ["/styles.css"]
     :viewport/defaults
     {:background/background-color "#fdeddd"}}}))
