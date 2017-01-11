/**
 * Deceased is a UI Element containing age of death and cause of death inputs for deceased life status
 *
 * @class Deceased
 *
 */
define([
        "pedigree/view/graphicHelpers"
    ], function(
        GraphicHelpers
    ){
    DeceasedMenu = Class.create({
        initialize : function() {
            this._justOpened = false;
            this.canvas = editor.getWorkspace().canvas || $('body');

            this.element = new Element('div', {'class' : 'callout deceased'});
            this.element.insert(new Element('span', {'class' : 'callout-handle'}));

            var container = new Element('div', {'class' : 'deceased-node-type-options'});

            this._at_age_input = new Element('input', {'class' : 'at-age-input', 'placeholder' : 'at age'});
            var at_age = new Element('div').insert(this._at_age_input);

            this._cause_input = new Element('input', {'class' : 'cause-input', 'placeholder' : 'cause'});
            var cause = new Element('div').insert(this._cause_input);

            container.insert(at_age).insert(cause);

            this.element.insert(container);

            // Insert in document
            this.hide();
            editor.getWorkspace().getWorkArea().insert(this.element);

            this._onClickOutside = this._onClickOutside.bindAsEventListener(this);
        },

        show : function(node, x, y) {
            var me = this;
            this._justOpened = true;
            setTimeout(function() { me._justOpened = false; }, 150);

            this.targetNode = node;
            if (!this.targetNode) return;

            this._at_age_input.id = 'at-age-input' + this.targetNode._id;
            this._cause_input.id = 'cause-input' + this.targetNode._id;

            this._at_age_input.value = this.targetNode.getDeceasedAge();
            this._cause_input.value = this.targetNode.getDeceasedCause();

            this.element.show();
            this.reposition(x, y);
            document.observe('mousedown', this._onClickOutside);
        },

        hide : function() {
            if (this._justOpened) {
                return;
            }
            document.stopObserving('mousedown', this._onClickOutside);
            if (this.targetNode) {
                var properties = {};
                properties["setDeceasedAge"] = $('at-age-input' + this.targetNode._id).value;
                properties["setDeceasedCause"] = $('cause-input' + this.targetNode._id).value;
                var event = { "nodeID": this.targetNode.getID(), "properties": properties };
                this.targetNode.onWidgetHide();
                delete this.targetNode;
                document.fire("pedigree:node:setproperty", event);
            }
            this.element.hide();
        },

        _onClickOutside: function (event) {
            if (!event.findElement('.callout')) {
                this.hide();
            }
        },

        reposition : function(x, y) {
            this.element.style.left = x + 'px';
            this.element.style.top  = y + 'px';
        }

    });
    return DeceasedMenu;
});
