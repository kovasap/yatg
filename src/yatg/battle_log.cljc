(ns yatg.battle-log 
  (:require
   [yatg.schemas :refer [GameState]]
   [clojure.data :refer [diff]]))

(defn log
  {:malli/schema [:-> :string GameState GameState]}
  [message game-state]
  (update-in game-state [:current-scene :battle :log]
             #(conj % message)))

(defn log-diff
  {:malli/schema [:-> :string GameState GameState GameState]}
  [description old-game-state new-game-state]
  (log (str description (diff old-game-state new-game-state)) new-game-state))
