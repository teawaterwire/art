(ns app.actions.browse
  (:require
   [app.actions.entrypoint :as actions]))

(def images 
  [;; poulet
   "https://bafybeieqayrn3rfwktwsu3m5qxnyy3qom2qpsc4bexgripirloei2n37ji.ipfs.nftstorage.link/"
   ;; grenouille
   "https://bafybeigvead5nydfmgpounsaaby6b25oyyqzaxfd4q65qucahcrnecjufu.ipfs.nftstorage.link/"
   ;; teide
   "https://bafybeicxu23xiav4wygaeleqyi37jqq7372q3k4ffeohb6e3otlypyi5pe.ipfs.nftstorage.link/"
   ;; montagne
   "https://bafybeiaeitwnvburowqoxzfhzzmuxrrtyep2vm2qgzaroxo4ulilkjwuhm.ipfs.nftstorage.link/"
   ;; oeil
   "https://bafybeib3gi6p6dgvap7ikqziupcnpadom4qyopkwrtk7apws3gdny7b3qq.ipfs.nftstorage.link/"
   ;; cross
   "https://bafybeihg7xc7hnuquinodenx6fsf3cgresguns435fkqrmmectrqrw2vky.ipfs.nftstorage.link/"
   ;; hey
   "https://bafybeie2z3cohdet3r76uu75hdyzgk25brma67qilqe262howlvef23wce.ipfs.nftstorage.link/"
   ])

(defn art-pieces []
  [:div {:class "grid gap-3 grid-cols-3"}
   (for [image images]
     ^{:key image}
     [:img {:class "cursor-pointer"
            :src image 
            :on-click #(actions/send ::see-details image)}])])

(defn art-piece [image]
  [:div 
   [:h1 "TITLE"]
   [:img#caca {:src image}]])

(defmethod actions/get-action ::see-details
  [_action _db image]
  {:component art-piece
   :state image})

(defmethod actions/get-action ::browse
  []
  {:component art-pieces})

(actions/add-primary-action ::browse "See art pieces" {:default? true})