(ns yatg.battle
  (:require [yatg.hex-grid :refer [generate-hexgrid]]
            [yatg.schemas :refer [Timeline Battle Character HexGrid]]
            [com.rpl.specter :as sp]))

(defn place-characters-on-map
  {:malli/schema [:-> HexGrid [:vector Character] HexGrid]}
  [hexgrid characters]
  (let [friendly-col 4
        enemy-col    6
        max-per-col  3
        starting-row 3]
    (loop [cur-grid             hexgrid
           enemies-placed       0
           friendlies-placed    0
           remaining-characters characters]
      (let [{:keys [id controlled-by-player?]} (first remaining-characters)
            row-to-place (+ starting-row
                            (mod (if controlled-by-player?
                                   friendlies-placed
                                   enemies-placed)
                                 max-per-col))
            col-to-place (if controlled-by-player?
                           (- friendly-col
                              (quot friendlies-placed max-per-col))
                           (+ enemy-col (quot enemies-placed max-per-col)))]
        (if (nil? id)
          cur-grid
          (recur (sp/setval [sp/ALL
                             (fn [{:keys [row-idx col-idx]}]
                               (and (= row-idx row-to-place)
                                    (= col-idx col-to-place)))
                             :character-id]
                            id
                            cur-grid)
                 (if controlled-by-player? enemies-placed (inc enemies-placed))
                 (if controlled-by-player?
                   (inc friendlies-placed)
                   friendlies-placed)
                 (rest remaining-characters)))))))

(defn place-move-on-timeline
  "Add a specific action to a specific tick-offset past the current-tick
  on the timeline.
  If that tick already contains actions, try to add to the next tick until we
  find an empty one."
  [timeline action tick-offset]
  (loop [{:keys [actions] :as cur-timeline} timeline
         cur-tick (+ tick-offset (:current-tick timeline))]
    (let [cur-actions (get actions cur-tick [])]
      (if (empty? cur-actions)
        (assoc-in cur-timeline [:actions cur-tick] [action])
        (recur cur-timeline (inc cur-tick))))))

(defn place-first-moves-on-timeline
  {:malli/schema [:-> Timeline [:vector Character] Timeline]}
  [timeline characters]
  (loop [cur-timeline         timeline
         remaining-characters (sort-by #(:speed (:resources %)) characters)]
    (let [{:keys [id controlled-by-player?] {:keys [speed]} :resources}
          (first remaining-characters)]
      (if (nil? id)
        cur-timeline
        (recur (place-move-on-timeline cur-timeline
                                       (if controlled-by-player?
                                         [:actions/start-player-turn id]
                                         [:actions/perform-turn id])
                                       speed)
               (rest remaining-characters))))))

(defn generate-battle
  {:malli/schema [:-> :int :int [:vector Character] Battle]}
  [num-rows num-cols participating-characters]
  {:hexgrid  (-> (generate-hexgrid num-rows num-cols)
                 (place-characters-on-map participating-characters))
   :acting-character-id nil
   :timeline (-> {:current-tick 0 :actions {}}
                 (place-first-moves-on-timeline participating-characters))})

