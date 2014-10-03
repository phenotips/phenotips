/**
 * The UI element ("bubble") that contains options for the creation of a new node.
 *
 * @class NodetypeSelectionBubble
 * @constructor
 */

var NodetypeSelectionBubble = Class.create({

    //The skeleton for the bubble contents
    buttonsDefs : [
        {
            key: "M",
            type: "person",
            label: "Male",
            tip  : "Create a person of male gender",
            symbol: "",
            cssclass: "fa fa-square-o",
            callback : "CreateChild",
            params: { parameters: {"gender": "M"} },
            inSiblingMode: true
        }, {
            key: "F",
            type: "person",
            label: "Female",
            tip  : "Create a person of female gender",
            symbol: "",
            cssclass: "fa fa-circle-thin",
            callback : "CreateChild",
            params: { parameters: {"gender": "F"} },
            inSiblingMode: true
        }, {
            key: "U",
            type: "person",
            label: "Unknown",
            tip  : "Create a person of unknown gender",
            symbol: "<div class='fa fa-square-o fa-rotate-45'></div>",
            cssclass: "",
            callback : "CreateChild",
            params: { parameters: {"gender": "U"} },
            inSiblingMode: true
        }, {
            key: "T",
            type: "person",
            label: "Twins",
            tip  : "Create twins (expandable to triplets or more)",
            symbol: "⋀",
            cssclass: "",
            callback : "CreateChild",
            params: { "twins": true, "parameters": {"gender": "U"} },
            expandsTo: 'expandTwins',
            inSiblingMode: true
        }, 
           {
            key: "m",
            type: "person",
            label: "Multiple",
            tip  : "Create a node representing multiple siblings",
            symbol: "〈n〉",
            cssclass: "",
            callback : "CreateChild",
            params: { "group": true },
            expandsTo: "expandPersonGroup",
            inSiblingMode: true
        }, {
            type: "separator"
        }, {
            key: "n",
            type: "marker",
            label: "No children",
            tip  : "Mark as childless by choice",
            symbol: "┴",
            cssclass: "",
            callback : "setProperty",
            params: { setChildlessStatus: "childless" },
            inSiblingMode: false
        }, {
            key: "i",
            type: "marker",
            label: "Infertile",
            tip  : "Mark as infertile",
            symbol: "╧",
            cssclass: "",
            callback : "setProperty",
            params: { setChildlessStatus: "infertile" },
            inSiblingMode: false
        }
    ],

    initialize : function(siblingMode) {
        this._siblingMode = siblingMode;

        this.element = new Element('div', {'class' : 'callout'});
        this.element.insert(new Element('span', {'class' : 'callout-handle'}));

        var container = new Element('div', {'class' : 'node-type-options'});
        this.expandedOptionsContainer = new Element('div', {'class' : 'node-type-options-extended'});
        this.element.insert(container);
        this.element.insert(this.expandedOptionsContainer);

        var _this = this;
        this.buttonsDefs.each(function(def) {
            if (!siblingMode || (def.hasOwnProperty("inSiblingMode") && def.inSiblingMode))
                container.insert(def.type == 'separator' ? _this._generateSeparator() : _this._createOption(def));
        });        
        this.element.hide();
        editor.getWorkspace().getWorkArea().insert(this.element);

        this._onClickOutside = this._onClickOutside.bindAsEventListener(this);

        this.resetParameters();
    },
    
    resetParameters: function() {
        this.numPersonsInGroup = 1;
        this.numTwinNodes      = 2;        
    },

    /**
     * Creates a button in the bubble corresponding do the definition from the skeleton
     *
     * @method _createOption
     * @param data The definition object from the bubble skeleton
     * @return {HTMLElement} The span containing the button
     * @private
     */
    _createOption : function(data) {
        var i = 1;
        if(!data) {
            return null;
        }
        var expandablePrefix = (typeof this[data.expandsTo] == "function") ? "expandable-" : "";
        var o = new Element('a', {
            'class' : data.cssclass + ' ' + expandablePrefix + 'node-type-option ' + (data.type || '') + '-type-option node-type-' + data.key,
            'title' : data.tip,
            'href' : '#'
        }).update(data.symbol); // TODO: eliminate symbol, do ".update(data.label)", add style (icons);
        var _this = this;
        o.observe('click', function(event) {
            event.stop();            
            if (!_this._node) return;
            console.log("observe nodetype click: " + data.callback);            
            if (data.callback == "setProperty") {
                var event = { "nodeID": _this._node.getID(), "properties": data.params };
                document.fire("pedigree:node:setproperty", event);
            }
            else if (data.callback == "CreateChild") {
                _this.handleCreateAction(data);                                      
            }                        
            _this.hide();            
        });
        var container = new Element("span");
        container.insert(o);
        expandablePrefix && container.insert(this.generateExpandArrow(data));
        return container;
    },

    handleCreateAction: function(data) {
        var id       = this._node.getID();
        var nodeType = this._node.getType();                                                   
        if (nodeType == "Person") {
            var event = { "personID": id, "childParams": data.params.parameters, "preferLeft": false };
            if (data.params.twins) {
                event["twins"] = this.numTwinNodes;
            }
            if (data.params.group) {
                event["groupSize"] = this.numPersonsInGroup;
            }
            
            if (this._siblingMode)
                document.fire("pedigree:person:newsibling", event);
            else
                document.fire("pedigree:person:newpartnerandchild", event);
        }
        else if (nodeType == "Partnership") {
            var event = { "partnershipID": id, "childParams": data.params.parameters };
            if (data.params.twins) {
                event["twins"] = this.numTwinNodes;
            }                    
            if (data.params.group) {
                event["groupSize"] = this.numPersonsInGroup;
            }
            document.fire("pedigree:partnership:newchild", event);
        }           
        this.hide();
    },
    
    /**
     * Creates an arrow button that expands or shrinks the bubble
     *
     * @method generateExpandArrow
     * @param data The definition object from the bubble skeleton
     * @return {HTMLElement} The span containing the button
     */
    generateExpandArrow: function(data) {
        var expandArrow = new Element('span', {
            'class' : 'expand-arrow collapsed',
            'title' : "show more options",
            'href' : '#'
        }).update("▾");

        expandArrow.expand = function() {
            $$(".expand-arrow").forEach(function(arrow) {arrow.collapse()});
            this[data.expandsTo](data);
            expandArrow.update("▴");
            Element.removeClassName(expandArrow, "collapsed");
        }.bind(this);

        expandArrow.collapse = function() {
            this.expandedOptionsContainer.update("");
            expandArrow.update("▾");
            Element.addClassName(expandArrow, "collapsed");
        }.bind(this);

        expandArrow.observe("click", function() {
            console.log("observe2");
            if(expandArrow.hasClassName("collapsed")) {
                expandArrow.expand();
            }
            else {
                expandArrow.collapse();
            }
        });
        return expandArrow;
    },

    /**
     * Creates a line to separate buttons in the bubble
     *
     * @method _generateSeparator
     * @return {HTMLElement}
     * @private
     */
    _generateSeparator : function() {
        return new Element('span', {'class' : 'separator'}).update(' | ');
    },

    /**
     * Repositions the bubble to the given coordinates coordinates
     *
     * @method _positionAt
     * @param {Number} x The x coordinate in the viewport
     * @param {Number} y The y coordinate in the viewport
     * @private
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

    /**
     * Displays the bubble for the specified node
     *
     * @method show
     * @param {AbstractNode} node The node for which the bubble is displayed
     * @param {Number} x The x coordinate in the viewport
     * @param {Number} y The y coordinate in the viewport
     */
    show : function(node, x, y) {
        this._node = node;
        if (!this._node) return;
        //console.log("show1");
        this._node.onWidgetShow();
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

    /**
     * Hides the bubble from the viewport
     *
     * @method hide
     */
    hide : function() {
        //console.log("hide1");
        document.stopObserving('mousedown', this._onClickOutside);        
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
        this.resetParameters();  // reset number of twins/number of persons
    },

    /**
     * Hides the bubble if the user clicks outside
     *
     * @method _onClickOutside
     * @param {Event} event
     * @private
     */
    _onClickOutside: function (event) {
        //console.log("observe clickoutside nodetype");
        if (!event.findElement('.callout')) {
            this.hide();
        }
    },

    /**
     * Decrement the number of nodes to be created
     *
     * @method _decrementNumNodes
     * @return {Number} The resulting number of nodes to be created
     * @private
     */
    _decrementNumNodes: function() {
        return this.numPersonsInGroup > 1 ? --this.numPersonsInGroup : this.numPersonsInGroup;
    },

    /**
     * Increment the number of nodes to be created
     *
     * @method _incrementNumNodes
     * @return {Number} The resulting number of nodes to be created
     * @private
     */
    _incrementNumNodes: function() {
        return this.numPersonsInGroup < 9 ? ++this.numPersonsInGroup : this.numPersonsInGroup;
    },
    
    /**
     * Decrement the number of twins to be created
     *
     * @method _decrementNumTwins
     * @return {Number} The resulting number of twins to be created
     * @private
     */
    _decrementNumTwins: function() {
        return this.numTwinNodes > 2 ? --this.numTwinNodes : this.numTwinNodes;
    },

    /**
     * Increment the number of twins to be created
     *
     * @method _incrementNumTwins
     * @return {Number} The resulting number of twins to be created
     * @private
     */
    _incrementNumTwins: function() {
        return this.numTwinNodes < 9 ? ++this.numTwinNodes : this.numTwinNodes;
    },    

    /**
     * Expand the bubble and show additional options for creation of PersonGroup nodes
     *
     * @method expandPersonGroup
     */
    expandPersonGroup: function(personGroupMenuInfo) {
        //create rhombus icon
        // put counter on top of rhombus
        //add plus minus buttons on the sides
        //add ok button
        //var icon = '<svg <desc>Number of children</desc><text x="250" y="150" font-family="Verdana" font-size="12" fill="blue" >n</text></svg>';
        var me = this;
        var generateIcon = function(){
                var iconText = (me.numPersonsInGroup > 1) ? String(me.numPersonsInGroup) : "n";
                return '<svg version="1.1" viewBox="0.0 0.0 100.0 100.0" width=50 height=50 fill="none" stroke="none" stroke-linecap="square" stroke-miterlimit="10" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><clipPath id="p.0"><path d="m0 0l960.0 0l0 720.0l-960.0 0l0 -720.0z" clip-rule="nonzero"></path></clipPath><g clip-path="url(#p.0)"><path fill="#000000" fill-opacity="0.0" d="m0 0l960.0 0l0 720.0l-960.0 0z" fill-rule="nonzero"></path><path fill="#cfe2f3" d="m1.2283465 49.97113l48.53543 -48.535435l48.53543 48.535435l-48.53543 48.53543z" fill-rule="nonzero"></path><path stroke="#000000" stroke-width="2.0" stroke-linejoin="round" stroke-linecap="butt" d="m1.2283465 49.97113l48.53543 -48.535435l48.53543 48.535435l-48.53543 48.53543z" fill-rule="nonzero"></path><path fill="#000000" fill-opacity="0.0" d="m20.661417 22.068241l58.204727 0l0 48.000004l-58.204727 0z" fill-rule="nonzero"></path></g><desc>Number of children</desc><text x="35" y="60" font-family="Verdana" font-size="40" fill="black">'
                    + iconText + '</text></svg>';

        };
        var createBtn = new Element("input", {'type': 'button', 'value': 'create', 'class': 'button'});
        var svgContainer = new Element('span').update(generateIcon());
        var minusBtn = new Element("span", {
            "class": 'minus-button value-control-button'
        }).update("-");
        var plusBtn = new Element("span", {
            "class": 'plus-button value-control-button'
        }).update("+");
        minusBtn.observe("click", function() { me._decrementNumNodes(); svgContainer.update(generateIcon())});
        plusBtn.observe ("click", function() { me._incrementNumNodes(); svgContainer.update(generateIcon())});
        createBtn.observe("click", function() {
            //console.log("observeCreate1");
            me.handleCreateAction(personGroupMenuInfo);
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

    /**
     * Expand the bubble and show additional options for creation of twin nodes
     *
     * @method expandTwins
     */
    expandTwins: function(twinMenuInfo) {
        var me = this;
        var generateIcon = function(){
            return '<svg version="1.1" viewBox="0.0 0.0 100.0 100.0" width=50 height=50 fill="none" stroke="none" stroke-linecap="square" stroke-miterlimit="10" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><clipPath id="p.0"><path d="m0 0l960.0 0l0 720.0l-960.0 0l0 -720.0z" clip-rule="nonzero"></path></clipPath><g clip-path="url(#p.0)"><path fill="#000000" fill-opacity="0.0" d="m0 0l960.0 0l0 720.0l-960.0 0z" fill-rule="nonzero"></path><path fill="#cfe2f3" d="m1.2283465 49.97113l48.53543 -48.535435l48.53543 48.535435l-48.53543 48.53543z" fill-rule="nonzero"></path><path stroke="#000000" stroke-width="2.0" stroke-linejoin="round" stroke-linecap="butt" d="m1.2283465 49.97113l48.53543 -48.535435l48.53543 48.535435l-48.53543 48.53543z" fill-rule="nonzero"></path><path fill="#000000" fill-opacity="0.0" d="m20.661417 22.068241l58.204727 0l0 48.000004l-58.204727 0z" fill-rule="nonzero"></path></g><desc>Number of children</desc><text x="35" y="60" font-family="Verdana" font-size="40" fill="black">'
                + me.numTwinNodes + '</text></svg>';
        };
        var createBtn = new Element("input", {'type': 'button', 'value': 'create', 'class': 'button'});
        var svgContainer = new Element('span').update(generateIcon());
        var minusBtn = new Element("span", {
            "class": 'minus-button value-control-button'
        }).update("-");
        var plusBtn = new Element("span", {
            "class": 'plus-button value-control-button'
        }).update("+");
        minusBtn.observe("click", function() { me._decrementNumTwins(); svgContainer.update(generateIcon())});
        plusBtn.observe("click",  function() { me._incrementNumTwins(); svgContainer.update(generateIcon())});        
        createBtn.observe("click", function() {
            me.handleCreateAction(twinMenuInfo);
        });
        this.expandedOptionsContainer.insert(minusBtn);
        this.expandedOptionsContainer.insert(svgContainer);
        this.expandedOptionsContainer.insert(plusBtn);
        this.expandedOptionsContainer.insert(createBtn);
    }
});