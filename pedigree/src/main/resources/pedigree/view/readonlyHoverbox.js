/**
 * A stub hoverbox used when generating read-only pedigrees
 */
var ReadOnlyHoverbox = Class.create(AbstractHoverbox, {

    initialize: function($super, node, x, y, shapes) {
        this._node   = node;
        this._nodeX  = x;
        this._nodeY  = y;
        this._shapes = shapes;
    },

    getWidth: function() {
        return 0;
    },

    getHeight: function() {
        return 0;
    },

    getNode: function() {
        return this._node;
    },

    generateButtons: function() {
    },

    removeButtons: function () {
    },

    hideButtons: function() {
    },

    showButtons: function() {
    },

    getCurrentButtons: function() {
        return this._currentButtons;
    },

    removeHandles: function () {
    },

    hideHandles: function() {
    },

    showHandles: function() {
    },

    generateHandles: function() {
    },

    regenerateHandles: function() {
    },

    getBoxOnHover: function() {
        return null;
    },

    isHovered: function() {
        return false;
    },

    setHovered: function(isHovered) { 
    },
    
    setHighlighted: function(isHighlighted) {
    },    

    getHoverZoneMask: function() {
        return null;
    },

    getFrontElements: function() {
        return this._shapes;
    },

    getBackElements: function() {
        return this._shapes;
    },

    isMenuToggled: function() {
        return false;
    },

    animateDrawHoverZone: function() {
    },

    animateHideHoverZone: function() {
    },

    disable: function() {
    },

    enable: function() {
    },

    remove: function() {
    },

    onWidgetHide: function() {
    },

    onWidgetShow: function() {
    }
});
