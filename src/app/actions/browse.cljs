(ns app.actions.browse
  (:require
   [app.actions.entrypoint :as actions]
   [lambdaisland.fetch :as fetch]
   [promesa.core :as p]
   ["react-photo-view" :refer [PhotoProvider PhotoView]]
   [re-frame.core :as rf]
   [app.config :as config]
   [app.utils :as ut]))

(def images 
  [;; poulet
   ["Poulet"
    "April 2023"
    "https://bafybeieqayrn3rfwktwsu3m5qxnyy3qom2qpsc4bexgripirloei2n37ji.ipfs.nftstorage.link/"]
   ;; grenouille
   ["Grenouille"
    "April 2023"
    "https://bafybeigvead5nydfmgpounsaaby6b25oyyqzaxfd4q65qucahcrnecjufu.ipfs.nftstorage.link/"]
   ;; teide
   ["Teide"
    "April 2023"
    "https://bafybeicxu23xiav4wygaeleqyi37jqq7372q3k4ffeohb6e3otlypyi5pe.ipfs.nftstorage.link/"]
   ;; montagne
   ["Montagne"
    "May 2023"
    "https://bafybeiaeitwnvburowqoxzfhzzmuxrrtyep2vm2qgzaroxo4ulilkjwuhm.ipfs.nftstorage.link/"]
   ;; oeil
   ["Oeil"
    "May 2023"
    "https://bafybeib3gi6p6dgvap7ikqziupcnpadom4qyopkwrtk7apws3gdny7b3qq.ipfs.nftstorage.link/"]
   ;; cross
   ["Cross"
    "June 2023"
    "https://bafybeihg7xc7hnuquinodenx6fsf3cgresguns435fkqrmmectrqrw2vky.ipfs.nftstorage.link/"]
   ;; hey
   ["Hey"
    "July 2023"
    "https://bafybeie2z3cohdet3r76uu75hdyzgk25brma67qilqe262howlvef23wce.ipfs.nftstorage.link/"]
   ])

(defn fetch-art-pieces! []
  (p/let [api-url (str "https://gnosisapi.nftscan.com/api/v2/assets/" config/art-contract)
          result (fetch/get api-url {:accept :json :headers {"X-API-KEY" config/nftscan-key}})]
    (rf/dispatch [:set ::art-pieces-data (-> (ut/j->c (:body result)) :data :content)])))

(rf/reg-sub 
 ::art-pieces 
 :<- [:get ::art-pieces-data]
 (fn [data]
   (some->> data
            (map (fn [art]
                   (select-keys art [:name :description :token_id :image_uri :amount]))))))

(defn art-pieces []
  (if-not @(rf/subscribe [:get ::art-pieces-data])
    (fetch-art-pieces!))
  (fn []
    [:<>
     [:div {:class "hand-written font-bold text-4xl"} "Art pieces"]
     [:br ]
     (if-let [art-pieces @(rf/subscribe [::art-pieces])]
       [:div {:class "grid gap-3 grid-cols-3"}
        (for [{:keys [:image_uri] :as art-piece} art-pieces]
          ^{:key image_uri}
          [:img {:class "cursor-pointer"
                 :src image_uri 
                 :on-click #(actions/send ::see-details art-piece)}])]
       [:div "fetching..."])]))

(rf/reg-sub
 ::collected?
 :<- [:app.actions.collect/art-pieces-collected]
 (fn [collected [_ token_id]]
   (some #(= % token_id) (map :token_id collected))))

(defn art-piece [ap]
  (fn []
    [:div 
     [:div {:class "hand-written font-bold text-4xl"} (:name ap)]
     [:div {:class "font-mono"} (:description ap) " / " (- config/max-mint (:amount ap)) " left"]
     [:> PhotoProvider
      [:> PhotoView {:src (:image_uri ap)}
       [:img {:src (:image_uri ap) :class "cursor-zoom-in"}]]]
     [:br]
     [:button {:class "btn-blue mr-4 disabled:opacity-70"
               :disabled @(rf/subscribe [::collected? (:token_id ap)])
               :on-click #(actions/send :app.actions.collect/collect ap)} 
      "Collect digital copy"]
     [:button {:class "btn-gray"
               :on-click #(actions/send ::buy ap)} 
      "Buy original copy"]]))

(defn buy-message [ap]
  [:div
   "Send me your offer via the chat below "
   "if you're willing to acquire the one and only physical copy of:"
   [:span {:class "hand-written text-4xl ml-4 "} (:name ap)]])

(defmethod actions/get-action ::buy
  [_action _db ap]
  {:component buy-message
   :state ap})

(defmethod actions/get-action ::see-details
  [_action _db ap]
  {:component art-piece
   :state ap})

(defmethod actions/get-action ::browse
  []
  {:component art-pieces})

(actions/add-primary-action ::browse "See art pieces" {:default? true})