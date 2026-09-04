(ns yatg.hex-grid.core
  (:require
   [yatg.schemas :refer [Character GameState get-acting-character-tile
                         get-enemy-tiles get-hexgrid HexGrid HexTile
                         TileSelector]]
   [yatg.utils :refer [get-by-id]]))

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

(defn get-adjacent-tiles
  {:malli/schema [:-> HexTile HexGrid [:vector HexTile]]}
  [tile hexgrid]
  (filterv #(adjacent? tile %) hexgrid))

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
    (and (let [has-character (not (nil? (:character-id query-tile)))]
           (case requires-character
             :friendly (and has-character
                            (on-same-side? character
                                           (get-by-id characters
                                                      (:character-id
                                                        query-tile))))
             :enemy    (and has-character
                            (not (on-same-side? character
                                                (get-by-id characters
                                                           (:character-id
                                                             query-tile)))))
             :any      has-character
             :none     (nil? (:character-id query-tile))
             true))
         (>= (or max-range 1000)
             (distance origin-tile query-tile)
             (or min-range 0)))))

(defn get-in-range-tiles
  {:malli/schema [:-> HexTile TileSelector GameState [:vector HexTile]]}
  [origin-tile tile-selector game-state]
  (into []
        (filter #(in-range? origin-tile % tile-selector game-state)
          (get-hexgrid game-state))))

(defn is-adjacent-to-enemy?
  {:malli/schema [:-> Character GameState :boolean]}
  [character {:keys [characters] :as game-state}]
  (->> (get-adjacent-tiles (get-acting-character-tile game-state)
                           (get-hexgrid game-state))
       (map :character-id)
       (remove nil?)
       (some #(not (on-same-side? character (get-by-id characters %))))))

(defn get-empty-tiles-adjacent-to-enemies
  {:malli/schema [:-> Character GameState [:vector HexTile]]}
  [character game-state]
  (->> (get-enemy-tiles character game-state)
       (map #(get-adjacent-tiles % (get-hexgrid game-state)))
       (apply concat)
       (filterv #(nil? (:character-id %)))))
