(ns phlegmaticprogrammer.btree.tests
  (:use phlegmaticprogrammer.btree)
  (:use clojure.test))

(def param-cmp (fn [x y] (.compareTo (:key x) (:key y))))

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