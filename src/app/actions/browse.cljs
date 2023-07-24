(ns app.actions.browse
  (:require
   [app.actions.entrypoint :as actions]))

(defn art-pieces []
  [:div "here are the art pieces"])

(defmethod actions/get-action ::browse
  []
  {:component art-pieces})

(actions/add-primary-action ::browse "See art pieces" {:default? true})