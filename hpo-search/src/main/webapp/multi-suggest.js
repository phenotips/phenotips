var MS = (function (MS) {
  var widgets = MS.widgets = MS.widgets || {};
  widgets.SuggestPicker = Class.create({

  options : {
    'showKey' : true,
    'showTooltip' : true
  },
  initialize: function(element, suggest, options) {
    this.input = element;
    this.suggest = suggest;
    this.inputName = this.input.name;
    this.input.name = this.input.name + "__suggested";
    this.suggest.options.callback = this.acceptSuggestion.bind(this);
    this.list = new Element('ul', {'class' : 'accepted-suggestions'});
    this.input.insert({'after' : this.list});
    this.options = Object.extend(Object.clone(this.options), options || { });
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
    var displayedKey = new Element("span", {"class": "key"}).update(key.escapeHTML());
    var displayedValue = new Element("span", {"class": "accepted-suggestion"});
    if (this.options.showKey) {
      displayedValue.insert({'bottom' : new Element("span", {"class": "key"}).update("[" + key.escapeHTML() + "]")});
      displayedValue.insert({'bottom' : new Element("span", {"class": "sep"}).update(" ")});
    }
    displayedValue.insert({'bottom' : new Element("span", {"class": "value"}).update(value.escapeHTML())});
    var deleteTool = new Element("span", {'class': "delete-tool", "title" : "Delete this term"}).update('&#x2716;');
    deleteTool.observe('click', this.removeItem.bindAsEventListener(this));
    var hiddenInput = new Element("input", {"type" : "hidden", "name" : this.inputName, "value" : key});
    listItem.appendChild(displayedValue);
    listItem.appendChild(deleteTool);
    listItem.appendChild(hiddenInput);
    if (this.options.showTooltip && info) {
      listItem.appendChild(new Element("div", {class : "tooltip"}).update(info));
    }
    this.list.appendChild(listItem);
  },

  removeItem : function(event) {
    var item = event.findElement('li');
    item.remove();
    this.input.value = "";
  }

});
  return MS;
}(MS || {}));
