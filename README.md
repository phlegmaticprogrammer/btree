# btree

Purely functional B-tree implementation in Clojure.

To use it, build with Leiningen and include the following dependency in your Clojure project:  

    [phlegmaticprogrammer/btree "0.2.0"]

It provides a protocol _BTreePool_, a function _btree-pool_ to create instances of that protocol, and a record type 
_M-Node_ for representing B-tree nodes in memory. All live in the
_phlegmaticprogrammer.btree_ namespace.

These are the main functions of the _BTreePool_ protocol:
- (btree-empty [this])  
  Creates an empty btree.
- (btree-insert [this btree content])  
  Inserts content into btree.
- (btree-delete [this btree key])    
  Deletes the content with given key from the btree.
- (btree-find [this btree key])  
  Looks up the content for the given key in the btree. Returns nil if no such content exists. 
- (btree-flatfold [this btree f-content f-address v])
  Folds f-content over the content of btree without recursing into subtrees but using f-address instead.
- (btree-fold [this btree f v]) 
  INEFFICIENT!! Folds f over the content of btree. Does recurse into subtrees.
- (btree-dir [this btree])   
  INEFFICIENT!! Returns a vector consisting of all contents in btree. 
- (btree-count [this btree]) 
  INEFFICIENT!! Returns the number of all contents in btree. 
  Equivalent to (count (btree-dir this btree)).

There are also functions in the _BTreePool_ protocol that allow to work with indices. 
They assume that a function _address-count_ is passed to them which could be defined as follows:

     (defn address-count [btree] (btree-count pool btree))

Instead of the above definition though, _address-count_ should be implemented by extracting the count from the address directly.
The functions working with indices are:
- (btree-indexed-find [this btree address-count key]) 
  Looks up the content for the given key in btree. 
  Returns {:index index :content content} if content was found successfully at index.
  Returns {:index index} if no such content exists but would be inserted at index.
- (btree-range-retrieve [this btree address-count range-start range-end]) 
  Returns a vector of the contents between index _range-start_ (inclusive) and index _range-end_ (exclusive).
  
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

The type of in-memory nodes is defined via

    (defrecord M-Node [leaf content]) 

where its components have the following meaning:
- _leaf_  
  either true or false
- _content_  
  if this is a leaf, then this is a vector of contents  
  if it is not a leaf, then this is a vector of addresses separated by contents

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

  





  



