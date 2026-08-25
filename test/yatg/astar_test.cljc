(ns yatg.astar-test  
  (:require [cljs.test :refer-macros [deftest is testing]]
            [yatg.astar :refer [route]]))

(deftest astar-test
  (let [edges [[:a :b 1]
               [:b :c 2]
               [:a :c 5]
               [:c :d 1]]
        heuristic {:a 3 :b 2 :c 1 :d 0}]

    (testing "Finds the standard shortest path"
      (is (= [:a :b :c :d] (route edges :a :d heuristic))))

    (testing "Returns a single-node path when start equals end"
      (is (= [:a] (route edges :a :a heuristic))))

    (testing "Returns nil when no path exists"
      (is (nil? (route edges :a :z heuristic)))))

  (let [edges [[:start :a 1]
               [:start :b 10]
               [:a :end 10]
               [:b :end 1]]
        ;; Heuristic guides the search straight to :b despite the high initial cost
        heuristic {:start 11 :a 10 :b 1 :end 0}]
    
    (testing "Heuristic correctly prioritizes a path with a high initial edge"
      (is (= [:start :b :end] (route edges :start :end heuristic))))))
