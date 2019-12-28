(ns camelot.model.bounding-box
  (:require
   [camelot.spec.schema.state :refer [State]]
   [schema.core :as sch]
   [camelot.util.db :as db]))

(def query (db/with-db-keys :bounding-box))

(sch/defrecord TBoundingBox
    [bounding-box-dimension-type :- sch/Str,
     bounding-box-min-x :- sch/Num,
     bounding-box-min-y :- sch/Num,
     bounding-box-width :- sch/Num,
     bounding-box-height :- sch/Num]
  {sch/Any sch/Any})

(sch/defrecord BoundingBox
    [bounding-box-id :- sch/Int
     bounding-box-dimension-type :- sch/Str
     bounding-box-min-x :- sch/Num
     bounding-box-min-y :- sch/Num
     bounding-box-width :- sch/Num
     bounding-box-height :- sch/Num]
  {sch/Any sch/Any})

(def ^:private bounding-box map->BoundingBox)
(def tbounding-box map->TBoundingBox)

(sch/defn create!
  [state :- State
   data :- TBoundingBox]
  (let [record (query state :create<! data)]
    (assoc data :bounding-box-id (int (:1 record)))))

(sch/defn delete!
  [state :- State
   id :- sch/Int]
  (query state :delete! {:bounding-box-id id})
  nil)
