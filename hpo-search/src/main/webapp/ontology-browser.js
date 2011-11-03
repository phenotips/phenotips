var MS = (function (MS) {
  var widgets = MS.widgets = MS.widgets || {};
  widgets.OntologyBrowser = Class.create({
  options : {
    script : "/solr/select?start=0&rows=1000&",
    varname: "q",
    method : "post",
    responseFormat : "application/xml",
    resultsParameter : "doc",
    resultId : "str[name=id]",
    resultValue : "str[name=name]",
    resultParent : {
      selector : 'arr[name=is_a] str',
      processingFunction : function (text){
	var data = {};
	data.id = text.replace(/\s+/gm, ' ').replace(/(HP:[0-9]+)\s*!\s*(.*)/m, "$1");
	data.value = text.replace(/\s+/gm, ' ').replace(/(HP:[0-9]+)\s*!\s*(.*)/m, "$2");
	return data;
      }
    },
    noresults: "No sub-terms",
    targetQueryProcessor: typeof(MS.widgets.SolrQueryProcessor) == "undefined" ? null : new MS.widgets.SolrQueryProcessor({
                           'id' : {'activationRegex' : 'HP:[0-9]+', 'stub': true, 'boost' : 50}
                         }),
    expandQueryProcessor: typeof(MS.widgets.SolrQueryProcessor) == "undefined" ? null : new MS.widgets.SolrQueryProcessor({
                            'is_a' : { 'stub': false, 'activationRegex' : 'HP:[0-9]+' }
                         })
  },
  
  initialize: function(suggest, dialog, options) {
    this.options = Object.extend(Object.clone(this.options), options || { });
    this.suggest = suggest;
    this.loadingMessage = "Loading..."
    if (dialog) {
      this.dialog = dialog;
    } else {
      this.dialog = new MS.widgets.ModalPopup(this.loadingMessage, {}, {
	title : "Related terms", 
	titleColor: "#333", 
	borderColor: "#cedeee",
	verticalPosition : "top"
      });
    }
  },
  
  load : function(id) {
      this.setDialogContent(this.loadingMessage);
      var query = id;
      if (this.options.targetQueryProcessor != null && typeof(this.options.targetQueryProcessor.processQuery) == "function") {
        query = this.options.targetQueryProcessor.processQuery(query);
      }
      var url = this.options.script + this.options.varname + "=" + encodeURIComponent(query);
      var headers = {};
      headers.Accept = this.options.responseFormat;

      var ajx = new Ajax.Request(url, {
        method: this.options.method,
        requestHeaders: headers,
        onSuccess: function (response) {
          //var tree = this.buildTree(response.responseXML);
          this.setDialogContent(this.buildTree(response.responseXML));
	}.bind(this),
        onFailure: function (response) {
          this.setDialogContent("Failed to retrieve data : " + respose.statusText);
        }.bind(this)
      });
  },
  
  expand : function (element, doPopulate) {
      var query = element.__termId;
      if (this.options.expandQueryProcessor != null && typeof(this.options.expandQueryProcessor.processQuery) == "function") {
        query = this.options.expandQueryProcessor.processQuery(query);
      }
      var url = this.options.script + this.options.varname + "=" + encodeURIComponent(query);
      var headers = {};
      headers.Accept = this.options.responseFormat;

      var ajx = new Ajax.Request(url, {
        method: this.options.method,
        requestHeaders: headers,
        onSuccess: function (response) {
	  var memo = {};
	  if (doPopulate) {
	    memo.data = this.buildDescendentsList(response.responseXML);
	  } else {
	    memo.count = this.countDescendents(response.responseXML);
	  }
          Event.fire(element, 'obrowser:expand:done', memo);
	}.bind(this),
        onFailure: function (response) {
          Event.fire(element, 'obrowser:expand:failed', {data: new Element('div', {'class' : 'error'}).update("Failed to retrieve data : " + respose.statusText), count: -1});
        }
      });
  },
  
  buildTree : function (xml) {
    var results = xml.getElementsByTagName(this.options.resultsParameter);
    if (results.length == 0) {
      return new Element('div', {'class' : 'error'}).update(this.options.noresults);
    }
    var targetResult = results[0];
    var newContent = new Element('div');
    var parents = new Element('ul', {'class': 'parents'});
    Element.select(targetResult, this.options.resultParent.selector).each(function(item) {
      var text = item.firstChild.nodeValue;
      var data = {};
      if (typeof (this.options.resultParent.processingFunction) == "function") {
        data = this.options.resultParent.processingFunction(text);
      }
      parents.insert({'bottom' : this._createParentBranch(data)});
    }.bind(this));
    if (parents.hasChildNodes()) {
      newContent.insert({'top' : parents});
    }
    var data = {
      id : Element.down(targetResult, this.options.resultId).firstChild.nodeValue,
      value : Element.down(targetResult, this.options.resultValue).firstChild.nodeValue
    }
    var root = this._createRoot(data);
    newContent.insert({'bottom' : root});
    this._toggleExpandState(root);
    return newContent;
  },
  
  countDescendents : function(xml) {
    return xml.getElementsByTagName(this.options.resultsParameter).length;
  },
  
  buildDescendentsList : function(xml) {
    var results = xml.getElementsByTagName(this.options.resultsParameter);
    var list = new Element('ul', {'class' : 'descendents'});
    for (var i = 0; i < results.length; i++) {
      if (results[i].hasChildNodes()) {
	var data = {
          id : Element.down(results[i], this.options.resultId).firstChild.nodeValue,
          value : Element.down(results[i], this.options.resultValue).firstChild.nodeValue
         };
	 list.insert({'bottom': this._createDescendentBranch(data)});
      }
    }
    if (list.hasChildNodes()) {
      return list;
    }
    return new Element('div', {'class' : 'descendents hint empty'}).update(this.options.noresults);
  },
  
  _createBranch: function (eltName, className, data, expandable) {
    var element =  new Element(eltName, {'class' : 'entry ' + className});
    element.__termId = data.id;
    var wrapper = new Element('div', {'class' : 'entry-data'});
    wrapper.insert({'top': 
		   new Element('span', {'class' : 'info'}).insert(
		     {'bottom' : new Element('span', {'class' : 'key'}).update('[' + data.id + ']')}).insert({'bottom' : ' '}).insert(
		     {'bottom' : new Element('span', {'class' : 'value'}).update(data.value)})
    });
    wrapper.insert({'bottom':
		   new Element('span', {'class' : 'entry-tools'}).insert(
		     {'bottom' : this._createTool('&#x260c;', 'browse-tool', "Browse related terms", this._browseEntry)}).insert(
		     {'bottom' : this._createTool('i', 'info-tool', "Information about this term", this._showEntryInfo)}).insert(
		     {'bottom' : this._createTool('&#x2713;', 'accept-tool', "Add this phenotype", this._acceptEntry)})
    });
    wrapper.down('.info').observe('click', this._acceptEntry.bindAsEventListener(this));
    element.update(wrapper);
    if (expandable) {
      var expandTool = new Element('span', {'class' : 'expand-tool'}).update(this._getExpandCollapseSymbol(true));
      expandTool.observe('click', function(event) {
	this._toggleExpandState(event.element().up('.entry'));
      }.bindAsEventListener(this));
      wrapper.insert({'top': expandTool});
      this.expand(element, element.hasClassName('root'));
      element.observe('obrowser:expand:done', this._obrowserExpandEventHandler.bindAsEventListener(this));
      element.observe('obrowser:expand:failed', this._obrowserExpandEventHandler.bindAsEventListener(this));
    }
    if (element.hasClassName('root')) {
      element.down('.browse-tool').remove();
    }
    return element;
  },
  
  _toggleExpandState : function(target) {
    if (target) {
      if (target.down('.descendents')) {
        target.toggleClassName('collapsed');
      } else {
        target.removeClassName('collapsed');
	if (target.down(".error")) {
	  target.down(".error").remove();
	}
        this.expand(target, true);
	target.observe('obrowser:expand:done', this._obrowserExpandEventHandler.bindAsEventListener(this));
	target.observe('obrowser:expand:failed', this._obrowserExpandEventHandler.bindAsEventListener(this));
      }
      target.down('.expand-tool').update(this._getExpandCollapseSymbol(target.hasClassName("collapsed")));
    }
  },
  
  _obrowserExpandEventHandler : function(event) {
     var element = event.element();
     if (!event.memo) {
       return;
    }
    if (event.memo.data) {
       element.insert({'bottom': event.memo.data});
    }
    if ((event.memo.count == "0") || (!element.hasClassName('root') && event.memo.data && !element.down('.descendents .entry, .error'))) {
      element.addClassName('collapsed');
      var expandTool = element.down('.expand-tool');
      if (expandTool) {
        expandTool.update(this._getExpandCollapseSymbol(true)).addClassName('disabled');
        expandTool.stopObserving('click');
      }
    }
    element.stopObserving('obrowser:expand:done');
    element.stopObserving('obrowser:expand:failed');
  },
  
  _getExpandCollapseSymbol : function (isCollapsed) {
    if (isCollapsed) {
      return "&#x25ba;";
    }
    return "&#x25bc;";
  },
  
  _createTool : function (text, className, title, method) {
    var element = new Element('span', {'class' : 'entry-tool ' + className, "title" : title}).update(text);
    element.observe('click', method.bindAsEventListener(this));
    return element;
  },
  _showEntryInfo : function(event) {
    var elt = event.element().up('.entry');
    alert("Not yet");
  },
  _acceptEntry : function(event) {
    event.stop();
    var elt = event.element().up('.entry');
    if (this.suggest) {
      var id = elt.__termId;
      var value = elt.down('.value').firstChild.nodeValue;
      this.suggest.acceptEntry(id, value, null, '', value, value);
      elt.toggleClassName('accepted');
      this.dialog.positionDialog();
    } else {
      alert("accept " + elt + " -->> " + elt.__termId);
    }
  },
  _browseEntry : function(event) {
    var elt = event.element().up('.entry');
    this.load(elt.__termId);
  },
  
  _createParentBranch: function (parent) {
    var parent = this._createBranch('li', 'parent', parent, false);
    //parent
    return parent;
  },
  
  _createRoot : function (data) {
    return this._createBranch('div', 'root', data, true);
  },
  
  _createDescendentBranch : function(data) {
    return this._createBranch('li', 'descendent', data, true);
  },
  
  setDialogContent : function(content) {
    this.dialog.setContent(new Element('div', {'class' : 'ontology-tree'}).update(content));
  },
  
  show : function(id) {
    if (id) {
      this.dialog.showDialog();
      this.load (id);
    }
  },
  
  hide : function() {
    this.dialog.closeDialog();
  }
  });
  return MS;
}(MS || {}));