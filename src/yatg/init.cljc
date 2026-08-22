(ns yatg.init
  (:require [yatg.schemas :refer [SpriteTemplate GameState]]
            [yatg.graphics.sprite :refer [generate-sprite-from-template]]
            [yatg.abilities.common :refer [move wait]]
            [yatg.utils :refer [get-by-id]]))

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
          :characters    [{:id           :adam
                           :controlled-by-player? true
                           :abilities    [move wait]
                           :affinities   [{:id :stone :level 1 :growth 1}
                                          {:id :air :level 1 :growth 1}
                                          {:id :fire :level 1 :growth 1}
                                          {:id :water :level 1 :growth 1}]
                           :sprite       (generate-sprite-from-template
                                           (get-by-id sprite-templates
                                                      :assassin))
                           :display-name "Adam"}]
          :overworld     {:path-to-svg "overworld.svg"}
          :current-scene {:location-id nil :battle nil}}))
