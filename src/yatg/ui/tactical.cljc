(ns yatg.ui.tactical
  (:require [yatg.hex-grid :refer [HexGrid HexTile]]
            [yatg.ui.schemas :refer [Hiccup]]
            [yatg.schemas :refer [Timeline Battle]]))

; Crucial css located at resources/public/styles.css

(defn render-tile
  {:malli/schema [:-> HexTile Hiccup]}
  [{:keys [row-idx col-idx selected? character-id] :as tile}]
  [:div.hextile
   {:on {:mouseenter [[:actions/select-tile tile]]
         :mouseleave [[:actions/deselect-tile tile]]}}
   row-idx "." col-idx
   (if selected? "!" "")
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
