(ns shoot.examples.basics
  (:require [shoot.core :refer [shoot]]))

;; lein exec -p src/shoot/examples/basics.clj

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
  [{:keys [x y]}]
  (* x y))

(defn echo
  [& opts]
  (println opts))

(when-let [v (shoot)]
  (println v))

