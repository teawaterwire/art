(ns app.actions.collect
  (:require
   
   [app.actions.entrypoint :as actions]
   [promesa.core :as p]
   [lambdaisland.fetch :as fetch]
   ["@zerodevapp/sdk" :refer [getZeroDevSigner getRPCProviderOwner]]
   ["magic-sdk" :refer [Magic]]

   [app.utils :as ut]
   [app.config :as config]
   [re-frame.core :as rf]))

(defonce magic (new Magic config/magic-key
                (clj->js {:networks {:rpcUrl config/rpc-url
                                     :chainId config/chain-id}})))

(defn connect! []
  (p/let [_ (.. magic -wallet connectWithUI)
          _ (js/console.log magic)
          ^js signer (getZeroDevSigner (clj->js
                                        {:projectId config/zerodev-id
                                         :owner (getRPCProviderOwner (.-rpcProvider magic))}))
          addr (.getAddress signer)]
    (rf/dispatch [:set ::wallet-address addr])
    (rf/dispatch [:set ::signer signer])))

(defn fetch-art-pieces-collected! [addr]
  (p/let [api-url (str "https://gnosisapi.nftscan.com/api/v2/account/own/" 
                       addr
                       "?erc_type=erc1155"
                       "&contract_address=" config/art-contract)
          result (fetch/get api-url {:accept :json :headers {"X-API-KEY" config/nftscan-key}})]
    (rf/dispatch [:set ::art-pieces-collected-data (-> (ut/j->c (:body result)) :data :content)])))

(defn collect! [ap]
  (rf/dispatch [:set ::collecting? true])
  (p/let [^js signer @(rf/subscribe [:get ::signer])
          addr @(rf/subscribe [:get ::wallet-address])
          artId (js/parseInt (:token_id ap))
          payload (str "0xa0712d68" ;; mint(uint256)
                       (.. artId (toString 16) (padStart 64 "0")))
          tx-params {:from addr
                     :to config/art-contract
                     :data payload}
          ^js tx (.sendTransaction signer (clj->js tx-params))
          ^js receipt (.wait tx)]
    (rf/dispatch [:set ::collecting? false])
    (actions/send ::minted [ap (.-transactionHash receipt)])))

(defn c-minted [[ap hash]]
  [:div "You collected the art piece: " 
   [:span {:class "hand-written text-4xl ml-4 "} (:name ap)]
   [:br]
   [:span "Here's the blockchain " 
    [:a.underline {:href (str "https://gnosisscan.io/tx/" hash) :target "_blank"} "receipt"]
    "."]])

(defmethod actions/get-action ::minted
  [_action _db params]
  {:component c-minted
   :state params})

(defn c-collect [ap]
  [:div
  "Download the application " 
   [:a.underline {:href "https://linen.app/" :target "_blank"} "Linen"]
   " on your phone and connect to collect:" 
   [:span {:class "hand-written text-4xl ml-4 "} (:name ap)]
   (if-let [addr @(rf/subscribe [:get ::wallet-address])]
     [:div "✅ Connected as: " [:code.text-xs addr] [:br] [:br]
      (if @(rf/subscribe [:app.actions.browse/collected? (:token_id ap)])
        [:div "Art piece already collected. " 
         [:button {:class "text-blue-500 hover:underline font-bold"
              :on-click #(actions/send ::browse-collected)} "See your collection"]]
        [:button {:class "btn-blue disabled:opacity-70"
                  :disabled @(rf/subscribe [:get ::collecting?])
                  :on-click #(collect! ap)}
         "Collect"])]
     [:div
      [:br]
      [:button {:class "btn-blue"
                :on-click #(connect!)}
       "Connect"]
      [:div.italic.mt-2 "(Go to the 'Actions' tab in Linen to select 'WalletConnect')"]])])

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

(defn c-collected []
  [:<>
   [:div {:class "hand-written font-bold text-4xl"} "Your collection"]
   [:div.text-left.text-xs "("
    [:button {:class "text-blue-500 hover:underline font-bold"
              :on-click #(fetch-art-pieces-collected! @(rf/subscribe [:get ::wallet-address]))} "Refresh"]
    ")"]
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
  (if @(rf/subscribe [:get ::wallet-address])
    [c-collected]
    [:div
     [:button {:class "btn-blue"
               :on-click #(connect!)}
      "Connect"]
     [:div.italic.mt-2 "(Connect with the crypto wallet you used to collect art pieces)"]]))

(defmethod actions/get-action ::browse-collected
  []
  {:component c-collected-pre})

(actions/add-primary-action ::browse-collected "Your collection" {:default? true})