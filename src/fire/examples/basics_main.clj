(ns fire.examples.basics-main
  (:require
   [fire.core :refer [fire]]))

;; bb -cp src -m fire.examples.basics-main
;; clj -m fire.examples.basics-main

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

(defn -main
  [& args]
  (fire))

