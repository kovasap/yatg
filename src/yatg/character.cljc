(ns yatg.character
  (:require
   [clojure.string :as st]
   [yatg.graphics.sprite :refer [generate-sprite-from-template]]
   [yatg.hex-grid.core :refer [get-adjacent-enemy-ids]]
   [yatg.items :refer [items]]
   [yatg.schemas :refer [Character GameState get-default-instance
                         get-modified-attributes SpriteTemplate TeamId]]
   [yatg.utils :refer [get-by-id]]))

(def biblical-names
  ["Aaron" "Abel" "Abner" "Adam" "Amos" "Asa" "Asher" "Barak" "Boaz" "Caleb"
   "Chloe" "Cyrus" "Dan" "David" "Eli" "Enoch" "Esau" "Ethan" "Eve" "Ezra"
   "Gideon" "Hosea" "Isaac" "Jacob" "Jesse" "Joel" "Jonah" "Joseph" "Joshua"
   "Jude" "Leah" "Levi" "Luke" "Lydia" "Mark" "Mary" "Micah" "Moses" "Naomi"
   "Noah" "Omar" "Paul" "Peter" "Philip" "Rachel" "Ruth" "Samson" "Samuel"
   "Sarah" "Seth" "Silas" "Simon" "Titus"])

; -------------------- Experience --------------------------------

(defn get-experience-for-kill
  {:malli/schema [:-> Character Character :int]}
  [killer killed]
  (let [level-diff (- (:level killed) (:level killer))]
    (cond (> 0 level-diff) 1
          :else            (* 10 level-diff))))

(defn get-level-for-experience
  {:malli/schema [:-> :int :int]}
  [exp]
  (cond
    (> 10 exp) 1
    (> 15 exp) 2
    (> 20 exp) 3
    (> 30 exp) 4
    (> 45 exp) 5
    (> 65 exp) 6))

(defn grant-experience
  {:malli/schema [:-> Character :int Character]}
  [character exp-amount]
  (as-> character $
    (update $ :experience #(+ % exp-amount))
    (assoc $ :level (get-level-for-experience (:experience $)))))

(defn grant-experience-for-kills
  {:malli/schema [:-> Character [:sequential Character] Character]}
  [killer killed]
  (grant-experience killer
                    (apply + (map #(get-experience-for-kill % %) killed))))

; -------------------- Engagement ----------------------

(defn update-character-engagements
  {:malli/schema [:-> Character GameState Character]}
  [character game-state]
  (let [max-engagements (:max-engagements (get-modified-attributes character))
        current-adjacent-enemy-ids (set (get-adjacent-enemy-ids character
                                                                game-state))]
    (update-in
      character
      [:resources :engaged-character-ids]
      ; Prefer keeping engagements the character already has
      (fn [engaged-character-ids]
        (let [persistent-engaged-ids (remove #(not (contains?
                                                     current-adjacent-enemy-ids
                                                     %))
                                       engaged-character-ids)
              new-engaged-ids        (filter #(not (contains?
                                                     (set
                                                       engaged-character-ids)
                                                     %))
                                       current-adjacent-enemy-ids)]
          (vec (take max-engagements
                     (concat persistent-engaged-ids new-engaged-ids))))))))


; -------------------- Character Generation ----------------------

(defn prep-for-combat
  {:malli/schema [:-> Character Character]}
  [character]
  (assoc character
    :resources {:stamina (:max-stamina (get-modified-attributes character))
                :tokens  []
                :engaged-character-ids []}))

(defn generate-character
  {:malli/schema [:->
                  :keyword
                  :keyword
                  TeamId
                  [:vector SpriteTemplate]
                  Character
                  Character]}
  [id sprite-id team sprite-templates overrides]
  (merge (get-default-instance Character
                               {:id           id
                                :controlled-by-player? (= team :with-player)
                                :team team
                                :sprite       (generate-sprite-from-template
                                                (get-by-id sprite-templates
                                                           sprite-id))
                                :display-name (st/capitalize (str id))})
         overrides))

(defn generate-random-character
  {:malli/schema [:-> TeamId GameState Character]}
  [team {:keys [sprite-templates characters]}]
  (let [existing-ids (set (map :id characters))
        id           (->> biblical-names
                          (map #(keyword (st/lower-case %)))
                          (remove #(contains? existing-ids %))
                          (rand-nth))
        sprite-id    (rand-nth (map :id sprite-templates))]
    (generate-character id
                        sprite-id
                        team
                        sprite-templates
                        {:items [(-> (get-by-id items :mace)
                                     (assoc :equipped? true))]})))
