(ns yatg.utils
  (:require [yatg.specter-with-better-errors :as sp]))

(defn get-by-id
  {:malli/schema [:->
                  [:vector [:map [:id :keyword]]]
                  :keyword
                  [:map [:id :keyword]]]}
  [coll id]
  (sp/select-one [sp/ALL #(= (:id %) id)] coll))

(defn only
  [coll]
  (assert (= (count coll) 1)
          (str "Multiple values in " coll))
  (first coll))

(defn insert-at
  {:malli/schema [:-> [:vector :any] :int :any [:vector :any]]}
  [v idx item]
  (into (conj (subvec v 0 idx) item) (subvec v idx)))
