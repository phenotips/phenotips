var MS = (function (MS) {
  var widgets = MS.widgets = MS.widgets || {};
  widgets.SuggestPicker = Class.create({

  options : {
    'showKey' : true,
    'showTooltip' : true,
    'showDeleteTool' : true,
    'enableSort' : true,
    'showClearTool' : true,
    'inputType': 'hidden',
    'listInsertionEltSelector' : null,
    'listInsertionPosition' : 'after',
  },
  initialize: function(element, suggest, options) {
    this.options = Object.extend(Object.clone(this.options), options || { });
    this.input = element;
    this.suggest = suggest;
    this.inputName = this.input.name;
    this.input.name = this.input.name + "__suggested";
    this.suggest.options.callback = this.acceptSuggestion.bind(this);
    this.list = new Element('ul', {'class' : 'accepted-suggestions'});
    var listInsertionElement;
    if (!this.options.listInsertionEltSelector || !(listInsertionElement = this.input.up().down(this.options.listInsertionEltSelector))) {
      listInsertionElement = this.input;
    }
    var insertion = {};
    insertion[this.options.listInsertionPosition] = this.list;
    listInsertionElement.insert(insertion);
    if (this.options.showClearTool) {
      this.clearTool = new Element('span', {'class' : 'clear-tool delete-tool invisible', 'title' : 'Clear the list of selected suggestions'}).update('Delete all &#x2716;');
      this.clearTool.observe('click', this.clearAcceptedList.bindAsEventListener(this));
      this.list.insert({'after': this.clearTool});
    }
    if (typeof(this.options.acceptAddItem) == "function") {
      this.acceptAddItem = this.options.acceptAddItem;
    }
  },

  acceptAddItem : function (key, value, info) {
    return true;
  },

  acceptSuggestion : function(obj) {
    if (this.acceptAddItem(obj.id || obj.value, obj.value, obj.info)) {
      this.addItem(obj.id || obj.value, obj.value, obj.info);
    }
    this.input.value = "";
    return false;
  },

  addItem : function(key, value, info) {
    if (!key) {
      return;
    }
    var listItem = new Element("li");
    var displayedValue = new Element("label", {"class": "accepted-suggestion"});
    // insert input
    var inputOptions = {"type" : this.options.inputType, "name" : this.inputName, "value" : key};
    if (this.options.inputType == 'checkbox') {
      inputOptions.checked = 'checked';
    }
    displayedValue.insert({'bottom' : new Element("input", inputOptions)});
    // if the key should be displayed, insert it
    if (this.options.showKey) {
      displayedValue.insert({'bottom' : new Element("span", {"class": "key"}).update("[" + key.escapeHTML() + "]")});
      displayedValue.insert({'bottom' : new Element("span", {"class": "sep"}).update(" ")});
    }
    // insert the displayed value
    displayedValue.insert({'bottom' : new Element("span", {"class": "value"}).update(value.escapeHTML())});
    listItem.appendChild(displayedValue);
    // delete tool
    if (this.options.showDeleteTool) {
      var deleteTool = new Element("span", {'class': "delete-tool", "title" : "Delete this term"}).update('&#x2716;');
      deleteTool.observe('click', this.removeItem.bindAsEventListener(this));
      listItem.appendChild(deleteTool);
    }
    // tooltip, if information exists and the options state there should be a tooltip
    if (this.options.showTooltip && info) {
      listItem.appendChild(new Element("div", {class : "tooltip"}).update(info));
    }
    this.list.appendChild(listItem);
    this.updateListTools();
  },

  removeItem : function(event) {
    var item = event.findElement('li');
    item.remove();
    this.input.value = "";
    this.updateListTools();
  },

  clearAcceptedList : function () {
    this.list.update("");
    this.updateListTools();
  },

  updateListTools : function () {
    if (this.clearTool) {
      if (this.list.select('li .accepted-suggestion').length > 0) {
        this.clearTool.removeClassName('invisible');
      } else {
        this.clearTool.addClassName('invisible');
      }
    }
    if (this.options.enableSort && this.list.select('li .accepted-suggestion').length > 0 && typeof(Sortable) != "undefined") {
      Sortable.create(this.list);
    }
  }
});
  return MS;
}(MS || {}));
