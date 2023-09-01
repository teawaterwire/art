(ns app.actions.collect
  (:require
   [app.actions.entrypoint :as actions]
   [promesa.core :as p]
   [lambdaisland.fetch :as fetch]
   ["@zerodevapp/sdk" :refer [getZeroDevSigner getPrivateKeyOwner]]
   [app.utils :as ut]
   [app.config :as config]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [clojure.string :as str]))

(defn fetch-art-pieces-collected! [addr]
  (p/let [api-url (str config/nftscan-base-api "account/own/" 
                       addr
                       "?erc_type=erc1155"
                       "&contract_address=" config/art-contract)
          result (fetch/get api-url {:accept :json :headers {"X-API-KEY" config/nftscan-key}})]
    (rf/dispatch [:set ::art-pieces-collected-data (-> (ut/j->c (:body result)) :data :content)])))

(defn connect! []
  (rf/dispatch [:set ::connecting? true])
  (p/let [pk (str "0x"
                  (.. @(rf/subscribe [:get :app.auth/user :secret])
                      (toString 16)
                      (padStart 64 "0")))
          ^js signer (getZeroDevSigner (clj->js
                                        {:projectId config/zerodev-id
                                         :owner (getPrivateKeyOwner pk)}))
          addr (.getAddress signer)]
    (fetch-art-pieces-collected! addr)
    (rf/dispatch [:set ::connecting? false])
    (rf/dispatch [:set ::wallet-address addr])
    (rf/dispatch [:set ::signer signer])))

(defn collect! [ap]
  (rf/dispatch [:set ::collecting? true])
  (p/let [^js signer @(rf/subscribe [:get ::signer])
          artId (js/parseInt (:token_id ap))
          payload (str "0xa0712d68" ;; mint(uint256)
                       (.. artId (toString 16) (padStart 64 "0")))
          tx-params {:to config/art-contract
                     :data payload
                     :value 0}
          ^js tx (.sendTransaction signer (clj->js tx-params))
          ^js receipt (.wait tx)]
    (rf/dispatch [:set ::just-collected? (:token_id ap) true])
    (actions/send ::minted [ap (.-transactionHash receipt)])))

(defn c-minted [[ap hash]]
  [:div "You collected the art piece: "
   [:span {:class "hand-written text-4xl ml-4 "} (:name ap)]
   [:br]
   [:span "Here's the blockchain "
    [:a.underline {:href (str/replace config/aa-explorer-base-url "HASH" hash) :target "_blank"} "receipt"]
    "."]])

(defmethod actions/get-action ::minted
  [_action _db params]
  {:component c-minted
   :state params})

(defn c-connect []
  [:button {:class "btn-blue disabled:opacity-70"
                :disabled @(rf/subscribe [:get ::connecting?])
                :on-click #(connect!)}
       (if @(rf/subscribe [:get ::connecting?]) "Connecting..." "Connect")])

(defn c-secret-revealed []
  (js/setTimeout #(rf/dispatch [:set ::secret-revealed? false]) 7000)
  (fn []
    [:code.text-xs @(rf/subscribe [:get :app.auth/user :secret])]))

(defn c-collect [ap]
  [:div
   "You have to connect a wallet to collect:"
   [:span {:class "hand-written text-4xl ml-4 "} (:name ap)]
   (if-let [addr @(rf/subscribe [:get ::wallet-address])]
     [:div 
      "✅ Connected as: " [:code.text-xs addr] [:br]
      "🚨 Make sure to save the session id: " 
      (if @(rf/subscribe [:get ::secret-revealed?])
        [c-secret-revealed]
        [:code
         {:class "cursor-pointer text-xs text-blue-500"
          :on-click #(rf/dispatch [:set ::secret-revealed? true])} 
         "reveal for 7 seconds"])
      [:br] [:br]
      (if (or
           @(rf/subscribe [:get ::just-collected? (:token_id ap)])
           @(rf/subscribe [:app.actions.browse/collected? (:token_id ap)]))
        [:div "Art piece already collected. " 
         [:button {:class "text-blue-500 hover:underline font-bold"
              :on-click #(actions/send ::browse-collected)} "See your collection"]]
        [:button {:class "btn-blue disabled:opacity-70"
                  :disabled @(rf/subscribe [:get ::collecting?])
                  :on-click #(collect! ap)}
         (if @(rf/subscribe [:get ::collecting?]) "Collecting..." "Collect")])]
     [:div
      [:br]
      [c-connect]])])

(defmethod actions/get-action ::collect
  [_action _db ap]
  {:component c-collect
   :state ap})

(rf/reg-sub 
 ::art-pieces-collected 
 :<- [:get ::art-pieces-collected-data]
 (fn [data]
   (some->> (seq data)
            (map (fn [art]
                   (select-keys art [:name :description :token_id :image_uri :amount]))))))

(defn c-refresh []
  (let [delay 60
        timer (r/atom 0)
        refresh! (fn []
                  (reset! timer 0)
                  (fetch-art-pieces-collected! @(rf/subscribe [:get ::wallet-address])))]
    (js/setInterval #(swap! timer inc) 1000)
    (fn []
      [:div.text-left.text-xs "("
       (if (> @timer delay)
         [:button {:class "text-blue-500 hover:underline font-bold"
                   :on-click refresh!} 
          "Refresh now"]
         [:span "Refresh in " (- delay @timer) " seconds"])
       ")"])))

(defn c-collected []
  [:<>
   [c-refresh]
   [:br]
   (if-let [art-pieces-collected @(rf/subscribe [::art-pieces-collected])]
     [:div {:class "grid gap-3 grid-cols-3"}
      (for [{:keys [:image_uri] :as art-piece} art-pieces-collected]
        ^{:key image_uri}
        [:img {:class "cursor-pointer"
               :src image_uri
               :on-click #(actions/send :app.actions.browse/see-details art-piece)}])]
     [:span
      "You haven't collected any art pieces yet. "
      [:button {:class "text-blue-500 hover:underline font-bold"
                :on-click #(actions/send :app.actions.browse/browse)} "See art pieces"]
      "."])])

(defn c-collected-pre []
  [:<> 
   [:div {:class "hand-written font-bold text-4xl"} "Your collection"]
   (if @(rf/subscribe [:get ::wallet-address])
     [c-collected]
     [:div.mt-2
      [c-connect]])])

(defmethod actions/get-action ::browse-collected
  []
  {:component c-collected-pre})

(actions/add-primary-action ::browse-collected "Your collection" {:default? true})