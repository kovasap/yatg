(ns yatg.hex-grid
  (:require [yatg.schemas :refer [HexTile HexGrid TileSelector Character]]))

; Useful resource: https://www.redblobgames.com/grids/hexagons/
;
; We are using an "odd-r" offset coordinate system here.

(defn distance
  {:malli/schema [:-> HexTile HexTile :int]}
  [{{origin_x :x origin_y :y origin_z :z} :cube-coords}
   {{dest_x :x dest_y :y dest_z :z} :cube-coords}]
  (max (abs (- origin_x dest_x))
       (abs (- origin_y dest_y))
       (abs (- origin_z dest_z))))

(defn cube->offset
  [{x :x _ :y z :z}]
  {:row-idx z :col-idx (+ x (/ (- z (bit-and z 1)) 2))})

(defn offset->cube
  [{:keys [row-idx col-idx]}]
  (let [x (- col-idx (/ (- row-idx (bit-and row-idx 1)) 2))]
    {:x x :y (- x row-idx) :z row-idx}))

(defn select-tiles
  {:malli/schema [:-> HexGrid TileSelector [:vector HexTile]]}
  [origin-tile hexgrid {:keys [max-range min-range]}]
  (into []
        (filter (fn [tile]
                  (> (or max-range 1000)
                     (distance origin-tile tile)
                     (or min-range 0)))
          hexgrid)))
  

(defn generate-hexgrid
  {:malli/schema [:-> :int :int HexGrid]}
  [num-rows num-cols]
  (into []
        (reduce concat
          (for [row-idx (range num-rows)]
            (for [col-idx (range num-cols)]
              {:row-idx      row-idx
               :col-idx      col-idx
               :cube-coords  (offset->cube {:row-idx row-idx :col-idx col-idx})
               :selected?    false
               :id           (keyword (str row-idx "-" col-idx))
               :character-id nil})))))

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
