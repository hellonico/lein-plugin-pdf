<html>
<head>
<link rel="stylesheet" type="text/css"  media="print"  href="test.css"></link>
</head>
<body>
<div id="container">
<h1>hello ${user}, I am a dynamic freemarker template</h1>
This is the date: ${date}
This is an image <img src="sunshine-for-a-m.jpg"/>
</div>
<#include "ftl/footer.ftl">
</body>
</html>