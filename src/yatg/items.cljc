(ns yatg.items)

(def axe
  {:id        :axe
   :weapon-type :blade
   :effects   []
   :attribute-modifier {}
   :abilities [{:id               :strike
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
                                   :requires-character :enemy}}]})

(def mace
  {:id        :mace
   :weapon-type :blunt
   :effects   []
   :attribute-modifier {}
   :abilities [{:id               :bash
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
                                   :requires-character :enemy}}]})
