(ns yatg.wounds 
  (:require
   [yatg.schemas :refer [WeaponType]]))

(def wounds
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
    :attribute-modifier {:defense -1}}])

(defn get-random-wound
  {:malli/schema [:-> WeaponType]}
  [weapon-type]
  (->> wounds
      (filter #(= weapon-type (:source-weapon-type %)))
      (rand-nth)))
