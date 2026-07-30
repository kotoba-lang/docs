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

## Test

```bash
clojure -M:test
```
