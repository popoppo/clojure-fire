#!/usr/bin/env bb

;; export BABASHKA_CLASSPATH=src

(ns shoot.examples.basics-bb
  (:require [shoot.core :refer [shoot]]))

(defn hello
  []
  "Hello")

(defn hello-world
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
  [{:keys [x y]}]
  (* x y))

(defn echo
  [& opts]
  (println opts))

(when-let [v (shoot)]
  (println v))
