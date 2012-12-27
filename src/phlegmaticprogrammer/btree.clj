(ns phlegmaticprogrammer.btree)

;; Purely functional btree, based on the algorithms described in:
;;    Cormen - Introduction to Algorithms
;;    Chapter 18: B-Trees

(defrecord M-Node [leaf content]) ; node in memory

;; A M-Node consists of the following components:
;;   :leaf
;;       either true or false
;;   :content
;;       if this is a leaf, then this is a vector of Content objects
;;       if it is not a leaf, then this is a vector of addresses separated by Content objects  

(defn- violation [s]
  (throw (RuntimeException. s)))

(defn- vec-concat [& items] (vec (apply concat items)))

(defn- vec-replace [v i j coll] (vec-concat (subvec v 0 i) coll (subvec v j)))

(defprotocol BTreePool
  (btree-empty [this])
  (btree-insert [this btree x])
  (btree-delete [this btree key])
  (btree-count [this btree])
  (btree-dir [this btree])
  (btree-find [this btree key])
  (btree-indexed-find [this btree address-count key])
  (btree-range-retrieve [this btree address-count range-start range-end])
  (btree-fold [this btree f v])
  (btree-flatfold [this btree f-content f-address v])
  (btree-store-node [this])
  (btree-load-node [this])
  (btree-cmp [this]))
  
