(ns yatg.graphics.asset-manifest
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [yatg.schemas :refer [AssetManifest]]
            [clojure.edn :as edn]))

(defn load-asset-manifest!
  {:malli/schema [:-> [:-> AssetManifest nil] nil]}
  [callback-fn]
  ; Block until this finishes
  (go (let [response (<! (http/get "/asset-manifest.edn"))]
        (if (= (:status response) 200)
          (callback-fn (edn/read-string (:body response)))
          (js/console.error "Failed to load manifest:" (:status response))))))
