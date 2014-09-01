/**
 * Class responsible for keeping track of disorders and their properties.
 * This information is graphically displayed in a 'Legend' box
 *
 * @class Legend
 * @constructor
 */
           
var DisorgerLegend = Class.create( {

    initialize: function() {
        this._disorderCache = {};
        
        this._specialDisordersRegexps = [new RegExp("^1BrCa", "i"),
                                         new RegExp("^2BrCa", "i"),
                                         new RegExp("^OvCa",  "i"),
                                         new RegExp("^ProCa", "i"),
                                         new RegExp("^PanCa", "i") ];

        this._disorderColors = {};
        this._affectedNodes  = {};

        this._legendBox = new Element('div', {'class' : 'legend-box', id: 'legend-box'});
        editor.getWorkspace().getWorkArea().insert(this._legendBox);
        this._legendBox.setOpacity(0);

        var legendTitle= new Element('h2', {'class' : 'legend-title'}).update('Key');
        this._legendBox.insert(legendTitle);

        this._disorderList = new Element('ul', {'class' : 'disorder-list'});
        this._legendBox.insert(this._disorderList);

        Element.observe(this._legendBox, 'mouseover', function() {
            $$('.menu-box').invoke('setOpacity', .5);
        });
        Element.observe(this._legendBox, 'mouseout', function() {
            $$('.menu-box').invoke('setOpacity', 1);
        });
        Droppables.add(editor.getWorkspace().canvas, {accept: 'disorder', onDrop: this._onDropDisorder.bind(this)});
    },
   
    /**
     * Returns the name of disorder with the given ID.
     * Returns "" for unknown disorders and disorders not loaded form the database
     *
     * @method getDisorderName
     * @return {String} Name of the given disorder
     */
    getDisorderName: function(disorderID) {
        return this.getDisorder(disorderID).getName();
    },

    /**
     * Retrieve the color associated to a specific disorder
     *
     * @method getDisorderColor
     * @param {String|Number} disorderID The id for the disorder, taken from the OMIM database
     * @return {String} CSS color value for that disorder
     */
    getDisorderColor: function(disorderID) {
        if (!this._disorderColors.hasOwnProperty(disorderID))
            return "#ff0000";
        return this._disorderColors[disorderID];
    },
        
    /**
     * Returns the disorder object with the given ID. If object is not in cache yet
     * returns a newly created one which may have the disorder name & other attributes not loaded yet
     *
     * @method getDisorder
     * @return {Object}
     */    
    getDisorder: function(disorderID) {
        if (!isInt(disorderID)) {
            disorderID = Disorder.sanitizeID(disorderID);
        }
        if (!this._disorderCache.hasOwnProperty(disorderID)) {
            var whenNameIsLoaded = function() { this._updateDisorderName(disorderID); }
            this._disorderCache[disorderID] = new Disorder(disorderID, null, whenNameIsLoaded.bind(this));            
        }
            
        return this._disorderCache[disorderID];
    },

    /**
     * Replaces the evaluation map with the map passed in the parameter, and updates
     * the graphical 'Legend' to represent the new information
     *
     * @method setEvaluations
     * @param map Formatted as follows:
     *
     {
        E1 : {conclusion: '+', result: '(36n/18n)', numAEvaluations: 4},
        ...
     }
     */
    //setEvaluations: function(map) {
    //    this._evaluations = map;
    //    //TODO: this.buildLegend
    //},

    /**
     * Registers an occurrence of a disorder. If disorder hasn't been documented yet,
     * designates a color for it.
     *
     * @method addCase
     * @param {Number|String} disorderID ID for this disorder taken from the OMIM database
     * @param {String} disorderName The name of the disorder
     * @param {Number} nodeID ID of the Person who has this disorder
     */
    addCase: function(disorderID, disorderName, nodeID) {
        if (!this._disorderCache.hasOwnProperty(disorderID))
            this._disorderCache[disorderID] = new Disorder(disorderID, disorderName);
                                
        if(Object.keys(this._affectedNodes).length == 0) {
            this._legendBox.setOpacity(0.9);
            //(new Effect.Opacity('legend-box', { from: 0, to:.9, duration: 0.5 }));
        }
        if(!this._hasAffectedNodes(disorderID)) {
            this._affectedNodes[disorderID] = [nodeID];
            var color = this._generateColor(disorderID);
            this._disorderColors[disorderID] = color;
            document.fire('disorder:color', {'id' : disorderID, color: color});
            var listElement = this._generateDisorderElement(disorderID, disorderName, color);
            this._disorderList.insert(listElement);
        }
        else {
            this._affectedNodes[disorderID].push(nodeID);
        }
        this._displayCasesForDisorder(disorderID);
    },

    /**
     * Removes an occurrence of a disorder if there are any. Removes the disorder
     * from the 'Legend' box if this disorder is not registered in any individual.
     *
     * @param {Number} disorderID ID for this disorder taken from the OMIM database
     * @param {Number} nodeID ID of the Person who has this disorder
     */
    removeCase: function(disorderID, nodeID) {
        if (this._hasAffectedNodes(disorderID)) {
            this._affectedNodes[disorderID] = this._affectedNodes[disorderID].without(nodeID);
            if(this._affectedNodes[disorderID].length == 0) {
                //console.log("no more disorders with id = " + disorderID); 
                delete this._affectedNodes[disorderID];
                delete this._disorderColors[disorderID];
                $('disorder-' + disorderID).remove();                
                if(Object.keys(this._affectedNodes).length == 0) {
                    this._legendBox.setOpacity(0);
                    //new Effect.Opacity('legend-box', { from:.9, to:0, duration: 0.5 });
                }
            }
            else
                this._displayCasesForDisorder(disorderID);
        }
    },
    
    /**
     * Updates the displayed number of affected cases for for a disorder in the legend UI.
     *
     * @method _displayCasesForDisorder
     * @param {Number} disorderID The identifier of the disorder to update
     * @private
     */
    _displayCasesForDisorder : function(disorderID) {
      var label = this._legendBox.down('li#disorder-' + disorderID + ' .disorder-cases');
      if (label) {
        var cases = this._affectedNodes.hasOwnProperty(disorderID) ? this._affectedNodes[disorderID].length : 0;
        label.update(cases + "&nbsp;case" + ((cases - 1) && "s" || ""));
      }
    },
    
    /**
     * Updates the displayed disorder name for the given disorder
     *
     * @method _updateDisorderName
     * @param {Number} disorderID The identifier of the disorder to update
     * @private
     */    
    _updateDisorderName: function(disorderID) {
        console.log("updating disorder display for " + disorderID + ", name = " + this.getDisorder(disorderID).getName());
        var name = this._legendBox.down('li#disorder-' + disorderID + ' .disorder-name');
        name.update(this.getDisorder(disorderID).getName());
    },   

    /**
     * Returns True if there are nodes reported to have this disorder to this DisorderLegend
     *
     * @method _hasAffectedNodes
     * @param {Number} disorderID The id for the disorder, taken from the OMIM database
     * @private 
     */
    _hasAffectedNodes: function(disorderID) {
        return this._affectedNodes.hasOwnProperty(disorderID);
    },

    /**
     * Generate the element that will display information about the given disorder in the legend
     *
     * @method _generateDisorderElement
     * @param {Number} id The id for the disorder, taken from the OMIM database
     * @param {String} name The human-readable disorder name
     * @param {String} color CSS color associated with the disorder, displayed on affected nodes in the pedigree
     * @return {HTMLLIElement} List element to be insert in the legend
     */
    _generateDisorderElement: function(id, name, color) {
        var item = new Element('li', {'class' : 'disorder', 'id' : 'disorder-' + id}).update(new Element('span', {'class' : 'disorder-name'}).update(name));
        var bubble = new Element('span', {'class' : 'disorder-color'});
        bubble.style.backgroundColor = color;
        item.insert({'top' : bubble});
        var countLabel = new Element('span', {'class' : 'disorder-cases'});
        var countLabelContainer = new Element('span', {'class' : 'disorder-cases-container'}).insert("(").insert(countLabel).insert(")");
        item.insert(" ").insert(countLabelContainer);
        var me = this;
        Element.observe(item, 'mouseover', function() {
            //item.setStyle({'text-decoration':'underline', 'cursor' : 'default'});
            item.down('.disorder-name').setStyle({'background': color, 'cursor' : 'default'});
            me._affectedNodes[id] && me._affectedNodes[id].forEach(function(nodeID) {
                var node = editor.getNode(nodeID);
                node && node.getGraphics().highlight();
            });
        });
        Element.observe(item, 'mouseout', function() {
            //item.setStyle({'text-decoration':'none'});
            item.down('.disorder-name').setStyle({'background':'', 'cursor' : 'default'});
            me._affectedNodes[id] && me._affectedNodes[id].forEach(function(nodeID) {
                var node = editor.getNode(nodeID);
                node && node.getGraphics().unHighlight();
            });
        });
        new Draggable(item, {
          revert: true,
          reverteffect: function(segment) {
          // Reset the in-line style.
            segment.setStyle({
              height: '',
              left: '',
              position: '',
              top: '',
              zIndex: '',
              width: ''
            });
          },
          ghosting: true
        });
        return item;
    },

    /**
     * Callback for dragging a disorder from the legend onto-nodes
     *
     * @method _onDropDisorder
     * @param {HTMLElement} [disorderLabel]
     * @param {HTMLElement} [target]
     * @param {Event} [event]
     * @private
     */
    _onDropDisorder: function(disorderLabel, target, event) {
        var divPos = editor.getWorkspace().viewportToDiv(event.pointerX(), event.pointerY());
        var pos = editor.getWorkspace().divToCanvas(divPos.x,divPos.y);
        var node = editor.getView().getPersonNodeNear(pos.x, pos.y);
        //console.log("Position x: " + pos.x + " position y: " + pos.y);
        if (node) {
            var disorderID = disorderLabel.id.substring( disorderLabel.id.indexOf('-') + 1);
            var currentDisorders = node.getDisorders().slice(0);
            if (currentDisorders.indexOf(disorderID) == -1) {   // only if the node does not have this disorder yet
                currentDisorders.push(disorderID);
                editor.getView().unmarkAll();
                var properties = { "setDisorders": currentDisorders };
                var event = { "nodeID": node.getID(), "properties": properties };
                document.fire("pedigree:node:setproperty", event);
            } else {
                alert("This person already has the specified disorder");
            }
        }
    },

    /**
     * Generates a CSS color. Has preference for 7 colors that can be distinguished in gray-scale.
     *
     * @method generateColor
     * @return {String} CSS color
     */
    _generateColor: function(disorderID) {
        if(this._disorderColors.hasOwnProperty(disorderID)) {
            return this._disorderColors[disorderID];
        }

        // check special disorder prefixes
        for (var i = 0; i < this._specialDisordersRegexps.length; i++) {
            if (disorderID.match(this._specialDisordersRegexps[i]) !== null) {
                for (var disorder in this._disorderColors) {
                    if (this._disorderColors.hasOwnProperty(disorder)) {
                        if (disorder.match(this._specialDisordersRegexps[i]) !== null)
                            return this._disorderColors[disorder];
                    }
                }
                break;
            }
        }

        var usedColors = Object.values(this._disorderColors),
            prefColors = ["#FEE090", '#E0F8F8', '#8ebbd6', '#4575B4', '#fca860', '#9a4500', '#81a270'];
        usedColors.each( function(color) {
            prefColors = prefColors.without(color);
        });
        if (disorderID == "affected" && usedColors.indexOf('#FEE090') > -1 ) {
            return "#dbad71";
        }
        if(prefColors.length > 0) {
            return prefColors[0];
        }
        else {
            var randomColor = Raphael.getColor();
            while(randomColor == "#ffffff" || usedColors.indexOf(randomColor) != -1) {
                randomColor = "#"+((1<<24)*Math.random()|0).toString(16);
            }
            return randomColor;
        }
    }
});
