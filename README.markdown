This lein plugin generates PDF based on different templating system.
I did not see anything out of the box for [lein](https://github.com/technomancy/leiningen) but since I am using clojure every day at work, I thought about making a little contribution.

The idea was that you could keep the documentation you have already build and just use this plugin to generate the PDF including a set of files.
This was developed when I was asked to create documentation for a project and we had no way to be multiple user to work on it at the same time.

Using this plugin you can get people working on different section and integrates everything when wanted, without people stepping on each other's work.

# Templating Support #

The following templating system are currently supported

## Plain Html (*.html) ## 

We are using [flying saucer](http://xhtmlrenderer.java.net/) to transform html to PDF and this will be the based of all our transformation in the other templates system. As far as Plain Html conversion is concerned, we are just feeding the original HTML document, links and images will be resolved and included in the generated PDF.

## Markdown (*.markdown) ##

Using [MarkdownJ](http://code.google.com/p/markdownj/), we are converting the markdown template to HTML, in turn included in the final PDF file.
Here is a reference for the [Markdown syntax](http://help.couch.it/Markdown_Syntax).

## textile + integrate CSS (*.textile)

The lack of documentation for the [Wikitext](http://wiki.eclipse.org/Mylyn/Incubator/WikiText) java library was scary. Had to decompile code and see what method could be called. 
We are using a convention here so that if a CSS file exist with the same base name as the textile template, we include it in the resulting HTML file.

For example, if we are converting a file:``test.textile`` then if there is a file named ``test.css`` it will be automatically integrated as a ``<style>`` tag in the resulting html file.

## freemarker + dynamic properties (*.ftl)

This provides support for [Freemarker](http://freemarker.sourceforge.net/) templates. Files will be converted to html using the ``.ftl`` file and some dynamic properties. See the [Dynamic Properties](#dp) section to see how to load the properties dynamically.

## string template + dynamic properties (*.st)

This uses [string template](http://www.stringtemplate.org/) to generate the HTML file them again with dynamic properties integrated to make the generation dynamic. See the [Dynamic Properties](#dp) 

# Dynamic Properties
<a name="dp"/>

In case one of the template supports dynamic properties,  the plugin will look for [Yaml](http://www.yaml.org/) or [Java Properties](http://download.oracle.com/javase/6/docs/api/java/util/Properties.html).
The way it look for the properties is by taking the basename of the template and loading &lt;basename>.yaml and &lt;basename>.properties as properties to use for the template.

# Plugin Usage

## Add to your clojure project

To add this plugin to your lein-based clojure project, here is what to put in the *project.clj* file:

``[lein-doc-pdf "1.0.0"]``

Then running 

``lein deps``
``lein help`` 

will display the new task:

``pdf         Convert text document under different markup languages to PDF``

## Run it
To run the task, just type

``lein pdf``

By default, this will pick up all the templates in a folder named ``src/doc`` and generate a resulting PDF. The order of the pages is computed by the order of the filename on the filesystem.

You can also specify a different folder, for example:

``lein pdf src/my-templates``

The name of the resulting pdf file will be based on the name of the folder.
