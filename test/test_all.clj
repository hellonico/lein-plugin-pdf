(ns test_all
	  (:use leiningen.pdf)
	  (:import [java.io File])
	  (:use clojure.test))

(def tests {
	:kitchen {
		:input-files "src/samples/kitchen"    
    	:output-file "classes/kitchen.pdf"
	}
    :samples-remote {
    	:input-files "src/samples/remote"    
    	:output-file "classes/remote.pdf"
        :fonts-folder "src/fonts"
    }
    :readme {
        :input-files "README.markdown"
        :output-file "classes/readme.pdf"
    }
;	:enlive {
;		:input-files "src/samples/enlive"
;		:output-file "classes/enlive.pdf"
;	}
	:seed {
		:input-files "src/samples/seed"
		:output-file "classes/seed.pdf"
	}
	:jobim {
		:input-files "src/samples/jobim"
		:output-file "classes/jobim.pdf"
	}
    :changes {
        :input-files "CHANGES.textile"
        :output-file "classes/changes.pdf"
        :style "src/style/changes.jar/changes.jar"
    }
    :xilize {
        :input-files "src/samples/xilize"
        :output-file "classes/xilize.pdf"
    }})

(defn single-pdf-test [params]
	(generate-pdf params)
	(is (.exists (File. (params :output-file))))
	(.delete (File. (params :output-file))))
	
(deftest pdf-all
	(doseq [key (keys tests)] 
	    (do 
		  (println "Executing " key "...")
          (single-pdf-test (tests key)))))