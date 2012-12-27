(ns phlegmaticprogrammer.btree.tests
  (:use phlegmaticprogrammer.btree)
  (:use clojure.test))

(defn extract-key [k]
  (str (if (:key k) (:key k) k)))

(def param-cmp (fn [x y] (.compareTo (extract-key x) (extract-key y))))

(defn store-node [m-node] {:stored-node m-node})

(defn load-node [address] (:stored-node address))

(defn test-content [num] {:key (str num) :content (str "This is number: " num)})

(defn btree-set [m btree] (apply sorted-set-by (cons param-cmp (btree-dir m btree))))

(defn cmp-content [x y]
  (and
   (= 0 (param-cmp x y))
   (= (:content x) (:content y))))

(defn btree-set-eq [a b]
  (if (empty? a)
    (empty? b)
    (if (empty? b)
      false
      (if (cmp-content (first a) (first b))
        (btree-set-eq (next a) (next b))
        false))))

(defn btree-ok [m btree]
  (let [v (btree-dir m btree)
        w (sort-by :key v)]
    (= (seq v) (seq w))))

(defn random-btree [m N count]
  (if (<= count 0)
    (btree-empty m)
    (let [n (rand-int N)
          btree (random-btree m N (- count 1))
          r (btree-insert m btree (test-content n))]
      r)))
    
(defn ins-test-step [m N btree]
  (let [n (rand-int N)
        c (test-content n)
        r (btree-insert m btree c)
        ] 
    (do
      (is (btree-ok m r))
      (is (btree-set-eq (conj (btree-set m btree) c)  (btree-set m r)))
      r)))
 
(defn del-test-step [m N btree]
  (let [n (rand-int N)
        c (test-content n)
        r (btree-delete m btree c)] 
    (do
      (is (btree-ok m r))
      (is (btree-set-eq (disj (btree-set m btree) c)  (btree-set m r)))
      r)))
     
(defn btree-test [m N count]
  (loop [i 0
         btree (btree-empty m)]
    (if (< i count)
      (recur (+ i 1)
             ((if (= 0 (rand-int 2))
                del-test-step
                ins-test-step) m N btree))
      btree)))

(defn make-btree-pool [t] (btree-pool {:param-t t :param-cmp param-cmp :load-node load-node :store-node store-node}))

(defn test-for-t [t]
  (let
      [m  (btree-pool {:param-t t
                       :param-cmp param-cmp
                       :load-node load-node
                       :store-node store-node})]
    (btree-test m 30 200)
    (btree-test m 800 50)))

(deftest test-btree
  (test-for-t 2)
  (test-for-t 3)
  (test-for-t 100))

(deftest test-indexed-find
  (let [pool (make-btree-pool 3)
        btree (random-btree pool 1000 100)
        dir (btree-dir pool btree)
        count (count dir)
        address-count #(btree-count pool %)
        test-positive (fn [index content]
                        (let [r (btree-indexed-find pool btree address-count (:key content))]
                          (is (= index (:index r)))
                          (is (= content (:content r)))))
        test-negative (fn [key]
                        (let [r (btree-indexed-find pool btree address-count key)
                              find (btree-find pool btree key)
                              index (:index r)]
                          (if find
                            (is (= find (:content r)))
                            (is (and (= nil (:content r))
                                     (>= index 0)
                                     (<= index count)
                                     (if (> index 0)
                                       (< (param-cmp (dir (- index 1)) key) 0)
                                       true)
                                     (if (< index count)
                                       (> (param-cmp (dir index) key) 0)
                                       true))))))
       ]
    ;; test positives
    (loop [i 0]
      (if (< i count)
        (do
          (test-positive i (dir i))
          (recur (+ i 1)))))
    ;; test negatives
    (loop [i -10]
      (if (< i 1010)
        (do
          (test-negative i)
          (recur (+ i 1)))))))

(deftest test-range-retrieve
  (let [pool (make-btree-pool 3)
        btree (random-btree pool 1000 100)
        dir (btree-dir pool btree)
        count (count dir)
        address-count #(btree-count pool %)
        test (fn [index1 index2]
               (let [range (btree-range-retrieve pool btree address-count index1 index2)
                     i1 (if (< index1 0) 0 (if (> index1 count) count index1))
                     i2 (if (> index2 count) count (if (< index2 i1) i1 index2))
                     ]
                 (is (= range (subvec dir i1 i2)))))
        ]
    (loop [i1 -20]
      (if (< i1 (+ count 20))
        (do
          (loop [i2 -20]
            (if (< i2 (+ count 20))
              (do (test i1 i2) (recur (+ i2 1)))))
          (recur (+ i1 1)))))))
