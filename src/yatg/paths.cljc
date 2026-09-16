(ns yatg.paths
  (:require
   [malli.core :as m]
   [yatg.schemas :refer [Ability Consequence get-default-instance Path Perk]]))

(defn one-ability-perk
  {:malli/schema [:-> Ability Perk]}
  [ability]
  {:id (:id ability)
   :display-name (:display-name ability)
   :description (:description ability)
   :granted-abilities [ability]})

(def move-active
  [:move-character {:destination :ability-arg-placeholder/target-tile-id
                    :traveller   :active-character}])
(m/validate Consequence move-active)
  
(defn token-target
  {:malli/schema [:-> TokenId Consequence]}
  [token]
  [:add-token {:target-tile-id :ability-arg-placeholder/target-tile-id
               :token       token}])

(def paths
  (map #(get-default-instance Path %)
    [{:id           :human
      :display-name "Human"
      :description  "Abilities and traits common to all human beings."
      :perks        [(one-ability-perk
                       ;
                       {:id               :move
                        :display-name     "Move"
                        :icon-path        ""
                        :description      "Take a step"
                        :tags             #{:mobility}
                        :stamina-cost     5
                        :time-cost        5
                        :consequences     [move-active]
                        :restrictions     [[:unengaged]]
                        :targetable-tiles {:min-range 1 :max-range 1}})
                     (one-ability-perk
                       ;
                       {:id               :disengage
                        :display-name     "Disengage"
                        :icon-path        ""
                        :description      "Break away from an engagement"
                        :tags             #{:mobility}
                        :stamina-cost     10
                        :time-cost        20
                        :restrictions     [[:engaged]]
                        :consequences     [move-active]
                        :targetable-tiles {:min-range 1 :max-range 1}})
                     (one-ability-perk
                       ;
                       {:id               :wait
                        :display-name     "Wait"
                        :icon-path        ""
                        :description      "Wait for time to pass"
                        :tags             #{}
                        :stamina-cost     0
                        :time-cost        5
                        :consequences     []
                        :targetable-tiles {:min-range 0 :max-range 0}})]}
     {:id :protective
      :display-name "Protective"
      :description "Defensive of self and other."
      :perks [(one-ability-perk
                ;
                {:id               :brace
                 :display-name     "Brace"
                 :description      "Prepare to recieve an attack"
                 :tags             #{}
                 :stamina-cost     0
                 :time-cost        5
                 :consequences     [(token-target :guarded)]
                 :targetable-tiles {:min-range 0 :max-range 0}})
              {:id           :thick-skin
               :display-name "Thick Skin"
               :depends-on   [:brace]
               :attribute-modifier {:defense 3}
               :description  "Takes less stamina to weather attacks."}
              {:id           :engagment-training
               :display-name "Engagement Training"
               :depends-on   [:brace]
               :attribute-modifier {:max-engagements 1}
               :description  "Can keep track of one more opponent at once."}
              {:id           :conditioning
               :display-name "Conditioning"
               :depends-on   [:thick-skin :engagment-training]
               :attribute-modifier {:max-stamina 20}
               :description  "Takes longer to get tired."}
              {:id :durable
               :display-name "Durable"
               :depends-on [:thick-skin]
               :attribute-modifier {:max-wounds 1}
               :description
               "Can take an additional wound before being stuck down."}]}]))



                      
          

