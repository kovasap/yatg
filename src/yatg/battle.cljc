(ns yatg.battle
  (:require [yatg.schemas :as schemas]))

(defn generate-battle
  {:malli/schema [:-> :int :int [:vector :keyword] schemas/Battle]}
  [num-rows num-cols participating-character-ids]
  ; TODO add characters to the map
  {:hexgrid (reduce concat
              (for [row-idx (range num-rows)]
                (for [col-idx (range num-cols)]
                  {:row-idx row-idx :col-idx col-idx
                   :character nil})))
   :timeline {:current-tick 0
              :actions {2 [[:print "hi"]]}}})
