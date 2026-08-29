(ns yatg.malli-utils
  (:require [clojure.string :as st]
            [malli.error :as me]
            [malli.dev.pretty :as pretty]))

(defn- get-caller-name []
  (let [stack (.-stack (js/Error.))
        lines (st/split stack #"\n")]
    ;; Adjust index (4 or 5) based on your framework wrappers
    (or (nth lines 4 nil) "Unknown caller")))

(defn custom-reporter [type data]
  ;; Malli passes :type (e.g., :malli.core/invalid-output or :malli.core/invalid-input)
  ;; and the failure map data
  (let [caller (get-caller-name)
        fn-name (:fn data)
        ;; 1. Humanize the error into a clean, simple map/string
        human-err (me/humanize (:explain data))]

    (prn data)
    
    ;; Print the call-site tracking into the console
    (js/console.error 
      (str "Malli Instrumentation Error in [" fn-name "]. Invoked by: " caller))
    
    ;; 2. Pretty print the structural layout directly into the browser console
    ((pretty/reporter) type data)
    
    ;; 3. Throw a runtime Exception containing the readable problem
    (throw (js/Error. (str "Malli Validation Failed for " fn-name 
                           ". Error details: " (pr-str human-err))))))
