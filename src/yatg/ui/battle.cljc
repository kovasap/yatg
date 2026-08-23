(ns yatg.ui.battle
  (:require [yatg.ui.schemas :refer [Hiccup]]
            [yatg.ui.character :refer [render-character-for-map]]
            [yatg.schemas :refer [GameState Ability Timeline Battle HexGrid HexTile]]
            [yatg.utils :refer [get-by-id]]
            [yatg.hex-grid :refer [row-count]]))

; Crucial css located at resources/public/styles.css

(defn render-ability-icon
  {:malli/schema [:-> Ability HexTile Hiccup]}
  [{:keys [display-name previewed?] :as ability} tile]
  [:div 
   {:class (if previewed? "hovered" "")
    :on {:mouseenter [[:actions/preview-ability ability (:id tile)]]
         :mouseleave [[:actions/unpreview-ability]]
         :click [[:actions/use-pending-ability-and-advance-timeline]]}}
   display-name])

(defn render-tile
  {:malli/schema [:-> HexTile GameState Hiccup]}
  [{:keys [row-idx col-idx hovered? character-id abilities-that-can-target]
    :as   tile}
   {:keys [characters]}]
  [:div.hextile {:class (if hovered? "hovered" "")
                 :on    {:mouseenter [[:actions/hover-tile tile]]
                         :mouseleave [[:actions/unhover-tile tile]]}}
   row-idx
   "."
   col-idx
   (if (and hovered? (not (nil? abilities-that-can-target)))
     (into [:div]
           (map #(render-ability-icon % tile) abilities-that-can-target))
     "")
   (if character-id
     (render-character-for-map (get-by-id characters character-id))
     "")])

(def tile-size-px 200)
(def tile-gap-px 10)
(defn calc-hexgrid-style
  "Overrides what's in yatg/resources/public/styles.css."
  [hexgrid]
  {:style {:--s   (str tile-size-px "px")
           :--g   (str tile-gap-px "px")
           :width (str (+ (* 6 tile-gap-px)
                          (* (row-count hexgrid) (+ tile-gap-px tile-size-px)))
                       "px")}})


(defn render-hex-grid
  {:malli/schema [:-> HexGrid GameState Hiccup]}
  [hexgrid game-state]
  (into [:div.hexgrid (calc-hexgrid-style hexgrid)]
        (map #(render-tile % game-state) hexgrid)))

(defn render-timeline
  {:malli/schema [:-> Timeline GameState Hiccup]}
  [{:keys [current-tick actions]} game-state]
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

(defn render-battle
  "A tactical map to fight upon."
  {:malli/schema [:-> Battle GameState Hiccup]}
  [{:keys [hexgrid timeline]} game-state]
  [:div
   [:button {:on {:click [[:actions/advance-timeline]]}}
    "Advance Timeline"]
   (render-hex-grid hexgrid game-state)
   (render-timeline timeline game-state)])
