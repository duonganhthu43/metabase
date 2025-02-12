(ns metabase.sync.sync-metadata.fields.fetch-metadata-test
  (:require [clojure.test :refer :all]
            [clojure.walk :as walk]
            [medley.core :as m]
            [metabase.models.database :refer [Database]]
            [metabase.models.table :refer [Table]]
            [metabase.sync.sync-metadata :as sync-metadata]
            [metabase.sync.sync-metadata.fields.fetch-metadata :as sync-fields.fetch-metadata]
            [metabase.test :as mt]
            [metabase.test.mock.toucanery :as toucanery]
            [metabase.util :as u]
            [toucan.db :as db]))

(deftest add-nested-field-columns-test
  (testing "adds nested field columns for one field"
    (let [our-field  {:name      "coherent_json_val"
                      :base-type :type/*
                      :database-type "blah",
                      :database-position 0,
                      :id        1}
          nfc-fields '#{{:name               "incoherent_json_val → b",
                          :database-type     "blah",
                          :base-type         :type/*,
                          :database-position 0,
                          :id                2,
                          :nfc-path          [:incoherent_json_val "b"]}
                         {:name              "coherent_json_val → a",
                          :database-type     "blah",
                          :base-type         :type/*,
                          :database-position 0,
                          :id                3,
                          :nfc-path          [:coherent_json_val "a"]}
                         {:name              "coherent_json_val → b",
                          :database-type     "blah",
                          :base-type         :type/*,
                          :database-position 0,
                          :id                4,
                          :nfc-path          [:coherent_json_val "b"]}}]
      (is (= (#'sync-fields.fetch-metadata/add-nested-field-columns our-field nfc-fields)
             {:name "coherent_json_val",
              :base-type :type/*,
              :database-type "blah",
              :database-position 0,
              :id 1,
              :nested-fields
              #{{:name "coherent_json_val → b",
                 :database-type "blah",
                 :base-type :type/*,
                 :database-position 0,
                 :id 4,
                 :nfc-path [:coherent_json_val "b"]}
                {:name "coherent_json_val → a",
                 :database-type "blah",
                 :base-type :type/*,
                 :database-position 0,
                 :id 3,
                 :nfc-path [:coherent_json_val "a"]}}})))))

;; `our-metadata` should match up with what we have in the DB
(deftest does-metadata-match-test
  (mt/with-temp Database [db {:engine ::toucanery/toucanery}]
    (sync-metadata/sync-db-metadata! db)
    (is (= #{{:name              "id"
              :database-type     "SERIAL"
              :base-type         :type/Integer
              :effective-type    :type/Integer
              :semantic-type     :type/PK
              :pk?               true}
             {:name              "buyer"
              :database-type     "OBJECT"
              :base-type         :type/Dictionary
              :effective-type    :type/Dictionary
              :pk?               false
              :nested-fields     #{{:name              "name"
                                    :database-type     "VARCHAR"
                                    :base-type         :type/Text
                                    :effective-type    :type/Text
                                    :pk?               false}
                                   {:name              "cc"
                                    :database-type     "VARCHAR"
                                    :base-type         :type/Text
                                    :effective-type    :type/Text
                                    :pk?               false}}}
             {:name              "ts"
              :database-type     "BIGINT"
              :base-type         :type/BigInteger
              :effective-type    :type/DateTime
              :coercion-strategy :Coercion/UNIXMilliSeconds->DateTime
              :pk?               false}
             {:name              "toucan"
              :database-type     "OBJECT"
              :base-type         :type/Dictionary
              :effective-type    :type/Dictionary
              :pk?               false
              :nested-fields     #{{:name              "name"
                                    :database-type     "VARCHAR"
                                    :base-type         :type/Text
                                    :effective-type    :type/Text
                                    :pk?               false}
                                   {:name              "details"
                                    :database-type     "OBJECT"
                                    :base-type         :type/Dictionary
                                    :effective-type    :type/Dictionary
                                    :pk?               false
                                    :nested-fields     #{{:name              "weight"
                                                          :database-type     "DECIMAL"
                                                          :base-type         :type/Decimal
                                                          :effective-type    :type/Decimal
                                                          :semantic-type     :type/Category
                                                          :pk?               false}
                                                         {:name              "age"
                                                          :database-type     "INT"
                                                          :base-type         :type/Integer
                                                          :effective-type    :type/Integer
                                                          :pk?               false}}}}}}

           (let [transactions-table-id   (u/the-id (db/select-one-id Table :db_id (u/the-id db), :name "transactions"))
                 remove-ids-and-nil-vals (partial walk/postwalk #(if-not (map? %)
                                                                   %
                                                                   ;; database-position isn't stable since they are
                                                                   ;; defined in sets. changing keys will change the
                                                                   ;; order in the set implementation
                                                                   (m/filter-vals some? (dissoc % :id :database-position))))]
             (remove-ids-and-nil-vals (#'sync-fields.fetch-metadata/our-metadata (Table transactions-table-id))))))))
