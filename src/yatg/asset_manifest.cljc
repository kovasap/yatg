(ns yatg.asset-manifest
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clojure.edn :as edn]
            [cljs.core :refer [await]]))

(def AssetManifest
  [:map {:description "A list of all resources we can pull from in our cljs."}
   [:image-filepaths
    {:description "A flat list of all image files in /public/resources."}
    [:vector :string]]])

#_(defn load-asset-manifest!
    [callback-fn]
    ; Block until this finishes
    (go (let [response (<! (http/get "/asset-manifest.edn"))]
          (if (= (:status response) 200)
            (callback-fn (edn/read-string (:body response)))
            (js/console.error "Failed to load manifest:" (:status response))))))


(defn ^:async load-asset-manifest!
  [callback-fn]
  (try
    ;; 1. Wait for the initial network response
    (let [response (await (js/fetch "/asset-manifest.edn"))]
      (if (.-ok response)
        ;; 2. Wait for the plain text body to read/parse
        (let [body-text (await (.text response))]
          (callback-fn (edn/read-string body-text)))
        (js/console.error "Failed to load manifest:" (.-status response))))
    (catch :default e
      (js/console.error "Network error loading manifest:" e))))
