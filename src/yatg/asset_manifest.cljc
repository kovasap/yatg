(ns yatg.asset-manifest
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clojure.edn :as edn]))

(def AssetManifest
  [:map {:description "A list of all resources we can pull from in our cljs."}
   :image-filepaths
   {:description "A flat list of all image files in /public/resources."}
   [:vector :string]])

(defn load-asset-manifest! [callback-fn]
  (go 
    (let [response (<! (http/get "/asset-manifest.edn"))] 
      (if (= (:status response) 200)
        (callback-fn (edn/read-string (:body response)))
        (js/console.error "Failed to load manifest:" (:status response))))))
