# clj-usage-graph

A Clojure library that emits usage diagrams for your project.

## Usage

Make sure you have graphviz installed and thus the `dot` command
available.

Add `[com.gfredericks/clj-usage-graph "0.1.0"]` to your deps somehow
and then:

```
find src -type f | \
xargs lein run -m com.gfredericks.clj-usage-graph/generate | \
dot -Tsvg -o g.svg
```

## License

Copyright © 2013 Gary Fredericks

Distributed under the Eclipse Public License, the same as Clojure.
