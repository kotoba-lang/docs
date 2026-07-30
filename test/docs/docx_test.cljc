(ns docs.docx-test
  (:refer-clojure :exclude [read])
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [docs.docx :as docx]
            [docs.model :as d]
            [docs.validate :as v]
            #?(:clj [ooxml.core])))

(defn- memo []
  (-> (d/document "memo" {:docs/title "月次報告"})
      (d/add-block (d/heading "h1" 1 "月次報告"))
      (d/add-block (d/paragraph "p1" "今月の売上は前月比 12% 増でした。"))
      (d/add-block (d/heading "h2" 2 "内訳"))
      (d/add-block (d/block :quote "q1" {:docs/text "決算は来週締めます。"}))
      (d/add-block (d/block :code "c1" {:docs/text "(defn f [x]\n  (* x 2))"}))
      (d/add-block (d/list-block "l1" ["下書き" "レビュー"] {:docs/ordered? false}))
      (d/add-block (d/list-block "l2" ["一" "二"] {:docs/ordered? true}))
      (d/add-block (d/table "t1" [["指標" "値"] ["売上" "120"]] {}))
      (d/add-block (d/ref-block :deck-ref "r1" "slides:intro"))))

(defn- kinds [doc] (mapv :docs/kind (:docs/blocks doc)))
(defn- round [doc] (docx/read (docx/docx-files doc) "memo"))

(deftest a-package-has-every-part-a-reader-will-look-for
  (is (= #{"[Content_Types].xml" "_rels/.rels" "word/_rels/document.xml.rels"
           "word/document.xml" "word/styles.xml" "word/numbering.xml"}
         (set (keys (docx/docx-files (memo)))))))

(deftest every-block-kind-survives-the-trip
  (let [back (round (memo))]
    (is (= (kinds (memo)) (kinds back)))
    (is (= "月次報告" (:docs/title back)))
    (is (v/valid? back))))

(deftest structure-is-written-as-structure
  ;; A heading is a paragraph carrying a style, not bold 18pt text. Word
  ;; renders both the same and only one can be read back as a heading,
  ;; collapsed into an outline, or restyled by whoever receives it.
  (let [xml (get (docx/docx-files (memo)) "word/document.xml")]
    (is (str/includes? xml "<w:pStyle w:val=\"Heading1\"/>"))
    (is (str/includes? xml "<w:pStyle w:val=\"Heading2\"/>"))
    (is (str/includes? xml "<w:numPr>") "a list is numbering, not a hyphen")
    (is (str/includes? xml "<w:tbl>") "a table is a table, not aligned spaces"))
  ;; And the styles it names are defined, because a style id referring to
  ;; nothing is a paragraph with no style at all.
  (let [styles (get (docx/docx-files (memo)) "word/styles.xml")]
    (doseq [id ["Heading1" "Heading2" "Quote" "HTMLPreformatted" "ListParagraph"]]
      (is (str/includes? styles (str "w:styleId=\"" id "\"")) id)))
  ;; A numId with no numbering.xml entry is a list Word shows with no
  ;; marker — indistinguishable from an indented paragraph.
  (let [numbering (get (docx/docx-files (memo)) "word/numbering.xml")]
    (is (str/includes? numbering "w:numId=\"1\""))
    (is (str/includes? numbering "w:numId=\"2\""))))

(deftest heading-levels-come-back
  (let [doc (reduce d/add-block (d/document "d" {:docs/title "T"})
                    (for [n (range 1 7)] (d/heading (str "h" n) n (str "見出し" n))))]
    (is (= [1 2 3 4 5 6] (mapv :docs/level (:docs/blocks (round doc)))))))

(deftest a-list-of-three-is-one-list-and-not-three
  ;; .docx has one paragraph per item, so a reader that took them
  ;; separately would turn a three-item list into three lists of one.
  (let [doc (-> (d/document "d" {:docs/title "T"})
                (d/add-block (d/list-block "l" ["一" "二" "三"] {:docs/ordered? true}))
                (d/add-block (d/paragraph "after" "続き")))
        back (round doc)]
    (is (= [:heading :list :paragraph] (kinds back)))
    (is (= ["一" "二" "三"] (:docs/items (second (:docs/blocks back)))))
    (is (true? (:docs/ordered? (second (:docs/blocks back)))))))

