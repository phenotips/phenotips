require.config({
  paths : {
    pedigree : "$stringtool.substringBefore($xwiki.getSkinFile('uicomponents/pedigree/pedigree.js', true), 'pedigree.js')" 
  }
})

require(["pedigree/pedigree"], function (PedigreeEditor){
  (XWiki && XWiki.domIsLoaded && new PedigreeEditor()) || document.observe("xwiki:dom:loaded", function(){new PedigreeEditor();});
});