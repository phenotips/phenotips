NodeTypeOptions = Class.create({
  buttonsDefs : [
    {
      key: "M",
      type: "person",
      label: "Male",
      tip  : "Create a person of male gender",
      symbol: "◻"
    }, {
      key: "F",
      type: "person",
      label: "Female",
      tip  : "Create a person of female gender",
      symbol: "◯"
    }, {
      key: "U",
      type: "person",
      label: "Unknown",
      tip  : "Create a person of unknown gender",
      symbol: "◇"
    }, {
      key: "T",
      type: "person",
      label: "Twins",
      tip  : "Create twins (expandable to triplets or more)",
      symbol: "⋀"
    }, {
      type: "separator"
    }, {
      key: "m",
      type: "person",
      label: "Multiple",
      tip  : "Create a node representing multiple siblings",
      symbol: "〈n〉"
    }, {
      type: "separator"
    }, {
      key: "n",
      type: "marker",
      label: "No children",
      tip  : "Mark as childless by choice",
      symbol: "┴",
      callback : "setNoChildren"
    }, {
      key: "i",
      type: "marker",
      label: "Infertile",
      tip  : "Mark as infertile",
      symbol: "╧",
      callback : "setInfertile"
    }
  ],

  initialize : function() {
    this.element = new Element('div', {'class' : 'callout'});
    this.element.insert(new Element('span', {'class' : 'callout-handle'}));
    
    var container = new Element('div', {'class' : 'node-type-options'});
    this.element.insert(container);
    
    var _this = this;
    this.buttonsDefs.each(function(def) {
      container.insert(def.type == 'separator' ? _this._separator() : _this._createOption(def, 'createChild'));
    });
    this.element.hide();
    $('body').insert({'bottom' : this.element});

    this._onClickOutside = this._onClickOutside.bindAsEventListener(this);
  },
    
  _createOption : function(data, callback) {
    var i = 1;
    if(!data) {
      return;
    }
    var o = new Element('a', {
      'class' : 'node-type-option ' + (data.type || '') + '-type-option node-type-' + data.key,
      'title' : data.tip,
      'href' : '#'
    }).update(data.symbol); // TODO: eliminate symbol, do ".update(data.label)", add style (icons);
    !callback && (callback = data.callback);
    var _this = this;
    o.observe('click', function(event) {
      event.stop();
      // TODO:
      //_this._node && typeof (_this._node[callback]) == 'function' && _this._node[callback]();
      alert(data.key + " (" + data.label + ")"); // TODO : remove me!
      _this.hide();
    });
    return o;
  },
    
  _separator : function() {
    return new Element('span', {'class' : 'separator'}).update(' | ');
  },
  _positionAt : function(x, y) {
    y = Math.round(y);
    if (y + this.element.getHeight() > document.viewport.getHeight()) {
      this.element.addClassName("upside");
      y = Math.round(y - this.element.getHeight());
    }
    this.element.style.top = y + "px";
    var dx = Math.round(this.element.getWidth()/2);
    if (x - dx + this.element.getWidth() > document.viewport.getWidth()) {
      dx = Math.round(this.element.getWidth() - (document.viewport.getWidth() - x));
    } else if (dx > x) {
      dx = Math.round(x);
    }
    this.element.down('.callout-handle').style.left = dx + "px";
    this.element.style.left = Math.round(x - dx) + "px";
  },
  show : function(node, x, y) {
    // Quick trick to prevent the bubble from immediately hiding on the click that is supposed to display it (see also the "_onClickOutside" function).
    this._ignore = true;
    this._node = node;
    if (!this._node) return;
    // TODO decide which options to display, depending on the source node's status
    // E.g.:
    // if (/* the node has actual (person) children */) {
    //   this.element.select('.marker-type-option').invoke('hide');
    // } else if (/* the node cannot have children */) {
    //   this.element.select('.person-type-option').invoke('hide');
    // }
    this.element.show();
    this._positionAt(x, y);
    document.observe('click', this._onClickOutside);
  },
  hide : function() {
    document.stopObserving('click', this._onClickOutside);
    if (this._node) {
      delete this._node;
      // reset the state
      this.element.select('.node-type-option').invoke('show');
      this.element.removeClassName("upside");
    }
    this.element.hide();
  },
  _onClickOutside: function (event) {
    if (editor.nodeTypeOptions && editor.nodeTypeOptions.isActive()) {
      if (!event.element().up || !event.element().up('.callout')) {
        // Quick trick to prevent the bubble from immediately hiding on the click that is supposed to display it  (see also the "show" function)
        if (this._ignore) {
          this._ignore = false;
        } else {
          editor.nodeTypeOptions.hide();
        }
      }
    }
  },
  isActive : function() {
    return !!this._node;
  }
});