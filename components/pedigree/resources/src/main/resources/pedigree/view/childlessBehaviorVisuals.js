define([
        "pedigree/pedigreeEditorParameters"
    ], function(
        PedigreeEditorParameters
    ){
    var ChildlessBehaviorVisuals = {

        /**
         * Returns the childless status shape for this Person
         *
         * @method getChildlessShape
         * @return {Raphael.el}
         */
        getChildlessShape: function() {
            return this._childlessShape;
        },

        /**
         * Returns the Raphaël element for this Person's childless status reason label
         *
         * @method getChildlessStatusLabel
         * @return {Raphael.el}
         */
        getChildlessStatusLabel: function() {
            return this._childlessStatusLabel;
        },
        
        /**
         * Updates the childless status icon for this Node based on the childless/infertility status.
         *
         * @method updateChildlessShapes
         */
        updateChildlessShapes: function() {
            var status = this.getNode().getChildlessStatus();
            this._childlessShape && this._childlessShape.remove();
            
            if(status) {
              var x    = this.getX();
              var y    = this.getY();
              var r    = PedigreeEditorParameters.attributes.infertileMarkerWidth;
              var lowY = this.getBottomY() + PedigreeEditorParameters.attributes.infertileMarkerHeight;
              
              var childlessPath = [["M", x, y],["L", x, lowY],["M", x - r, lowY], ["l", 2 * r, 0]];
              if(status == 'infertile')
                  childlessPath.push(["M", x - r, lowY + 5], ["l", 2 * r, 0]);

                this._childlessShape = editor.getPaper().path(childlessPath);
                if (status == 'childless' && this.getChildlessShapeAttr) {
                    this._childlessShape.attr(this.getChildlessShapeAttr());
                } else {
                    this._childlessShape.attr(PedigreeEditorParameters.attributes.childlessShapeAttr);
                }
                this._childlessShape.toBack();
            }
        },

        /**
         * Updates the childless status reason label for this Person
         *
         * @method updateChildlessStatusLabel
         */
        updateChildlessStatusLabel: function() {
            this._childlessStatusLabel && this._childlessStatusLabel.remove();
            this._childlessStatusLabel = null;
            
            var text = "";
            this.getNode().getChildlessReason() && (text += this.getNode().getChildlessReason());

            if(text.strip() != '') {
                this._childlessStatusLabel = editor.getPaper().text(this.getX(), this.getBottomY() + 18, "(" + text.slice(0, 15) +")" );
                this._childlessStatusLabel.attr({'font-size': 18, 'font-family': 'Cambria'});
                this._childlessStatusLabel.toBack();
            }

            this.drawLabels();
        }
    };
    return ChildlessBehaviorVisuals;
});