(ns yatg.macros
  (:require [malli.core :as m]
            [malli.dev.pretty :as pretty]))

(defmacro ifn
  "Creates a CLJS anonymous function with a Malli schema and instruments it."
  [schema & fn-tail]
  `(let [f#           (fn ~@fn-tail)
         schema#      ~schema
         annotated-f# (with-meta f# {:malli/schema schema#})]
     (m/-instrument {:schema ~schema :report (pretty/reporter)} annotated-f#)
     annotated-f#))
