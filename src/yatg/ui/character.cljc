(ns yatg.ui.character
  (:require
   [yatg.graphics.sprite :refer [get-current-imgpath]]
   [yatg.schemas :refer [Ability Character get-abilities Wound]]
   [yatg.ui.schemas :refer [Hiccup]]))

(defn render-character-image
  {:malli/schema [:-> Character Hiccup]}
  [{:keys [sprite team]}]
  [:img
   {:src   (get-current-imgpath sprite)
    ; Flip image if the character is an enemy
    :style {:transform (if (= team :with-player) "scaleX(1)" "scaleX(-1)")}}])

(defn render-character-for-map
  {:malli/schema [:-> Character Hiccup]}
  [{{:keys [stamina tokens]} :resources :as character}]
  [:div.character
   (render-character-image character)
   [:span stamina]
   (into [:span] (map #(first (name %)) tokens))])

(defn render-ability-line-item
  {:malli/schema [:-> Ability Hiccup]}
  [{:keys [display-name]}]
  [:span display-name])

(defn render-wound-line-item
  {:malli/schema [:-> Wound Hiccup]}
  [{:keys [display-name]}]
  [:span {:style {:color "red"}} display-name])

(defn render-character-panel
  {:malli/schema [:-> Character Hiccup]}
  [{:keys
    [dead? items composition display-name attributes wounds level experience]
    {:keys [stamina engaged-character-ids]} :resources
    :as character}]
  [:div.character-panel
   [:h1 {:style {:text-decoration (if dead? "line-through" "")}}
    display-name]
   (render-character-image character)
   [:div (str "Level " level ", " experience " experience")]
   (into [:div "Items (* means equipped): "]
         (for [item items]
           (str (name (:id item)) (if (:equipped? item) "*" ""))))
   [:div.resources-list
    [:div (str stamina " / " (:max-stamina attributes))]
    [:div
     (str (count engaged-character-ids) " / " (:max-engagements attributes))]]
   (into [:div.modifiers-list]
         ; TODO add passive abilities here and other stat modifiers from
         ; items etc.
         (map render-wound-line-item wounds))
   (into [:div.abilities-list]
         (map render-ability-line-item (get-abilities character)))
   (into [:div.attributes-table]
         (for [[k v] attributes]
           [:div (str (name k) ": " v)]))
   [:div (str composition)]])
