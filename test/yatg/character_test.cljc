(ns yatg.character-test
  (:require #?(:cljs [cljs.test :refer-macros [deftest is testing]]
               :clj [clojure.test :refer [deftest is testing]])
            [clojure.set :as set]
            [clojure.string :as str]
            [malli.core :as m]
            [yatg.character :as c]
            [yatg.hex-grid.core :refer [generate-hexgrid]]
            [yatg.schemas :refer [Character]]))

(def test-sprite-templates
  [{:id         :warrior
    :animations [{:id :idle :frame-img-paths ["warrior/idle.png"]}
                 {:id :attack
                  :frame-img-paths ["warrior/attack/1.png"
                                    "warrior/attack/2.png"]}]}
   {:id :mage :animations [{:id :idle :frame-img-paths ["mage/idle.png"]}]}])

; -------------------- Experience Tests --------------------------------

(deftest get-experience-for-kill-test
  (testing "Returns 1 experience when killer is higher level than killed"
    (let [killer {:level 3}
          killed {:level 1}]
      (is (= 1 (c/get-experience-for-kill killer killed))))
    (let [killer {:level 2}
          killed {:level 1}]
      (is (= 1 (c/get-experience-for-kill killer killed)))))
  (testing "Returns 0 experience when killer and killed are the same level"
    (let [killer {:level 1}
          killed {:level 1}]
      (is (= 0 (c/get-experience-for-kill killer killed))))
    (let [killer {:level 5}
          killed {:level 5}]
      (is (= 0 (c/get-experience-for-kill killer killed)))))
  (testing "Returns 10 * level-diff when killer is lower level than killed"
    (let [killer {:level 1}
          killed {:level 2}]
      (is (= 10 (c/get-experience-for-kill killer killed))))
    (let [killer {:level 1}
          killed {:level 4}]
      (is (= 30 (c/get-experience-for-kill killer killed))))
    (let [killer {:level 2}
          killed {:level 5}]
      (is (= 30 (c/get-experience-for-kill killer killed))))))

(deftest get-level-for-experience-test
  (testing "Experience thresholds map to correct levels"
    ;; Level 1: exp < 10
    (is (= 1 (c/get-level-for-experience 0)))
    (is (= 1 (c/get-level-for-experience 5)))
    (is (= 1 (c/get-level-for-experience 9)))
    ;; Level 2: 10 <= exp < 15
    (is (= 2 (c/get-level-for-experience 10)))
    (is (= 2 (c/get-level-for-experience 12)))
    (is (= 2 (c/get-level-for-experience 14)))
    ;; Level 3: 15 <= exp < 20
    (is (= 3 (c/get-level-for-experience 15)))
    (is (= 3 (c/get-level-for-experience 17)))
    (is (= 3 (c/get-level-for-experience 19)))
    ;; Level 4: 20 <= exp < 30
    (is (= 4 (c/get-level-for-experience 20)))
    (is (= 4 (c/get-level-for-experience 25)))
    (is (= 4 (c/get-level-for-experience 29)))
    ;; Level 5: 30 <= exp < 45
    (is (= 5 (c/get-level-for-experience 30)))
    (is (= 5 (c/get-level-for-experience 37)))
    (is (= 5 (c/get-level-for-experience 44)))
    ;; Level 6: 45 <= exp
    (is (= 6 (c/get-level-for-experience 45)))
    (is (= 6 (c/get-level-for-experience 55)))
    (is (= 6 (c/get-level-for-experience 64)))
    (is (= 6 (c/get-level-for-experience 65)))
    (is (= 6 (c/get-level-for-experience 100)))))

(deftest grant-experience-test
  (testing "Grants experience and keeps same level if threshold not met"
    (let [char   {:id :hero :experience 0 :level 1}
          result (c/grant-experience char 5)]
      (is (= 5 (:experience result)))
      (is (= 1 (:level result)))
      (is (= :hero (:id result)))))
  (testing "Grants experience and levels up when threshold reached"
    (let [char   {:id :hero :experience 0 :level 1}
          result (c/grant-experience char 10)]
      (is (= 10 (:experience result)))
      (is (= 2 (:level result)))))
  (testing "Can advance multiple levels with large experience grant"
    (let [char   {:id :hero :experience 0 :level 1}
          result (c/grant-experience char 30)]
      (is (= 30 (:experience result)))
      (is (= 5 (:level result)))))
  (testing "Accumulates experience on top of existing experience"
    (let [char   {:id :hero :experience 12 :level 2}
          result (c/grant-experience char 8)]
      (is (= 20 (:experience result)))
      (is (= 4 (:level result))))))

