(ns yatg.hex-grid.pathfinding
  (:require
   [yatg.hex-grid.core :refer [adjacent? distance get-in-range-tiles]]
   [yatg.schemas :refer [GameState get-hexgrid HexGrid HexTile TileSelector]]
   [yatg.utils :refer [get-by-id]]))

(defn build-graph
  "Transforms a vector of [from to weight] edges into an adjacency list.
   Assumes a directed graph. For undirected graphs, add the reverse edge."
  [edges]
  (reduce
   (fn [graph [from to weight]]
     (update graph from (fnil conj []) {:to to :weight weight}))
   {}
   edges))

(defn reconstruct-path
  "Reconstructs the shortest path from the start node to the end node
   using the map of tracking parents."
  [came-from current]
  (loop [curr current
         path (list curr)]
    (if-let [parent (get came-from curr)]
      (recur parent (conj path parent))
      (vec path))))

(defn astar-route
  "Finds the shortest path between start and end nodes using the A* algorithm.
   Takes an edge list, start node, end node, and a heuristic map."
  [edges start end heuristic]
  (let [graph (build-graph edges)]
    (loop [;; open-set maps node -> f-score
           open-set {start (get heuristic start 0)}
           came-from {}
           ;; g-score maps node -> cost from start to current node
           g-score {start 0}]
      (if (empty? open-set)
        nil ;; Path not found
        (let [[current _] (apply min-key val open-set)]
          (if (= current end)
            (reconstruct-path came-from current)
            (let [neighbors (get graph current [])
                  remaining-open (dissoc open-set current)
                  [next-open next-came-from next-g-score]
                  (reduce
                   (fn [[os cf gs] {:keys [to weight]}]
                     (let [tentative-g-score (+ (get gs current ##Inf) weight)]
                       (if (< tentative-g-score (get gs to ##Inf))
                         [(assoc os to (+ tentative-g-score (get heuristic to 0)))
                          (assoc cf to current)
                          (assoc gs to tentative-g-score)]
                         [os cf gs])))
                   [remaining-open came-from g-score]
                   neighbors)]
              (recur next-open next-came-from next-g-score))))))))

(defn get-tile-connections
  "Returns [tile1 tile2 weight] for each connected tile pair in the grid."
  {:malli/schema [:-> HexGrid [:vector [:tuple :keyword :keyword :int]]]}
  [hexgrid]
  (vec (apply concat
         (for [tile1 hexgrid]
           (for [tile2 hexgrid
                 :when (and (not (= (:id tile1) (:id tile2)))
                            ; We cannot travel TO a tile with a character on
                            ; it.
                            (nil? (:character-id tile2))
                            (adjacent? tile1 tile2))]
             [(:id tile1) (:id tile2) 1])))))

(defn shortest-path
  "Returns a list of tile ids showing the path from the start tile to the end tile.
  Omits the start tile from the list, but includes the end tile.
  Returns nil if no path is found."
  {:malli/schema [:-> HexGrid :keyword :keyword [:maybe [:vector :keyword]]]}
  [hexgrid start-tile-id end-tile-id]
  (let [end-tile    (get-by-id hexgrid end-tile-id)
        connections (get-tile-connections hexgrid)
        distance-to-goal-estimates (into {}
                                         (for [tile hexgrid]
                                           [(:id tile)
                                            (distance tile end-tile)]))]
    (if-let [route (astar-route connections
                                start-tile-id
                                end-tile-id
                                distance-to-goal-estimates)]
      (vec (rest route))
      nil)))

(defn get-paths-to-closest-tiles
  "Find the shortest paths to all the provided tiles, sorted by
  the shortest path first."
  {:malli/schema
   [:-> HexTile [:vector HexTile] GameState [:vector [:vector :keyword]]]}
  [start-tile target-tiles game-state]
  #_(prn (map (fn [t]
                [(:id t)
                 (shortest-path (get-hexgrid game-state)
                                (:id start-tile)
                                (:id t))])
           target-tiles))
  (->> target-tiles
       (map #(shortest-path (get-hexgrid game-state) (:id start-tile) (:id %)))
       (remove nil?)
       (sort-by count)
       (into [])))

(defn get-first-step-to-closest-tile
  {:malli/schema [:-> HexTile [:vector HexTile] GameState [:maybe :keyword]]}
  [start-tile target-tiles game-state]
  (first (first
           (get-paths-to-closest-tiles start-tile target-tiles game-state))))
