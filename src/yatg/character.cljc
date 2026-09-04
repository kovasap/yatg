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
  [character]
  (assoc character :resources {:health 2 :stamina 100}))

(defn generate-character
  {:malli/schema
   [:-> :keyword :keyword :boolean [:vector SpriteTemplate] Character]}
  [id sprite-id controlled-by-player? sprite-templates]
  {:id           id
   :controlled-by-player? controlled-by-player?
   :abilities    [attack move wait]
   :composition  {:stone 1 :water 1 :earth 1 :air 1 :metal 1 :fire 1}
   :items        []
   :wounds       []
   :attributes   {:defense 1 :speed 0}
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
