(ns hotscrap.ui
  (:require [clojure.string :as str]
            [hotscrap.core :as c]))

(def all-heroes );;(keys hotscrap.scrapper/odds))

(defn parse-heroname [name]
  (loop [tests all-heroes]
    (if (.contains 
          (.replaceAll (str/lower-case (first tests)) "[' .]" "") 
          (str/lower-case name))
      (first tests)
      (recur (rest tests)))))

(defn parse-heronames [namestring]
  (map parse-heroname (str/split namestring #" ")))

(defmacro counter [Map & heroes]
  `(c/counter ~(keyword Map) ~(str/join \space heroes)))
