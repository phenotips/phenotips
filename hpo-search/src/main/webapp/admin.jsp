<%@ page import="java.util.Collection" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<head>
<%@ include file="resources.jsp" %>
<%= displayTitle("Administration") %>
</head>

<body>
<%= displayContentTitle("Admin") %>
<%if(request.getParameter("index") != null) {%>
  <ul>
<%String[] vals = request.getParameterValues("url");
  for (int i = 0; i < vals.length; i++) {%>
<%}%>
  </ul>
<%}%>

<form action="admin.jsp" method="post">

<h2>Index an ontology</h2>
The new indexed data will overwrite the current index.

  <label for="ontology_url">URL</label>
  <p class="hint">External URL of an .obo file, e.g. http://compbio.charite.de/svn/hpo/trunk/src/ontology/human-phenotype-ontology.obo</p>
  <input type="text" id="ontology_url" name="url" class="" value="http://compbio.charite.de/svn/hpo/trunk/src/ontology/human-phenotype-ontology.obo" size="16"/>
  <p><input type="submit" value="Index this file" name="index"/></p>

<h2>Clear the current index</h2>
<p>Everything indexed so far will be deleted!</p>
<div><input type="submit" value="I'm sure, clear the index" name="clear"/></div>
</form>

</body>
</html>