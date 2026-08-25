(ns yatg.hex-grid
  (:require [yatg.schemas :refer [HexTile HexGrid TileSelector Character]]
            [yatg.astar :refer [route]]
            [yatg.utils :refer [only get-by-id]]))

; ----------------- Hex Grid Logic ------------------------------------

; Useful resource: https://www.redblobgames.com/grids/hexagons/
;
; We are using an "odd-r" offset coordinate system here.

(defn cube->offset
  [{x :x _ :y z :z}]
  {:row-idx z :col-idx (+ x (/ (- z (bit-and z 1)) 2))})

(defn offset->cube
  [{:keys [row-idx col-idx]}]
  (let [x (- col-idx (/ (- row-idx (bit-and row-idx 1)) 2))]
    {:x x :y row-idx :z (- (+ x row-idx))}))

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
               :hovered?     false
               :id           (keyword (str row-idx "-" col-idx))
               :character-id nil})))))

(defn row-count
  {:malli/schema [:-> HexGrid :int]}
  [hexgrid]
  (+ 1 (apply max (map :row-idx hexgrid))))

(defn col-count
  {:malli/schema [:-> HexGrid :int]}
  [hexgrid]
  (+ 1 (apply max (map :col-idx hexgrid))))

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

(defn get-tile-connections
  "Returns [tile1 tile2 weight] for each connected tile pair in the grid."
  {:malli/schema [:-> HexGrid [:vector [:tuple :keyword :keyword :int]]]}
  [hexgrid]
  (vec (flatten (for [tile1 hexgrid]
                  (for [tile2 hexgrid
                        :when (and 
                                (nil? (:character-id tile1))
                                (nil? (:character-id tile2))
                                (adjacent? tile1 tile2))]
                    [(:id tile1) (:id tile2) 1])))))

(defn shortest-path
  {:malli/schema [:-> HexGrid :keyword :keyword [:vector :keyword]]}
  [hexgrid start-tile-id end-tile-id]
  (let [end-tile    (get-by-id hexgrid end-tile-id)
        connections (get-tile-connections hexgrid)
        distance-to-goal-estimates (into {}
                                         (for [tile hexgrid]
                                           [(:id tile)
                                            (distance tile end-tile)]))]
    (route connections start-tile-id end-tile-id distance-to-goal-estimates)))


; ----------------- Game Specific Stuff ------------------------------------

(defn in-range?
  {:malli/schema [:-> HexTile HexTile TileSelector :boolean]}
  [origin-tile query-tile {:keys [max-range min-range]}]
  (prn (:id origin-tile) (:id query-tile) (distance origin-tile query-tile))
  (>= (or max-range 1000)
      (distance origin-tile query-tile)
      (or min-range 0)))

(defn get-character-tile
  {:malli/schema [:-> HexGrid Character HexTile]}
  [hexgrid character]
  (only (filter #(= (:id character) (:character-id %)) hexgrid)))
