(ns yatg.schemas
  (:require [yatg.hex-grid :refer [HexGrid]]))

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

(def SceneId
  [:map
   [:type [:enum :overworld :location :tactical-map]]
   [:id [:maybe :keyword]]])

(def GameState
  [:map
   [:locations [:vector Location]]
   [:overworld Overworld]
   [:current-scene [:map 
                    ; If we are not at a location, we are at the overworld.
                    [:location-id [:maybe Location]]
                    [:battle [:maybe Battle]]]]])
