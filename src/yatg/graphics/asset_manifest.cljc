(ns yatg.graphics.asset-manifest
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [yatg.schemas :refer [AssetManifest]]
            [clojure.edn :as edn]))

(defn load-asset-manifest!
  ; We don't care about the return value of this function.
  ; It just does whatever stateful stuff is in the callback-fn.
  {:malli/schema [:-> [:-> AssetManifest :any] :any]}
  [callback-fn]
  ; Block until this finishes
  (go (let [response (<! (http/get "/asset-manifest.edn"))]
        (if (= (:status response) 200)
          (callback-fn (edn/read-string (:body response)))
          (js/console.error "Failed to load manifest:" (:status response))))))
