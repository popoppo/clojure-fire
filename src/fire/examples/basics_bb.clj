#!/usr/bin/env bb

;; export BABASHKA_CLASSPATH=src

(ns fire.examples.basics-bb
  (:require [fire.core :refer [fire]]))

(defn hello
  "Say hello!!"
  []
  "Hello")

(defn hello-world
  "Target can be spcecified with :name"
  [& {:keys [name]
      :or {name "World"}}]
  (str "Hello " name))

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
