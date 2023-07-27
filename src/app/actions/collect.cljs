(ns app.actions.collect
  (:require
   [clojure.string :as str]
   [app.actions.entrypoint :as actions]
   [promesa.core :as p]
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