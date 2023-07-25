(ns app.actions.browse
  (:require
   [app.actions.entrypoint :as actions]
   ["react-photo-view" :refer [PhotoProvider PhotoView]]))

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

(defn art-pieces []
  [:<>
   [:div {:class "hand-written font-bold text-4xl"} "Art pieces"]
   [:br ]
;;    [:div {:class "font-mono"} "In chronological order"]
   [:div {:class "grid gap-3 grid-cols-3"}
    (for [[title _date url :as image] images]
      ^{:key title}
      [:img {:class "cursor-pointer"
             :src url 
             :on-click #(actions/send ::see-details image)}])]])

(defn art-piece [[title date url]]
  [:div 
   [:div {:class "hand-written font-bold text-4xl"} title]
   [:div {:class "font-mono"} date]
   [:> PhotoProvider
    [:> PhotoView {:src url}
     [:img {:src url :class "cursor-zoom-in"}]]]])

(defmethod actions/get-action ::see-details
  [_action _db image]
  {:component art-piece
   :state image})

(defmethod actions/get-action ::browse
  []
  {:component art-pieces})

(actions/add-primary-action ::browse "See art pieces" {:default? true})