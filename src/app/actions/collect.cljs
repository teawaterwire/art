(ns app.actions.collect
  (:require
   [clojure.string :as str]
   [app.actions.entrypoint :as actions]
   [promesa.core :as p]
   [lambdaisland.fetch :as fetch]
   ["@walletconnect/sign-client" :as SignClient]
   ["@multishq/walletconnect-modal" :refer [WalletConnectModal]]
   [app.utils :as ut]
   [app.config :as config]
   [re-frame.core :as rf]))

(def project-id config/walletconnect-project-id)
(def gnosis-chain config/gnosis-chain)

(defn- instanciate-modal
  [project-id]
  (WalletConnectModal. (clj->js {:projectId        project-id
                                 :standaloneChains [gnosis-chain]})))

(defn- make-sign-client
  [project-id]
  (.. SignClient/default (init #js {:projectId project-id})))

(defn connect! []
  (p/let [^js modal (instanciate-modal project-id)
          ^js sign-client (make-sign-client project-id)
          ^js signer (.. sign-client (connect (clj->js {:requiredNamespaces
                                                        {:eip155 {:methods ["personal_sign"
                                                                            "eth_sendTransaction"
                                                                            "eth_signTypedData"]
                                                                  :chains  [gnosis-chain]
                                                                  :events  ["accountsChanged"]}}})))
          _ (.. modal (openModal (clj->js {:uri (-> signer .-uri)})))
          ^js approval (-> signer .-approval)
          session (approval)
          _ (.. modal (closeModal))]
    (rf/dispatch [:set ::sign-client sign-client])
    (rf/dispatch [:set ::session (ut/j->c session)])))

(defn collect! [artId]
  (let [sign-client @(rf/subscribe [:get ::sign-client])
        session @(rf/subscribe [:get ::session])
        addr @(rf/subscribe [::wallet-address])
        payload (str "0xa0712d68" ;; mint(uint256)
                     (.. artId (toString 16) (padStart 64 "0")))]
    (rf/dispatch [:set ::collecting? true])
    (-> 
     (.. sign-client (request (clj->js {:topic   (:topic session)
                                        :chainId gnosis-chain
                                        :request {:method "eth_sendTransaction"
                                                  :params [{:from addr
                                                            :to config/art-contract
                                                            :data payload}]}})))
     (.then (fn []))
     (.catch (fn []))
     (.finally (fn [] (rf/dispatch [:set ::collecting? false]))))))

(rf/reg-sub
 ::wallet-address
 :<- [:get ::session :namespaces :eip155 :accounts]
 (fn [accounts]
   (some-> accounts first (str/split ":") last)))

(defn c-collect [ap]
  [:div
  "Download the application " 
   [:a.underline {:href "https://linen.app/" :target "_blank"} "Linen"]
   " on your phone and connect to collect:" 
   [:span {:class "hand-written text-4xl ml-4 "} (:name ap)]
   (if-let [addr @(rf/subscribe [::wallet-address])]
     [:div "✅ Connected as: " [:code.text-xs addr] [:br] [:br]
      [:button {:class "btn-blue disabled:opacity-70"
                :disabled @(rf/subscribe [:get ::collecting?])
                :on-click #(collect! (js/parseInt (:token_id ap)))}
       "Collect"]]
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


(defn fetch-art-pieces-collected! [addr]
  (p/let [api-url (str "https://gnosisapi.nftscan.com/api/v2/account/own/" 
                       addr
                       "?erc_type=erc1155"
                       "&contract_address=" config/art-contract)
          result (fetch/get api-url {:accept :json :headers {"X-API-KEY" config/nftscan-key}})]
    (rf/dispatch [:set ::art-pieces-collected-data (-> (ut/j->c (:body result)) :data :content)])))

(rf/reg-sub 
 ::art-pieces-collected 
 :<- [:get ::art-pieces-collected-data]
 (fn [data]
   (some->> data
            (map (fn [art]
                   (select-keys art [:name :description :token_id :image_uri :amount]))))))

(defn c-collected [addr]
  (if-not @(rf/subscribe [:get ::art-pieces-collected-data])
    (fetch-art-pieces-collected! addr))
  (fn []
    [:<>
     [:div {:class "hand-written font-bold text-4xl"} "Your collection"]
     [:br]
     [:div {:class "grid gap-3 grid-cols-3"}
      (for [{:keys [:image_uri] :as art-piece} @(rf/subscribe [:get ::art-pieces-collected])]
        ^{:key image_uri}
        [:img {:class "cursor-pointer"
               :src image_uri
               :on-click #(actions/send :app.actions.browse/see-details [art-piece true])}])]]))

(defn c-collected-pre []
  (if-let [addr @(rf/subscribe [::wallet-address])]
    [c-collected addr]
    [:div
     [:button {:class "btn-blue"
               :on-click #(connect!)}
      "Connect"]
     [:div.italic.mt-2 "(Connect with the crypto wallet you used to collect art pieces)"]]))

(defmethod actions/get-action ::browse-collected
  []
  {:component c-collected-pre})

(actions/add-primary-action ::browse-collected "Your collection" {:default? true})