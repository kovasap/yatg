(ns yatg.character
  (:require
    [yatg.graphics.sprite :refer [generate-sprite-from-template]]
    [yatg.schemas :refer [Character SpriteTemplate]]
    [yatg.utils :refer [get-by-id]]
    [yatg.abilities.common :refer [move wait]]))

(defn prep-for-combat
  {:malli/schema [:-> Character Character]}
  [{:keys [affinities] :as character}]
  (assoc character
    :resources {:health  (:level (get-by-id affinities :stone))
                :stamina 100
                :speed   (* 5 (:level (get-by-id affinities :air)))}))

(defn generate-character
  {:malli/schema [:-> :keyword :boolean [:vector SpriteTemplate] Character]}
  [id controlled-by-player? sprite-templates]
  {:id           id
   :controlled-by-player? controlled-by-player?
   :abilities    [move wait]
   :affinities   [{:id :stone :level 1 :growth 1}
                  {:id :air :level 1 :growth 1}
                  {:id :fire :level 1 :growth 1}
                  {:id :water :level 1 :growth 1}]
   :sprite       (generate-sprite-from-template
                   (get-by-id sprite-templates :assassin))
   :display-name (str id)})
