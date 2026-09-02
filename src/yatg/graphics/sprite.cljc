(ns yatg.graphics.sprite
  (:require [yatg.schemas :refer [AssetManifest SpriteTemplate Sprite]]
            [yatg.utils :refer [get-by-id]]
            [clojure.string :as s]))

(defn set-frame
  {:malli/schema [:-> Sprite :keyword :int Sprite]}
  [sprite animation-id frame-idx]
  (-> sprite
      (assoc :current-animation animation-id)
      (assoc :current-frame frame-idx)))

(defn get-current-imgpath
  {:malli/schema [:-> Sprite :string]}
  [{:keys [current-animation current-frame animations]}]
  (get (:frame-img-paths (get-by-id animations current-animation))
       current-frame))

(declare -get-frame-from-path)
(defn load-sprite-templates
  {:malli/schema [:-> AssetManifest [:vector SpriteTemplate]]}
  [asset-manifest]
  (->> (:image-filepaths asset-manifest)
       ;; 1. Parse all paths into flat maps containing metadata
       (map -get-frame-from-path)
       ;; 2. Group by class-id to isolate each SpriteTemplate
       (group-by :class-id)
       ;; 3. Transform each group into the SpriteTemplate schema
       (mapv (fn [[class-id class-entries]]
               {:id         class-id
                :animations (->> class-entries
                                 ;; Group the class entries by their
                                 ;; animation-id
                                 (group-by :anim-id)
                                 ;; Transform each animation group into the
                                 ;; Animation schema
                                 (map (fn [[anim-id anim-entries]]
                                        {:id anim-id
                                         :frame-img-paths (->> anim-entries
                                                               (sort-by
                                                                 :frame-idx)
                                                               (map :path)
                                                               vec)}))
                                 vec)}))))

(defn generate-sprite-from-template
  {:malli/schema [:-> SpriteTemplate Sprite]}
  [{:keys [animations]}]
  {:current-animation :idle
   :current-frame     0
   :animations        animations})

(defn -get-frame-from-path
  [path]
  (let [parts     (s/split path #"/")
        class-id  (keyword (nth parts 1))
        remaining (drop 2 parts)]
    (if (= (count remaining) 1)
      ;; Handle single files like "idle.png" -> animation: :idle
      {:class-id  class-id
       :anim-id   (keyword (s/replace (first remaining) #"\.[^.]+$" ""))
       :frame-idx 0
       :path      path}
      ;; Handle nested frames like "attack/1.png"
      {:class-id  class-id
       :anim-id   (keyword (first remaining))
       :frame-idx (let [frame-name (s/replace (second remaining) #"\.[^.]+$" "")]
                    #?(:clj (Integer/parseInt frame-name)
                       :cljs (js/parseInt frame-name)))
       :path      path})))

