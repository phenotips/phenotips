<!--[if !(gte IE 8)]>
<script type="text/javascript">
window.location = "unsupported-browser.html";
</script>
<![endif]-->
<link rel="stylesheet" type="text/css" href="style.css"></link>
<link rel="stylesheet" type="text/css" href="blue.css"></link>
<link rel="stylesheet" type="text/css" href="modal-popup.css"></link>
<link rel="stylesheet" type="text/css" href="suggest.css"></link>
<link rel="stylesheet" type="text/css" href="multi-suggest.css"></link>
<link rel="stylesheet" type="text/css" href="ontology-browser.css"></link>
<link rel="icon" href="favicon.ico" type="image/ico"></link>
<link rel="shortcut icon" href="favicon.ico" type="image/ico"></link>
<script type="text/javascript" src="prototype.js"></script>
<script type="text/javascript" src="xlist.js"></script>
<script type="text/javascript" src="suggest.js"></script>
<script type="text/javascript" src="multi-suggest.js"></script>
<script type="text/javascript" src="shortcuts.js"></script>
<script type="text/javascript" src="modal-popup.js"></script>
<script type="text/javascript" src="solr-query-processor.js"></script>
<script type="text/javascript" src="ontology-browser.js"></script>
<script type="text/javascript" src="form-behavior.js"></script>
<script type="text/javascript" src="term-suggest.js"></script>
<script type="text/javascript" src="form-validation.js"></script>


<%!
public String getTitle(String section)
{
  return "HPO Search" + ("".equals(section) ? "" : ": " + section);
}

public String displayTitle(String section)
{
  return "<title>" + getTitle(section) + "</title>";
}

public String getContentTitle(String section)
{
  String appTitle = "<abbr class='emphasize' title='Human Phenotype Ontology'>HPO</abbr><span class='invisible'> </span>search";
  if ("".equals(section)) {
    return appTitle;
  } else {
    String homeLink = "<a href='.' title='" + getTitle("") + "'>" + appTitle + "</a>";
    return homeLink + " &raquo; " + "<span class='emphasize'>" + section + "</span>";
  }
}

public String displayContentTitle(String section)
{
  return "<h1 class='header'>" + getContentTitle(section) + "</h1>";
}
%>