(ns yatg.init
  (:require [yatg.schemas :refer [SpriteTemplate GameState]]
            [yatg.character :refer [generate-character]]))

(defn initialize-store
  "Sets up the initial game state.
  Assumes that the incoming store has some fields set already such as
  :asset-manifest and :sprite-templates.
  "
  {:malli/schema
   [:-> [:map [:sprite-templates [:vector SpriteTemplate]]] GameState]}
  [{:keys [sprite-templates] :as store}]
  (merge store
         {:locations     [{:id           :capitol
                           :display-name "Capitol City"
                           :path-to-img  "castle.png"
                           :battles      [{:rows         10
                                           :cols         10
                                           :display-name "Defend the Capitol!"}]
                           :screen-coordinates {:x 400 :y 400}}]
          :characters    [(generate-character :adam :assassin
                                              true  sprite-templates)]
          :overworld     {:path-to-svg "overworld.svg"}
          :current-scene {:location-id nil :battle nil}}))
