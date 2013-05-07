/**
 * Class responsible for keeping track of disorders and their properties.
 * This information is graphically displayed in a 'Legend' box
 *
 * @class Legend
 * @constructor
 */

var Legend = Class.create( {

    initialize: function() {
        this._disorderNames = {};
        this._disorderColors = {};
        this._affectedNodes = {};
        this._evaluations = {};

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
     * Returns an object of disorder IDs mapped to their names.
     *
     * @method getDisorders
     * @return {Object}
     */
    getDisorderNames: function() {
        return this._disorderNames;
    },

    /**
     * Returns an object of disorder IDs mapped to a list of IDs of nodes who have this disorder
     *
     * @method getAffectedNodes
     * @returns {Object}
     */
    getAffectedNodes: function() {
        return this._affectedNodes
    },

    /**
     * Returns an object of evaluation ID's mapped to the evaluation properties.
     *
     * @method getEvaluations
     * @return {Object}
     */
    getEvaluations: function() {
        return this._evaluations;
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
    setEvaluations: function(map) {
        this._evaluations = map;
        //TODO: this.buildLegend
    },

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
        if(Object.keys(this.getAffectedNodes()).length == 0) {
            (new Effect.Opacity('legend-box', { from: 0, to:.9, duration: 0.5 }));
        }
        if(!this.containsDisorder(disorderID)) {
            this.getAffectedNodes()[disorderID] = [nodeID];
            var color = this.generateColor(disorderID);
            this.getDisorderColors()[disorderID] = color;
            document.fire('disorder:color', {'id' : disorderID, color: color});
            this.getDisorderNames()[disorderID] = disorderName;
            var listElement = this.generateDisorderElement(disorderID, disorderName, color);
            this._disorderList.insert(listElement);
        }
        else if(this.getAffectedNodes()[disorderID].indexOf(nodeID) == -1) {
            this.getAffectedNodes()[disorderID].push(nodeID);
        }
        this._displayCasesForDisorder(disorderID);
    },

    /**
     * Removes an occurrence of a disorder if there are any. Removes the disorder
     * from the 'Legend' box if this disorder is not registered in any individual.
     *
     * @param {Number|String} disorderID ID for this disorder taken from the OMIM database
     * @param {Number} nodeID ID of the Person who has this disorder
     */
    removeCase: function(disorderID, nodeID) {
        if (this.containsDisorder(disorderID)) {
            this.getAffectedNodes()[disorderID] = this.getAffectedNodes()[disorderID].without(nodeID);
            if(this.getAffectedNodes()[disorderID].length == 0) {
                delete this.getAffectedNodes()[disorderID];
                delete this.getDisorderNames()[disorderID];
                delete this.getDisorderColors()[disorderID];
                var removeFromLegend = function() {$('disorder-' + disorderID).remove()};
                if(Object.keys(this.getAffectedNodes()).length == 0) {
                    new Effect.Opacity('legend-box', { from:.9, to:0, duration: 0.5, afterFinish: removeFromLegend});
                }
                else {
                    removeFromLegend();
                }
            }
            else
                this._displayCasesForDisorder(disorderID);
        }
    },
    
    /**
     * Updates the displayed number of affected cases for for a disorder in the legend UI.
     *
     * @method __displayCasesForDisorder
     * @param {String|Number} disorderID The identifier of the disorder to update
     * @private
     */
    _displayCasesForDisorder : function(disorderID) {
      var label = this._legendBox.down('li#disorder-' + disorderID + ' .disorder-cases');
      if (label) {
        var cases = this.getAffectedNodes()[disorderID] ? this.getAffectedNodes()[disorderID].length : 0;
        label.update(cases + " case" + ((cases - 1) && "s" || ""));
      }
    },

    /**
     * Returns True if the disorder with the given ID is registered in the Legend
     *
     * @method containsDisorder
     * @param {String|Number} disorderID The id for the disorder, taken from the OMIM database
     */
    containsDisorder: function(disorderID) {
        return !!this.getAffectedNodes()[disorderID];
    },

    /**
     * Retrieve the color associated to a specific disorder
     *
     * @method getDisorderColor
     * @param {String|Number} disorderID The id for the disorder, taken from the OMIM database
     * @return {String} CSS color value for that disorder
     */
    getDisorderColor: function(disorderID) {
        return this._disorderColors[disorderID];
    },

    /**
     * Returns an object with disorder IDs mapped to CSS colors
     *
     * @method getDisorderColors
     * @return {Object}
     */
    getDisorderColors: function() {
        return this._disorderColors;
    },

    /**
     * Generate the element that will display information about the given disorder in the legend
     *
     * @method generateDisorderElement
     * @param {String|Number} id The id for the disorder, taken from the OMIM database
     * @param {String} name The human-readable disorder name
     * @param {String} color CSS color associated with the disorder, displayed on affected nodes in the pedigree
     * @return {HTMLLIElement} List element to be insert in the legend
     */
    generateDisorderElement: function(id, name, color) {
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
            me.getAffectedNodes()[id] && me.getAffectedNodes()[id].forEach(function(nodeID) {
                var node = editor.getNode(nodeID);
                node && node.getGraphics().highlight();
            });
        });
        Element.observe(item, 'mouseout', function() {
            //item.setStyle({'text-decoration':'none'});
            item.down('.disorder-name').setStyle({'background':'', 'cursor' : 'default'});
            me.getAffectedNodes()[id] && me.getAffectedNodes()[id].forEach(function(nodeID) {
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
        var node = editor.getNodeIndex().getNodeNear(pos.x, pos.y);
        console.log("Position x: " + pos.x + " position y: " + pos.y);
        if (node && node.getType() == 'Person') {
            var id = disorderLabel.id.substring( disorderLabel.id.indexOf('-') + 1);
            var disName = disorderLabel.value = disorderLabel.down('.disorder-name').firstChild.nodeValue;
            node.addDisorderAction(new Disorder(id, disName));
        }
    },

    /**
     * Generates a CSS color. Has preference for 5 colors that can be distinguished in gray-scale.
     *
     * @method generateColor
     * @return {String} CSS color
     */
    generateColor: function(disorderID) {
        if(this.getDisorderColors()[disorderID]) {
            return this.getDisorderColors()[disorderID];
        }
        var usedColors = Object.values(this.getDisorderColors()),
            prefColors = ["#FEE090", '#E0F3F8', '#91BFDB', '#4575B4'];
        usedColors.each( function(color) {
            prefColors = prefColors.without(color);
        });
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
