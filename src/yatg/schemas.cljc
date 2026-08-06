(ns yatg.schemas
  (:require [yatg.hex-grid :refer [HexGrid]]
            [yatg.asset-manifest :refer [AssetManifest]]
            [yatg.sprite :refer [SpriteTemplate]]))

(def Battle
  [:map
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
   [:locations [:vector Location]]
   [:overworld Overworld]
   [:current-scene [:map 
                    ; If we are not at a location, we are at the overworld.
                    [:location-id [:maybe :keyword]]
                    [:battle [:maybe Battle]]]]])
