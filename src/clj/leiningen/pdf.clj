(ns leiningen.pdf
  "Convert text document under different markup languages to PDF"
  ; some needed clojure imports
  (:use clojure.contrib.java-utils)
  (:use leiningen.watch)
  (:require [clojure.contrib.io :as io])
  (:require [clojure.contrib.logging :as log])
  
  ; below are java libraries only

  ; standard java
  (:import [java.security PrivateKey KeyStore])
  (:import [java.net Proxy InetSocketAddress])
  (:import [java.io 
            InputStreamReader 
            FileReader
		    BufferedReader
            FileInputStream
            FileOutputStream
            File
            FileWriter])
            
  ; external APIs
  (:import [com.centeredwork.xilize Xilize2 ReporterStd BeanShell])
  (:import [org.apache.commons.io FileUtils])
  (:import [com.lowagie.text.pdf PdfReader PdfEncryptor PdfStamper PdfSignatureAppearance])
  (:import [org.ho.yaml Yaml])
  (:import [org.eclipse.mylyn.wikitext.core.parser MarkupParser])
  (:import [org.eclipse.mylyn.wikitext.textile.core TextileLanguage])
  (:import [org.eclipse.mylyn.wikitext.core.parser.builder HtmlDocumentBuilder])
  (:import [com.petebevin.markdown MarkdownProcessor])
  (:import [org.antlr.stringtemplate StringTemplate])
  (:import [org.xhtmlrenderer.pdf ITextRenderer]))

;;;;
;;;; This lein plugin generates PDF based on multiple templating system inputs
;;;;

;;;;  Dev documentation taken from those places:

; http://code.google.com/p/flying-saucer/wiki/FAQPDF#Versions_of_iText_Supported
; http://code.google.com/p/flying-saucer/wiki/FAQPDF#How_can_I_set_the_document_properties_when_generating_a_PDF?

;;;; Formatting quick links

 ;;;;Textile ;;;;
; http://www.textism.com/tools/textile/
 ;;;; Markdown ;;;;
; http://daringfireball.net/projects/markdown/syntax
 ;;;;; StringTemplate
; http://www.antlr.org/wiki/display/ST/Introduction


; find the basename and the suffix of the given file
(defn get-prefix-suffix
  [file]
  (let [filename (.getName file)dot-index (.indexOf filename ".")]
   (if (> dot-index 0)
     [(.substring filename 0 dot-index) (.substring filename (+ 1 dot-index))]
     [filename ""])))


; this is the file currently being processed.
; we only use this when loading external clojure templates
; so those templates can find relative resources
(def current-file (ref ()))

; handling of temporary files
(def files-to-remove (ref ()))
(defn delete-after-run 	[file] ; call this when you want the resource to be removed	
  (dosync (alter files-to-remove conj file)))
