#!/usr/bin/env bb

(ns fire.examples.basics-bb
  (:require
   [babashka.deps :as deps]))

(deps/add-deps '{:deps {org.clojars.popoppo/clojure-fire {:mvn/version "0.0.4"}}})
(require '[fire.core :refer [fire]])

(defn hello
  "Say hello!!"
  []
  "Hello")

(defn hello-world
  "Target can be specified with :name"
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

(fire)
