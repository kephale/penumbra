;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns penumbra.geometry
  (:use [clojure.contrib.lazy-seqs :only (primes)]))

;;;

(defn- prime-factors
  "Returns prime factors of a number"
  ([n] (prime-factors primes [] n))
  ([primes factors n]
	 (let [p (first primes)]
	   (cond
		 (= n 1) factors
		 (zero? (rem n p)) (recur primes (conj factors p) (/ n p))
		 :else (recur (rest primes) factors n)))))

(defn rectangle [n]
  (let [factors   (prime-factors n)
        reordered (take (count factors) (interleave factors (reverse factors)))
        sqrt      (int (Math/sqrt n))
        divisor   (reduce #(if (>= sqrt (* %1 %2)) (* %1 %2) %1) 1 reordered)]
    [divisor (/ n divisor)]))

;;;

(defn dot [u v]
  (apply + (map * u v)))

(defn length-squared [v]
  (dot v v))

(defn length [v]
  (Math/sqrt (length-squared v)))

 (defn normalize [v]
  (let [len (length v)]
    (map #(/ % len) v)))

(defn cross [[ax ay az] [bx by bz]]
  [(- (* ay bz) (* az by))
   (- (* az bx) (* ax bz))
   (- (* ax bz) (* ay bx))])

(defn radians [x]
  (* (/ Math/PI 180) x))

(defn degrees [x]
  (* (/ 180 Math/PI) x))

(defn polar [v]
  (condp = (count v)
    2 (let [[x y] v]
         [(degrees (Math/atan2 y x))
          (length v)])
    3 (let [[x y z] v
            len (length v)]
        [(first (polar [x z]))
         (degrees (Math/asin (/ y len)))
         len])))

(defn cartesian [v]
  (if (number? v)
    (cartesian [v 1])
    (condp = (count v)
      1 (cartesian [v 1])
      2 (let [[theta r] v
              theta (radians theta)]
          (map (partial * r) [(Math/cos theta) (Math/sin theta)]))
      3 (let [[theta phi r] v
              theta (radians theta)
              phi (radians (- 90 phi))
              ts (Math/sin theta)
              tc (Math/cos theta)
              ps (Math/sin phi)
              pc (Math/cos phi)]
          [(* r ps tc) (* r pc) (* r ps ts)]))))

;;;

(defn identity-matrix []
  [1 0 0 0
   0 1 0 0
   0 0 1 0
   0 0 0 1])

(defn translation-matrix [x y z]
  [1 0 0 x
   0 1 0 y
   0 0 1 z
   0 0 0 1])

(defn scaling-matrix [x y z]
  [x 0 0 0
   0 y 0 0
   0 0 z 0
   0 0 0 1])

(defn rotation-matrix [theta x y z]
  (let [s (Math/sin (* Math/PI (/ theta 180)))
        c (Math/cos (* Math/PI (/ theta 180)))
        t (- 1 c)]
  [(+ c (* t x x))        (- (* t x y) (* s z))   (+ (* t x z) (* s y))   0
   (+ (* t x y) (* s z))  (+ (* t y y) c)         (- (* t y z) (* s x))   0
   (- (* t x z) (* s y))  (+ (* t y z) (* s x))   (+ (* t z z) c)         0
   0                      0                       0                       1]))

(defn- index [m i j] (m (+ i (* j 4))))

(defn mult-matrix [a b]
  (let [indices (for [i (range 4) j (range 4)] [i j])
        traverse (fn [[i j]] (apply + (map #(* (index a % i) (index b j %)) (range 4))))]
    (vec (map traverse indices))))

(defn apply-matrix [m v]
  (let [traverse-fn (fn [i] #(* (v %) (index m % i)))]
    (vec (map #(apply + (map (traverse-fn %) (range 4))) (range 4)))))

;;;

