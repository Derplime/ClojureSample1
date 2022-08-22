(ns sample1.core
  (:require
    [reagent.core :as r]
    [reagent.dom :as rdom]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [sample1.ajax :as ajax]
    [ajax.core :refer [GET POST]]
    [reitit.core :as reitit]
    [clojure.string :as string])
  (:import goog.History))

(defonce session (r/atom {:page :home}))

(defn nav-link [uri title page]
      [:a.navbar-item
       {:href  uri
        :class (when (= page (:page @session)) "is-active")}
       title])

(def app-data (r/atom {:x 0 :y 0 :total 0}))

(defn swap [val]
      (swap! app-data assoc
             :total val)
      (js/console.log "The value from plus API is" (str (:total @app-data)))); Value comes out in console

(defn add [params]
      (POST "/api/math/plus"
            {:headers {"accept" "application/transit-json"}
             :params  @params
             :handler #(swap (:total %))}))

(defn getAdd []
      (GET "/api/math/plus?x=1&y=2"
           {:headers {"accept" "application/json"}
            :handler #(swap (:total %))}))

; TODO - update to clojure parseInt
(defn int-value [v]
      (-> v .-target .-value int))

(comment

  (:total @app-data)
  (POST "/api/math/plus"
        {:headers {"accept" "application/transit-json"}
         :params  {:x 1 :y 2}
         :handler #(swap (:total %))})
  )

(defn navbar []
      (r/with-let [expanded? (r/atom false)]
                  [:nav.navbar.is-info>div.container
                   [:div.navbar-brand
                    [:a.navbar-item {:href "/" :style {:font-weight :bold}} "sample1"]
                    [:span.navbar-burger.burger
                     {:data-target :nav-menu
                      :on-click    #(swap! expanded? not)
                      :class       (when @expanded? :is-active)}
                     [:span] [:span] [:span]]]
                   [:div#nav-menu.navbar-menu
                    {:class (when @expanded? :is-active)}
                    [:div.navbar-start
                     [nav-link "#/" "Home" :home]
                     [nav-link "#/about" "About" :about]]]]))

(defn about-page []
      [:section.section>div.container>div.content
       [:img {:src "/img/warning_clojure.png"}]])

(defn home-page []
      (let [params (r/atom {})]
      [:section.section>div.container>div.content
       [:h1 "Hello World!"]
       [:h3 "Button to add 1 + 2. Result in text at the bottom."]
       [:button {:on-click #(getAdd)} "1 + 2"]
       [:h3 "Fill out fields and press button to add the values. Result in text at the bottom."]
        [:form
         [:div.form-group
          [:label "Value 1: "]
          [:input {:type :text :placeholder "0" :on-change #(swap! params assoc :x (int-value %))}]]
         [:div.form-group
          [:label "Value 2: "]
          [:input {:type :text :placeholder "0" :on-change #(swap! params assoc :y (int-value %))}]]]
        [:br]
        [:button {:on-click #(add params)} "Click here to add the two values."]
        [:br]
        [:br]
        [:p "Your total sum is: " (:total @app-data)]
       ]))

(def pages
  {:home  #'home-page
   :about #'about-page})

(defn page []
      [(pages (:page @session))])

;; -------------------------
;; Routes

(def router
  (reitit/router
    [["/" :home]
     ["/about" :about]]))

(defn match-route [uri]
      (->> (or (not-empty (string/replace uri #"^.*#" "")) "/")
           (reitit/match-by-path router)
           :data
           :name))
;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
      (doto (History.)
            (events/listen
              HistoryEventType/NAVIGATE
              (fn [^js/Event.token event]
                  (swap! session assoc :page (match-route (.-token event)))))
            (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
      (GET "/docs" {:handler #(swap! session assoc :docs %)}))

(defn ^:dev/after-load mount-components []
      (rdom/render [#'navbar] (.getElementById js/document "navbar"))
      (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
      (ajax/load-interceptors!)
      (fetch-docs!)
      (hook-browser-navigation!)
      (mount-components))
