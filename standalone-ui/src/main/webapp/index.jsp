<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<head>
<%@ include file="resources.jsp" %>
<%= displayTitle("") %>
</head>

<body>
<%= displayContentTitle("") %>
<p><a href="http://lucene.apache.org/solr/">Solr</a>-based search for
<a href="www.human-phenotype-ontology.org/"><abbr title="Human Phenotype Ontology">HPO</abbr></a> terms</p>

<h2><a href="demo.jsp">Demo</a></h2>
<p>Simple demonstration of term search</p>

<h2><a href="clinical-info.jsp">Clinical data form</a></h2>
<p>Proof of concept: a clinical data form relying on ontology search</p>

<% /*
<h2><a href="admin.jsp">Administration</a></h2>
<p>Clear or reload the indexed ontology</p>
*/ %>

</body>
</html>
