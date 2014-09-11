/**
 * Base class for various "legend" widgets
 *
 * @class Legend
 * @constructor
 */
           
var Legend = Class.create( {

    initialize: function(title) {
        var legendContainer = $('legend-container');
        if (legendContainer == undefined) {
            var legendContainer = new Element('div', {'class': 'legend-container', 'id': 'legend-container'});
            editor.getWorkspace().getWorkArea().insert(legendContainer);
        }

        this._legendBox = new Element('div', {'class' : 'legend-box', id: 'legend-box'});
        this._legendBox.hide();
        legendContainer.insert(this._legendBox);

        var legendTitle= new Element('h2', {'class' : 'legend-title'}).update(title);
        this._legendBox.insert(legendTitle);

        Element.observe(this._legendBox, 'mouseover', function() {
            $$('.menu-box').invoke('setOpacity', .1);
        });
        Element.observe(this._legendBox, 'mouseout', function() {
            $$('.menu-box').invoke('setOpacity', 1);
        });
    }
});
