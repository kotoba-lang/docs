(ns docs.model-test
  (:require [clojure.test :refer [deftest is]]
            [docs.model :as d]
            [docs.validate :as v]))

(deftest document-model
  (let [doc (-> (d/document "memo" {:docs/title "Memo"})
                (d/add-block (d/heading "h1" 1 "Title"))
                (d/add-block (d/paragraph "p1" "Body")))]
    (is (= [{:docs/id "h1" :docs/level 1 :docs/text "Title"}] (d/outline doc)))
    (is (= "Body" (:docs/text (d/block-by-id doc "p1"))))
    (is (v/valid? doc))))
