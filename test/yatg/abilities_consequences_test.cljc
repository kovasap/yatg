(ns yatg.abilities-consequences-test
  (:require #?(:cljs [cljs.test :refer-macros [deftest is testing]]
               :clj [clojure.test :refer [deftest is testing]])
            [yatg.abilities.consequences :as cons]
            [yatg.character :as c]
            [yatg.hex-grid.core :refer [generate-hexgrid]]
            [yatg.items :refer [items]]
            [yatg.schemas :refer [Character]]
            [yatg.utils :refer [get-by-id]]))

(def test-sprite-templates
  [{:id         :warrior
    :animations [{:id :idle :frame-img-paths ["warrior/idle.png"]}]}])

(defn make-test-character
  "Helper to build a valid character for testing."
  [id team stamina equipped-weapon-type & [overrides]]
  (let [weapon (when equipped-weapon-type
                 {:id          (keyword (str (name equipped-weapon-type)
                                             "-weapon"))
                  :weapon-type equipped-weapon-type
                  :equipped?   true
                  :effects     []
                  :attribute-modifier {}
                  :abilities   []})]
    (c/generate-character
      id
      :warrior
      team
      test-sprite-templates
      (merge {:resources {:stamina stamina :tokens [] :engaged-character-ids []}
              :items     (if weapon [weapon] [])}
             overrides))))

(defn make-test-game-state
  "Helper to create a game-state with placed characters on a 3x3 hexgrid."
  [characters tile-placements]
  (let [base-grid (generate-hexgrid 3 3)
        grid      (mapv (fn [tile]
                          (if-let [cid (get tile-placements (:id tile))]
                            (assoc tile :character-id cid)
                            tile))
                    base-grid)]
    {:characters    characters
     :current-scene {:battle {:acting-character-id (:id (first characters))
                              :timeline {:current-tick 0 :actions {}}
                              :log      []
                              :hexgrid  grid}}}))

; ------------------ Placeholder Replacement Tests -------------------

(deftest replace-consequence-ability-arg-placeholders-test
  (testing
    "Replaces namespaced ability-arg-placeholder keywords with actual argument values"
    (let [ability-args {:target-tile-id :0-1 :other-val 42}
          consequence  [:change-stamina {:target-tile-id
                                         :ability-arg-placeholder/target-tile-id
                                         :amount -15
                                         :weapon-type :blade}]
          result       (cons/replace-consequence-ability-arg-placeholders
                         ability-args
                         consequence)]
      (is (= [:change-stamina
              {:target-tile-id :0-1 :amount -15 :weapon-type :blade}]
             result))))
  (testing "Leaves consequences without placeholders untouched"
    (let [ability-args {:target-tile-id :0-1}
          consequence  [:add-token {:target-id :hero :token :guarded}]
          result       (cons/replace-consequence-ability-arg-placeholders
                         ability-args
                         consequence)]
      (is (= consequence result)))))

; ------------------ Weapon Triangle Bonus Tests ---------------------

(deftest apply-weapon-triangle-bonus-test
  (testing "Weapon triangle gives 1.5x bonus when attacker has advantage"
    ;; blade beats blunt
    (is (= -30 (cons/apply-weapon-triangle-bonus -20 :blade :blunt)))
    ;; blunt beats piercing
    (is (= -30 (cons/apply-weapon-triangle-bonus -20 :blunt :piercing)))
    ;; piercing beats blade
    (is (= -30 (cons/apply-weapon-triangle-bonus -20 :piercing :blade))))
  (testing "No bonus when attacker does not have advantage"
    ;; blunt does not beat blade
    (is (= -20 (cons/apply-weapon-triangle-bonus -20 :blunt :blade)))
    ;; same weapon types
    (is (= -20 (cons/apply-weapon-triangle-bonus -20 :blade :blade)))
    ;; nil weapon types
    (is (= -20 (cons/apply-weapon-triangle-bonus -20 nil :blade)))
    (is (= -20 (cons/apply-weapon-triangle-bonus -20 :blade nil)))
    (is (= -20 (cons/apply-weapon-triangle-bonus -20 nil nil)))))