(deftest grant-experience-for-kills-test
  (testing "Grants 0 experience when killed list is empty"
    (let [killer {:id :hero :experience 5 :level 1}
          result (c/grant-experience-for-kills killer [])]
      (is (= 5 (:experience result)))
      (is (= 1 (:level result)))))
  (testing "Grants experience for a single kill"
    (let [killer {:id :hero :experience 0 :level 1}
          killed [{:id :mob1 :level 2}]
          result (c/grant-experience-for-kills killer killed)]
      (is (= 10 (:experience result)))
      (is (= 2 (:level result)))))
  (testing "Grants accumulated experience for multiple kills"
    ;; mob1 (lvl 2 -> 10 exp), mob2 (lvl 3 -> 20 exp) = 30 exp -> level 5
    (let [killer {:id :hero :experience 0 :level 1}
          killed [{:id :mob1 :level 2} {:id :mob2 :level 3}]
          result (c/grant-experience-for-kills killer killed)]
      (is (= 30 (:experience result)))
      (is (= 5 (:level result)))))
  (testing "Grants 1 exp each for lower-level kills"
    (let [killer {:id :hero :experience 0 :level 3}
          killed [{:id :mob1 :level 1} {:id :mob2 :level 1}]
          result (c/grant-experience-for-kills killer killed)]
      (is (= 2 (:experience result)))
      (is (= 1 (:level result))))))

; -------------------- Combat Prep Tests -------------------------------

(deftest prep-for-combat-test
  (testing "Initializes combat resources with default max stamina"
    (let [character (c/generate-character :hero
                                          :warrior
                                          :with-player
                                          test-sprite-templates
                                          {})
          damaged   (assoc character
                      :resources {:stamina 5
                                  :tokens  [:guarded]
                                  :engaged-character-ids [:enemy1]})
          prepped   (c/prep-for-combat damaged)]
      (is (= 30 (get-in prepped [:resources :stamina])))
      (is (= [] (get-in prepped [:resources :tokens])))
      (is (= [] (get-in prepped [:resources :engaged-character-ids])))))
  (testing "Respects modified max-stamina from items"
    (let [item-with-stamina {:id        :armor
                             :effects   []
                             :attribute-modifier {:max-stamina 15}
                             :abilities []}
          character         (c/generate-character :hero
                                                  :warrior
                                                  :with-player
                                                  test-sprite-templates
                                                  {:items [item-with-stamina]})
          prepped           (c/prep-for-combat character)]
      (is (= 45 (get-in prepped [:resources :stamina]))))))

; -------------------- Engagement Tests --------------------------------

