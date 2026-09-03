(ns yatg.abilities.consequences
  (:require-macros [yatg.macros :refer [ifn]]) 
  (:require
   [yatg.schemas :refer [AbilityArgKey AbilityArgs Consequence GameState
                         get-acting-character get-character path-to-character
                         path-to-characters-tile path-to-tile]]
   [yatg.specter-with-better-errors :as sp]))

; ------------------ Utilities -------------------------------

(defn ConsequenceFn
  [args-map-schema]
  [:-> args-map-schema AbilityArgs GameState GameState])
  
(defn- get-consequence-fn
  {:malli/schema [:-> Consequence [:-> AbilityArgs GameState GameState]]}
  [[k args]]
  (partial (symbol (name k)) args))
  

(defn apply-consequences
  [ability-args consequences game-state]
  (->> consequences
       (map get-consequence-fn)
       (reduce (fn [gs f] (f ability-args gs)) game-state)))

; ------------------ Consequences -------------------------------

(defn reduce-stamina
  {:malli/schema (ConsequenceFn [:map [:target AbilityArgKey] [:amount :int]])}
  [{:keys [target amount]} ability-args game-state]
  (let [target-character-id (sp/select-one (concat (path-to-tile
                                                     (target ability-args))
                                                   [:character-id])
                                           game-state)]
    (sp/transform (concat (path-to-character target-character-id)
                          [:resources :stamina])
                  #(- % amount)
                  game-state)))
 
(defn move-character
  {:malli/schema (ConsequenceFn [:map
                                 [:destination AbilityArgKey]
                                 [:traveller
                                  [:or AbilityArgKey :active-character]]])}
  [{:keys [destination traveller]} ability-args game-state]
  (->> game-state
       (sp/setval (concat (path-to-characters-tile
                            (if (= traveller :active-character)
                              (get-acting-character game-state)
                              (get-character (traveller ability-args)
                                             game-state)))
                          [:character-id])
                  sp/NONE)
       (sp/setval (concat (path-to-tile (destination ability-args))
                          [:character-id])
                  traveller)))
