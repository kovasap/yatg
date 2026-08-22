(ns yatg.timeline
  (:require [yatg.schemas
             :refer
             [Timeline Action Battle BattleSpec Character HexGrid GameState]]))

(defn place-move
  "Add a specific action to a specific tick-offset past the current-tick
  on the timeline.
  If that tick already contains actions, try to add to the next tick until we
  find an empty one."
  {:malli/schema [:-> Timeline Action :int Timeline]}
  [timeline action tick-offset]
  (loop [{:keys [actions] :as cur-timeline} timeline
         cur-tick (+ tick-offset (:current-tick timeline))]
    (let [cur-actions (get actions cur-tick [])]
      (if (empty? cur-actions)
        (assoc-in cur-timeline [:actions cur-tick] [action])
        (recur cur-timeline (inc cur-tick))))))

(defn place-next-move
  {:malli/schema [:-> Timeline Character Timeline]}
  [timeline {:keys [id controlled-by-player?] {:keys [speed]} :resources}]
  (place-move timeline
              (if controlled-by-player?
                [:actions/start-player-turn id]
                [:actions/perform-turn id])
              speed))
  
(defn place-first-moves
  {:malli/schema [:-> Timeline [:vector Character] Timeline]}
  [timeline characters]
  (loop [cur-timeline         timeline
         remaining-characters (sort-by #(:speed (:resources %)) characters)]
    (if (empty? remaining-characters)
      cur-timeline
      (recur (place-next-move cur-timeline (first remaining-characters))
             (rest remaining-characters)))))

(defn get-next-tick-with-actions
  {:malli/schema [:-> Timeline [:maybe :int]]}
  [{:keys [actions current-tick]}]
  (->> actions
       (filter (fn [[k v]]
                 (and (> k current-tick)
                      ; allow all actions to automatically
                      ; process except when a character the
                      ; player controls starts their turn
                      (contains? (set (map first v))
                                 :actions/start-player-turn))))
       (keys)
       (apply min)))