(deftest two-lists-in-a-row-stay-two-lists
  (let [doc (-> (d/document "d" {:docs/title "T"})
                (d/add-block (d/list-block "a" ["丸一"] {:docs/ordered? false}))
                (d/add-block (d/list-block "b" ["番一"] {:docs/ordered? true})))
        back (round doc)]
    (is (= [:heading :list :list] (kinds back)))
    (is (= [false true] (mapv :docs/ordered? (rest (:docs/blocks back)))))))

(deftest a-code-block-keeps-its-newlines
  ;; A `w:t` shows a newline as a space, so the writer splits by line — and
  ;; the reader has to put them back or every code block arrives as one
  ;; long line.
  (let [source "line one\nline two\nline three"
        back (round (-> (d/document "d" {:docs/title "T"})
                        (d/add-block (d/block :code "c" {:docs/text source}))))]
    (is (= source (:docs/text (second (:docs/blocks back)))))))

(deftest the-title-is-not-written-twice
  (let [xml (get (docx/docx-files (memo)) "word/document.xml")]
    (is (= 1 (count (re-seq #"月次報告" xml)))))
  ;; But a document that does not open with its own h1 keeps its title.
  (let [doc (-> (d/document "d" {:docs/title "無題"})
                (d/add-block (d/paragraph "p" "本文")))]
    (is (= "無題" (:docs/title (round doc))))
    (is (= [:heading :paragraph] (kinds (round doc))))))

(deftest ids-are-regenerated
  ;; .docx has nowhere to put a block id, the same as Markdown. Asserted
  ;; directly rather than by comparing whole documents and passing by luck.
  (let [back (round (-> (d/document "d" {:docs/title "T"})
                        (d/add-block (d/paragraph "alpha" "一"))
                        (d/add-block (d/paragraph "beta" "二"))))]
    (is (= ["b1" "b2" "b3"] (mapv :docs/id (:docs/blocks back))))))

(deftest a-table-is-not-read-twice
  ;; `find-all` is depth-first over the whole tree, so a `w:p` inside a
  ;; `w:tc` comes back as a paragraph of its own — the table's text would
  ;; appear in the table and again as loose paragraphs after it.
  (let [back (round (-> (d/document "d" {:docs/title "T"})
                        (d/add-block (d/table "t" [["あ" "い"] ["う" "え"]] {}))))]
    (is (= [:heading :table] (kinds back)))
    (is (= [["あ" "い"] ["う" "え"]] (:docs/rows (second (:docs/blocks back)))))))

(deftest a-ragged-table-comes-out-square
  ;; The model allows rows of different lengths; Word draws that with a
  ;; ragged edge. Padding is what the file needs, and the padding reads back.
  (let [back (round (-> (d/document "d" {:docs/title "T"})
                        (d/add-block (d/table "t" [["あ" "い" "う"] ["え"]] {}))))]
    (is (= [["あ" "い" "う"] ["え" "" ""]] (:docs/rows (second (:docs/blocks back)))))))

(deftest a-paragraph-word-split-into-runs-is-one-paragraph
  ;; What a real .docx looks like: a spell-check marker, a language change,
  ;; a bookmark, and one sentence in five runs. A reader taking the first
  ;; run would truncate most real documents to their opening characters.
  (let [files {"word/document.xml"
               (str "<?xml version=\"1.0\"?>"
                    "<w:document xmlns:w=\"http://schemas.openxmlformats.org/"
                    "wordprocessingml/2006/main\"><w:body>"
                    "<w:p><w:pPr><w:pStyle w:val=\"Heading1\"/></w:pPr>"
                    "<w:bookmarkStart w:id=\"0\" w:name=\"_Toc1\"/>"
                    "<w:r><w:rPr><w:b/></w:rPr><w:t>月次</w:t></w:r>"
                    "<w:proofErr w:type=\"spellStart\"/>"
                    "<w:r><w:t>報告</w:t></w:r>"
                    "<w:bookmarkEnd w:id=\"0\"/></w:p>"
                    "<w:p><w:r><w:t xml:space=\"preserve\">前半 </w:t></w:r>"
                    "<w:r><w:rPr><w:i/></w:rPr><w:t>と</w:t></w:r>"
                    "<w:r><w:t> 後半</w:t></w:r></w:p>"
                    "<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/></w:sectPr>"
                    "</w:body></w:document>")}
        doc (docx/read files "memo")]
    (is (= [:heading :paragraph] (kinds doc)))
    (is (= "月次報告" (:docs/text (first (:docs/blocks doc)))))
    (is (= "月次報告" (:docs/title doc)))
    ;; The spaces between runs survive, which is what xml:space is for.
    (is (= "前半 と 後半" (:docs/text (second (:docs/blocks doc)))))))

(deftest another-namespace-prefix-is-still-a-document
  ;; `w:` is what every producer uses and is not what the format requires.
  ;; Matching on the local name means an unusual prefix is read rather than
  ;; returning an empty document, which would look exactly like a document
  ;; with nothing in it.
  (let [files {"word/document.xml"
               (str "<x:document xmlns:x=\"http://schemas.openxmlformats.org/"
                    "wordprocessingml/2006/main\"><x:body>"
                    "<x:p><x:pPr><x:pStyle x:val=\"Heading1\"/></x:pPr>"
                    "<x:r><x:t>見出し</x:t></x:r></x:p>"
                    "<x:p><x:r><x:t>本文</x:t></x:r></x:p>"
                    "</x:body></x:document>")}
        doc (docx/read files "memo")]
    (is (= [:heading :paragraph] (kinds doc)))
    (is (= "見出し" (:docs/title doc)))))

(deftest an-empty-paragraph-is-spacing-not-content
  (let [files {"word/document.xml"
               (str "<w:document xmlns:w=\"http://schemas.openxmlformats.org/"
                    "wordprocessingml/2006/main\"><w:body>"
                    "<w:p/><w:p><w:r><w:t>本文</w:t></w:r></w:p><w:p/>"
                    "</w:body></w:document>")}]
    (is (= [:paragraph] (kinds (docx/read files "memo"))))))

(deftest reading-nothing-is-not-an-error
  (doseq [files [{} {"word/document.xml" ""}
                 {"word/document.xml" "<w:document/>"}
                 {"word/document.xml" "not xml at all"}]]
    (let [doc (docx/read files "memo")]
      (is (some? doc) (pr-str files))
      (is (vector? (:docs/blocks doc)) (pr-str files))
      (is (v/valid? doc) (pr-str files)))))

(deftest writing-nil-and-missing-fields-does-not-throw
  ;; Blocks arrive from a wire projection and a hand-edited JSON pane, so a
  ;; missing :docs/text or :docs/rows is input rather than an impossibility.
  (doseq [block [(d/block :paragraph "p" {})
                 (d/block :heading "h" {})
                 (d/block :heading "h9" {:docs/level 99})
                 (d/block :quote "q" {})
                 (d/block :code "c" {})
                 (d/block :list "l" {})
                 (d/block :table "t" {})
                 (d/block :table "t2" {:docs/rows []})
                 (d/block :deck-ref "r" {})]]
    (let [doc (-> (d/document "d" {:docs/title "T"}) (d/add-block block))]
      (is (string? (get (docx/docx-files doc) "word/document.xml")) (pr-str block))
      (is (some? (round doc)) (pr-str block)))))

(deftest xml-is-escaped
  (let [doc (-> (d/document "d" {:docs/title "A & B"})
                (d/add-block (d/paragraph "p" "<script>alert(1)</script>")))
        xml (get (docx/docx-files doc) "word/document.xml")]
    (is (str/includes? xml "&amp;"))
    (is (str/includes? xml "&lt;script&gt;"))
    (is (= "<script>alert(1)</script>"
           (:docs/text (second (:docs/blocks (round doc))))))))

#?(:clj
   (deftest the-bytes-are-a-zip-a-reader-can-walk
     (let [entries (docx/docx-entries (docx/docx-bytes (memo)))]
       (is (contains? entries "word/document.xml"))
       (is (contains? entries "word/styles.xml"))
       (is (= :docx (ooxml.core/package-kind entries))))))

#?(:clj
   (deftest the-bytes-round-trip
     (let [back (docx/document-from-bytes (docx/docx-bytes (memo)) "memo")]
       (is (= (kinds (memo)) (kinds back)))
       (is (= "月次報告" (:docs/title back)))
       (is (= "決算は来週締めます。"
              (:docs/text (nth (:docs/blocks back) 3)))))))
