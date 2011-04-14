This lein plugin generates PDF based on different templating system.
I did not see anything out of the box for [lein](https://github.com/technomancy/leiningen) but since I am using clojure every day at work, I thought about making a little contribution.

The idea was that you could keep the documentation you have already build and just use this plugin to generate the PDF including a set of files.
This was developed when I was asked to create documentation for a project and we had no way to be multiple user to work on it at the same time.

Using this plugin you can get people working on different section and integrates everything when wanted, without people stepping on each other's work.

# Templating Support #

The following templating system are currently supported

## Plain Html (*.html) 

We are using [flying saucer](http://xhtmlrenderer.java.net/) to transform html to PDF and this will be the based of all our transformation in the other templates system. As far as Plain Html conversion is concerned, we are just feeding the original HTML document, links and images will be resolved and included in the generated PDF.

## Markdown (*.markdown) 

Using [MarkdownJ](http://code.google.com/p/markdownj/), we are converting the markdown template to HTML, in turn included in the final PDF file.
Here is a reference for the [Markdown syntax](http://help.couch.it/Markdown_Syntax).

Markdown templates generated HTML will be added all the CSS in the local folders automatically along with necessary html headers.

## textile + integrate CSS (*.textile)

The lack of documentation for the [Wikitext](http://wiki.eclipse.org/Mylyn/Incubator/WikiText) java library was scary. Had to decompile code and see what method could be called. 
We are using a convention here so that if a CSS file exist with the same base name as the textile template, we include it in the resulting HTML file.

For example, if we are converting a file:``test.textile`` then if there is a file named ``test.css`` it will be automatically integrated as a ``<style>`` tag in the resulting html file.

## Freemarker and dynamic properties (*.ftl)

This provides support for [Freemarker](http://freemarker.sourceforge.net/) templates. Files will be converted to html using the ``.ftl`` file and some dynamic properties. See the [Dynamic Properties](#dp) section to see how to load the properties dynamically.

## StringTemplate and dynamic properties (*.st)

This uses [string template](http://www.stringtemplate.org/) to generate the HTML file them again with dynamic properties integrated to make the generation dynamic. See the [Dynamic Properties](#dp) 

## Remote document (*.url)

A text filename ending in ".url" can contain a clojure map, like this:

''{:url "http://www.webheadstart.org/snippets/index4ef9.html?id=12"}`` 

That will point the plugin to download the document at the given URL and will include a PDF.
You can also use a proxy, the following way:
''{
:url "http://www.webheadstart.org/snippets/index4ef9.html?id=12"
:proxy-host "221.213.50.115"
:proxy-port 8000
}''

## Clojure, Enlive templates (*.clj)

This opens the door to what ever scripting you want to do to generate the document. 
The [enlive sample](src/samples/enlive/test.clj) shows how to embed the concept of templates within a clojure script. 

``(apply str  (microblog-template "Hello Enlive templates!" 
               {:title "post1" :body "content of post"}))`` 

The only requirement for the script is that the return value must be the full html content of the document.

# Dynamic Properties
<a name="dp"/>

In case one of the template supports dynamic properties,  the plugin will look for [Yaml](http://www.yaml.org/) or [Java Properties](http://download.oracle.com/javase/6/docs/api/java/util/Properties.html).
The way it look for the properties is by taking the basename of the template and loading &lt;basename>.yaml and &lt;basename>.properties as properties to use for the template.

# PDF Plugin Usage

## Add to your clojure project

The plugin is on [clojars](http://clojars.org/repo/lein-doc-pdf/lein-doc-pdf/).
To add this plugin to your lein-based clojure project, here is what to put in the *project.clj* file:

``[lein-doc-pdf "1.0.0"]``

Then running 

``lein deps``

``lein help`` 

will display the new task:

``pdf         Convert text document under different markup languages to PDF``

## Run it

### Basic
To run the task, just type

``lein pdf``

By default, this will pick up all the templates in a folder named ``src/doc`` and generate a resulting PDF. The order of the pages is computed by the order of the filename on the filesystem.

You can also specify a different folder, for example:

``lein pdf src/my-templates``

The name of the resulting pdf file will be based on the name of the folder.

### Support for project metadata

You can add the following parameters in your ``project.clj`` file:

* output-file: the name of the file to output 
* fonts-folder: the place to load font for the resulting PDF. Those fonts will be included in the resulting file
* input-files: the source folder, or file to load templates from. If this points to a folder, this will include all the files in that folder.

### Support for utf-8

You need to force the JVM to use the file encoding to handle encoding characters along those line:
``export JAVA_OPTS="-Dfile.encoding=utf-8" ; lein pdf``

Then the JVM will pick up the proper encoding to handle files and will display the fonts in the resulting document.

### Support for styles

Project metadata supports styles in the following way:

``:style "src/style/changes"``

or

``:style "src/style/changes.jar/changes.jar"``

The content of the changes, changes.zip or changes.jar style is a set of resources that you can distribute and reuse throughout 
different sets of documents.

At runtime, the content of the folder or archive is extracted to the working directory, and all useful resources are included.
When the run is finished, those resources are being cleaned up.
 
You can overwrite a partial set of those resources by using the same file name, in this case, that resource will not be deleted.

### Support for encryption

The following set of metadata:

``:encryption {:userpassword "user" :ownerpassword "owner" :strength true :permissions 0}``

will encrypt the resulting PDF with the given password. 

### Support for signature

The following set of metadata:

``:sign {:keystore "src/security/keystore.sample" :password "nicolas" :keyalias "docpdf" :keypwd "nicolas" :certificate "docpdf"}``

will sign the document with the key and certificate contained in the given keystore.
This is very basic at the moment.

### Interactive development

Start lein in interactive mode:

``lein interactive``

And you can now use and re-use the ``pdf`` commands without restarting the JVM.