; ------------------ Target Retrieval Tests --------------------------

(deftest get-target-character-test
  (let [hero  (make-test-character :hero :with-player 30 :blade)
        enemy (make-test-character :enemy :enemies 20 :blunt)
        gs    (make-test-game-state [hero enemy] {:0-0 :hero :0-1 :enemy})]
    (testing "Retrieves target character by :target-id"
      (is (= hero (cons/get-target-character {:target-id :hero} gs)))
      (is (= enemy (cons/get-target-character {:target-id :enemy} gs))))
    (testing "Retrieves target character by :target-tile-id"
      (is (= hero (cons/get-target-character {:target-tile-id :0-0} gs)))
      (is (= enemy (cons/get-target-character {:target-tile-id :0-1} gs))))
    (testing
      "Throws an error if neither :target-id nor :target-tile-id is provided"
      (is (thrown? #?(:cljs js/Error
                      :clj Exception)
                   (cons/get-target-character {} gs))))))

; ------------------ Change Stamina Tests ----------------------------

(deftest change-stamina-test
  (let [hero  (make-test-character :hero :with-player 30 :blade)
        ;; enemy has default defense = 1, max-stamina = 30, max-wounds = 2
        enemy (make-test-character :enemy :enemies 20 :blunt)
        gs    (make-test-game-state [hero enemy] {:0-0 :hero :0-1 :enemy})]
    (testing "Reduces target stamina accounting for defense"
      ;; enemy defense is 1, so -10 damage becomes -9 stamina loss -> 20 -
      ;; 9 = 11
      (let [result        (cons/change-stamina {:target-id :enemy :amount -10}
                                               gs)
            updated-enemy (get-by-id (:characters result) :enemy)]
        (is (= 11 (get-in updated-enemy [:resources :stamina])))))
    (testing "Applies weapon triangle advantage bonus during stamina reduction"
      ;; attacker has :blade, defender has :blunt -> advantage!
      ;; -10 * 1.5 = -15. With 1 defense: -15 + 1 = -14. 20 - 14 = 6
      (let [result        (cons/change-stamina
                            {:target-id :enemy :amount -10 :weapon-type :blade}
                            gs)
            updated-enemy (get-by-id (:characters result) :enemy)]
        (is (= 6 (get-in updated-enemy [:resources :stamina])))))
    (testing
      "Clamps stamina delta to at most 0 so positive amounts do not increase stamina"
      (let [damaged-hero (assoc-in hero [:resources :stamina] 15)
            gs-damaged   (assoc gs :characters [damaged-hero enemy])
            result       (cons/change-stamina {:target-id :hero :amount 10}
                                              gs-damaged)
            updated-hero (get-by-id (:characters result) :hero)]
        (is (= 15 (get-in updated-hero [:resources :stamina])))))
    (testing "Wounds target when stamina reaches 0"
      (let [result        (cons/change-stamina
                            {:target-id :enemy :amount -100 :weapon-type :blade}
                            gs)
            updated-enemy (get-by-id (:characters result) :enemy)]
        (is (= 0 (get-in updated-enemy [:resources :stamina])))
        (is (= 1 (count (:wounds updated-enemy))))
        (is (= :blade (:source-weapon-type (first (:wounds updated-enemy)))))
        (is (false? (:dead? updated-enemy)))))
    (testing "Marks character as dead when wounds reach max-wounds"
      ;; default max-wounds is 2, so with 1 prior wound, reaching 0 stamina
      ;; inflicts 2nd wound -> dead!
      (let [wounded-enemy (assoc enemy
                            :wounds [{:id :test-wound
                                      :display-name "Wound"
                                      :description ""
                                      :source-weapon-type :blade}])
            gs-wounded    (assoc gs :characters [hero wounded-enemy])
            result        (cons/change-stamina
                            {:target-id :enemy :amount -100 :weapon-type :blunt}
                            gs-wounded)
            updated-enemy (get-by-id (:characters result) :enemy)]
        (is (= 0 (get-in updated-enemy [:resources :stamina])))
        (is (= 2 (count (:wounds updated-enemy))))
        (is (true? (:dead? updated-enemy)))))))

