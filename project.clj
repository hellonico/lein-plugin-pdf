(defproject lein-doc-pdf "1.0.9"
  :url "https://github.com/hellonico/Lein-Plugin-PDF"
  :description "Lein plugin for PDF generation"
  :license {:name "GNU General Public License"
            :url "http://www.gnu.org/licenses/gpl-3.0.txt"}
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
                   [enlive "1.0.0"]
                   ; xilize templates
                   [xilize/xilize-engine "3.0.3"]
                   ; pdf generation
                   [com.lowagie/itext "2.0.8"
                    :exclusions [bctsp/bcmail-jdk14 org.apache.ant/ant org.apache.ant/ant-launcher]]
                   ; html -> pdf conversion
                   [de.huxhorn.lilith/de.huxhorn.lilith.3rdparty.flyingsaucer.core-renderer "8RC1" 
                    :exclusions [bctsp/bcmail-jdk14 org.apache.ant/ant org.apache.ant/ant-launcher]]]
  :repositories  {"stuartsierra" "http://stuartsierra.com/maven2" "conjars" "http://conjars.org/repo/"}
  :source-path "src/clj"

  :doc-pdf {
    :samples-remote {
    	:input-files "src/samples/remote"    
    	:output-file "classes/remote.pdf"
        :fonts-folder "src/fonts"
    }
    :readme {
        :input-files "README.markdown"
        :output-file "readme.pdf"
    }
    :changes {
        :input-files "CHANGES.textile"
        :output-file "changes.pdf"
        :style "src/style/changes.jar/changes.jar"
    }
    :xilize {
        :input-files "src/samples/xilize"
        :output-file "xilize.pdf"
    }
    ; :style "src/style/changes"
	; :sign {:keystore "src/security/keystore.sample" :password "nicolas" :keyalias "docpdf" :keypwd "nicolas" :certificate "docpdf"}
	; :encryption {:userpassword "user" :ownerpassword "owner" :strength true :permissions 0}
  }

  :dev-dependencies [
                     ;[lein-doc-pdf "1.0.7"]
					 [lein-clojars/lein-clojars "0.6.0"]
                     [lein-eclipse "1.0.0"]] ) 