(defn btree-pool
  "Creates a BTreePool instance.
   param-t        minimum degree of B-tree, >= 2
   param-cmp      comparator (returning < 0, = 0 or > 0), must be able to compare:
                    content with content
                    key with key
                    content with key and vice versa
   store-node     convert M-Node into an address
   load-node      convert address into an M-Node"
  [{param-t :param-t param-cmp :param-cmp load-node :load-node store-node :store-node}] (letfn
[

 (store-m-node [leaf content]
   (store-node (M-Node. leaf content))) 
 
 (btree-empty- [] (store-m-node true []))

 (btree-split-child [parent-content child child-index]
   (let [t param-t
         {child-leaf :leaf child-content :content} child
         key-index (if child-leaf (- t 1) (- (* 2 t) 1))
         key (child-content key-index)
         left-child-content (subvec child-content 0 key-index)
         left-child (M-Node. child-leaf left-child-content)
         right-child-content (subvec child-content (+ key-index 1))
         right-child (M-Node. child-leaf right-child-content)
         ]
     {:content (vec-concat
                (subvec parent-content 0 child-index)
                [(store-node left-child) key (store-node right-child)]
                (subvec parent-content (+ child-index 1)))
      :left-child left-child
      :right-child right-child}))
 
 (btree-is-full [{btree-leaf :leaf btree-content :content}]
   (if btree-leaf
     (= (count btree-content) (- (* 2 param-t) 1))
     (= (count btree-content) (- (* 4 param-t) 1))))

 (btree-insert-nonfull [m-node key]
   (let [content (:content m-node)
         count (count content)]
     (if (:leaf m-node)
       (loop [i 0]
         (if (< i count)
           (let [c (param-cmp key (content i))]
             (cond
               (= c 0) (store-m-node true (assoc content i key))
               (< c 0) (store-m-node true (vec-replace content i i [key]))
               (> c 0) (recur (+ i 1))))
           (store-m-node true (vec-concat content [key]))))
       (loop [i 1]
         (if (< i count)
           (let [c (param-cmp key (content i))]
             (cond
               (= c 0) (store-m-node false (assoc content i key))
               (< c 0) (store-m-node false (btree-insert-into-child content (- i 1) key))
               (> c 0) (recur (+ i 2))))
           (store-m-node false (btree-insert-into-child content (- count 1) key)))))))
           
 (btree-insert-into-child [content pos key]
   (let [child (load-node (content pos))]
     (if-not (btree-is-full child)
       (vec-replace content pos (+ pos 1) [(btree-insert-nonfull child key)])
       (let [{new-content :content left-child :left-child right-child :right-child}
             (btree-split-child content child pos)
             i (+ pos 1)
             c (param-cmp key (new-content i))]
         (if-not (= c 0)
           (let [[child-index child] (if (< c 0) [(- i 1) left-child] [(+ i 1) right-child])]
             (vec-replace new-content child-index (+ child-index 1)
                          [(btree-insert-nonfull child key)]))
           (assoc new-content i key))))))
 
 (btree-insert- [s-node key]
   (let [m-node (load-node s-node)]
     (if-not (btree-is-full m-node)
       (btree-insert-nonfull m-node key)
       (let [{[left middle right] :content
              left-child :left-child
              right-child :right-child }
             (btree-split-child [s-node] m-node 0)
             c (param-cmp key middle)]
         (store-m-node false
                       (cond
                         (= c 0) [left key right]
                         (< c 0) [(btree-insert-nonfull left-child key) middle right]
                         (> c 0) [left middle (btree-insert-nonfull right-child key)]))))))
 
 (btree-is-thin [{leaf :leaf content :content}]
   (if leaf
     (= (count content) (- param-t 1))
     (= (count content) (- (* 2 param-t) 1))))

 (find-key-in-content [start step key content]
   (let [len (count content)]
     (loop [i start]
       (if (>= i len)
         {:not-found i}
         (let [c (param-cmp key (content i))]
           (cond
             (= c 0) {:found i}
             (< c 0) {:not-found i}
             (> c 0) (recur (+ i step))))))))

 (find-max-key [{leaf :leaf content :content}]
   (let [c (content (- (count content) 1))]
     (if leaf c (find-max-key (load-node c)))))

 (find-min-key [{leaf :leaf content :content}]
   (let [c (content 0)]
     (if leaf c (find-min-key (load-node c)))))

 (delete-max-key [m-node]
   (let [k (find-max-key m-node)]
     [(btree-delete-not-thin m-node k) k]))
 
 (delete-min-key [m-node]
   (let [k (find-min-key m-node)]
     [k (btree-delete-not-thin m-node k)]))
 
 (btree-delete-internal-key [content key key-index]
   (let [left-child (load-node (content (- key-index 1)))]
     (if (not (btree-is-thin left-child))
       (store-m-node false (vec-replace content (- key-index 1) (+ key-index 1)
                                        (delete-max-key left-child)))
       (let [right-child (load-node (content (+ key-index 1)))]
         (if (not (btree-is-thin right-child))
           (store-m-node false (vec-replace content key-index (+ key-index 2)
                                            (delete-min-key right-child))) 
           (let [new-child (store-m-node (:leaf left-child)
                                         (vec-concat (:content left-child) [(content key-index)] (:content right-child)))
                 merged {:leaf false :content (vec-replace content (- key-index 1) (+ key-index 2) [new-child])}]
             (btree-delete-not-thin merged key)))))))

 (btree-rotate-right-delete [content child-index child left-child key]
   (if (:leaf child)
     (let [k (content (- child-index 1))
           lc (:content left-child)
           lcc (count lc)
           new-left-child (store-m-node true (subvec lc 0 (- lcc 1)))
           new-child {:leaf true :content (vec-concat [k] (:content child))}
           result-child (btree-delete-not-thin new-child key)
           ]
       (store-m-node false (vec-replace content (- child-index 2) (+ child-index 1)
                                        [new-left-child (lc (- lcc 1)) result-child])))
     (let [k (content (- child-index 1))
           lc (:content left-child)
           lcc (count lc)
           new-left-child (store-m-node false (subvec lc 0 (- lcc 2)))
           new-child {:leaf false :content (vec-concat [(lc (- lcc 1)) k] (:content child))}
           result-child (btree-delete-not-thin new-child key)
           ]
       (store-m-node false (vec-replace content (- child-index 2) (+ child-index 1)
                                        [new-left-child (lc (- lcc 2)) result-child]))))) 
 
 (btree-rotate-left-delete [content child-index child right-child key]
   (if (:leaf child)
     (let [k (content (+ child-index 1))
           rc (:content right-child)
           rcc (count rc)
           new-right-child (store-m-node true (subvec rc 1))
           new-child {:leaf true :content (vec-concat (:content child) [k])}
           result-child (btree-delete-not-thin new-child key)
           ]
       (store-m-node false (vec-replace content child-index (+ child-index 3)
                                        [result-child (rc 0) new-right-child])))
     (let [k (content (+ child-index 1))
           rc (:content right-child)
           rcc (count rc)
           new-right-child (store-m-node false (subvec rc 2))
           new-child {:leaf false :content (vec-concat (:content child) [k (rc 0)])}
           result-child (btree-delete-not-thin new-child key)
           ]
       (store-m-node false (vec-replace content child-index (+ child-index 3)
                                        [result-child (rc 1) new-right-child])))))
 
 (btree-merge-delete [content index left-child right-child key]
   (let [merged {:leaf (:leaf left-child)
                 :content (vec-concat (:content left-child) [(content index)] (:content right-child))}
         result (btree-delete-not-thin merged key)]
     (store-m-node false (vec-replace content (- index 1) (+ index 2) [result]))))
 
 (btree-merge-left-delete [content child-index child left-child key]
   (btree-merge-delete content (- child-index 1) left-child child key))
 
 (btree-merge-right-delete [content child-index child right-child key]
   (btree-merge-delete content  (+ child-index 1) child right-child key))
 
 (btree-delete-in-child [content child-index key]
   (let [child (load-node (content child-index))]
     (if (not (btree-is-thin child))
       (let [new-child (btree-delete-not-thin child key)]
         (store-m-node false (vec-replace content child-index (+ child-index 1) [new-child])))
       (let [left-child-index (- child-index 2)
             has-left-sibling (>= left-child-index 0)
             right-child-index (+ child-index 2)
             has-right-sibling (< right-child-index (count content))]        
         (if has-left-sibling 
           (let [left-child (load-node (content left-child-index))]
             (if (not (btree-is-thin left-child))
               (btree-rotate-right-delete content child-index child left-child key)
               (if has-right-sibling
                 (let [right-child (load-node (content right-child-index))]
                   (if (not (btree-is-thin right-child))
                     (btree-rotate-left-delete content child-index child right-child key)
                     (btree-merge-left-delete content child-index child left-child key)))
                 (btree-merge-left-delete content child-index child left-child key))))
           (if has-right-sibling
             (let [right-child (load-node (content right-child-index))]
               (if (not (btree-is-thin right-child))          
                 (btree-rotate-left-delete content child-index child right-child key)
                 (btree-merge-right-delete content child-index child right-child key)))    
             (violation "Impossible, every child is supposed to have at least one sibling!")))))))
 
 (btree-delete-not-thin [btree key]
   (if (:leaf btree)
     (let [content (:content btree)
           {i :found} (find-key-in-content 0 1 key content)]
       (if i
         (store-m-node true (vec-concat (subvec content 0 i) (subvec content (+ i 1))))
         (store-m-node (:leaf btree) (:content btree))))
     (let [content (:content btree)
           {f :found nf :not-found} (find-key-in-content 1 2 key content)]
       (if f
         (btree-delete-internal-key content key f)
         (btree-delete-in-child content (- nf 1) key)))))
 
 (btree-delete- [btree key]
   (let [result-s (btree-delete-not-thin (load-node btree) key)
         result (load-node result-s)]
     (if (and (not (:leaf result)) (= (count (:content result)) 1))  
       ((:content result) 0)
       result-s)))
 
 (btree-find- [btree key]
   (let [m-node (load-node btree)
         content (:content m-node)]
     (if (:leaf m-node)
       (let [f (:found (find-key-in-content 0 1 key content))]
         (if f (content f) nil))
       (let [r (find-key-in-content 1 2 key content)
             {f :found nf :not-found} r]
         (if f
           (content f)
           (btree-find- (content (- nf 1)) key))))))

 (count-content [address-count content end-index]
   (loop [index 0
          count 0]
     (if (>= index end-index)
       count
       (recur (+ index 1)
              (+ count (if (odd? index)
                         1
                         (address-count (content index))))))))    
 
 (btree-indexed-find- [btree address-count offset key]
   (let [m-node (load-node btree)
         content (:content m-node)]
     (if (:leaf m-node)
       (let [r (find-key-in-content 0 1 key content)
             f (:found r)
             nf (:not-found r)]
         (if f
           {:index (+ f offset) :content (content f)}
           {:index (+ nf offset)}))
       (let [r (find-key-in-content 1 2 key content)
             {f :found nf :not-found} r]
         (if f
           {:content (content f) :index (+ offset (count-content address-count content f))} 
           (btree-indexed-find- (content (- nf 1)) address-count
                                (+ offset (count-content address-count content (- nf 1)))
                                key))))))

 (reduce-with-index [f v coll]
   (let [g (fn [[u index] c] [(f u index c) (+ index 1)])]
     (first (reduce g [v 0] coll))))
 
 (btree-flatfold- [btree f-content f-address v]
   (let [m-node (load-node btree)
         content (:content m-node)]
     (if (:leaf m-node)
       (reduce f-content v content)
       (reduce-with-index
         (fn [v index elem]
           (if (odd? index)
             (f-content v elem)
             (f-address v elem)))
         v content))))

 (btree-fold- [btree f v]
   (let [f-address (fn [v address] (btree-fold- address f v))]
     (btree-flatfold- btree f f-address v)))

 (btree-range-retrieve- [btree address-count range-start range-end index collected]
   (let [f-content (fn [[index collected] c]
                     (if (and (>= index range-start) (< index range-end))
                       [(+ index 1) (cons c collected)]
                       [(+ index 1) collected]))
         f-address (fn [[index collected] address]
                     (if (>= index range-end)
                       [index collected]
                       (let [count (address-count address)]
                         (if (< (- (+ index count) 1) range-start)
                           [(+ index count) collected]
                           (btree-range-retrieve- address address-count range-start range-end index collected)))))]
     (btree-flatfold- btree f-content f-address [index collected])))
                           
 ]
(reify BTreePool

  (btree-empty [this] (btree-empty-))
  (btree-insert [this btree x] (btree-insert- btree x))
  (btree-delete [this btree key] (btree-delete- btree key))
  (btree-find [this btree key] (btree-find- btree key))
  (btree-indexed-find [this btree address-count key] (btree-indexed-find- btree address-count 0 key))
  (btree-range-retrieve [this btree address-count range-start range-end]
    (apply vector (reverse (second (btree-range-retrieve- btree address-count range-start range-end 0 '())))))
  (btree-fold [this btree f v] (btree-fold- btree f v))
  (btree-flatfold [this btree f-content f-address v] (btree-flatfold- btree f-content f-address v))
  (btree-count [this btree] (btree-fold- btree (fn [v c] (+ v 1)) 0))
  (btree-dir [this btree] (apply vector (reverse (btree-fold- btree (fn [v c] (cons c v)) '()))))
  (btree-store-node [this] store-node)
  (btree-load-node [this] load-node)
  (btree-cmp [this] param-cmp)

)))

  