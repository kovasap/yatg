(ns yatg.specter-with-better-errors
  (:require [com.rpl.specter :as sp]))

(defn select-one
  [apath structure]
  (try (sp/select-one apath structure)
       (catch :default e
         (let [info {:apath apath :structure structure}]
           (throw (ex-info (str "select-one failed with args " info) info e))))))

(defn setval
  [apath aval structure]
  (try (sp/setval apath aval structure)
       (catch :default e
         (let [info {:apath apath :aval aval :structure structure}]
           (throw (ex-info (str "setval failed with args " info) info e))))))

(defn transform
  [apath transform-fn structure]
  (try (sp/transform apath transform-fn structure)
       (catch :default e
         (let [info
               {:apath apath :transform-fn transform-fn :structure structure}]
           (throw (ex-info (str "transform failed with args " info) info e))))))

(def ALL sp/ALL)
(def NONE sp/NONE)
