(ns yatg.paths 
  (:require
   [yatg.schemas :refer [get-default-instance Path]]))

(def paths
  (map #(get-default-instance Path %)
    [{:id           :human
      :display-name "Human"
      :description  "Abilities and traits common to all human beings."
      :perks
      ;
      [{:id :move
        :display-name "Move"
        :description "Take a step"
        :granted-abilities
        ;
        [{:id               :move
          :display-name     "mv"
          :tags             #{:mobility}
          :stamina-cost     5
          :time-cost        5
          :consequences     [[:move-character
                              {:destination
                               :ability-arg-placeholder/target-tile-id
                               :traveller :active-character}]]
          :restrictions     [[:unengaged]]
          :targetable-tiles {:min-range 1 :max-range 1}}
         {:id :disengage
          :display-name "Disengage"
          :description "Break away from an engagement"
          :granted-abilities
          ;
          [{:id               :disengage
            :display-name     "de"
            :tags             #{:mobility}
            :stamina-cost     10
            :time-cost        20
            :restrictions     [[:engaged]]
            :consequences     [[:move-character
                                {:destination
                                 :ability-arg-placeholder/target-tile-id
                                 :traveller :active-character}]]
            :targetable-tiles {:min-range 1 :max-range 1}}]}
         {:id :wait
          :display-name "Wait"
          :description "Wait for time to pass"
          :granted-abilities
          ;
          [{:id           :wait
            :display-name "wt"
            :tags         #{}
            :stamina-cost 0
            :time-cost    5
            :consequences []
            :targetable-tiles {:min-range 0 :max-range 0}}]}]}]}]))
