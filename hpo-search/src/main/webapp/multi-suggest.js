var MS = (function (MS) {
  var widgets = MS.widgets = MS.widgets || {};
  widgets.SuggestPicker = Class.create({
  initialize: function(element, suggest) {
    this.input = element;
    this.suggest = suggest;
    this.inputName = this.input.name;
    this.input.name = this.input.name + "__suggested";
    this.suggest.options.callback = this.acceptSuggestion.bind(this);
    this.list = new Element('ul', {'class' : 'accepted-suggestions'});
    this.input.insert({'after' : this.list});
  },

  acceptSuggestion : function(obj) {
    this.addItem(obj.id || obj.value, obj.value);
    this.input.value = "";
    return false;
  },

  addItem : function(key, value) {
    if (!key) {
      return;
    }
    var listItem = new Element("li");
    var displayedKey = new Element("span", {"class": "key"}).update(key.escapeHTML());
    var displayedValue = new Element("span", {"class": "accepted-suggestion"});
    displayedValue.insert({'bottom' : new Element("span", {"class": "key"}).update("[" + key.escapeHTML() + "]")});
    displayedValue.insert({'bottom' : new Element("span", {"class": "sep"}).update(" ")});
    displayedValue.insert({'bottom' : new Element("span", {"class": "value"}).update(value.escapeHTML())});
    var deleteTool = new Element("span", {'class': "delete-tool", "title" : "Delete this term"}).update('&#x2716;');
    deleteTool.observe('click', this.removeItem.bindAsEventListener(this));
    var hiddenInput = new Element("input", {"type" : "hidden", "name" : this.inputName, "value" : key});
    listItem.appendChild(displayedValue);
    listItem.appendChild(deleteTool);
    listItem.appendChild(hiddenInput);
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
