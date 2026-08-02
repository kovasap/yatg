(ns yatg.schemas)

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
   [:current-scene SceneId]])
