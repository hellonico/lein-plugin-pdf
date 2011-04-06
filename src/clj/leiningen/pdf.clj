(ns leiningen.pdf
  "Convert text document under different markup languages to PDF"
  (:use clojure.contrib.java-utils)
  (:import [org.ho.yaml Yaml])
  (:import [org.eclipse.mylyn.wikitext.core.parser MarkupParser])
  (:import [org.eclipse.mylyn.wikitext.textile.core TextileLanguage])
  (:import [java.io FileReader FileInputStream FileOutputStream File FileWriter])
  (:import [org.eclipse.mylyn.wikitext.core.parser.builder HtmlDocumentBuilder])
  (:import [com.petebevin.markdown MarkdownProcessor])
  (:import [org.antlr.stringtemplate StringTemplate])
  (:import [org.xhtmlrenderer.pdf ITextRenderer]))
;;;;
;;;; This lein plugin generates PDF based on html and textile inputs
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

(defn get-prefix-suffix
  [file]
  (let [filename (.getName file)dot-index (.indexOf filename ".")]
   (if (> dot-index 0)
     [(.substring filename 0 dot-index) (.substring filename (+ 1 dot-index))]
     [filename ""])))

(defn get-sibling
  [document ext] 
    (File. (str (.getParent document) "/" (first (get-prefix-suffix document)) "." ext)))

; add the document to the renderer if the document 
; exists
(defn add-document
  [renderer document]
  (if (and (not (empty document)) (.isFile document) )
    (do (println "[\tAdding\t:"document)
    (doto renderer
      (.setDocument document)
      (.layout)
      (.writeNextDocument)))))

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
(defn handle-html [document] document)

; handle textile
; this could be modified to handle more languages

(defn handle-markdown [document]
  (let[file (get-sibling document "html")
       markdown (com.petebevin.markdown.MarkdownProcessor.)]
    (spit file (.markdown markdown (slurp document)))
    ;(.deleteOnExit file) 
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
    (.deleteOnExit file)
    (.process (.getTemplate configuration (.getName document)) properties writer)
    (.close writer)
  file))

(defn handle-stringtemplate [document]
  (let[file (get-sibling document "sthtml")
        template (StringTemplate. (slurp document))
        attributes (get-properties document)
       ]
    (.setAttributes template attributes)
    (.deleteOnExit file)
    (spit file (.toString template))
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
  ; make sure we do not leave intermediate files
  (.deleteOnExit file)  
  ; convert the textile content 
  (.parse marker (slurp document))
  ; finish writer the html file
  (.close writer)
  file))

; here is the core method that add documents to the pdf
; we support html, and textile, but we see how to expand that easily
(defn handle-doc [file]
  ; TODO: recursive support ?
  (if (.isDirectory file) ()
  (let [suffix (second (get-prefix-suffix file))]
      (condp = suffix
       "html" (handle-html file)
       "st" (handle-stringtemplate file)
       "textile" (handle-textile file)
       "markdown" (handle-markdown file)
       "ftl" (handle-freemarker file)
       ()))))

; return the first process-able document
; this is needed to get the proper format for PDF creation
(defn- get-first-document
  [input-files]
  (loop [files input-files]
    (let [java-file  (first files)
          intermediate-file (handle-doc java-file)
          docl (not (seq? intermediate-file ))]
    (if docl
      (do
        ; ugly, we delete the file we just have processed while looking up
        ; since most methods only delete on exit
        ; UPDATE: not needed ?
        ; (.delete intermediate-file) 
        java-file)
      (recur (rest files))))))

; main method for plugin
(defn pdf [project & args]
  (let
    [
     first-arg (first args)
     second-arg (second args)
     
     inputfile (if (nil? first-arg)  "src/doc" first-arg)
     input-file (File. inputfile)
     input-files (if (.isFile input-file) input-file (seq (.listFiles input-file))) 
     ofilename (str "classes/" (first (get-prefix-suffix input-file)) ".pdf")
     outputfile  (if (nil? second-arg) ofilename second-arg)
     os (FileOutputStream. (File. outputfile))    
     renderer (ITextRenderer.)   
  
     ; the first file is handled slightly differently by flying saucer
     first-doc (get-first-document input-files)
     input-files (remove #{first-doc}  input-files )
     h-first-doc (handle-doc first-doc)
    ]
    (println (str "[\tFirst File\t: " first-doc))
    (println (str "[\tRemaing Files\t: " (count input-files)))
    (println (str "[\tGenerating\t: " ofilename))
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
  ; finish PDF
  (doto renderer (.finishPDF))
  (.close os) ))
