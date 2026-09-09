(ns yatg.schemas
  (:require
   [clojure.string :as st]
   [malli.core :as m]
   [yatg.schema-validation :refer [find-invalid-schema-nodes]]
   [yatg.specter-with-better-errors :as sp]
   [yatg.utils :refer [get-by-id only]]
   [malli.transform :as mt]))


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

(def Message [:map [:tick :int] [:message :string]])

(defn get-default-instance
  [schema overrides]
  (m/decode schema
            overrides
            ; The constantly thing is a workaround to make sure maps are
            ; populated recursively. See
            ; https://github.com/metosin/malli/tree/master#default-values
            ; The vector one makes all vectors empty by default.
            (mt/default-value-transformer
              {:defaults {:vector (constantly []) :map (constantly {})}})))

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

(def ConsequenceParams
  [:map])

(def Consequence
  [:tuple
   ; This keyword must map directly to a function in
   ; yatg.abilities.consequences (have the same string value).
   :keyword
   ConsequenceParams])

(def Restriction
  [:tuple
   :keyword])

(def AbilityArgs
  [:map
   [:target-tile-id {:optional true}
    :keyword]])

(def Ability
  [:map
   [:id :keyword]
   ; Useful for bot behavior coding
   [:tags [:set [:enum :attack :mobility]]]
   [:display-name :string]
   [:animation-id {:optional true}
    [:maybe :keyword]]
   [:stamina-cost :int]
   [:time-cost :int]
   [:consequences [:vector Consequence]]
   [:restrictions {:optional true} [:vector Restriction]]
   ; If the ability is currently "primed", this key will be set with the
   ; args that the ability will be called with if it is executed.
   [:primed-args {:optional true}
    AbilityArgs]
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

(defn get-hovered-tile
  {:malli/schema [:-> HexGrid [:maybe HexTile]]}
  [hexgrid]
  (first (filter :hovered? hexgrid)))

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
   [:acting-character-id {:optional true}
    [:maybe :keyword]]
   [:hexgrid HexGrid]
   [:log [:vector Message]]])

; ---------- Effects ---------------------------

(def EffectTrigger
  [:enum :after-ability-use])

; An effect is something that causes some consequence when it is triggered (at
; a specific point in the game).
(def Effect
  [:map
   [:trigger EffectTrigger]
   [:consequences [:vector Consequence]]])

(def Attributes
  [:map
   [:defense [:int {:default 1}]]
   [:speed [:int {:default 0}]]
   [:stamina-regen [:int {:default 2}]]
   [:max-stamina [:int {:default 100}]]
   [:max-wounds [:int {:default 2}]]
   [:max-engagements [:int {:default 2}]]])
  
; Just like attributes, but each value needs to be a modifiter
; (like +1, -1) to the attribute it modifies.
(def AttributeModifier
  (into [:map]
        (map (fn [[k v]]
               [k [(first v) {:default 0}]])
          (rest Attributes))))

; ---------- Items ---------------------------

; :blade > :blunt > :piercing > :blade > ...
(def WeaponType
  [:enum :blade :blunt :piercing])

(defn has-advantage?
  {:malli/schema [:-> [:maybe WeaponType] [:maybe WeaponType] :boolean]}
  [attacking-type defending-type]
  (cond (or (nil? attacking-type) (nil? defending-type)) false
        (and (= attacking-type :piercing) (= defending-type :blade)) true
        (= attacking-type
           (first (filter #{attacking-type defending-type} (rest WeaponType))))
        true))

(def Item
  [:map
   [:id :keyword]
   [:weapon-type {:optional true} [:maybe WeaponType]]
   [:equipped? {:optional true} :boolean]
   [:effects [:vector Effect]]
   [:attribute-modifier AttributeModifier]
   [:abilities [:vector Ability]]])

; ---------- Characters ---------------------------

(def CharacterId :keyword)

(def Elements
   [:enum :stone :water :earth :air :metal :fire])

(def Resources
  [:map [:stamina :int] [:engaged-character-ids [:vector CharacterId]]])

(def Wound
  [:map
   [:id :keyword]
   [:effects [:vector Effect]]
   [:attribute-modifier AttributeModifier]])

(def PerkId :keyword)
(def Perk
  [:map
   [:id PerkId]
   [:display-name :string]
   [:description :string]
   [:effects [:vector Effect]]
   [:attribute-modifier AttributeModifier]
   [:granted-abilities [:vector Ability]]
   [:depends-on [:vector PerkId]]
   [:unlocked? [:boolean {:default true}]]])

(def Path
  [:map 
   [:id :keyword]
   [:display-name :string]
   [:description :string]
   [:perks [:vector Perk]]])

(def Character
  [:map
   [:id CharacterId]
   [:controlled-by-player? :boolean]
   [:display-name :string]
   [:composition
    (into [:map]
          (map (fn [e]
                 [e [:int {:default 1}]])
            (rest Elements)))]
   [:wounds [:vector Wound]]
   [:items [:vector Item]]
   [:paths [:vector Path]]
   [:attributes Attributes]
   ; These are values that we expect to change dynamically in a combat
   ; encounter.
   [:resources {:optional true}
    Resources]
   [:sprite Sprite]])

(defn get-equipped-weapon
  {:malli/schema [:-> Character [:maybe Item]]}
  [character]
  (first (filter :equipped? (:items character))))

(defn get-abilities
  {:malli/schema [:-> Character [:sequential Ability]]}
  [character]
  (concat
    (sp/select
      [:paths sp/ALL :perks sp/ALL #(:unlocked? %) :granted-abilities sp/ALL]
      character)
    (sp/select [:items sp/ALL :abilities sp/ALL] character)))

(defn merge-attribute-modifiers
  {:malli/schema [:-> [:sequential AttributeModifier] AttributeModifier]}
  [modifiers]
  (reduce (partial merge-with +) modifiers)) 

(defn apply-attribute-modifiers
  {:malli/schema [:-> Attributes [:sequential AttributeModifier] Attributes]}
  [attributes modifiers]
  (merge-with + attributes (merge-attribute-modifiers modifiers))) 

(defn get-attribute-modifiers
  {:malli/schema [:-> Character [:sequential AttributeModifier]]}
  [character]
  (concat (map :attribute-modifier (:wounds character))
          (map :attribute-modifier (:items character))))

(defn get-modified-attributes
  {:malli/schema [:-> Character Attributes]}
  [character]
  (apply-attribute-modifiers (:attributes character)
                             (get-attribute-modifiers character)))

(defn collect-effects-for-trigger
  {:malli/schema [:-> Character EffectTrigger [:sequential Effect]]}
  [character trigger]
  (filter #(= (:trigger %) trigger)
    (concat (map :effects (:wounds character))
            (map :effects (:items character)))))

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
   [:settings [:map [:auto-advance-timeline :boolean]]]
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

(try
  (m/schema GameState)
  (catch :default e
    (prn "GameState schema is invalid!")
    (prn (find-invalid-schema-nodes GameState))
    (throw e)))

(defn get-hexgrid 
  {:malli/schema [:-> GameState HexGrid]}
  [game-state]
  (get-in game-state [:current-scene :battle :hexgrid]))

(defn get-enemies
  {:malli/schema [:-> Character GameState [:vector Character]]}
  [{:keys [controlled-by-player?]} {:keys [characters]}]
  (->> characters
       (filterv #(not (= (:controlled-by-player? %) controlled-by-player?)))))

(defn get-character
  {:malli/schema [:-> CharacterId GameState Character]}
  [id game-state]
  (get-by-id (:characters game-state) id))

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
