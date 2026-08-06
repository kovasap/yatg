(ns yatg.hex-grid
  (:require [yatg.character :refer [Character]]))

(def HexTile
  [:map
   [:row-idx :int]
   [:col-idx :int]
   [:character-id [:maybe :keyword]]
   [:selected? :boolean]])

(defn is-same-tile?
  {:malli/schema [:-> HexTile HexTile :boolean]}
  [t1 t2]
  (prn t1 t2)
  (and
    (= (:row-idx t1) (:row-idx t2))
    (= (:col-idx t1) (:col-idx t2))))

(def HexGrid
  [:vector HexTile])

(defn one-away?
  {:malli/schema [:-> :int :int :boolean]}
  [n1 n2]
  (or (= n1 (dec n2))
      (= n1 (inc n2))))

(defn adjacent?
  "Checks if two tiles are adjacent or not."
  {:malli/schema [:-> HexTile HexTile :boolean]}
  [{row-idx1 :row-idx col-idx1 :col-idx} {row-idx2 :row-idx col-idx2 :col-idx}]
  (or
    ; Same row
    (and (= row-idx1 row-idx2) (one-away? col-idx1 col-idx2))
    (and (one-away? row-idx1 row-idx2)
         (or (and (even? row-idx1) (or (= col-idx1 col-idx2)
                                       (= col-idx1 (inc col-idx2))))
             (and (odd? row-idx1) (or (= col-idx1 col-idx2)
                                      (= col-idx1 (dec col-idx2))))))))
