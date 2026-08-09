(ns yatg.dev
  (:require [dataspex.core :as dataspex]
            [yatg.core :as yatg]
            [nexus.action-log :as action-log]
            [malli.dev.cljs :as dev]))

(def store (atom nil))

(defn ^:dev/after-load  configure []
  (dataspex/inspect "Game state" store)
  (dev/start!)
  (yatg/main store))

(defn main []
  (action-log/inspect {:max-age {:hours 3}})
  (configure))
