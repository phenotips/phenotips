<!--[if !(gte IE 8)]>
<script type="text/javascript">
window.location = "unsupported-browser.html";
</script>
<![endif]-->
<link rel="stylesheet" type="text/css" href="style.css"></link>
<link rel="stylesheet" type="text/css" href="blue.css"></link>
<link rel="stylesheet" type="text/css" href="widgets/modal-popup.css"></link>
<link rel="stylesheet" type="text/css" href="widgets/suggest.css"></link>
<link rel="stylesheet" type="text/css" href="widgets/multi-suggest.css"></link>
<link rel="stylesheet" type="text/css" href="widgets/ontology-browser.css"></link>
<link rel="icon" href="favicon.ico" type="image/ico"></link>
<link rel="shortcut icon" href="favicon.ico" type="image/ico"></link>
<script type="text/javascript" src="jslib/prototype.js"></script>
<script type="text/javascript" src="jslib/scriptaculous.js"></script>
<script type="text/javascript" src="widgets/xlist.js"></script>
<script type="text/javascript" src="widgets/suggest.js"></script>
<script type="text/javascript" src="widgets/multi-suggest.js"></script>
<script type="text/javascript" src="jslib/shortcuts.js"></script>
<script type="text/javascript" src="widgets/modal-popup.js"></script>
<script type="text/javascript" src="jsform/solr-query-processor.js"></script>
<script type="text/javascript" src="widgets/ontology-browser.js"></script>
<script type="text/javascript" src="jsform/form-behavior.js"></script>
<script type="text/javascript" src="jsform/term-suggest.js"></script>
<script type="text/javascript" src="jsform/form-validation.js"></script>


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