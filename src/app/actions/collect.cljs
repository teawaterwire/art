(ns app.actions.collect
  (:require
   [app.actions.entrypoint :as actions]
   [re-frame.core :as rf]
   [app.utils :as ut]
   ["/gen/rainbowkit.js" :as rk]
;;    ["@rainbow-me/rainbowkit" :as rk]
   ["wagmi" :as wagmi]
   ["@wagmi/core" :as wagmi-core]
   ["wagmi/chains" :refer (polygon)]
   ["wagmi/providers/public" :refer (publicProvider)]))

(def configured-chains (wagmi/configureChains #js [polygon] #js [(publicProvider)]))
(def chains (.-chains configured-chains))

(def default-wallets (rk/getDefaultWallets #js {:appName "WW" :chains chains}))

(def wagmi-client (wagmi/createClient #js {:autoConnect false
                                           :connectors (.-connectors default-wallets)
                                           :provider (.-provider configured-chains)}))

(defonce unwatch (wagmi-core/watchAccount #(rf/dispatch [:set ::account (ut/j->c %)])))

(defn c-connect []
  [:> wagmi/WagmiConfig {:client wagmi-client}
   [:> rk/RainbowKitProvider {:chains chains}
    [:div 
     [:> rk/ConnectButton]]]])

(defn c-collect [title]
  [:div "You need to connect a crypto wallet to collect " 
   [:div {:class "hand-written text-4xl mt-4 mb-4"} title]
   "If you don't have one I recommend that you download Linen." [:br]

   [c-connect]
   
   #_[:button {:class "btn-blue"
             :on-click #(actions/send ::connect)} 
    "Connect"]
   ])

(defmethod actions/get-action ::connect
  [_action _db _args]
  {:component c-connect
   :state {}})

(defmethod actions/get-action ::collect
  [_action _db title]
  {:component c-collect
   :state title})