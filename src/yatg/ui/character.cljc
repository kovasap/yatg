(ns yatg.ui.character
  (:require [yatg.ui.schemas :refer [Hiccup]]
            [yatg.schemas :refer [Character]]
            [yatg.graphics.sprite :refer [get-current-imgpath]]))

(defn render-character-for-map
  {:malli/schema [:-> Character Hiccup]}
  [{:keys [sprite controlled-by-player?] {:keys [stamina speed]} :resources}]
  [:div.character
   [:img
    {:src   (get-current-imgpath sprite)
     ; Flip image if the character is not controlled by a player
     :style {:transform (if controlled-by-player? "scaleX(1)" "scaleX(-1)")}}]
   [:span stamina " " speed]])
