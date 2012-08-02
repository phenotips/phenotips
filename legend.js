var Legend = Class.create( {


/*
 * Class responsible for keeping track of disorders on canvas and their properties.
 * This information is graphically displayed in a 'Legend' box
 */
    initialize: function() {
        this._disorders = new Hash({});
        this._evaluations = {};
        this._usedColors = [];

        var canvas = $('canvas');

        this._legendBox = new Element('div', {'class' : 'legend-box', id: 'legend-box'});
        canvas.insert({'after' : this._legendBox});

//        this._legendTab = new Element('div', {'class' : 'legend-tab'});
//        this._legendBox.insert(this._legendTab);
        this._legendBox.setOpacity(0);
        //(new Effect.Opacity('legend-box', {to:0, duration: 0}));


        var legendTitle= new Element('h2', {'class' : 'legend-title'}).update('Key');
        this._legendBox.insert(legendTitle);

        this._disorderList = new Element('ul', {'class' : 'disorder-list'});
        this._legendBox.insert(this._disorderList);



        //this._legendBox.setOpacity(0);


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
        //TODO: this.buildLegend
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
            this.addUsedColor(color);
            var listElement = this.generateDisorderElement(disorder['id'], disorder['value'], color);
            this._disorderList.insert(listElement);
        }
        (new Effect.Opacity('legend-box', { from: 0, to:.9, duration: 0.5 }));
        this.getDisorder(disorder['id']).addAffectedNode(node);
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
        var wasntEmpty = this.getDisorders().keys().length > 0;
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
     * Returns an array of colors associated with disorders in the Legend
     *
     * @param disorderID the id number for the disorder, taken from the OMIM database
     */
    getUsedColors: function() {
        return this._usedColors;
    },

    /*
     * Replaces the array of colors associated with disorders in the Legend
     *
     * @param colors an array of strings representing CSS colors. e.g. ['blue', '#DADADA']
     */
    setUsedColors: function(colors) {
        this._usedColors = colors;
    },

    /*
     * Adds color to the list of colors used in the Legend
     *
     * @param color is a string representing a CSS color e.g. 'blue' or '#DADADA'
     */
    addUsedColor: function(color) {
        this.getUsedColors().push(color);
    },

    /*
     * Removes color from the list of colors used in the Legend
     *
     * @param color is a string representing a CSS color e.g. 'blue' or '#DADADA'
     */
    removeUsedColor: function(color) {
        this.setUsedColors(this.getUsedColors().without(color));
    },

    generateDisorderElement: function(id, name, color) {
        var item = new Element('li', {'class' : 'disorder', 'id' : 'disorder-' + id}).update(name);
        var bubble = new Element('span', {'class' : 'disorder-color'});
        bubble.style.backgroundColor = color;
        item.insert({'top' : bubble});
        var me = this;
        Element.observe(item, 'mouseover', function() {
            item.setStyle({'text-decoration':'underline', 'cursor' : 'default'});
            me.getDisorder(id).getAffectedNodes().each(function(node) {
                node.getGraphics().highlight();
            });
        });
        Element.observe(item, 'mouseout', function() {
            item.setStyle({'text-decoration':'none'});
            me.getDisorder(id).getAffectedNodes().each(function(node) {
                node.getGraphics().unHighlight();
            });
        });
        return item;
    }
});
