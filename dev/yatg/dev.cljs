(ns yatg.dev
  (:require
   [dataspex.core :as dataspex]
   [malli.dev.cljs :as dev]
   [nexus.action-log :as action-log]
   [yatg.core :as yatg]
   [yatg.malli-utils :refer [custom-reporter]]))

(def store (atom nil))

(defn ^:dev/after-load configure
  []
  (dataspex/inspect "Game state" store {:track-changes? true :history-limit 25})
  (dev/start! {:report custom-reporter})
  (yatg/main store))

(defn main []
  (action-log/inspect {:max-age {:hours 3}})
  (configure))
