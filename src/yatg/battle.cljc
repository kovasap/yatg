(ns yatg.battle
  (:require
    [yatg.hex-grid :refer [generate-hexgrid row-count col-count]]
    [yatg.timeline :refer [place-first-moves]]
    [yatg.schemas :refer [Battle BattleSpec Character HexGrid GameState]]
    [com.rpl.specter :as sp]
    [yatg.character :refer [prep-for-combat]]))

(defn place-characters-on-map
  {:malli/schema [:-> HexGrid [:vector Character] HexGrid]}
  [hexgrid characters]
  (let [rows         (row-count hexgrid)
        cols         (col-count hexgrid)
        friendly-col (quot cols 3)
        enemy-col    (* 2 (quot cols 3))
        max-per-col  (- rows 2)
        starting-row 1]
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

(defn generate-battle
  {:malli/schema [:-> BattleSpec [:vector Character] Battle]}
  [{:keys [rows cols]} participating-characters]
  {:hexgrid  (-> (generate-hexgrid rows cols)
                 (place-characters-on-map participating-characters))
   :timeline (-> {:current-tick 0 :actions {}}
                 (place-first-moves participating-characters))})

(defn start-battle
  {:malli/schema [:-> GameState BattleSpec GameState]}
  [store spec]
  (let [prepped-characters (mapv prep-for-combat (:characters store))]
    (-> store
        (assoc :characters prepped-characters)
        (assoc-in [:current-scene :battle]
                  ; TODO select a subset of characters somehow
                  (generate-battle spec prepped-characters)))))
