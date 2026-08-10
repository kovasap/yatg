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

(defn adjacent?
  "Checks if two tiles are adjacent or not."
  {:malli/schema [:-> HexTile HexTile :boolean]}
  [tile1 tile2]
  (= 1 (distance tile1 tile2)))

(defn cube->offset
  [{x :x _ :y z :z}]
  {:row-idx z :col-idx (+ x (/ (- z (bit-and z 1)) 2))})

(defn offset->cube
  [{:keys [row-idx col-idx]}]
  (let [x (- col-idx (/ (- row-idx (bit-and row-idx 1)) 2))]
    {:x x :y (- x row-idx) :z row-idx}))

(defn in-range?
  {:malli/schema [:-> HexTile HexTile TileSelector :boolean]}
  [origin-tile query-tile {:keys [max-range min-range]}]
  (> (or max-range 1000)
     (distance origin-tile query-tile)
     (or min-range 0)))

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
