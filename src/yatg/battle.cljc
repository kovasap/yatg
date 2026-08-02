(ns yatg.battle
  (:require [yatg.schemas :as schemas]))

(defn generate-battle
  {:malli/schema [:-> :int :int schemas/Battle]}
  [num-rows num-cols]
  {:hexgrid (reduce concat
              (for [row-idx (range num-rows)]
                (for [col-idx (range num-cols)]
                  {:row-idx row-idx :col-idx col-idx})))})
