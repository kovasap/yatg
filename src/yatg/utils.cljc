(ns yatg.utils
  (:require [com.rpl.specter :as sp]))

(defn get-by-id
  {:malli/schema [:->
                  [:vector [:map [:id :keyword]]]
                  :keyword
                  [:maybe [:map [:id :keyword]]]]}
  [coll id]
  (sp/select-one [sp/ALL #(= (:id %) id)] coll))
