(ns yatg.schemas
  (:require [com.rpl.specter :as sp]))


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
   [:max-range {:optional true} :int]
   [:min-range {:optional true} :int]])

(def Ability
  [:map
   [:id :keyword]
   [:display-name :string]
   [:stamina-cost :int]
   [:previewed? {:optional true} :boolean]
   [:targetable-tiles TileSelector]])

(defn path-to-ability
  "Path relative to GameState"
  [character-id ability-id]
  [:characters
   sp/ALL
   #(= character-id (:id %))
   :abilities
   sp/ALL
   #(= ability-id (:id %))])

(def HexTile
  [:map
   [:id :keyword]
   [:row-idx :int]
   [:col-idx :int]
   [:cube-coords [:map [:x :int] [:y :int] [:z :int]]]
   [:character-id [:maybe :keyword]]
   [:hovered? :boolean]
   ; nil unless we are trying to use an ability currently
   ; if this is empty during ability usage, the tile should be greyed out
   [:abilities-that-can-target {:optional true} [:maybe [:vector Ability]]]])

(defn path-to-tile
  "Path relative to GameState"
  [tile-id]
  [:current-scene :battle :hexgrid sp/ALL #(= tile-id (:id %))])

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
   [:acting-character-id [:maybe :keyword]]
   [:pending-ability [:maybe Ability]]
   [:pending-target-ids [:vector :keyword]]
   [:hexgrid HexGrid]])

; ---------- Characters ---------------------------

(def Affinity 
  [:map
   [:element [:enum :stone :air :fire :water]]
   ; 1, 2, or 3 stars like in battle brothers
   [:growth :int]
   [:level :int]])

(def Character
  [:map
   [:id :keyword]
   [:controlled-by-player? :boolean]
   [:display-name :string]
   [:affinities [:vector Affinity]]
   [:abilities [:vector Ability]]
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
    [:current-scene [:map 
                     ; If we are not at a location, we are at the overworld.
                     [:location-id [:maybe :keyword]]
                     [:battle [:maybe Battle]]]]])
(defn get-acting-character-id
  [game-state]
  (get-in game-state
          [:current-scene :battle :acting-character-id]))
  