(deftest update-character-engagements-test
  (let [hero       (c/generate-character :hero
                                         :warrior
                                         :with-player
                                         test-sprite-templates
                                         {})
        enemy1     (c/generate-character :enemy1
                                         :warrior
                                         :enemies
                                         test-sprite-templates
                                         {})
        enemy2     (c/generate-character :enemy2
                                         :warrior
                                         :enemies
                                         test-sprite-templates
                                         {})
        enemy3     (c/generate-character :enemy3
                                         :warrior
                                         :enemies
                                         test-sprite-templates
                                         {})
        ally       (c/generate-character :ally
                                         :warrior
                                         :with-player
                                         test-sprite-templates
                                         {})
        base-grid  (generate-hexgrid 3 3)
        grid       (mapv (fn [tile]
                           (case (:id tile)
                             :0-0 (assoc tile :character-id :hero)
                             :0-1 (assoc tile :character-id :enemy1)
                             :1-0 (assoc tile :character-id :enemy2)
                             :2-2 (assoc tile :character-id :enemy3)
                             tile))
                     base-grid)
        game-state {:characters    [hero enemy1 enemy2 enemy3 ally]
                    :current-scene {:battle {:hexgrid grid}}}]
    (testing "Engages adjacent enemies when initially having no engagements"
      (let [hero-with-empty (assoc-in hero
                              [:resources :engaged-character-ids]
                              [])
            updated         (c/update-character-engagements hero-with-empty
                                                            game-state)
            engaged         (get-in updated
                                    [:resources :engaged-character-ids])]
        (is (= 2 (count engaged)))
        (is (set/subset? (set engaged) #{:enemy1 :enemy2}))))
    (testing "Preserves existing persistent engagements first"
      (let [hero-with-prev (assoc-in hero
                             [:resources :engaged-character-ids]
                             [:enemy2])
            updated        (c/update-character-engagements hero-with-prev
                                                           game-state)
            engaged        (get-in updated [:resources :engaged-character-ids])]
        (is (= :enemy2 (first engaged)))
        (is (= #{:enemy1 :enemy2} (set engaged)))))
    (testing "Removes engagements for enemies that are no longer adjacent"
      (let [hero-with-far (assoc-in hero
                            [:resources :engaged-character-ids]
                            [:enemy3])
            updated       (c/update-character-engagements hero-with-far
                                                          game-state)
            engaged       (get-in updated [:resources :engaged-character-ids])]
        (is (not (contains? (set engaged) :enemy3)))
        (is (set/subset? (set engaged) #{:enemy1 :enemy2}))))
    (testing "Does not engage allies adjacent to the character"
      (let [grid-with-ally (mapv (fn [tile]
                                   (case (:id tile)
                                     :0-0 (assoc tile :character-id :hero)
                                     :0-1 (assoc tile :character-id :ally)
                                     tile))
                             base-grid)
            gs-with-ally   {:characters    [hero ally]
                            :current-scene {:battle {:hexgrid grid-with-ally}}}
            updated        (c/update-character-engagements hero gs-with-ally)
            engaged        (get-in updated [:resources :engaged-character-ids])]
        (is (empty? engaged))))))

; -------------------- Character Generation Tests ----------------------

(deftest generate-character-test
  (testing "Generates a player-controlled character conforming to schema"
    (let [char (c/generate-character :adam
                                     :warrior
                                     :with-player
                                     test-sprite-templates
                                     {})]
      (is (= :adam (:id char)))
      (is (= :with-player (:team char)))
      (is (true? (:controlled-by-player? char)))
      (is (= ":adam" (:display-name char)))
      (is (= :idle (get-in char [:sprite :current-animation])))
      (is (= 0 (get-in char [:sprite :current-frame])))
      (is (m/validate Character char))))
  (testing "Generates an enemy character with controlled-by-player? false"
    (let [char (c/generate-character :orc
                                     :warrior
                                     :enemies
                                     test-sprite-templates
                                     {})]
      (is (= :orc (:id char)))
      (is (= :enemies (:team char)))
      (is (false? (:controlled-by-player? char)))
      (is (m/validate Character char))))
  (testing "Applies overrides correctly"
    (let [overrides {:display-name "Adam the Brave" :level 3 :experience 15}
          char      (c/generate-character :adam
                                          :warrior
                                          :with-player
                                          test-sprite-templates
                                          overrides)]
      (is (= "Adam the Brave" (:display-name char)))
      (is (= 3 (:level char)))
      (is (= 15 (:experience char)))
      (is (m/validate Character char)))))

(deftest generate-random-character-test
  (let [existing-char (c/generate-character :aaron
                                            :warrior
                                            :with-player
                                            test-sprite-templates
                                            {})
        game-state    {:characters       [existing-char]
                       :sprite-templates test-sprite-templates}]
    (testing "Generates a valid character conforming to Character schema"
      (let [random-char (c/generate-random-character :enemies game-state)]
        (is (m/validate Character random-char))
        (is (= :enemies (:team random-char)))
        (is (false? (:controlled-by-player? random-char)))
        (let [equipped-item (first (filter :equipped? (:items random-char)))]
          (is (some? equipped-item))
          (is (= :mace (:id equipped-item))))))
    (testing
      "Picks an ID not present in existing characters from biblical-names"
      (let [random-char (c/generate-random-character :enemies game-state)]
        (is (not= :aaron (:id random-char)))
        (is (contains? (set (map #(keyword (str/lower-case %))
                              c/biblical-names))
                       (:id random-char)))))))
