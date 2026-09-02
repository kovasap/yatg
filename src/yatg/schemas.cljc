(ns yatg.schemas
  (:require [yatg.specter-with-better-errors :as sp]
            [yatg.utils :refer [get-by-id only]]))


; ---------- New Specs --------------

; TODO use this everywhere
(defn ObjectVector
  {:malli/schema [:-> [:map [:id :keyword]] :any]}
  [object-spec]
  [:and
   [:vector object-spec]
   [:fn {:error/message {:en "All items must have a unique :id"}}
    #(or (empty? %) (apply distinct? (map :id %)))]])

; ---------- Infra Stuff --------------

; This is an action to be handled by the event handling system in
; src/yatg/events.cljc
(def Action [:vector :any])

; ---------- Graphics ---------------------------

(def AssetManifest
  [:map {:description "A list of all resources we can pull from in our cljs."}
   [:image-filepaths
    {:description "A flat list of all image files in /public/resources."}
    [:vector :string]]])

(def Animation
  [:map
   [:id :keyword]
   [:frame-img-paths [:vector :string]]])

(def Sprite
  [:map
   [:current-animation :keyword]
   [:current-frame :int]
   [:animations [:vector Animation]]])

; Some data we can use to generate new Sprites to use for unique objects
; (these are not unique).
(def SpriteTemplate
  [:map
   [:id :keyword]
   [:animations [:vector Animation]]])

; ---------- Tactical Battle Elements --------------

(def TileSelector
  [:map
   [:requires-character {:optional true}
    [:maybe [:enum :friendly :enemy :any :none]]]
   [:max-range {:optional true}
    :int]
   [:min-range {:optional true}
    :int]])

(def AbilityArgs
  [:map
   [:target-tile-id {:optional true}
    :keyword]])

(def Ability
  [:map
   [:id :keyword]
   [:display-name :string]
   [:animation-id {:optional true} [:maybe :keyword]]
   [:stamina-cost :int]
   ; If the ability is currently "pending" (being previewed), this key will
   ; be set with the args that the ability will be called with if it is
   ; executed.
   [:pending-args {:optional true} AbilityArgs]
   [:targetable-tiles TileSelector]])

(defn path-to-character
  "Path relative to GameState"
  [character-id]
  [:characters
   sp/ALL
   #(= character-id (:id %))])

(defn path-to-character-abilities
  "Path relative to GameState"
  [character-id]
  (concat (path-to-character character-id)
          [:abilities sp/ALL]))

(defn path-to-ability
  "Path relative to GameState"
  [character-id ability-id]
  (concat (path-to-character-abilities character-id)
          [#(= ability-id (:id %))]))

; Parameters used to generate a tactical battle map
(def BattleSpec
  [:map
   [:display-name :string]
   [:rows :int]
   [:cols :int]
   [:num-enemies :int]
   ; Not yet used
   [:setting {:optional true} :keyword]])

(def HexTile
  [:map
   [:id :keyword]
   [:row-idx :int]
   [:col-idx :int]
   [:cube-coords [:map [:x :int] [:y :int] [:z :int]]]
   [:character-id {:optional true}
    [:maybe :keyword]]
   [:hovered? :boolean]
   ; nil unless we are trying to use an ability currently if this is empty
   ; during ability usage, the tile should be greyed out
   [:abilities-that-can-target {:optional true}
    [:maybe (ObjectVector Ability)]]])

(defn path-to-tile
  "Path relative to GameState"
  [tile-id]
  [:current-scene :battle :hexgrid sp/ALL #(= tile-id (:id %))])

(defn path-to-characters-tile
  "Path relative to GameState"
  [character-id]
  [:current-scene :battle :hexgrid sp/ALL #(= character-id (:character-id %))])

(def HexGrid
  [:vector HexTile])

(def Timeline
  [:map
   [:current-tick :int]
   ; A map where keys are ticks when things should happen.
   [:actions
    [:map-of
     :int [:vector Action]]]])

(def Battle
  [:map
   [:timeline Timeline]
   ; nil when the battle starts
   [:acting-character-id {:optional true} [:maybe :keyword]]
   [:hexgrid HexGrid]])

; ---------- Characters ---------------------------

(def Affinity 
  [:map
   [:id [:enum :stone :air :fire :water]]
   ; 1, 2, or 3 stars like in battle brothers
   [:growth :int]
   [:level :int]])

(def CharacterId :keyword)
(def Character
  [:map
   [:id CharacterId]
   [:controlled-by-player? :boolean]
   [:display-name :string]
   [:affinities [:vector Affinity]]
   [:abilities (ObjectVector Ability)]
   ; These are values that we expect to change dynamically in a combat
   ; encounter.
   [:resources {:optional true}
    [:map [:health :int] [:stamina :int] [:speed :int]]]
   [:sprite Sprite]])

; ---------- Overworld Map Elements --------------

(def Overworld
  [:map
   [:path-to-svg :string]])

(def Location
  [:map
   [:id :keyword]
   [:display-name :string]
   [:path-to-img :string]
   [:battles [:vector BattleSpec]]
   [:screen-coordinates [:map [:x :float]
                              [:y :float]]]])

; ---------- Global Stuff ---------------

; Malli has no schemas for atoms, we encode them as an any for now
(def GameStateAtom :any)

(def GameState
  [:map
   [:asset-manifest AssetManifest]
   [:sprite-templates [:vector SpriteTemplate]]
   [:characters [:vector Character]]
   [:locations [:vector Location]]
   [:overworld Overworld]
   [:current-scene
    [:map
     ; If we are not at a location, we are at the overworld.
     [:location-id [:maybe :keyword]]
     [:battle [:maybe Battle]]]]])

(defn get-hexgrid 
  {:malli/schema [:-> GameState HexGrid]}
  [game-state]
  (get-in game-state [:current-scene :battle :hexgrid]))

(defn get-enemies
  {:malli/schema [:-> Character GameState [:vector Character]]}
  [{:keys [controlled-by-player?]} {:keys [characters]}]
  (->> characters
       (filterv #(not (= (:controlled-by-player? %) controlled-by-player?)))))

(defn get-acting-character
  {:malli/schema [:-> GameState Character]}
  [game-state]
  (get-by-id (:characters game-state)
             (get-in game-state [:current-scene :battle :acting-character-id])))

(defn get-character-tile
  {:malli/schema [:-> HexGrid Character HexTile]}
  [hexgrid character]
  (only (filter #(= (:id character) (:character-id %)) hexgrid)))

(defn get-acting-character-tile
  {:malli/schema [:-> GameState HexTile]}
  [game-state]
  (get-character-tile (get-hexgrid game-state)
                      (get-acting-character game-state)))

(defn get-enemy-tiles
  {:malli/schema [:-> Character GameState [:vector HexTile]]}
  [character game-state]
  (->> game-state
       (get-enemies character)
       (mapv #(get-character-tile (get-hexgrid game-state) %))))
