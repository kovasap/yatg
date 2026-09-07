(ns yatg.ui.character
  (:require [yatg.ui.schemas :refer [Hiccup]]
            [yatg.schemas :refer [Character]]
            [yatg.graphics.sprite :refer [get-current-imgpath]]))

(defn
 render-character-image
 {:malli/schema [:-> Character Hiccup]}
 [{:keys [sprite controlled-by-player?]}]
 [:img
    {:src   (get-current-imgpath sprite)
     ; Flip image if the character is not controlled by a player
     :style {:transform (if controlled-by-player? "scaleX(1)" "scaleX(-1)")}}])

(defn render-character-for-map
  {:malli/schema [:-> Character Hiccup]}
  [{{:keys [stamina]} :resources :as character}]
  [:div.character (render-character-image character) [:span stamina]])

(defn render-character-panel
  {:malli/schema [:-> Character Hiccup]}
  [{:keys [composition display-name attributes]
    {:keys [stamina engaged-character-ids]} :resources
    :as   character}]
  [:div.character-panel
   [:h1 display-name]
   (render-character-image character)
   [:div.resources-list
    [:div (str stamina " / " (:max-stamina attributes))]
    [:div
     (str (count engaged-character-ids) " / " (:max-engagements attributes))]]
   (into [:div.attributes-table]
         (for [[k v] attributes]
           [:div (str (name k) ": " v)]))
   [:div (str composition)]])
