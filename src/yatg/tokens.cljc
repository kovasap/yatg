(ns yatg.tokens 
  (:require
   [yatg.schemas :refer [get-default-instance Token]]))

(def tokens
  (map #(get-default-instance Token %)
       [{:id :guarded
         :display-name "Guarded"
         :description "Ready to block an attack."
         :attribute-modifier {:defense 5}}]))