; ------------------ Move Character Tests ----------------------------

(deftest move-character-test
  (let [hero  (make-test-character :hero :with-player 30 :blade)
        enemy (make-test-character :enemy :enemies 20 :blunt)
        gs    (make-test-game-state [hero enemy] {:0-0 :hero :0-1 :enemy})]
    (testing "Moves the active character to the destination tile"
      (let [result      (cons/move-character {:destination :1-1
                                              :traveller   :active-character}
                                             gs)
            hexgrid     (get-in result [:current-scene :battle :hexgrid])
            origin-tile (get-by-id hexgrid :0-0)
            dest-tile   (get-by-id hexgrid :1-1)]
        (is (nil? (:character-id origin-tile)))
        (is (= :hero (:character-id dest-tile)))))
    (testing "Moves an explicitly named character to the destination tile"
      (let [result      (cons/move-character {:destination :2-2
                                              :traveller   :enemy}
                                             gs)
            hexgrid     (get-in result [:current-scene :battle :hexgrid])
            origin-tile (get-by-id hexgrid :0-1)
            dest-tile   (get-by-id hexgrid :2-2)]
        (is (nil? (:character-id origin-tile)))
        (is (= :enemy (:character-id dest-tile)))))))

; ------------------ Add Token Tests ---------------------------------

(deftest add-token-test
  (let [hero (make-test-character :hero :with-player 30 :blade)
        gs   (make-test-game-state [hero] {:0-0 :hero})]
    (testing "Adds a token to the target character's resources"
      (let [result       (cons/add-token {:target-id :hero :token :guarded} gs)
            updated-hero (get-by-id (:characters result) :hero)]
        (is (= [:guarded] (get-in updated-hero [:resources :tokens])))))
    (testing "Appends to existing tokens"
      (let [hero-with-token (assoc-in hero [:resources :tokens] [:blind])
            gs-token        (assoc gs :characters [hero-with-token])
            result          (cons/add-token {:target-tile-id :0-0
                                             :token :focused}
                                            gs-token)
            updated-hero    (get-by-id (:characters result) :hero)]
        (is (= [:blind :focused]
               (get-in updated-hero [:resources :tokens])))))))

; ------------------ Apply Consequences Tests ------------------------

(deftest apply-consequences-test
  (let [hero         (make-test-character :hero :with-player 30 :blade)
        enemy        (make-test-character :enemy :enemies 20 :blunt)
        gs           (make-test-game-state [hero enemy]
                                           {:0-0 :hero :0-1 :enemy})
        consequences [[:move-character {:destination :1-0
                                        :traveller   :active-character}]
                      [:change-stamina {:target-id :enemy :amount -10}]
                      [:add-token {:target-id :hero :token :guarded}]]]
    (testing "Applies a sequence of multiple consequences in order"
      (let [result        (cons/apply-consequences consequences gs)
            hexgrid       (get-in result [:current-scene :battle :hexgrid])
            updated-hero  (get-by-id (:characters result) :hero)
            updated-enemy (get-by-id (:characters result) :enemy)]
        ;; hero moved from 0-0 to 1-0
        (is (nil? (:character-id (get-by-id hexgrid :0-0))))
        (is (= :hero (:character-id (get-by-id hexgrid :1-0))))
        ;; enemy took 9 damage (10 - 1 defense) -> 11 stamina
        (is (= 11 (get-in updated-enemy [:resources :stamina])))
        ;; hero gained guarded token
        (is (= [:guarded] (get-in updated-hero [:resources :tokens])))))))
