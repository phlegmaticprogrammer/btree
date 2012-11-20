# btree

Purely functional B-tree implementation in Clojure.

To use it, build with Leiningen and include the following dependency in your Clojure project:  

    [phlegmaticprogrammer/btree "0.1.0"]

It provides a protocol _BTreePool_ and a function _btree-pool_ to create instances of that protocol. Both live in the
_phlegmaticprogrammer.btree_ namespace.

These are the functions of the _BTreePool_ protocol:
- (btree-empty [this])  
  Creates an empty btree.
- (btree-insert [this btree content])  
  Inserts content into btree.
- (btree-delete [this btree key])    
  Deletes the content with given key from the btree.
- (btree-find [this btree key])  
  Looks up the content for the given key in the btree. Returns nil if no such content exists. 
- (btree-dir [this btree])   
  Returns a vector consisting of all contents in btree.

The signature of _btree-pool_ is:

    (btree-pool
      "Creates a BTreePool instance.
       param-t        minimum degree of B-tree, >= 2
       param-cmp      comparator (returning < 0, = 0 or > 0), must be able to compare:
                        content with content
                        content with key and vice versa
       store-node     convert M-Node into an address
       load-node      convert address into an M-Node"
      [{param-t :param-t param-cmp :param-cmp load-node :load-node store-node :store-node}])

---

This implementation is based on:

- **Introduction to Algorithms**  
  by _Thomas Cormen_ et al.  
  Chapter 18: B-Trees

---
## License

Copyright (C) 2012 Steven Obua

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

  





  



