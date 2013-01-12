/*
 * NodeTypeOptions is the bubble that appears when the child creation handle bubble is clicked.
 * The user chooses a gender or property from the bubble, and creates the child with the corresponding
 * property.
 */

NodeTypeOptions = Class.create({

    //The skeleton for the bubble contents
    buttonsDefs : [
        {
            key: "M",
            type: "person",
            label: "Male",
            tip  : "Create a person of male gender",
            symbol: "◻",
            callback : "createChild",
            params: ["Person", "M", 1]
        }, {
            key: "F",
            type: "person",
            label: "Female",
            tip  : "Create a person of female gender",
            symbol: "◯",
            callback : "createChild",
            params: ["Person", "F", 1]
        }, {
            key: "U",
            type: "person",
            label: "Unknown",
            tip  : "Create a person of unknown gender",
            symbol: "◇",
            callback : "createChild",
            params: ["Person", "U", 1]
        }, {
            key: "T",
            type: "person",
            label: "Twins",
            tip  : "Create twins (expandable to triplets or more)",
            symbol: "⋀",
            callback : "createChild",
            params: ["Person", "U", 2]
        }, {
            type: "separator"
        }, {
            key: "m",
            type: "person",
            label: "Multiple",
            tip  : "Create a node representing multiple siblings",
            symbol: "〈n〉",
            callback : "createChild",
            params: ["PersonGroup", "U", 1]
        }, {
            type: "separator"
        }, {
            key: "n",
            type: "marker",
            label: "No children",
            tip  : "Mark as childless by choice",
            symbol: "┴",
            callback : "setChildlessStatus",
            params: ["childless"]
        }, {
            key: "i",
            type: "marker",
            label: "Infertile",
            tip  : "Mark as infertile",
            symbol: "╧",
            callback : "setChildlessStatus",
            params: ["infertile"]
        }
    ],

    initialize : function() {
        this.element = new Element('div', {'class' : 'callout'});
        this.element.insert(new Element('span', {'class' : 'callout-handle'}));

        var container = new Element('div', {'class' : 'node-type-options'});
        this.element.insert(container);

        var _this = this;
        this.buttonsDefs.each(function(def) {
            container.insert(def.type == 'separator' ? _this._generateSeparator() : _this._createOption(def, 'createChild'));
        });
        this.element.hide();
        editor.getWorkspace().getWorkArea().insert(this.element);

        this._onClickOutside = this._onClickOutside.bindAsEventListener(this);
    },

    /*
     * Creates a button in the bubble corresponding do the defenition from the skeleton
     *
     * @param data the definition object from the bubble skeleton
     * @param callback the function executed by the button
     */
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
        callback = data.callback;
        var _this = this;
        o.observe('click', function(event) {
            event.stop();
            _this._node && typeof (_this._node[callback]) == 'function' && _this._node[callback].apply(_this._node, data.params);
            _this.hide();
        });
        return o;
    },

    /*
     * Creates a line to separate buttons in the bubble
     */
    _generateSeparator : function() {
        return new Element('span', {'class' : 'separator'}).update(' | ');
    },

    /*
     * Repositions the bubble to the given coordinates coordinates
     *
     * @param x the x coordinate in the viewport
     * @param y the y coordinate in the viewport
     */
    _positionAt : function(x, y) {
        y = Math.round(y);
        if (y + this.element.getHeight() > editor.getWorkspace().getWorkArea().getHeight()) {
            this.element.addClassName("upside");
            y = Math.round(y - this.element.getHeight());
        }
        this.element.style.top = y + "px";
        var dx = Math.round(this.element.getWidth()/2);
        if (x - dx + this.element.getWidth() > editor.getWorkspace().getWorkArea().getWidth()) {
            dx = Math.round(this.element.getWidth() - (editor.getWorkspace().getWorkArea().getWidth() - x));
        } else if (dx > x) {
            dx = Math.round(x);
        }
        this.element.down('.callout-handle').style.left = dx + "px";
        this.element.style.left = Math.round(x - dx) + "px";
    },

    /*
     * Displays the bubble for the specified node
     *
     * @param node the node for which the bubble is displayed
     * @param x the x coordinate in the viewport
     * @param y the y coordinate in the viewport
     */
    show : function(node, x, y) {
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
        document.observe('mousedown', this._onClickOutside);
    },

    /*
     * Hides the bubble from the viewport
     */
    hide : function() {
        document.stopObserving('click', this._onClickOutside);
        if (this._node) {
            this._node.onWidgetHide();
            delete this._node;
            // reset the state
            this.element.select('.node-type-option').invoke('show');
            this.element.removeClassName("upside");
        }
        this.element.hide();
    },

    /*
     * Hides the bubble if the user clicks outside the bubble
     *
     * @event the click event
     */
    _onClickOutside: function (event) {
        if (this.isActive()) {
            if (!event.element().up || !event.element().up('.callout')) {
                this.hide();
            }
        }
    },

    /*
     * Returns true if the bubble is currently visible
     */
    isActive : function() {
        return !!this._node;
    }
});