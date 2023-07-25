(ns app.actions.onboarding
  (:require 
   [app.actions.entrypoint :as actions]))

(defn c-onboarding []
  [:div {:class "text-center"}
   [:span.font-bold.text-xl {:class "hand-written text-3xl"} "Welcome to my Art gallery 🤗"]
   [:br]
   [:div.text-left.mt-2
    "My name is Theo. I draw with double ended fiber-tip pens. If you see something you like, feel free to collect  it. "
    [:button {:class "text-blue-500 hover:underline font-bold"
              :on-click #(actions/send :app.actions.browse/browse)} "See art pieces"]
    "."]
   [:br]
   [:div.italic.text-left "Want to keep this session? Save the session id found below." [:br] 
    "Need help? Just start a chat with me"]])

(defmethod actions/get-action ::onboarding
  [_ _ args]
  {:component c-onboarding
   :state {:username (first args)}})