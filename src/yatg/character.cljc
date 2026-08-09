(ns yatg.character
  (:require
    [yatg.sprite :refer [generate-sprite-from-template Sprite SpriteTemplate]]
    [yatg.utils :refer [get-by-id]]))

(def Affinity [:enum :stone :air :fire :water])

(def Character
  [:map
   [:id :keyword]
   [:controlled-by-player? :boolean]
   [:display-name :string]
   [:affinities [:map-of Affinity :int]]
   ; These are values that we expect to change dynamically in a combat
   ; encounter.
   [:resources {:optional true}
    [:map [:health :int] [:stamina :int] [:speed :int]]]
   [:sprite Sprite]])

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
   :affinities   {:stone 1 :air 1 :fire 1 :water 1}
   :sprite       (generate-sprite-from-template
                   (get-by-id sprite-templates :assassin))
   :display-name "Rando"})
