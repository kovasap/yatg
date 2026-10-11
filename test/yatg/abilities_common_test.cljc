(ns yatg.abilities-common-test
  (:require #?(:cljs [cljs.test :refer-macros [deftest is testing]]
               :clj [clojure.test :refer [deftest is testing]])
            [yatg.abilities.common :as common]
            [yatg.character :as c]
            [yatg.hex-grid.core :refer [generate-hexgrid]]
            [yatg.schemas :refer [Ability get-default-instance]]
            [yatg.utils.core :refer [get-by-id]]))

(def test-sprite-templates
  [{:id         :warrior
    :animations [{:id :idle :frame-img-paths ["warrior/idle.png"]}]}])

(defn make-test-ability
  "Helper to build a valid ability with sensible defaults."
  [id overrides]
  (get-default-instance Ability
                        (merge {:id               id
                                :tags             #{:attack}
                                :display-name     (name id)
                                :description      "Test ability"
                                :icon-path        ""
                                :stamina-cost     5
                                :time-cost        10
                                :consequences     []
                                :targetable-tiles {:min-range 1 :max-range 1}}
                               overrides)))

(defn make-test-character
  "Helper to build a character with specific abilities and resources."
  [id team abilities & [overrides]]
  (c/generate-character
    id
    :warrior
    team
    test-sprite-templates
    (merge {:resources {:stamina 30 :tokens [] :engaged-character-ids []}
            :items     [{:id        :test-weapon
                         :equipped? true
                         :effects   []
                         :attribute-modifier {}
                         :abilities (vec abilities)}]}
           overrides)))

