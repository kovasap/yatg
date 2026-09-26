(ns yatg.abilities.consequences
  (:require
   [malli.util :as mu]
   [yatg.schemas
             :refer
             [AbilityArgs Character CharacterId Consequence GameState
              get-acting-character get-character get-equipped-weapon
              get-modified-attributes has-advantage? path-to-character
              path-to-characters-tile path-to-tile TileId TokenId WeaponType]]
   [yatg.specter-with-better-errors :as sp]
   [yatg.utils :refer [throw-str]]
   [yatg.wounds :refer [get-random-wound]]))

; ------------------ Utilities -------------------------------

(declare keyed-consequences)
(defn- get-consequence-fn
  "Turn a data representation of a consequence into an executable function."
  {:malli/schema [:-> Consequence [:-> GameState GameState]]}
  [[consequence-name-key consequence-params]]
  (assert (contains? keyed-consequences consequence-name-key))
  (partial (consequence-name-key keyed-consequences) consequence-params))

(defn replace-consequence-ability-arg-placeholders
  {:malli/schema [:-> AbilityArgs Consequence Consequence]}
  [ability-args [consequence-name-key consequence-args]]
  [consequence-name-key
   (update-vals consequence-args
                #(if (and (keyword? %)
                          (= (namespace %) "ability-arg-placeholder"))
                   ((keyword (name %)) ability-args)
                   %))])
  
(defn apply-consequences
  {:malli/schema [:-> [:sequential Consequence] GameState GameState]}
  [consequences game-state]
  (reduce (fn [gs f] (f gs)) game-state (map get-consequence-fn consequences)))

; ----------- Consequence Implementation Helper Functions ------

(defn apply-weapon-triangle-bonus
  {:malli/schema [:-> :int [:maybe WeaponType] [:maybe WeaponType] :int]}
  [base-stamina-loss attacking-weapon-type defending-weapon-type]
  (if (has-advantage? attacking-weapon-type defending-weapon-type)
    (* base-stamina-loss 1.5)
    base-stamina-loss))

(def TargetSpecifier
  [:map
   ; Can provide a character id directly, or a tile on
   ; which the target character is standing.
   [:target-tile-id {:optional true}
    TileId]
   [:target-id {:optional true}
    CharacterId]])
  
(defn get-target-character
  {:malli/schema [:-> TargetSpecifier GameState Character]}
  [{:keys [target-id target-tile-id] :as args} game-state]
  (get-character
    (cond (keyword? target-id)      target-id
          (keyword? target-tile-id) (sp/select-one (concat (path-to-tile
                                                             target-tile-id)
                                                           [:character-id])
                                                   game-state)
          :else                     (throw-str
                                      "Args " args
                                      " must contain :target-id " target-id
                                      " or :target-tile-id " target-tile-id))
    game-state))

(defn- get-new-stamina
  {:malli/schema [:->
                  :int
                  [:map
                   [:amount :int]
                   [:weapon-type {:optional true}
                    [:maybe WeaponType]]]
                  Character
                  :int]}
  [existing-stamina {:keys [amount weapon-type] :as args} target-character]
  (let [{:keys [max-stamina defense]} (get-modified-attributes
                                        target-character)
        delta (as-> amount $
                (apply-weapon-triangle-bonus
                  $
                  weapon-type
                  (:weapon-type (get-equipped-weapon target-character)))
                (if (neg? $) (+ $ defense) $)
                (min $ 0))]
    (-> (+ existing-stamina delta)
        (min max-stamina)
        (max 0))))


; ------------------ Consequences -------------------------------
; These are one time things that happen, perhaps as a result of abilities, or
; other things.

(defn change-stamina
  {:malli/schema [:->
                  (mu/merge TargetSpecifier
                            [:map
                             [:amount :int]
                             [:weapon-type {:optional true}
                              [:maybe WeaponType]]])
                  GameState
                  GameState]}
  [{:keys [weapon-type] :as args} game-state]
  (let [target-character (get-target-character args game-state)
        new-stamina      (get-new-stamina (:stamina (:resources
                                                      target-character))
                                          args
                                          target-character)]
    (sp/setval (path-to-character (:id target-character))
               (as-> target-character $
                 (assoc-in $ [:resources :stamina] new-stamina)
                 (update $
                         :wounds
                         #(if (= 0 new-stamina)
                            (conj % (get-random-wound weapon-type))
                            %))
                 (assoc $
                   :dead? (>= (count (:wounds $))
                              (:max-wounds (get-modified-attributes $)))))
               game-state)))
 
(defn move-character
  {:malli/schema [:->
                  [:map
                   [:destination TileId]
                   [:traveller [:or :keyword [:enum :active-character]]]]
                  GameState
                  GameState]}
  [{:keys [destination traveller]} game-state]
  (let [travelling-character (if (= traveller :active-character)
                               (get-acting-character game-state)
                               (get-character traveller game-state))]
    (->> game-state
         (sp/setval (concat (path-to-characters-tile (:id travelling-character))
                            [:character-id])
                    sp/NONE)
         (sp/setval (concat (path-to-tile destination) [:character-id])
                    (:id travelling-character)))))

(defn add-token
  {:malli/schema [:->
                  (mu/merge TargetSpecifier [:map [:token TokenId]])
                  GameState
                  GameState]}
  [{:keys [token] :as args} game-state]
  (sp/transform (concat (path-to-character
                          (:id (get-target-character args game-state)))
                        [:resources :tokens])
                #(conj % token)
                game-state))

(def keyed-consequences
  {:change-stamina change-stamina
   :add-token add-token
   :move-character move-character})
