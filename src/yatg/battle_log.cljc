(ns yatg.battle-log 
  (:require
   [clojure.data :refer [diff]]
   [yatg.schemas :refer [GameState get-current-tick Message]]))

(defn log-str
  {:malli/schema [:-> GameState [:maybe :string] GameState]}
  [game-state message-str]
  (if (nil? message-str)
    game-state  ; do nothing
    (update-in game-state [:current-scene :battle :log]
               #(conj % {:tick (get-current-tick game-state)
                         :message message-str}))))

(defn log
  {:malli/schema [:-> GameState Message GameState]}
  [game-state message]
  (update-in game-state [:current-scene :battle :log]
             #(conj % (assoc message :tick (get-current-tick game-state)))))

(defn log-diff
  {:malli/schema [:-> :string GameState GameState GameState]}
  [description old-game-state new-game-state]
  (log new-game-state
       {:message (str description (diff old-game-state new-game-state))}))
