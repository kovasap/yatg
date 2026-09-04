(ns yatg.abilities.consequences
  (:require
   [yatg.schemas :refer [CharacterId Consequence ConsequenceParams GameState
                         get-acting-character get-character path-to-character
                         path-to-characters-tile path-to-tile]]
   [yatg.specter-with-better-errors :as sp]
   [yatg.utils :refer [throw-str]]))

; ------------------ Utilities -------------------------------

(defn ConsequenceFn
  ; the ":any" here is a malli schema
  {:malli/schema [:-> ConsequenceParams :any]}
  [params-schema]
  [:-> params-schema [:map] GameState GameState])
  
(declare keyed-consequences)
(defn- get-consequence-fn
  "Turn a data representation of a consequence into an executable function."
  {:malli/schema [:-> Consequence [:-> [:map] GameState GameState]]}
  [[consequence-name-key consequence-params]]
  (assert (contains? keyed-consequences consequence-name-key))
  (partial (consequence-name-key keyed-consequences) consequence-params))
  
(defn apply-consequences
  {:malli/schema [:-> [:map] [:sequential Consequence] GameState GameState]}
  [args consequences game-state]
  (->> consequences
       (map get-consequence-fn)
       (reduce (fn [gs f] (f args gs)) game-state)))

; ------------------ Consequences -------------------------------
; These are one time things that happen, perhaps as a result of abilities, or
; other things.

(defn change-stamina
  {:malli/schema (ConsequenceFn [:map
                                 ; Can provide a character id directly, or
                                 ; a tile on which the target character is
                                 ; standing.
                                 [:target-tile-id {:optional true}
                                  :keyword]
                                 [:target-id {:optional true}
                                  CharacterId]
                                 [:amount :int]])}
  [{:keys [target-id target-tile-id amount]} args game-state]
  (let [target-character-id
        (cond (and (keyword? target-id) (contains? args target-id)) (target-id
                                                                      args)
              (and (keyword? target-tile-id) (contains? args target-tile-id))
              (sp/select-one (concat (path-to-tile (target-tile-id args))
                                     [:character-id])
                             game-state)
              :else (throw-str "Args " args
                               " must contain :target-id " target-id
                               " or :target-tile-id " target-tile-id))]
    ; TODO if this puts the character below 0 stamina, wound them!
    (sp/transform (concat (path-to-character target-character-id)
                          [:resources :stamina])
                  #(+ % amount)
                  game-state)))
 
(defn move-character
  {:malli/schema (ConsequenceFn [:map
                                 [:destination :keyword]
                                 [:traveller
                                  [:or :keyword [:enum :active-character]]]])}
  [{:keys [destination traveller]} args game-state]
  (assert (contains? args destination))
  (let [travelling-character (if (= traveller :active-character)
                               (get-acting-character game-state)
                               (do (assert (contains? args traveller))
                                   (get-character (traveller args)
                                                  game-state)))]
    (->> game-state
         (sp/setval (concat (path-to-characters-tile (:id travelling-character))
                            [:character-id])
                    sp/NONE)
         (sp/setval (concat (path-to-tile (destination args)) [:character-id])
                    (:id travelling-character)))))

(def keyed-consequences
  {:change-stamina change-stamina
   :move-character move-character})
