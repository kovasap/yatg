(ns yatg.init
  (:require [yatg.schemas :as schemas]
            [yatg.sprite :refer [generate-sprite-from-template]]
            [yatg.utils :refer [get-by-id]]))

(defn initialize-store
  "Sets up the initial game state.

  Assumes that the incoming store has some fields set already such as
  :asset-manifest and :sprite-templates.
  "
  {:malli/schema [:-> schemas/GameState schemas/GameState]}
  [{:keys [sprite-templates] :as store}]
  (merge
    store
    {:locations     [{:id :capitol
                      :display-name "Capitol City"
                      :path-to-img "castle.png"
                      :screen-coordinates {:x 400 :y 400}}]
     :characters    [{:id           :adam
                      :controlled-by-player? true
                      :affinities   {:stone 1 :air 1 :fire 1 :water 1}
                      :sprite       (generate-sprite-from-template
                                      (get-by-id sprite-templates :assassin))
                      :display-name "Adam"}]
     :overworld     {:path-to-svg "overworld.svg"}
     :current-scene {:location-id nil :battle nil}}))
