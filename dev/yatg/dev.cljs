(ns yatg.dev
  (:require [dataspex.core :as dataspex]
            [yatg.core :as yatg]))

(def store (atom nil))

(defn ^:dev/after-load configure []
  (dataspex/inspect "Game state" store)
  (yatg/main store))

(defn main []
  (configure)
  ;; Trigger the first render by initializing the game.
  (yatg/start-new-game store))
