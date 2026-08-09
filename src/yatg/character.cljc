(ns yatg.character
  (:require
    [yatg.sprite :refer [generate-sprite-from-template]]
    [yatg.schemas :refer [Character SpriteTemplate]]
    [yatg.utils :refer [get-by-id]]))

(defn prep-for-combat
  {:malli/schema [:-> Character Character]}
  [{:keys [affinities] :as character}]
  (assoc character
    :resources {:health  (:stone affinities)
                :stamina 100
                :speed   (* 5 (:air affinities))}))

; TODO make this actual generate random characters
(defn generate-random
  {:malli/schema [:-> [:vector SpriteTemplate] Character]}
  [sprite-templates]
  {:id           :rando
   :controlled-by-player? true
   :affinities   [{:element :stone :level 1}
                  {:element :air :level 1}
                  {:element :fire :level 1}
                  {:element :water :level 1}]
   :sprite       (generate-sprite-from-template
                   (get-by-id sprite-templates :assassin))
   :display-name "Rando"})
