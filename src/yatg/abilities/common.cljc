(ns yatg.abilities.common
  (:require [yatg.hex-grid :refer [TileSelector]]))


(def Ability
  [:map
   [:stamina-cost :int]
   [:targetable-tiles TileSelector]])
