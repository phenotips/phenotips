var highlightChecked = function(element) {
      var subsection = element.up('.subsection');
      if (subsection) {
	var subsectionTitle = subsection.previous('label.section');
      }
      if (element.checked) {
        element.up('label').addClassName('selected');
	if (subsectionTitle) {
	  subsectionTitle.addClassName('selected');
	}
      } else {
        element.up('label').removeClassName('selected');
	if (subsectionTitle) {
	  subsectionTitle.removeClassName('selected');
	}
      }
};
var enableHighlightChecked = function(element) {
      highlightChecked(element);
      ['click', 'change', 'suggest:change'].each(function(eventName) {
        element.observe(eventName, highlightChecked.bind(element,element));
      });
};

document.observe('dom:loaded', function() {
    // ------------------------------------------------------------------------
    // Selected term highlighting 
    $$('label input[type=checkbox]').each(enableHighlightChecked);
    
    // ------------------------------------------------------------------------
    // Behavior of the quick search box
    
    var qsBox = $('quick-search-box');
    if (qsBox) {
      var content = qsBox.next('div');
      var qsInput = qsBox.down('input[type=text]');
      var qsResetPosition = function() {
	if (qsInput._activeSuggest) {
	  return;
	}
	var boxHeight = qsBox.getHeight();
	var boxWidth = qsBox.getWidth();
	var boxMinTop = content.cumulativeOffset().top ;
	var boxMaxTop = content.cumulativeOffset().top + content.getHeight() - boxHeight;
	var boxLeft = qsBox.cumulativeOffset().left;
	if (document.viewport.getScrollOffsets().top >= boxMinTop && document.viewport.getScrollOffsets().top < boxMaxTop) {
	  if (qsBox.style.position != 'fixed') {
	    qsBox.style.position = 'fixed';
	    qsBox.style.left = boxLeft + 'px';
	    qsBox.style.width = boxWidth + 'px';
	    qsBox.style.top = 0;
	  }
	} else if (document.viewport.getScrollOffsets().top >= boxMaxTop) {
	  if (qsBox.style.position = 'absolute'){
	    qsBox.style.position = 'absolute';
	    qsBox.style.top = boxMaxTop + 'px';
	    qsBox.style.left = '';
	    qsBox.style.right = 0;
	  }
	} else {
	  if (qsBox.style.position = ''){
	    qsBox.style.position = '';
	    qsBox.style.top = '';
	    qsBox.style.left = '';
	    qsBox.style.width = '';
	  }
	}
      }
      Event.observe(document, 'ms:suggest:containerCreated', function(event) {
	if (event.memo.suggest.fld == qsInput) {
	  qsInput._activeSuggest = true;
	  if (qsBox.style.position == 'fixed') {
	    qsBox.style.position = 'absolute';
	    qsBox.style.top = ((document.viewport.getScrollOffsets().top - content.cumulativeOffset().top) + 14) + 'px';
	    qsBox.style.left = '';
	    qsBox.style.right = 0;
	  }
	  var qsSuggest = event.memo.container;
	  qsSuggest.style.top = (qsInput.cumulativeOffset().top + qsInput.getHeight()) + 'px';
	  qsSuggest.style.left = qsInput.cumulativeOffset().left;
	  qsSuggest.style.marginTop = '1.6em';
	}
      });
      Event.observe(document, 'ms:suggest:clearSuggestions', function(event) {
	if (event.memo.suggest.fld == qsInput) {
	  qsInput._activeSuggest = false;
	  qsResetPosition();
	}
      });
      Event.observe(window, 'scroll', qsResetPosition);
    }
    
    // ------------------------------------------------------------------------
    // Expand/collapse phenotype groups
    try {
      $$('fieldset.phenotype-group legend').invoke('observe', 'click', function(event) {
        event.element().up('fieldset.phenotype-group').toggleClassName('collapsed');
      });
    } catch (error){}
});