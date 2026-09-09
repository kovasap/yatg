(ns yatg.items 
  (:require
   [yatg.schemas :refer [get-default-instance Item]]))

(def items
  (map #(get-default-instance Item %)
    [{:id          :axe
      :weapon-type :blade
      :abilities
      ;
      [{:id               :strike
        :display-name     "stk"
        :animation-id     :attack
        :tags             #{:attack}
        :stamina-cost     10
        :time-cost        5
        :consequences     [[:change-stamina
                            {:target-tile-id
                             :ability-arg-placeholder/target-tile-id
                             :amount -20
                             :weapon-type :blade}]]
        :targetable-tiles {:min-range          1
                           :max-range          1
                           :requires-character :enemy}}]}
     {:id          :mace
      :weapon-type :blunt
      :abilities
      ;
      [{:id               :bash
        :display-name     "bsh"
        :animation-id     :attack
        :tags             #{:attack}
        :stamina-cost     10
        :time-cost        5
        :consequences     [[:change-stamina
                            {:target-tile-id
                             :ability-arg-placeholder/target-tile-id
                             :amount -20
                             :weapon-type :blunt}]]
        :targetable-tiles {:min-range          1
                           :max-range          1
                           :requires-character :enemy}}]}]))
