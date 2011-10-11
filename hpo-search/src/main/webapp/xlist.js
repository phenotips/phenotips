var MS = function(MS){
    
    var widgets = MS.widgets = MS.widgets || {};
    
    widgets.XList = Class.create({
        initialize: function(items, options) {
          this.items = items || [];
          this.options = options || {}
          this.listElement = new Element(this.options.ordered ? "ol" : "ul", {
            'class' : 'xlist' + (this.options.classes ? (' ' + this.options.classes) : '')
          });
          if (this.items && this.items.length > 0) {
            for (var i=0;i<this.items.length;i++) {
              this.addItem(this.items[i]);
            }
          }
        },
        addItem: function(item){ /* future: position (top, N) */
          if (!item || !(item instanceof MS.widgets.XListItem)) {
             item = new MS.widgets.XListItem(item);
          }
          var listItemElement = item.getElement();
          if (this.options.itemClasses && !this.options.itemClasses.blank()) {
            listItemElement.addClassName(this.options.itemClasses);
          }
          this.listElement.insert(listItemElement);
          if (typeof this.options.eventListeners == 'object') {
            item.bindEventListeners(this.options.eventListeners);
          }
          if (this.options.icon && !this.options.icon.blank()) {
            item.setIcon(this.options.icon, this.options.overrideItemIcon);
          }
          item.list = this; // associate list item to this XList 
        },
        getElement: function() {
          return this.listElement;
        }
    });
    
    widgets.XListItem = Class.create({
        initialize: function(content, options) {
          this.options = options || {};
          var classes = 'xitem ' + (this.options.noHighlight ? '' : 'xhighlight ');
          classes += this.options.classes ? this.options.classes: '';
          this.containerElement = new Element("div", {'class': 'xitemcontainer'}).insert(content || '');
          this.containerElement.addClassName(this.options.containerClasses || '');
          this.containerElement.setStyle({textIndent: '0px'});
          if (this.options.value) {
            this.containerElement.insert(new Element('div', {'class':'hidden value'}).insert(this.options.value));
          }
          this.listItemElement = new Element("li", {'class' : classes}).update( this.containerElement );
          if (this.options.icon && !this.options.icon.blank()) {
            this.setIcon(this.options.icon);
            this.hasIcon = true;
          }
          if (typeof this.options.eventListeners == 'object') {
            this.bindEventListeners(this.options.eventListeners);
          }
        },
        getElement: function() {
          return this.listItemElement;
        },
        setIcon: function(icon, override) {
          if (!this.hasIcon || override) {
            this.iconImage = new Image();
            this.iconImage.onload = function(){
                this.listItemElement.setStyle({
                  backgroundImage: "url(" + this.iconImage.src + ")",
                  backgroundRepeat: 'no-repeat',
                  // TODO: support background position as option
                  backgroundPosition : '3px 3px'
                });
                this.listItemElement.down(".xitemcontainer").setStyle({
                  textIndent:(this.iconImage.width + 6) + 'px'
                });
            }.bind(this)
            this.iconImage.src = icon;
          }
        },
        bindEventListeners: function(eventListeners) {
          var events = Object.keys(eventListeners);
          for (var i=0;i<events.length;i++) {
            this.listItemElement.observe(events[i], eventListeners[events[i]].bind(this.options.eventCallbackScope ? this.options.eventCallbackScope : this));
          }
        }
    });
    
    return MS;
    
}(MS || {});
