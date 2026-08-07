(ns yatg.ui.tactical
  (:require [yatg.hex-grid :refer [HexGrid HexTile]]
            [yatg.ui.schemas :refer [Hiccup]]))

; Crucial css located at resources/public/styles.css

(defn render-tile
  {:malli/schema [:-> HexTile Hiccup]}
  [{:keys [row-idx col-idx selected?] :as tile}]
  [:div.hextile
   {:on {:mouseenter [:select-tile tile]
         :mouseleave [:deselect-tile tile]}}
   row-idx "." col-idx
   (if selected? "!" "")])

(defn render-hex-grid
  {:malli/schema [:-> HexGrid Hiccup]}
  [grid]
  (into [:div.hexgrid] (map render-tile grid)))

(defn render-tactical-map
  "A tactical map to fight upon."
  [{:keys [hexgrid]}]
  [:div
   [:button {:on {:click [:advance-timeline]}}
    "Advance Timeline"]
   (render-hex-grid hexgrid)])
