/**
 * Class responsible for the graph layout.
 */
var Layout = Class.create( {

    initialize: function() {
        $('action-layout').observe('click', this.doLayout.bind(this));
    },

    /**
     * Generate the perfect layout for this graph
     */
    doLayout : function() {
        var abstractGraph = this._buildAbstractGraph();
        this._hierarchicalLayout(abstractGraph);
        this._fixTangledPartnerships(abstractGraph)
        this._updateLayout(abstractGraph);
    },

    /**
     * Build the skeleton for the layout - the specification for the hierarchy of nodes
     * @return
     * @private
     */
    _buildAbstractGraph : function() {
        var graph = editor.getGraph().serialize();
        var abstractGraph = {};
        graph.persons.concat(graph.personGroups).concat(graph.placeHolders).each(function(p) {
            abstractGraph["Pers" + p.id] = {x: p.x, y: p.y, down: [], up: []};
        });
        graph.partnerships.each(function(p) {
            abstractGraph["Part" + p.id + "u"] = {down: ["Part" + p.id + "m", "Pers" + p.partner1ID, "Pers" + p.partner2ID], up: []};
            abstractGraph["Part" + p.id + "m"] = {down: ["Part" + p.id + "d"], up: ["Part" + p.id + "u"]};
            abstractGraph["Part" + p.id + "d"] = {down: [], up: ["Part" + p.id + "m", "Pers" + p.partner1ID, "Pers" + p.partner2ID]};
            abstractGraph["Pers" + p.partner1ID].down.push("Part" + p.id + "d");
            abstractGraph["Pers" + p.partner1ID].up.push("Part" + p.id + "u");
            abstractGraph["Pers" + p.partner2ID].down.push("Part" + p.id + "d");
            abstractGraph["Pers" + p.partner2ID].up.push("Part" + p.id + "u");
        });
        graph.pregnancies.each(function(p) {
            abstractGraph["Preg" + p.id + "u"] = {down: ["Preg" + p.id + "d"], up: ["Part" + p.partnershipID + "d"]};
            abstractGraph["Part" + p.partnershipID + "d"].down.push("Preg" + p.id + "u");
            abstractGraph["Preg" + p.id + "d"] = {down: [], up: ["Preg" + p.id + "u"]};
            p.childrenIDs.each(function(child) {
                abstractGraph["Preg" + p.id + "d"].down.push("Pers" + child);
                abstractGraph["Pers" + child].up.push("Preg" + p.id + "d");
            });
        });
        return abstractGraph;
    },

    /**
     * Repositions the nodes in a hierarchical layout
     * @param abstractGraph The specification for the hierarchy of nodes.
     * @private
     */
    _hierarchicalLayout : function(abstractGraph) {
    },

    /**
     * Modifies the layout to fix overlapping edges in the graph
     * @param abstractGraph The specification for the hierarchy of nodes.
     * @private
     */
    _fixTangledPartnerships : function(abstractGraph) {
    },

    _updateLayout : function(abstractGraph) {
    }
});
