# docs

[![CI](https://github.com/kotoba-lang/docs/actions/workflows/ci.yml/badge.svg)](https://github.com/kotoba-lang/docs/actions/workflows/ci.yml)

Portable CLJC model for kotoba-lang/docs.

Pages editor: https://kotoba-lang.github.io/docs/

The Pages UI is local to kotoba-lang and does not redirect to external hosts.

## Compatibility direction

The document model keeps document semantics needed for Google Docs and
Word-style roundtrips: ordered blocks, headings, paragraphs, lists, tables,
inline text style runs, comments, suggestions, and refs to sheets/files/decks.
The shared wire format is the Kotoba office envelope via
`docs.wire/document-envelope`, using `application/json` and the
`:docs/document` resource kind.

## Coming back

The envelope is a lossy projection: `:docs/type`, a block's `:docs/kind` and a
suggestion's `:docs/op` all leave as bare strings, and a text run's style is a
map whose keys belong to whoever wrote it. Telling those apart needs the
schema, so the reader is here rather than in `transit`:

```clojure
(wire/document-of-envelope (:body envelope))   ; read + rehydrate
(wire/rehydrate-document projected)            ; if you already read it
```

Rehydrate before validating. `docs.validate` reads namespaced keys, and on a
projected payload it finds none — reporting no problems rather than reporting
that it cannot see any.

## Markdown

A document could only leave as EDN, which is the same as not leaving. `docs.markdown`
writes one as Markdown and reads one back:

```clojure
(md/write doc)        ; => "# 月次報告\n\n..."
(md/read text "id")   ; => a document docs.validate will accept
(md/unexpressed doc)  ; => what write will drop, before it drops it
```

`unexpressed` is the part worth knowing about. Markdown cannot carry block ids,
comments, suggestions, a style it has no syntax for, a table cell that is not
text, or a Drive reference — so a document can be asked what it will lose
before someone exports it rather than after. Entries are shaped like
`docs.validate/problems`, all `:info`: losing something on the way to Markdown
is a property of Markdown, not a fault in the document.

Reading accepts more than writing produces, because whatever was pasted in was
not written here, and it never throws — malformed input becomes the nearest
block and the validator is what reports it.

## Test

```bash
clojure -M:test                                                       # JVM
nbb --classpath "src:test:$(clojure -Spath)" scripts/test-cljs.cljs   # ClojureScript
```

Run both. `docs.markdown` is regular expressions and string surgery, which is
where two hosts differ most quietly.