(defn clean 	[]  ; this is called at the very end
  (doall (map #(delete-file-recursively % true) @files-to-remove)))

; make sure we use getAbsolutePath and retrieve the full 
; path of the document
(defn- get-parent
  [document]
  (.getParentFile (File. (.getAbsolutePath document))))

; useful for naming convention
; we get a file with the same base name and a different extension
(defn get-sibling
  [document ext] 
    (File. 
        (str 
            (.getPath (get-parent document)) 
            "/" 
            (first (get-prefix-suffix document)) 
            "." 
            ext)))

; return a list of the CSS files located in the same folder 
; as the given document
(defn get-local-css
  [document]
  (filter #(> (.indexOf % ".css") 0) (.list (get-parent document))))

; create the html needed to add all the CSS found in the same folder
;  as the given document
(defn add-local-css
  [document]
  (let[css-list (get-local-css document)
       css-html (StringBuffer. )]
    (doseq [css css-list] 
        (.append css-html 
            (str "<link rel=\"stylesheet\" type=\"text/css\" href=\"" css "\"></link>"))) 
   css-html))

; slurp the content with utf-8 encoding
(defn get-file-content
  [document]
    (slurp (InputStreamReader. (FileInputStream. document) "utf-8")))

; add the document to the renderer if the document 
; exists and is a document this plugin handles
(defn add-document
  [renderer document]
  (if (and (not (nil? document)) (not (empty? (seq document))) (.isFile document) )
    (do (println "[\tAdding\t:"document)
    (doto renderer
      (.setDocument document)
      (.layout)
      (.writeNextDocument)))))

; get properties
(defn- get-properties
  [document]
  (
    let[ 
        java-file (get-sibling document "properties")
        yaml-file (get-sibling document "yaml")
        java-props (if (.exists java-file) (read-properties java-file) (as-properties #{}))
        yaml-props  (if (.exists yaml-file) (Yaml/load (FileReader. yaml-file)) (as-properties #{}))
        merg (.putAll java-props yaml-props)]
    java-props))

; handle an html document by just returning the reference to it
(defn handle-html
    [document] 
        document)

(defn handle-markdown [document]
    (let[file (get-sibling document "html")
        markdown (MarkdownProcessor.)]
        (spit file (str "<html><head>" (add-local-css document)  " </head><body>" (.markdown markdown (get-file-content document)) "</body></html>")) 
        (delete-after-run file)
    file))

(defn handle-freemarker [document]
  (let[file (get-sibling document "html")
       configuration (freemarker.template.Configuration.)
       parent-folder (.getParentFile document)
       properties (get-properties document)
       writer (FileWriter. file)
       ]
    (doto configuration
      (.setDirectoryForTemplateLoading parent-folder)
      (.setDefaultEncoding "UTF-8"))
 	(delete-after-run file)
    (.process (.getTemplate configuration (.getName document)) properties writer)
    (.close writer)
  file))

(defn handle-stringtemplate [document]
  (let[file (get-sibling document "sthtml")
        template (StringTemplate. (slurp document))
        attributes (get-properties document)
       ]
    (.setAttributes template attributes)
   	(delete-after-run file)
    (spit file (.toString template))
    file))

; this fetch url supports proxy
; could also support basic authentication
(defn fetch-url
  "Return the web page as a string."
  [address meta]
  (let 
     [
     url (java.net.URL. address)     
     a-stream 
		(if (contains? meta :proxy-host)
		   ; if proxy-host is defined create a proxy
		   (.getInputStream (.openConnection url 
			(Proxy. 
				java.net.Proxy$Type/HTTP 
				(InetSocketAddress. (get meta :proxy-host) (get meta :proxy-port)))))
		   ; if no proxy-host then open the url as usual
	   	   (.openStream (java.net.URL. address)))] ; end of let block
    (with-open [stream a-stream]
      (let [buf (BufferedReader. (InputStreamReader. stream))]
        (apply str (line-seq buf))))))

(defn handle-url
	[document]
		(let[
		meta (load-file (.getPath document))
		url (get meta :url)
		file (get-sibling document "html")
		]
	  (println (str "[\tFetching remote document: " url))
	  (spit file (fetch-url url meta))
	  (delete-after-run file)
	file))

(defn handle-textile [document]
  (let[ 
       css (get-sibling document "css")
       file (get-sibling document "html") 
       writer (FileWriter. file)
       builder (HtmlDocumentBuilder. writer)
       marker (MarkupParser. (TextileLanguage.) builder)
       ]
  ; add external CSS to file
  (if (.exists css) (.addCssStylesheet builder css))
  (.setEncoding builder "UTF-8") 
  ; make sure we do not leave intermediate files
  (delete-after-run file)
  ; convert the textile content 
  (.parse marker (get-file-content document))
  ; finish writer the html file
  (.close writer)
  file))
  
(defn handle-xilize [document]
    (Xilize2/startup (ReporterStd.) (BeanShell.) (java.util.HashMap.))
    (let[
        file (get-sibling document "html") 
        xil (Xilize2.)
        ]
    (delete-after-run file)
    (.xilizeFile xil (get-parent document)  (File. (.getAbsolutePath document)))
    (.translate xil)
    (Xilize2/shutdown)
    file
    ))

(defn handle-clj
  [document]
  (let[ 
       css (get-sibling document "css")
       file (get-sibling document "html")] 
  (delete-after-run file)
  (spit file (doall (load-file (.getAbsolutePath document))))
  file))

; here is the core method that add documents to the pdf
; we support html, and textile, but we see how to expand that easily
(defn handle-doc [file]
  (dosync (ref-set current-file file))
  ; TODO: recursive support ?
  (if (.isDirectory file) ()
  (let [suffix (second (get-prefix-suffix file))]
      (condp = suffix
          
       "html"       (handle-html file)
       "st"         (handle-stringtemplate file)
       "textile"    (handle-textile file)
       "markdown"   (handle-markdown file)
       "ftl"        (handle-freemarker file)
       "clj"        (handle-clj file)
       "url"        (handle-url file)
       "xil"        (handle-xilize file)
       ()
		))))

; return the first process-able document
; this is needed to get the proper format for PDF creation
(defn- get-first-document
  [input-files]
  (loop [files input-files]
    (let [java-file  (first files)
          intermediate-file (handle-doc java-file)
          docl (not (seq? intermediate-file ))
		 ]	
    (if docl
      (do
        ; ugly, we delete the file we just have processed while looking up
        ; since most methods only delete on exit
        ; UPDATE: not needed ?
        ; (.delete intermediate-file) 
        java-file)
      (recur (rest files))))))

; retrieve parameters for this run
(defn get-parameter
	[key metadata & args]
	(let
		[project-value (metadata key)]
		(condp = key
            :style
                project-value
			:output-file 
			 	(if (not (empty? (metadata :second))) 
			 	    (metadata :second) 
			 	    (if (nil? project-value) 
			 	        (.getAbsolutePath (get-sibling (first args) "pdf"))
			 	        project-value))
			:fonts-folder 
				(File. (if (nil? project-value) "src/fonts"  project-value))
			:input-files
				(let[
					command-line-file (metadata :first)
					input-file 
				(File. (if (not (nil? command-line-file)) command-line-file (if (nil? project-value) "src/doc" project-value)))
				]
 			(if (.isFile input-file) (seq [input-file]) (seq (.listFiles input-file))))
			"")))

; filter method to encrypt the PDF with a passw
(defn encrypt
	[document metadata] 
	(let
		[
		encryption (get metadata :encryption)
		encrypt? (not (nil? encryption))
		sibling (get-sibling document "encrypted.pdf")
		]
		(if encrypt?
		(do
			(PdfEncryptor/encrypt 
				(PdfReader. (.getAbsolutePath document)) 
				(FileOutputStream. sibling) 
				(get encryption :strength false) 
				(get encryption :userpassword "")
				(get encryption :ownerpassword "")  
				(get encryption :permissions ""))
			 (.delete document)
			 (.renameTo sibling document)))))

; filter method to create a signature of the PDF				
(defn signature
    [document metadata]
    (let [
        sign (get metadata :sign)
        sign? (not (nil? sign))]
        (if sign?
            (let[
                key-store (KeyStore/getInstance (KeyStore/getDefaultType))
                document-path (.getAbsolutePath document)
                sibling (get-sibling document "signed.pdf")
                fos (FileOutputStream. sibling)
                stamper (PdfStamper/createSignature  (PdfReader. document-path) fos '\0' nil)
                pdfSignatureAppearance (.getSignatureAppearance stamper)
                ]
                (do
                    ; load keystore
                    (.load key-store (FileInputStream. (sign :keystore)) (.toCharArray (sign :password)))
                    ; create signature
                    (.setCrypto pdfSignatureAppearance 
                        (.getKey key-store (sign :keyalias) (.toCharArray (sign :keypwd)))
                        (.getCertificateChain key-store (sign :certificate)) 
                        nil 
                    PdfSignatureAppearance/SELF_SIGNED)
                    (.setCertificationLevel pdfSignatureAppearance PdfSignatureAppearance/CERTIFIED_NO_CHANGES_ALLOWED)
                    (.setVisibleSignature pdfSignatureAppearance (com.lowagie.text.Rectangle. 10 10 10 10) 1 nil)

                    ; clean up
                    (.close stamper)	
                    (.delete document)
                (.renameTo sibling (File. document-path)))))))

; collect all the fonts from the font folder
; add them to the PDF
(defn install-fonts
	[metadata renderer first-doc]
	(let
		[resolver (.getFontResolver renderer)
    style-font-folder (File. (str (.getParent first-doc) "/fonts")) ; priority for themed fonts and local fonts
		font-folder (get-parameter :fonts-folder metadata) ; generic fallback font folder
		font-files (if (.exists style-font-folder) (.listFiles style-font-folder) (.listFiles font-folder))]
		(println (str "[\tLoading Font:\t " (count font-files)))
		(loop [fonts font-files]
			(let[ current-font (first fonts)]
				(if (not (nil? current-font))
					(do
						(println (str "[\tAdding: " current-font))
						(.addFont resolver (.getAbsolutePath current-font) com.lowagie.text.pdf.BaseFont/IDENTITY_H true)
					(recur (rest fonts))))))))

; home cooking for streams
(defn in-out
  [in-stream out-stream]
  (let[
   in     (java.io.BufferedInputStream. in-stream)
   out    (java.io.BufferedOutputStream. out-stream)
   buffer (make-array Byte/TYPE 1024)
   ]
     (loop [g (.read in buffer) r 0]
       (if-not (= g -1)
         (do
           (.write out buffer 0 g)
           (recur (.read in buffer) (+ r g)))))
     (.close in)
     (.close out) ))

; install the style by expanding a jar/zip file if necessary
; or taking a folder
; and copying all the files to the same folder as the document file
; we take care of deleting all the added resources after a run
(defn install-style 
  [style document] 
   (if (.isDirectory (File. style)) 
   (FileUtils/copyDirectory (File. style) (get-parent document))
   (with-open [z (java.util.zip.ZipFile. style)]
             (doseq [e (enumeration-seq (.entries z))]
               (let [
                    parent (.getParentFile document)
                    file (File. (if (nil? parent) (.getName e) (do (.mkdirs parent) (str parent "/" (.getName e)))))
                    ]
                    (if (not (.exists file))
                    (do
                    ; TODO check the file does not exist already, otherwise we may delete already 
                    ; existing files
                    
                    (if (.isDirectory e)
                      (.mkdir file)
                      (in-out (.getInputStream z e) (FileOutputStream. file)))
                    (delete-after-run file))))))
   )
   true
   )

; generate based on a set of metadata
(defn generate-pdf [metadata]
   (let
    [
     ; the first file is handled slightly differently by flying saucer
     input-files (get-parameter :input-files metadata)
     first-doc (get-first-document input-files)

     ; prepare output file
	 output-file (File. (get-parameter :output-file metadata first-doc))
     os (FileOutputStream. output-file)    
     renderer (ITextRenderer.)
     
     ; install style before first processing
     style (get-parameter :style metadata)
     style-installed (if (nil? style) 
                          false 
                          (install-style style first-doc))
     
     ; remove the first file from the list of files to process
     input-files (remove #{first-doc}  input-files)
     
     h-first-doc (handle-doc first-doc)
    ]

	(println (str "[\tProject settings"))	
    (println (str "[\tUsing encoding\t: " (System/getProperty "file.encoding")))
    (println (str "[\tFirst File\t: " first-doc))
    (println (str "[\tRemaing Files\t: " (count input-files)))
    (println (str "[\tStyle\t\t: " style))
    (println (str "[\tGenerating\t: " output-file))
    (println (str "[\t----------\t "))

    (install-fonts metadata renderer first-doc)
    (println (str "[\t----------\t "))    

  ; first file and pdf creation
  (println (str "[\tAdding\t: " h-first-doc))
  (doto renderer 
     (.setDocument h-first-doc) 
     (.layout)
     (.createPDF os false))
  ; additional files 
  (dorun 
    (map 
      #(add-document renderer (handle-doc %)) 
       input-files))

  ; finish base PDF
  (doto renderer (.finishPDF))
  (.close os) 

  ; clean up intermediate files
  (clean)
  ; sign if needed
  (signature output-file metadata)
  ; encrypt if needed
  (encrypt output-file metadata)
  
  ))
  
;
(defn watch
  [metadata]
  (let
    [input (File. (metadata :input-files))
     ; the function to call when a file change has been found
     funktion (fn[] (generate-pdf metadata))]
    (watch-and-play input funktion)))

; main method for plugin
(defn pdf [project & args]
  (let[first-char (first (first args))]
       
   (condp = first-char
       \: (generate-pdf  (get (project :doc-pdf)  (keyword (.substring (first args) 1))))
       \@ (watch (get (project :doc-pdf)  (keyword (.substring (first args) 1))))
      ; regular parameters
   (generate-pdf (merge (get project :doc-pdf {}) {:first (first args) :second  (second args) })))))