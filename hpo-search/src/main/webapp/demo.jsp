<%@ page import="java.util.Collection" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<head>
<%@ include file="resources.jsp" %>
<%= displayTitle("Demo") %>
</head>

<body>
<%= displayContentTitle("Demo") %>

<div class="half-width">
<%if(request.getParameter("phenotype") != null) {%>
  <ul>
<%String[] vals = request.getParameterValues("phenotype");
  for (int i = 0; i < vals.length; i++) {%>
  <li><%= vals[i] %></li>
<%}%>
  </ul>
<%} else {%>

<form action="demo.jsp" method="post">
  <label for="phenotype__suggestions">Phenotype</label>
  <p class="hint">Enter a free text and choose among the suggesteded ontology terms.</p>
  <input type="text" id="phenotype__suggestions" name="phenotype" class="suggested multi suggest-hpo" value="" 
size="16"/>
  <div><input type="submit" value="Submit"/></div>
</form>

<%}%>
</body>
</html>
