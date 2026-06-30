(ns docs.validate
  (:require [docs.model :as model]))

(defn problem [severity code id msg]
  {:docs/severity severity :docs/code code :docs/id id :docs/msg msg})

(defn problems [doc]
  (let [ids (map :docs/id (:docs/blocks doc))]
    (vec
     (concat
      (when (or (nil? (:docs/title doc)) (= "" (:docs/title doc)))
        [(problem :warning :document/missing-title (:docs/id doc) "document has no title")])
      (for [b (:docs/blocks doc)
            :when (not (contains? model/block-kinds (:docs/kind b)))]
        (problem :error :block/unknown-kind (:docs/id b) "unknown block kind"))
      (for [[id n] (frequencies ids)
            :when (> n 1)]
        (problem :error :block/duplicate-id id "duplicate block id"))))))

(defn valid? [doc]
  (not-any? #(= :error (:docs/severity %)) (problems doc)))
