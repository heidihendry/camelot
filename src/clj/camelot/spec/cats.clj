(ns camelot.spec.cats
  "Spec utils for cats."
  (:require [cats.monad.either :as either]
            [clojure.spec.gen.alpha :as gen]
            [cats.core :as m]
            [clojure.spec.alpha :as s]))

(defn- matches-side? [pred spec]
  #(s/and (either/either? %)
          (pred %)
          (s/valid? spec (m/extract %))))

(defn right-of? [spec]
  (s/with-gen (matches-side? either/right? spec)
    #(gen/fmap either/right
               (s/gen spec))))

(defn left-of? [spec]
  (s/with-gen (matches-side? either/left? spec)
    #(gen/fmap either/left
               (s/gen spec))))

(defn either? [left-spec right-spec]
  (s/or :left (left-of? left-spec)
        :right (right-of? right-spec)))
