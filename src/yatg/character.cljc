(ns yatg.character
  (:require
   [clojure.string :as st]
   [yatg.abilities.common :refer [attack move wait]]
   [yatg.graphics.sprite :refer [generate-sprite-from-template]]
   [yatg.schemas :refer [Character GameState SpriteTemplate]]
   [yatg.utils :refer [get-by-id]]))

(def biblical-names
  ["Aaron" "Abel" "Abner" "Adam" "Amos" "Asa" "Asher" "Barak" "Boaz" "Caleb"
   "Chloe" "Cyrus" "Dan" "David" "Eli" "Enoch" "Esau" "Ethan" "Eve" "Ezra"
   "Gideon" "Hosea" "Isaac" "Jacob" "Jesse" "Joel" "Jonah" "Joseph" "Joshua"
   "Jude" "Leah" "Levi" "Luke" "Lydia" "Mark" "Mary" "Micah" "Moses" "Naomi"
   "Noah" "Omar" "Paul" "Peter" "Philip" "Rachel" "Ruth" "Samson" "Samuel"
   "Sarah" "Seth" "Silas" "Simon" "Titus"])

(defn prep-for-combat
  {:malli/schema [:-> Character Character]}
  [{:keys [affinities] :as character}]
  (assoc character
    :resources {:health  (:level (get-by-id affinities :stone))
                :stamina 100
                :speed   (* 5 (:level (get-by-id affinities :air)))}))

(defn generate-character
  {:malli/schema
   [:-> :keyword :keyword :boolean [:vector SpriteTemplate] Character]}
  [id sprite-id controlled-by-player? sprite-templates]
  {:id           id
   :controlled-by-player? controlled-by-player?
   :abilities    [attack move wait]
   :affinities   [{:id :stone :level 1 :growth 1}
                  {:id :air :level 1 :growth 1}
                  {:id :fire :level 1 :growth 1}
                  {:id :water :level 1 :growth 1}]
   :sprite       (generate-sprite-from-template (get-by-id sprite-templates
                                                           sprite-id))
   :display-name (st/capitalize (str id))})

(defn generate-random-character
  {:malli/schema [:-> :boolean GameState Character]}
  [controlled-by-player? {:keys [sprite-templates characters]}]
  (let [existing-ids (set (map :id characters))
        id           (->> biblical-names
                          (map #(keyword (st/lower-case %)))
                          (remove #(contains? existing-ids %))
                          (rand-nth))
        sprite-id    (rand-nth (map :id sprite-templates))]
    (generate-character id sprite-id controlled-by-player? sprite-templates)))
