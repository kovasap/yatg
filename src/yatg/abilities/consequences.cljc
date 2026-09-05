(ns yatg.abilities.consequences
  (:require
   [yatg.schemas :refer [AbilityArgs CharacterId Consequence GameState
                         get-acting-character get-character path-to-character
                         path-to-characters-tile path-to-tile]]
   [yatg.specter-with-better-errors :as sp]
   [yatg.utils :refer [throw-str]]))

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

; ------------------ Consequences -------------------------------
; These are one time things that happen, perhaps as a result of abilities, or
; other things.

(defn change-stamina
  {:malli/schema [:->
                  [:map
                   ; Can provide a character id directly, or a tile on
                   ; which the target character is standing.
                   [:target-tile-id {:optional true}
                    :keyword]
                   [:target-id {:optional true}
                    CharacterId]
                   [:amount :int]]
                  GameState
                  GameState]}
  [{:keys [target-id target-tile-id amount] :as args} game-state]
  (prn args)
  (let [target-character-id  (cond (keyword? target-id) target-id
                                   (keyword? target-tile-id)
                                   (sp/select-one (concat (path-to-tile
                                                            target-tile-id)
                                                          [:character-id])
                                                  game-state)
                                   :else (throw-str "Args " args
                                                    " must contain :target-id "
                                                    target-id
                                                    " or :target-tile-id "
                                                    target-tile-id))
        {:keys [attributes]} (get-character target-character-id game-state)]
    ; TODO if this puts the character below 0 stamina, wound them!
    ; if they have the max number of wounds, kill them!
    (sp/transform (concat (path-to-character target-character-id)
                          [:resources :stamina])
                  #(min (:max-stamina attributes) (+ % amount))
                  game-state)))
 
(defn move-character
  {:malli/schema [:->
                  [:map
                   [:destination :keyword]
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

(def keyed-consequences
  {:change-stamina change-stamina
   :move-character move-character})
