(ns yatg.astar)

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

(defn route
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
