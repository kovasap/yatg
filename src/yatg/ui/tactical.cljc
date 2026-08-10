(ns yatg.ui.tactical
  (:require [yatg.ui.schemas :refer [Hiccup]]
            [yatg.schemas :refer [Ability Timeline Battle HexGrid HexTile]]))

; Crucial css located at resources/public/styles.css

(defn render-ability-icon
  {:malli/schema [:-> Ability Hiccup]}
  [{:keys [display-name previewed?] :as ability}]
  [:div 
   {:style (if previewed? {:color "gold"} {})}
   {:on {:mouseenter [[:actions/preview-ability ability]]
         :mouseleave [[:actions/unpreview-ability ability]]
         :click [[:actions/use-ability ability]]}}
   display-name])

(defn render-tile
  {:malli/schema [:-> HexTile Hiccup]}
  [{:keys [row-idx col-idx hovered? character-id abilities-that-can-target]
    :as   tile}]
  [:div.hextile {:on {:mouseenter [[:actions/hover-tile tile]]
                      :mouseleave [[:actions/unhover-tile tile]]}}
   row-idx
   "."
   col-idx
   (if hovered?
     (if (nil? abilities-that-can-target)
       "!"
       (into [:div] (map render-ability-icon abilities-that-can-target)))
     "")
   (if character-id [:div character-id] "")])

(defn render-hex-grid
  {:malli/schema [:-> HexGrid Hiccup]}
  [grid]
  (into [:div.hexgrid] (map render-tile grid)))

(defn render-timeline
  {:malli/schema [:-> Timeline Hiccup]}
  [{:keys [current-tick actions]}]
  (into [:div.timeline]
        (for [i (range 20)
              :let [tick-idx (+ i current-tick)
                    actions (get actions tick-idx [])]]
          [:div.tick tick-idx
           (if (empty? actions)
             ""
             [:div
              [:br]
              actions])])))

(defn render-tactical-map
  "A tactical map to fight upon."
  {:malli/schema [:-> Battle Hiccup]}
  [{:keys [hexgrid timeline]}]
  [:div
   [:button {:on {:click [[:actions/advance-timeline]]}}
    "Advance Timeline"]
   (render-hex-grid hexgrid)
   (render-timeline timeline)])
