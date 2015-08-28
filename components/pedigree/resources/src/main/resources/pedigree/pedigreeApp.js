//Configures requireJS and starts the PedigreeEditor

require(["pedigree/pedigree"], function (PedigreeEditor){
  (XWiki && XWiki.domIsLoaded && new PedigreeEditor()) || document.observe("xwiki:dom:loaded", function(){new PedigreeEditor();});
});