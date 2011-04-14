(defproject lein-doc-pdf "1.0.7"
  :description "Lein plugin for PDF generation"
  :dependencies [
                   ; main clojure library
                   [org.clojure/clojure "1.2.1"]
                   [commons-io/commons-io "2.0.1"]
                   ; textile format 
                   [org.fusesource.wikitext/textile-core "1.1"
                    :exclusions [org.apache.ant/ant org.apache.ant/ant-launcher]]
                   [org.fusesource.wikitext/wikitext-core "1.1"
                    :exclusions [org.apache.ant/ant org.apache.ant/ant-launcher]]
                   ; markdown support
                   [org.markdownj/markdownj "0.3.0-1.0.2b4"]
                   ; freemarker support
                   [freemarker/freemarker "2.3.9"]
                   ; yaml properties
                   [org.jyaml/jyaml "1.3"]
                   ; string template support
                   [org.clojars.ghoseb/stringtemplate "3.2.1"]
                   ; support for enlive templates
                   [enlive "1.0.0-SNAPSHOT"]
                   ; pdf generation
                   [com.lowagie/itext "2.0.8"
                    :exclusions [bctsp/bcmail-jdk14 org.apache.ant/ant org.apache.ant/ant-launcher]]
                   ; html -> pdf conversion
                   [de.huxhorn.lilith/de.huxhorn.lilith.3rdparty.flyingsaucer.core-renderer "8RC1" 
                    :exclusions [bctsp/bcmail-jdk14 org.apache.ant/ant org.apache.ant/ant-launcher]]]
  :repositories  {"stuartsierra" "http://stuartsierra.com/maven2" "conjars" "http://conjars.org/repo/"}
  :source-path "src/clj"

  :doc-pdf {
	:input-files "src/samples/remote"
   ;	:output-file "seed.pdf"
	:fonts-folder "src/fonts"
  ;:style "src/style/changes"
  :style "src/style/changes.jar/changes.jar"
	;:sign {:keystore "src/security/keystore.sample" :password "nicolas" :keyalias "docpdf" :keypwd "nicolas" :certificate "docpdf"}
	;:encryption {:userpassword "user" :ownerpassword "owner" :strength true :permissions 0}
  }

  :dev-dependencies [
                     ;[lein-doc-pdf "1.0.5"]
					 [lein-clojars/lein-clojars "0.6.0"]
                     [lein-eclipse "1.0.0"]] ) 