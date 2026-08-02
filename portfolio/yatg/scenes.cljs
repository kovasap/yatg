(ns yatg.scenes
  (:require [portfolio.replicant :refer-macros [defscene]]
            [portfolio.ui :as portfolio]
            [yatg.ui :as ui]))

(defscene overworld
          (ui/render-overworld {:locations [{:id :capitol
                                             :display-name "Capitol City"
                                             :path-to-img "castle.png"
                                             :screen-coordinates {:x 400
                                                                  :y 400}}]
                                :overworld {:path-to-svg "overworld.svg"}}))

(defn main []
  (portfolio/start!
   {:config
    {:css-paths ["/styles.css"]
     :viewport/defaults
     {:background/background-color "#fdeddd"}}}))
