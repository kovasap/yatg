(ns yatg.character
  (:require [yatg.sprite :refer [Sprite]]))

(def Character
  [:map
   [:id :keyword]
   [:display-name :string]
   [:sprite Sprite]])
