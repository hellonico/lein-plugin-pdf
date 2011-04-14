(ns leiningen.pdf
  (:use [net.cgrand.enlive-html :as html :only [deftemplate append at content set-attr attr? strict-mode]] ))

(deftemplate microblog-template
 (str (.getParent @current-file) "/templates/index.html")  
 [titre post]
   [:title] (content titre)
   [:h1] (content titre)
   [:div.no-msg] #(when (empty? post) %) 
   [:div.posts1] #(at %
                    [:h2] (content (post :title))
                    [:p] (content (post :body)))
   [:div.posts2] (content (str (java.util.Date. ) ":"  (post :body))))
              
(apply str  (microblog-template "Hello Enlive templates!" 
               {:title "post1" :body "content of post"}))