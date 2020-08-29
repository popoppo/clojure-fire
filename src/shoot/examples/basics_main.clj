(ns shoot.examples.basics-main
  (:require [shoot.core :refer [shoot]]))

;; bb -cp src -m shoot.examples.basics-main
;; clj -m shoot.examples.basics-main

(defn hello
  []
  (println "hello"))

(defn hello-world
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
  [{:keys [x y]}]
  (* x y))

(defn echo
  [& opts]
  (println opts))

(defn -main
  [& args]
  (when-let [v (shoot)]
    (println v)))

