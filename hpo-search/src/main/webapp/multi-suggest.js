var MS = (function (MS) {
  var widgets = MS.widgets = MS.widgets || {};
  widgets.SuggestPicker = Class.create({

  options : {
    'showKey' : true,
    'showTooltip' : true,
    'showDeleteTool' : true,
    'showClearTool' : true
  },
  initialize: function(element, suggest, options) {
    this.options = Object.extend(Object.clone(this.options), options || { });
    this.input = element;
    this.suggest = suggest;
    this.inputName = this.input.name;
    this.input.name = this.input.name + "__suggested";
    this.suggest.options.callback = this.acceptSuggestion.bind(this);
    this.list = new Element('ul', {'class' : 'accepted-suggestions'});
    this.input.insert({'after' : this.list});
    if (this.options.showClearTool) {
      this.clearTool = new Element('span', {'class' : 'clear-tool delete-tool invisible', 'title' : 'Clear the list of selected suggestions'}).update('Delete all &#x2716;');
      this.clearTool.observe('click', this.clearAcceptedList.bindAsEventListener(this));
      this.list.insert({'after': this.clearTool});
    }
  },

  acceptSuggestion : function(obj) {
    this.addItem(obj.id || obj.value, obj.value, obj.info);
    this.input.value = "";
    return false;
  },

  addItem : function(key, value, info) {
    if (!key) {
      return;
    }
    var listItem = new Element("li");
    var displayedValue = new Element("span", {"class": "accepted-suggestion"});
    if (this.options.showKey) {
      displayedValue.insert({'bottom' : new Element("span", {"class": "key"}).update("[" + key.escapeHTML() + "]")});
      displayedValue.insert({'bottom' : new Element("span", {"class": "sep"}).update(" ")});
    }
    displayedValue.insert({'bottom' : new Element("span", {"class": "value"}).update(value.escapeHTML())});
    listItem.appendChild(displayedValue);
    if (this.options.showDeleteTool) {
      var deleteTool = new Element("span", {'class': "delete-tool", "title" : "Delete this term"}).update('&#x2716;');
      deleteTool.observe('click', this.removeItem.bindAsEventListener(this));
      listItem.appendChild(deleteTool);
    }
    var hiddenInput = new Element("input", {"type" : "hidden", "name" : this.inputName, "value" : key});
    listItem.appendChild(hiddenInput);
    if (this.options.showTooltip && info) {
      listItem.appendChild(new Element("div", {class : "tooltip"}).update(info));
    }
    this.list.appendChild(listItem);
    this.updateClearTool();
  },

  removeItem : function(event) {
    var item = event.findElement('li');
    item.remove();
    this.input.value = "";
    this.updateClearTool();
  },

  clearAcceptedList : function () {
    this.list.update("");
    this.updateClearTool();
  },

  updateClearTool : function () {
    if (this.list.select('li .accepted-suggestion').length > 0) {
      this.clearTool.removeClassName('invisible');
    } else {
      this.clearTool.addClassName('invisible');
    }
  }
});
  return MS;
}(MS || {}));