(defn make-test-game-state
  "Helper to build a game state with placed characters on a 3x3 grid."
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

; ------------------ Priming Abilities Tests -------------------------

(deftest get-and-prime-ability-test
  (let [ability (make-test-ability :strike {:stamina-cost 5})
        hero    (make-test-character :hero :with-player [ability])
        enemy   (make-test-character :enemy :enemies [])
        gs      (make-test-game-state [hero enemy] {:0-0 :hero :0-1 :enemy})]
    (testing "Returns nil when acting character has no primed ability"
      (is (nil? (common/get-primed-ability gs))))
    (testing "Primes ability on acting character and retrieves it"
      (let [gs-primed (common/prime-acting-character-ability ability gs)]
        (is (= ability (common/get-primed-ability gs-primed)))
        ;; Only acting character gets primed
        (is (= ability
               (:primed-ability (get-by-id (:characters gs-primed) :hero))))
        (is (nil? (:primed-ability (get-by-id (:characters gs-primed)
                                              :enemy))))))
    (testing "Unprimes abilities on all characters"
      (let [gs-primed   (-> (common/prime-acting-character-ability ability gs)
                            (update-in [:characters 1]
                                       #(assoc % :primed-ability ability)))
            gs-unprimed (common/unprime-abilities gs-primed)]
        (is (nil? (common/get-primed-ability gs-unprimed)))
        (is (nil? (:primed-ability (get-by-id (:characters gs-unprimed)
                                              :hero))))
        (is (nil? (:primed-ability (get-by-id (:characters gs-unprimed)
                                              :enemy))))))))

; ------------------ Dead Character IDs Tests ------------------------

(deftest get-dead-character-ids-test
  (let [hero   (make-test-character :hero :with-player [])
        enemy1 (make-test-character :enemy1 :enemies [])
        enemy2 (make-test-character :enemy2 :enemies [])
        gs     (make-test-game-state [hero enemy1 enemy2]
                                     {:0-0 :hero :0-1 :enemy1 :0-2 :enemy2})]
    (testing "Returns empty set when no characters are dead"
      (is (= #{} (common/get-dead-character-ids gs))))
    (testing "Returns set of dead character IDs"
      (let [gs-with-dead (assoc gs
                           :characters [(assoc hero :dead? true)
                                        enemy1
                                        (assoc enemy2 :dead? true)])]
        (is (= #{:hero :enemy2}
               (common/get-dead-character-ids gs-with-dead)))))))

; ------------------ Restrictions Tests ------------------------------

(deftest is-restriction-active-test
  (let [unengaged-char (make-test-character :hero
                                            :with-player
                                            []
                                            {:resources {:engaged-character-ids
                                                         []}})
        engaged-char   (make-test-character :hero
                                            :with-player
                                            []
                                            {:resources {:engaged-character-ids
                                                         [:enemy]}})]
    (testing
      ":unengaged restriction is active when unengaged, inactive when engaged"
      (is (true? (common/is-restriction-active? [:unengaged] unengaged-char)))
      (is (false? (common/is-restriction-active? [:unengaged] engaged-char))))
    (testing
      ":engaged restriction is active when engaged, inactive when unengaged"
      (is (false? (common/is-restriction-active? [:engaged] unengaged-char)))
      (is (true? (common/is-restriction-active? [:engaged] engaged-char))))))

(deftest get-possible-abilities-test
  (let [move-ability      (make-test-ability :move
                                             {:restrictions [[:unengaged]]})
        disengage-ability (make-test-ability :disengage
                                             {:restrictions [[:engaged]]})
        wait-ability      (make-test-ability :wait {:restrictions []})
        all-abilities     [move-ability disengage-ability wait-ability]
        unengaged-char    (make-test-character :hero
                                               :with-player
                                               all-abilities
                                               {:resources
                                                {:engaged-character-ids []}})
        engaged-char      (make-test-character
                            :hero
                            :with-player
                            all-abilities
                            {:resources {:engaged-character-ids [:enemy]}})]
    (testing "Unengaged character can use unengaged and unrestricted abilities"
      (let [possible (common/get-possible-abilities unengaged-char)]
        (is (= #{:move :wait} (set (map :id possible))))
        (is (not (contains? (set (map :id possible)) :disengage)))))
    (testing "Engaged character can use engaged and unrestricted abilities"
      (let [possible (common/get-possible-abilities engaged-char)]
        (is (= #{:disengage :wait} (set (map :id possible))))
        (is (not (contains? (set (map :id possible)) :move)))))))

; ------------------ Targetable Abilities on Hexgrid Tests -----------

(deftest targetable-abilities-hexgrid-test
  (let [melee-ability  (make-test-ability :melee
                                          {:targetable-tiles {:min-range 1
                                                              :max-range 1}})
        ranged-ability (make-test-ability :ranged
                                          {:targetable-tiles {:min-range 2
                                                              :max-range 3}})
        hero           (make-test-character :hero
                                            :with-player
                                            [melee-ability ranged-ability])
        enemy          (make-test-character :enemy :enemies [])
        gs             (make-test-game-state [hero enemy]
                                             {:0-0 :hero :0-1 :enemy})
        hexgrid        (get-in gs [:current-scene :battle :hexgrid])
        hero-tile      (get-by-id hexgrid :0-0)
        adjacent-tile  (get-by-id hexgrid :0-1)
        far-tile       (get-by-id hexgrid :2-2)]
    (testing
      "set-targetable-abilties attaches only abilities in range of the tile"
      (let [adj-with-abilities (common/set-targetable-abilties adjacent-tile
                                                               hero
                                                               hero-tile
                                                               gs)
            far-with-abilities (common/set-targetable-abilties far-tile
                                                               hero
                                                               hero-tile
                                                               gs)]
        ;; adjacent tile (dist 1) gets melee ability
        (is (= [:melee]
               (mapv :id (:abilities-that-can-target adj-with-abilities))))
        ;; far tile (dist 3) gets ranged ability
        (is (= [:ranged]
               (mapv :id (:abilities-that-can-target far-with-abilities))))))
    (testing "set-all-targetable-abilities updates all tiles across hexgrid"
      (let [updated-grid (common/set-all-targetable-abilities hexgrid hero gs)
            updated-adj  (get-by-id updated-grid :0-1)
            updated-far  (get-by-id updated-grid :2-2)]
        (is (= [:melee] (mapv :id (:abilities-that-can-target updated-adj))))
        (is (= [:ranged] (mapv :id (:abilities-that-can-target updated-far))))))
    (testing "clear-all-targetable-abilities clears abilities from all tiles"
      (let [populated-grid (common/set-all-targetable-abilities hexgrid hero gs)
            cleared-grid   (common/clear-all-targetable-abilities
                             populated-grid)]
        (is (every? #(nil? (:abilities-that-can-target %)) cleared-grid))))))

; ------------------ Use Ability Tests -------------------------------

(deftest use-ability-test
  (let [strike (make-test-ability
                 :strike
                 {:display-name     "Strike"
                  :stamina-cost     5
                  :time-cost        10
                  :consequences     [[:change-stamina
                                      {:target-tile-id
                                       :ability-arg-placeholder/target-tile-id
                                       :amount -15
                                       :weapon-type :blade}]]
                  :targetable-tiles {:min-range 1 :max-range 1}})
        hero   (make-test-character :hero
                                    :with-player
                                    [strike]
                                    {:resources {:stamina 30
                                                 :tokens  []
                                                 :engaged-character-ids []}})
        enemy  (make-test-character :enemy
                                    :enemies
                                    []
                                    {:resources {:stamina 20
                                                 :tokens  []
                                                 :engaged-character-ids []}})
        gs     (make-test-game-state [hero enemy] {:0-0 :hero :0-1 :enemy})]
    (testing "Throws assertion error if ability is not primed"
      (is (thrown? #?(:cljs js/Error
                      :clj Exception)
                   (common/use-ability gs strike))))
    (testing "Executes primed ability correctly"
      (let [primed-strike  (assoc strike :primed-args {:target-tile-id :0-1})
            gs-with-primed (common/prime-acting-character-ability primed-strike
                                                                  gs)
            result         (common/use-ability gs-with-primed primed-strike)
            updated-hero   (get-by-id (:characters result) :hero)
            updated-enemy  (get-by-id (:characters result) :enemy)
            timeline       (get-in result [:current-scene :battle :timeline])
            log-entries    (get-in result [:current-scene :battle :log])]
        ;; Hero stamina deducted by stamina-cost minus defense (30 - (5 -
        ;; 1) = 26)
        (is (= 26 (get-in updated-hero [:resources :stamina])))
        ;; Enemy took damage: -15 + 1 defense = -14 -> 20 - 14 = 6 stamina
        (is (= 6 (get-in updated-enemy [:resources :stamina])))
        ;; Abilities unprimed on characters
        (is (nil? (:primed-ability updated-hero)))
        ;; Timeline advanced: next move placed at tick 10 (0 + 10 time-cost
        ;; - 0 speed)
        (is (= [[:actions/start-player-turn :hero]]
               (get-in timeline [:actions 10])))
        ;; Battle log contains ability execution message
        (is (some (fn [{:keys [message]}]
                    (and message (re-find #"uses ability Strike" message)))
                  log-entries))))
    (testing "Detects and logs newly dead characters"
      ;; Enemy has 10 stamina and 1 prior wound. 15 damage (with 1 defense
      ;; = 14 damage) drops stamina to 0, inflicting 2nd wound -> enemy
      ;; dies!
      (let [weak-enemy    (assoc enemy
                            :resources {:stamina 10
                                        :tokens  []
                                        :engaged-character-ids []}
                            :wounds    [{:id :old-wound
                                         :display-name "Old"
                                         :description ""
                                         :source-weapon-type :blade}])
            gs-weak       (assoc gs :characters [hero weak-enemy])
            primed-strike (assoc strike :primed-args {:target-tile-id :0-1})
            result        (common/use-ability gs-weak primed-strike)
            dead-ids      (:newly-dead-character-ids result)
            log-entries   (get-in result [:current-scene :battle :log])]
        (is (= #{:enemy} dead-ids))
        (is (some (fn [{:keys [message]}]
                    (and message (re-find #"enemy died!" message)))
                  log-entries))))))
