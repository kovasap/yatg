(ns yatg.scenes
  (:require [portfolio.replicant :refer-macros [defscene]]
            [portfolio.ui :as portfolio]
            [yatg.ui.overworld :refer [render-overworld]]
            [yatg.ui.battle :refer [render-battle]]
            [yatg.battle :refer [generate-battle]]
            [yatg.character :refer [generate-character]]
            [nexus.registry :as nxr]
            [replicant.dom :as r]
            [dataspex.core :as dataspex]
            [malli.dev.cljs :as dev]
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

(defscene
  battle
  (render-battle
    (generate-battle 3 3 [(generate-character :they true sprite-templates)])))
                           
(def store (atom nil))
  
(defn main []
  (dataspex/inspect "Game state" store)
  (dev/start! {:report (pretty/reporter)})
  (r/set-dispatch! #(nxr/dispatch store %1 %2))
  (portfolio/start!
   {:config
    {:css-paths ["/styles.css"]
     :viewport/defaults
     {:background/background-color "#fdeddd"}}}))
