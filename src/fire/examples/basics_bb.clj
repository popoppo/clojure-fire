#!/usr/bin/env bb

;; export BABASHKA_CLASSPATH=src

(ns fire.examples.basics-bb
  (:require [fire.core :refer [fire]]))

(defn hello
  "Just say hello!!"
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
  "Calc area with x and y"
  [{:keys [x y]}]
  (* x y))

(defn echo
  {:fire {:p {:description "Port number"}
          :q {:description "Quiet mode"}
          :now {:default
                #(-> (java.time.ZonedDateTime/now)
                     (.format (java.time.format.DateTimeFormatter/ofPattern "HH:mm")))}}}
  [& opts]
  (println opts))

(when-let [v (fire)]
  (println v))
