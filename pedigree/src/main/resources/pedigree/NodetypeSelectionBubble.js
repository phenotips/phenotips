/*
 * NodeTypeOptions is the bubble that appears when the child creation handle bubble is clicked.
 * The user chooses a gender or property from the bubble, and creates the child with the corresponding
 * property.
 */

var NodetypeSelectionBubble = Class.create({

    //The skeleton for the bubble contents
    buttonsDefs : [
        {
            key: "M",
            type: "person",
            label: "Male",
            tip  : "Create a person of male gender",
            symbol: "◻",
            callback : "createNodeAction",
            params: ["Person", "M"]
        }, {
            key: "F",
            type: "person",
            label: "Female",
            tip  : "Create a person of female gender",
            symbol: "◯",
            callback : "createNodeAction",
            params: ["Person", "F"]
        }, {
            key: "U",
            type: "person",
            label: "Unknown",
            tip  : "Create a person of unknown gender",
            symbol: "◇",
            callback : "createNodeAction",
            params: ["Person", "U"]
        }, {
            key: "T",
            type: "person",
            label: "Twins",
            tip  : "Create twins (expandable to triplets or more)",
            symbol: "⋀",
            callback : "createTwinsAction",
            params: [],
            expandsTo: 'expandTwins'
        }, {
            type: "separator"
        }, {
            key: "m",
            type: "person",
            label: "Multiple",
            tip  : "Create a node representing multiple siblings",
            symbol: "〈n〉",
            callback : "createNodeAction",
            params: ["PersonGroup", "U"],
            expandsTo: "expandPersonGroup"
        }, {
            type: "separator"
        }, {
            key: "n",
            type: "marker",
            label: "No children",
            tip  : "Mark as childless by choice",
            symbol: "┴",
            callback : "setChildlessStatusAction",
            params: ["childless"]
        }, {
            key: "i",
            type: "marker",
            label: "Infertile",
            tip  : "Mark as infertile",
            symbol: "╧",
            callback : "setChildlessStatusAction",
            params: ["infertile"]
        }
    ],

    initialize : function() {
        this.element = new Element('div', {'class' : 'callout'});
        this.element.insert(new Element('span', {'class' : 'callout-handle'}));

        var container = new Element('div', {'class' : 'node-type-options'});
        this.expandedOptionsContainer = new Element('div', {'class' : 'node-type-options-extended'});
        this.element.insert(container);
        this.element.insert(this.expandedOptionsContainer);


        var _this = this;
        this.buttonsDefs.each(function(def) {
            container.insert(def.type == 'separator' ? _this._generateSeparator() : _this._createOption(def));
        });
        this.element.hide();
        editor.getWorkspace().getWorkArea().insert(this.element);

        this._onClickOutside = this._onClickOutside.bindAsEventListener(this);

        this.numPersonGroupNodes = 1;
        this.numTwinNodes = 2;

    },

    /*
     * Creates a button in the bubble corresponding do the defenition from the skeleton
     *
     * @param data the definition object from the bubble skeleton
     * @param callback the function executed by the button
     */
    _createOption : function(data) {
        var i = 1;
        if(!data) {
            return null;
        }
        var expandablePrefix = (typeof this[data.expandsTo] == "function") ? "expandable-" : "";
        var o = new Element('a', {
            'class' : expandablePrefix + 'node-type-option ' + (data.type || '') + '-type-option node-type-' + data.key,
            'title' : data.tip,
            'href' : '#'
        }).update(data.symbol); // TODO: eliminate symbol, do ".update(data.label)", add style (icons);
        var _this = this;
        o.observe('click', function(event) {
            event.stop();
            _this._node && typeof (_this._node[data.callback]) == 'function' && _this._node[data.callback].apply(_this._node, data.params);
            _this.hide();
        });
        var container = new Element("span");
        container.insert(o);
        expandablePrefix && container.insert(this.generateExpandArrow(data));
        return container;
    },

    generateExpandArrow: function(data) {
        var expandArrow = new Element('span', {
            'class' : 'expand-arrow collapsed',
            'title' : "show more options",
            'href' : '#'
        }).update("V");

        expandArrow.expand = function() {
            $$(".expand-arrow").forEach(function(arrow) {arrow.collapse()});
            this[data.expandsTo]();
            expandArrow.update("^");
            Element.removeClassName(expandArrow, "collapsed");
        }.bind(this);

        expandArrow.collapse = function() {
            this.expandedOptionsContainer.update("");
            expandArrow.update("V");
            Element.addClassName(expandArrow, "collapsed");
            this.numPersonGroupNodes = 1;
        }.bind(this);

        expandArrow.observe("click", function() {
            if(expandArrow.hasClassName("collapsed")) {
                expandArrow.expand();
            }
            else {
                expandArrow.collapse();
            }
        });
        return expandArrow;
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
        this.expandedOptionsContainer.update("");
        this._positionAt(x, y);
        document.observe('mousedown', this._onClickOutside);
    },

    /*
     * Hides the bubble from the viewport
     */
    hide : function() {
        document.stopObserving('click', this._onClickOutside);
        var me = this;
        $$(".expand-arrow").forEach(function(arrow) {
            arrow.collapse();
        });
        if (this._node) {
            this._node.onWidgetHide();
            delete this._node;
            // reset the state
            this.element.select('.node-type-option').invoke('show');
            this.element.removeClassName("upside");
        }
        this.element.hide();
        this.numPersonGroupNodes = 1;
    },

    /*
     * Hides the bubble if the user clicks outside the bubble
     *
     * @event the click event
     */
    _onClickOutside: function (event) {
        if (this.isActive()) {
            if (!event.findElement('.callout')) {
                this.hide();
            }
        }
    },

    /*
     * Returns true if the bubble is currently visible
     */
    isActive : function() {
        return !!this._node;
    },

    _decrementNumNodes: function() {
        return this.numPersonGroupNodes > 1 ? --this.numPersonGroupNodes : this.numPersonGroupNodes;
    },

    _incrementNumNodes: function() {
        return this.numPersonGroupNodes < 9 ? ++this.numPersonGroupNodes : this.numPersonGroupNodes;
    },

    expandPersonGroup: function() {
        //create rhombus icon
        // put counter on top of rhombus
        //add plus minus buttons on the sides
        //add ok button
        //var icon = '<svg <desc>Number of children</desc><text x="250" y="150" font-family="Verdana" font-size="12" fill="blue" >n</text></svg>';
        var me = this;
        var generateIcon = function(){
                var iconText = (me.numPersonGroupNodes > 1) ? String(me.numPersonGroupNodes) : "n";
                return '<svg version="1.1" viewBox="0.0 0.0 100.0 100.0" width=50 height=50 fill="none" stroke="none" stroke-linecap="square" stroke-miterlimit="10" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><clipPath id="p.0"><path d="m0 0l960.0 0l0 720.0l-960.0 0l0 -720.0z" clip-rule="nonzero"></path></clipPath><g clip-path="url(#p.0)"><path fill="#000000" fill-opacity="0.0" d="m0 0l960.0 0l0 720.0l-960.0 0z" fill-rule="nonzero"></path><path fill="#cfe2f3" d="m1.2283465 49.97113l48.53543 -48.535435l48.53543 48.535435l-48.53543 48.53543z" fill-rule="nonzero"></path><path stroke="#000000" stroke-width="2.0" stroke-linejoin="round" stroke-linecap="butt" d="m1.2283465 49.97113l48.53543 -48.535435l48.53543 48.535435l-48.53543 48.53543z" fill-rule="nonzero"></path><path fill="#000000" fill-opacity="0.0" d="m20.661417 22.068241l58.204727 0l0 48.000004l-58.204727 0z" fill-rule="nonzero"></path></g><desc>Number of children</desc><text x="35" y="60" font-family="Verdana" font-size="40" fill="black">'
                    + iconText + '</text></svg>';

        };
        var createBtn = new Element("button").update("create");
        var svgContainer = new Element('span').update(generateIcon());
        var minusBtn = new Element("span", {
            "class": 'minus-button'
        }).update("-");
        var plusBtn = new Element("span", {
            "class": 'plus-button'
        }).update("+");
        minusBtn.observe("click", function() {me._decrementNumNodes(); svgContainer.update(generateIcon())});
        plusBtn.observe("click", function() {me._incrementNumNodes(); svgContainer.update(generateIcon())});
        createBtn.observe("click", function() {
            me._node.createNodeAction("PersonGroup", "U").setNumPersons(me.numPersonGroupNodes);
        });
        this.expandedOptionsContainer.insert(minusBtn);
        this.expandedOptionsContainer.insert(svgContainer);
        this.expandedOptionsContainer.insert(plusBtn);
        this.expandedOptionsContainer.insert(createBtn);

//        var height = this.element.getHeight();
//        new Effect.Morph( this.element, {
//            style: 'background:#f00; color: #fff;', // CSS Properties
//            width: 400,
//            duration: 0.8 // Core Effect properties
//        });
        //this.element.morph('background:#00ff00; height:' + (height + 20) + 'px;');
    },

    expandTwins: function() {
        var me = this;
        var generateIcon = function(){
            return '<svg version="1.1" viewBox="0.0 0.0 100.0 100.0" width=50 height=50 fill="none" stroke="none" stroke-linecap="square" stroke-miterlimit="10" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><clipPath id="p.0"><path d="m0 0l960.0 0l0 720.0l-960.0 0l0 -720.0z" clip-rule="nonzero"></path></clipPath><g clip-path="url(#p.0)"><path fill="#000000" fill-opacity="0.0" d="m0 0l960.0 0l0 720.0l-960.0 0z" fill-rule="nonzero"></path><path fill="#cfe2f3" d="m1.2283465 49.97113l48.53543 -48.535435l48.53543 48.535435l-48.53543 48.53543z" fill-rule="nonzero"></path><path stroke="#000000" stroke-width="2.0" stroke-linejoin="round" stroke-linecap="butt" d="m1.2283465 49.97113l48.53543 -48.535435l48.53543 48.535435l-48.53543 48.53543z" fill-rule="nonzero"></path><path fill="#000000" fill-opacity="0.0" d="m20.661417 22.068241l58.204727 0l0 48.000004l-58.204727 0z" fill-rule="nonzero"></path></g><desc>Number of children</desc><text x="35" y="60" font-family="Verdana" font-size="40" fill="black">'
                + me.numTwinNodes + '</text></svg>';
        };
        var createBtn = new Element("button").update("create");
        var svgContainer = new Element('span').update(generateIcon());
        var minusBtn = new Element("span", {
            "class": 'minus-button'
        }).update("-");
        var plusBtn = new Element("span", {
            "class": 'plus-button'
        }).update("+");
        minusBtn.observe("click", function() {me._decrementNumNodes(); svgContainer.update(generateIcon())});
        plusBtn.observe("click", function() {me._incrementNumNodes(); svgContainer.update(generateIcon())});
        createBtn.observe("click", function() {
            me._node.createNodeAction("PersonGroup", "U").setNumPersons(me.numPersonGroupNodes);
        });
        this.expandedOptionsContainer.insert(minusBtn);
        this.expandedOptionsContainer.insert(svgContainer);
        this.expandedOptionsContainer.insert(plusBtn);
        this.expandedOptionsContainer.insert(createBtn);
    }
});