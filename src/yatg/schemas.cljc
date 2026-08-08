(ns yatg.schemas
  (:require [yatg.hex-grid :refer [HexGrid]]
            [yatg.asset-manifest :refer [AssetManifest]]
            [yatg.sprite :refer [SpriteTemplate]]
            [yatg.character :refer [Character]]))

; This is an action to be handled by the event handling system in
; src/yatg/events.cljc
(def Action [:vector :any])

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
   [:hexgrid HexGrid]])

; ---------- Map Elements --------------

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
