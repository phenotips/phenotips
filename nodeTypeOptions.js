NodeTypeOptions = Class.create({
  personTypes : {
    'M' : {
      label: "Male",
      tip  : "Create a person of male gender"
    },
    'F' : {
      label: "Female",
      tip  : "Create a person of female gender"
    },
    'U' : {
      label: "Unknown",
      tip  : "Create a person of unknown gender"
    },
    'T' : {
      label: "Twins",
      tip  : "Create twins (expandable to triplets or more)"
    },
    'm' : {
      label: "Multiple",
      tip  : "Create a node representing multiple siblings"
    }
  },

  otherTypes : {
    'i' : {
      label: "Infertile",
      tip  : "Mark as infertile",
      callback : "setInfertile"
    },
    'n' : {
      label: "No children",
      tip  : "Mark as childless by choice",
      callback : "setNoChildren"
    }
  },

  initialize : function() {
    this.element = new Element('div', {'class' : 'node-type-options'});
    this.options = {};
    var _this = this;
    Object.keys(this.personTypes).each(function(key) {
      _this.element.insert(_this._createOption(key, 'personTypes', 'person-type-option', 'createChild'));
    });
    this.element.insert(this._separator());
    Object.keys(this.otherTypes).each(function(key) {
      _this.element.insert(_this._createOption(key, 'otherTypes', 'other-type-option'));
    });
  },
    
  _createOption : function(key, category, cssClass, callback) {
    var data = this[category] && this[category][key];
    if(!data) {
      return;
    }
    var o = new Element('a', {
      'class' : 'node-type-option ' + (cssClass || '') + ' node-type-' + key,
      'title' : data[key].tip,
      'href' : '#'
    }).update(data[key].label);
    !callback && (callback = data[key].callback);
    o.observe('click', function(event) {
      event.stop();
      this._node && typeof (this._node[callback]) == 'function' && this._node[callback]();
      this.hide();
    });
  },
    
  _separator : function() {
    return new Element('span', {'class' : 'separator'}).update(' | ');
  },
  show : function(x, y, node) {
    this._node = node;
    if (!this._node) return;
    if (node.getLowerNeighbors().length) {

    }
  },
  hide : function() {
    delete this._node;
  }
});