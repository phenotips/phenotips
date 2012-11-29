/*
 * Class responsible for keeping track of disorders and their properties.
 * This information is graphically displayed in a 'Legend' box
 */

var Legend = Class.create( {

    initialize: function() {
        this._disorders = new Hash({});
        this._evaluations = {};
        this._disorderColors = new Hash({});

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

    /*
     * Returns an object of disorder ID's mapped to Disorder objects.
     */
    getDisorders: function() {
        return this._disorders;
    },

    /*
     * Replaces the disorder Hash with the Hash passed in the parameter, and updates
     * the graphical 'Legend' to represent the new information
     *
     * @param hash is a Hash object that has disorder IDs as keys and Disorder objects as values
     */
    setDisorders: function(hash) {
        this._disorders = hash;
    },

    /*
     * Returns an object of evaluation ID's mapped to the evaluation properties.
     */
    getEvaluations: function() {
        return this._evaluations;
    },

    /*
     * Replaces the evaluation map with the map passed in the parameter, and updates
     * the graphical 'Legend' to represent the new information
     *
     * @param map is an object formatted as follows { E1 : {conclusion: '+', result: '(36n/18n)', numAEvaluations: 4}}
     */
    setEvaluations: function(map) {
        this._evaluations = map;
        //TODO: this.buildLegend
    },

    /*
     * Returns the Disorder object with the given disorder ID.
     *
     * @param disorderID the id number for the disorder, taken from the OMIM database
     */
    getDisorder: function(disorderID) {
        return this.getDisorders().get(disorderID);
    },

    /*
     * Replaces the Disorder object with the given disorder ID.
     *
     * @param disorderID the id number for the disorder, taken from the OMIM database
     * @param disorder a Disorder object
     */
    setDisorder: function(disorderID, disorder) {
        this.getDisorders().set(disorderID, disorder);
    },

    /*
     * Registers an occurrence of a disorder. If disorder hasn't been documented yet,
     * designates a color for it.
     *
     * @param disorder an object with fields 'id' and 'value', where id is the id number
     * for the disorder, taken from the OMIM database, and 'value' is the name of the disorder.
     * eg. {id: 33244, value: 'Down Syndrome'}
     */
    addCase: function(disorder, node) {
        if (!this.containsDisorder(disorder['id'])) {
            this.setDisorder(disorder['id'], new Disorder(disorder['id'], disorder['value'], null, []));
            var color = this.getDisorder(disorder['id']).getColor();
            this.addUsedColor(disorder['id'], color);
            var listElement = this.generateDisorderElement(disorder['id'], disorder['value'], color);
            this._disorderList.insert(listElement);
        }
        (new Effect.Opacity('legend-box', { from: 0, to:.9, duration: 0.5 }));
        this.getDisorder(disorder['id']).addAffectedNode(node);
        this._displayCasesForDisorder(disorder['id']);
    },

    /*
     * Removes an occurrence of a disorder if there are any. Removes the disorder
     * from the 'Legend' box if this disorder is not registered in any individual.
     *
     * @param disorder an object with fields 'id' and 'value', where id is the id number
     * for the disorder, taken from the OMIM database, and 'value' is the name of the disorder.
     * eg. {id: 33244, value: 'Down Syndrome'}
     */
    removeCase: function(disorder, node) {
        if (this.containsDisorder(disorder['id'])) {
            this.getDisorder(disorder['id']).removeAffectedNode(node);
            if(this.getDisorder(disorder['id']).getNumAffected() < 1) {
                var removeFromLegend = function() {$('disorder-' + disorder['id']).remove()};
                this.getDisorders().unset(disorder['id']);
                if(this.getDisorders().keys().length == 0) {
                    new Effect.Opacity('legend-box', { from:.9, to:0, duration: 0.5, afterFinish: removeFromLegend});
                }
                else {
                    removeFromLegend();
                }
            }
            else
                this._displayCasesForDisorder(disorder['id']);
        }
    },
    
    /**
     * Update the displayed number of affected cases for for a disorder in the legend UI.
     * 
     * @param disorderID the identifier of the disorder to update
     */
    _displayCasesForDisorder : function(disorderID) {
      var label = this._legendBox.down('li#disorder-' + disorderID + ' .disorder-cases');
      if (label) {
        var cases = this.getDisorder(disorderID).getNumAffected();
        label.update(cases + " case" + ((cases - 1) && "s" || ""));
      }
    },

    /*
     * Returns true if the disorder with the given ID is registered in the Legend
     *
     * @param disorderID the id number for the disorder, taken from the OMIM database
     */
    containsDisorder: function(disorderID) {
        return this.getDisorders().keys().indexOf(disorderID)  > -1;
    },

    /*
     * Adds color to the list of colors used in the Legend
     *
     * @param color is a string representing a CSS color e.g. 'blue' or '#DADADA'
     */
    addUsedColor: function(disorderID, color) {
        this.getDisorderColors().set(disorderID, color);
        document.fire('disorder:color', {'id' : disorderID, color: color});
    },

    /**
     * Retrieve the color associated to a specific disorder
     *
     * @param disorderID the identifier of the disorder
     * @return the color for that disorder, as a string #rrggbb, or undefined if the disorder is not registered
     */
    getDisorderColor: function(disorderID) {
        return this._disorderColors.get(disorderID);
    },

    getDisorderColors: function() {
        return this._disorderColors;
    },

    /**
     * Generate the element that will display information about the given disorder in the legend
     * 
     * @param id the (internal) identifier of the disorder
     * @param name the human-readable disorder name
     * @param color the color associated with the disorder, displayed on affected nodes in the pedigree
     * @return a 'li' element to be insert in the legend's list
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
            me.getDisorder(id).getAffectedNodes().each(function(node) {
                node.getGraphics().highlight();
            });
        });
        Element.observe(item, 'mouseout', function() {
            //item.setStyle({'text-decoration':'none'});
            item.down('.disorder-name').setStyle({'background':'', 'cursor' : 'default'});
            me.getDisorder(id).getAffectedNodes().each(function(node) {
                node.getGraphics().unHighlight();
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

    _onDropDisorder: function(disorder, target, event) {
        var divPos = editor.getWorkspace().viewportToDiv(event.pointerX(), event.pointerY());
        var pos = editor.getWorkspace().divToCanvas(divPos.x,divPos.y);
        var node = editor.getNodeIndex().getNodeNear(pos.x, pos.y);
        console.log(node);
        if (node && node.getType() == 'Person') {
            var disorderObj = {};
            disorderObj.id = disorder.id.substring( disorder.id.indexOf('-') + 1);
            disorderObj.value = disorder.down('.disorder-name').firstChild.nodeValue;
            node.addDisorder(disorderObj, true);
        }
    },
});
