(ns build.manifest
  (:require [clojure.java.io :as io]
            [clojure.string :as s]))

(defn generate-asset-list
  "Scans public/images and writes an asset-manifest.edn file."
  {:shadow.build/stage :flush}
  [build-state & args]
  (let [;; Path to scan
        dir (io/file "resources/public/class-images")
        ;; Filter files and map to web-accessible paths
        files (if (.exists dir)
                (->> (file-seq dir)
                     (filter #(.isFile %))
                     (mapv #(-> %
                                (.getAbsolutePath)
                                (s/split #"/public/")
                                (second))))
                [])
        ;; Destination file
        target (io/file "resources/public/asset-manifest.edn")]
    (spit target (pr-str {:image-filepaths files}))
    (println "Generated manifest for" (count files) "assets.")
    build-state))
