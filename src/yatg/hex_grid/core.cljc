(ns yatg.hex-grid.core
  (:require
   [yatg.schemas :refer [Character GameState get-hexgrid HexGrid HexTile
                         TileSelector]]
   [yatg.utils :refer [get-by-id only]]))

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

(defn on-same-side?
  {:malli/schema [:-> Character Character :boolean]}
  [char1 char2]
  (= (:controlled-by-player? char1)
     (:controlled-by-player? char2)))
  
(defn in-range?
  {:malli/schema [:-> HexTile HexTile TileSelector GameState :boolean]}
  [origin-tile
   query-tile
   {:keys [max-range min-range requires-character]}
   {:keys [characters]}]
  ; Assume we care about the character on the origin-tile
  (let [character (get-by-id characters (:character-id origin-tile))]
    (and (case requires-character
           :friendly (on-same-side? character
                                    (get-by-id characters
                                               (:character-id query-tile)))
           :enemy    (not (on-same-side?
                            character
                            (get-by-id characters (:character-id query-tile))))
           :any      (not (nil? (:character-id query-tile)))
           :none     (nil? (:character-id query-tile))
           true)
         (>= (or max-range 1000)
             (distance origin-tile query-tile)
             (or min-range 0)))))

(defn get-in-range-tiles
  {:malli/schema [:-> HexTile TileSelector GameState [:vector HexTile]]}
  [origin-tile tile-selector game-state]
  (into []
        (filter #(in-range? origin-tile % tile-selector game-state)
          (get-hexgrid game-state))))
  

(defn get-character-tile
  {:malli/schema [:-> HexGrid Character HexTile]}
  [hexgrid character]
  (only (filter #(= (:id character) (:character-id %)) hexgrid)))
