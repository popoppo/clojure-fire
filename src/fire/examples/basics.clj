(ns fire.examples.basics
  (:require
   [fire.core :refer [fire]]))

;; lein exec -p src/clojure_fire/examples/basics.clj

(defn hello
  "Say hello!!"
  []
  (println "hello"))

(defn hello-world
  "Target can be spcecified with :name"
  [& {:keys [name]
      :or {name "World"}}]
  (println "Hello " name))

(defn add
  ([[x y]] (+ x y))
  ([x y] (+ x y)))

(defn multi
  [x y & vs]
  (apply * (list* x y vs)))

(defn area
  "Calc area from x and y"
  [{:keys [x y]}]
  (* x y))

(defn echo
  [& opts]
  (println opts))

(when-let [v (fire)]
  (println v))

