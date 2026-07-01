(ns docs.model-test
  (:require [clojure.test :refer [deftest is]]
            [docs.model :as d]
            [docs.validate :as v]
            [docs.wire :as wire]))

(deftest document-model
  (let [doc (-> (d/document "memo" {:docs/title "Memo"})
                (d/add-block (d/heading "h1" 1 "Title"))
                (d/add-block (d/paragraph "p1" "Body")))]
    (is (= [{:docs/id "h1" :docs/level 1 :docs/text "Title"}] (d/outline doc)))
    (is (= "Body" (:docs/text (d/block-by-id doc "p1"))))
    (is (v/valid? doc))))

(deftest document-semantics-and-transit-wire
  (let [doc (-> (d/document "memo" {:docs/title "Memo"})
                (d/add-block (d/heading "h1" 1 "Title"))
                (d/add-block (d/add-text-style
                              (d/paragraph "p1" "Styled paragraph")
                              0 6 {:bold true}))
                (d/add-block (d/list-block "todo" ["Draft" "Review"] {:docs/ordered? false}))
                (d/add-block (d/table "metrics" [["Metric" "Value"] ["Revenue" 120]] {}))
                (d/add-comment {:docs/id "c1"
                                :docs/anchor {:docs/block "p1" :docs/from 0 :docs/to 6}
                                :docs/text "Check wording"})
                (d/add-suggestion {:docs/id "s1"
                                   :docs/op :replace-text
                                   :docs/block "p1"}))
        envelope (wire/document-envelope doc {:request-id "req-1"})]
    (is (v/valid? doc))
    (is (= doc (wire/read-document-envelope (:body envelope))))))
