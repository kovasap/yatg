(ns yatg.schema-validation
  (:require [malli.core :as m]))

(defn validate-node [node path]
  (try
    (m/schema node)
    nil ; Valid schema
    (catch :default e
      [{:path path
        :node node
        :error (ex-message e)}])))

(defn find-invalid-schema-nodes
  "Recursively walks a nested Malli vector schema, maintaining schema context 
   to validate sub-schemas without incorrectly evaluating map entry key-value pairs."
  ([schema-vec] (find-invalid-schema-nodes schema-vec []))
  ([schema-vec path]
   (if-not (vector? schema-vec)
     nil
     (let [type-key    (first schema-vec)
           properties? (map? (second schema-vec))
           children    (drop (if properties? 2 1) schema-vec)]
       (cond
         ;; Map schemas: validate entry value components (child at index 1
         ;; or 2 if properties present)
         (= :map type-key)
         (let [child-errors
               (mapcat (fn [entry]
                         (if (vector? entry)
                           (let [k (first entry)
                                 ;; Entry format can be [:k :schema] or
                                 ;; [:k {:props} :schema]
                                 v (if (map? (second entry))
                                     (nth entry 2 nil)
                                     (second entry))]
                             (find-invalid-schema-nodes v (conj path :map k)))
                           nil))
                 children)]
           (or (seq child-errors) (validate-node schema-vec path)))
         ;; Collection types (:vector, :set, :sequential, etc.): validate
         ;; element schemas
         (contains? #{:vector :set :sequential :maybe} type-key)
         (let [child-errors (mapcat (fn [idx child]
                                      (find-invalid-schema-nodes
                                        child
                                        (conj path type-key idx)))
                              (range)
                              children)]
           (or (seq child-errors) (validate-node schema-vec path)))
         ;; Tuples / Or / And: validate all child schemas
         (contains? #{:tuple :or :and :enum} type-key)
         (let [child-errors (mapcat (fn [idx child]
                                      (find-invalid-schema-nodes
                                        child
                                        (conj path type-key idx)))
                              (range)
                              children)]
           (or (seq child-errors) (validate-node schema-vec path)))
         ;; Leaf or custom schemas: validate node directly
         :else (validate-node schema-vec path))))))
