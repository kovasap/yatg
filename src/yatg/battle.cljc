(ns yatg.battle
  (:require [yatg.schemas :as schemas]
            [yatg.hex-grid :refer [generate-hexgrid HexGrid]]
            [yatg.character :refer [Character]]
            [com.rpl.specter :as sp]))

(defn place-characters
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

(defn generate-battle
  {:malli/schema [:-> :int :int [:vector Character] schemas/Battle]}
  [num-rows num-cols participating-characters]
  {:hexgrid  (-> (generate-hexgrid num-rows num-cols)
                 (place-characters participating-characters))
   :timeline {:current-tick 0 :actions {2 [[:effects/log "hi"]]}}})

