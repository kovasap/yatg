(ns yatg.wounds 
  (:require
   [yatg.schemas :refer [get-default-instance WeaponType Wound]]))

(def wounds
  (map #(get-default-instance Wound %)
    [{:id :cut-leg
      :display-name "Cut Leg"
      :description ""
      :source-weapon-type :blade
      :attribute-modifier {:max-stamina -10}}
     {:id :concussion
      :display-name "Concussion"
      :description ""
      :source-weapon-type :blunt
      :attribute-modifier {:max-engagements -1}}
     {:id :pierced-side
      :display-name "Pierced Side"
      :description ""
      :source-weapon-type :piercing
      :attribute-modifier {:defense -1}}
     {:id :overstrained
      :display-name "Overstrained"
      :description ""
      :source-weapon-type nil
      :attribute-modifier {:max-stamina -5}}]))

(defn get-random-wound
  {:malli/schema [:-> [:maybe WeaponType] Wound]}
  [weapon-type]
  (->> wounds
      (filter #(= weapon-type (:source-weapon-type %)))
      (rand-nth)))
