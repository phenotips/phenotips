/**
 * View is responsible for graphical representation of th epedigree as well as user interaction
 *
 * @class View
 * @constructor
 */
define([
        "pedigree/pedigreeEditorParameters",
        "pedigree/model/helpers",
        "pedigree/view/lineSet",
        "pedigree/view/graphicHelpers",
        "pedigree/view/partnership",
        "pedigree/view/person",
        "pedigree/view/personGroup",
        "pedigree/view/personPlaceholder"
    ], function(
        PedigreeEditorParameters,
        Helpers,
        LineSet,
        GraphicHelpers,
        Partnership,
        Person,
        PersonGroup,
        PersonPlaceholder
    ){
    var View = Class.create({

        initialize: function() {
            console.log("--- view init ---");

            this.preGenerateGraphics();

            this._nodeMap = {};    // {nodeID} : {AbstractNode}

            this.hoverModeZones = editor.getPaper().set();

            this._currentMarkedNew   = [];
            this._currentGrownNodes  = [];
            this._currentHoveredNode = null;
            this._currentDraggable   = null;

            this._lineSet = new LineSet();   // used to track intersecting lines
        },

        /**
         * Saves all pedigree-specific settings/user choices/color scheme into an object
         */
        getSettings: function() {
            return {
                "legendSettings": {
                    "preferences": {
                        "style": "multisector"   // "multiSector"/"fixedSector"
                    },
                    "abnormalities": {
                        "disorders": editor.getDisorderLegend().getAllSettings(),
                        "candidateGenes": editor.getCandidateGeneLegend().getAllSettings(),
                        "causalGenes": editor.getCausalGeneLegend().getAllSettings(),
                        "carrierGenes": editor.getCarrierGeneLegend().getAllSettings(),
                        "phenotypes": editor.getHPOLegend().getAllSettings(),
                        "cancers": editor.getCancerLegend().getAllSettings()
                    }
                }
            }
        },

        /**
         * Restores pedigree-specific settings/user choices/color scheme from an object
         */
        loadSettings: function(settingsObject) {
            if (settingsObject.hasOwnProperty("legendSettings")) {
                if (settingsObject.legendSettings.hasOwnProperty("preferences")
                    && settingsObject.legendSettings.preferences.hasOwnProperty("style")) {
                    PedigreeEditorParameters.attributes.legendStyle = settingsObject.legendSettings.preferences.style;
                }
                if (settingsObject.legendSettings.hasOwnProperty("abnormalities")) {
                    if (settingsObject.legendSettings.abnormalities.hasOwnProperty("disorders")) {
                        editor.getDisorderLegend().setAllSettings(settingsObject.legendSettings.abnormalities.disorders);
                    }
                    if (settingsObject.legendSettings.abnormalities.hasOwnProperty("candidateGenes")) {
                        editor.getCandidateGeneLegend().setAllSettings(settingsObject.legendSettings.abnormalities.candidateGenes);
                    }
                    if (settingsObject.legendSettings.abnormalities.hasOwnProperty("causalGenes")) {
                        editor.getCausalGeneLegend().setAllSettings(settingsObject.legendSettings.abnormalities.causalGenes);
                    }
                    if (settingsObject.legendSettings.abnormalities.hasOwnProperty("carrierGenes")) {
                        editor.getCarrierGeneLegend().setAllSettings(settingsObject.legendSettings.abnormalities.carrierGenes);
                    }
                    if (settingsObject.legendSettings.abnormalities.hasOwnProperty("phenotypes")) {
                        editor.getHPOLegend().setAllSettings(settingsObject.legendSettings.abnormalities.phenotypes);
                    }
                    if (settingsObject.legendSettings.abnormalities.hasOwnProperty("cancers")) {
                        editor.getCancerLegend().setAllSettings(settingsObject.legendSettings.abnormalities.cancers);
                    }
                }
            }
        },

        /**
         * Pre-generates paths and pre-computes bounding boxes for shapes which are commonly used in the graph.
         * Raphael is slow and re-computing each path/box for every node is noticeably slow
         *
         * @method preGenerateGraphics
         */
        preGenerateGraphics: function() {
            //
            // computing scaled icons:
            //   var iconScale = 0.6;
            //   var path = "...";
            //   console.log("scaled path: " + Raphael.transformPath(path, ["s", iconScale, iconScale, 0, 0]));
            //

            // 1) delete button
            // nonScaledPath = var path = "M24.778,21.419 19.276,15.917 24.777,10.415 21.949,7.585 16.447,13.087 10.945,7.585 8.117,10.415 13.618,15.917 8.116,21.419 10.946,24.248 16.447,18.746 21.948,24.248z";
            this.__deleteButton_svgPath = "M14.867,12.851C14.867,12.851,11.566,9.55,11.566,9.55C11.566,9.55,14.866,6.249,14.866,6.249C14.866,6.249,13.169,4.551,13.169,4.551C13.169,4.551,9.868,7.852,9.868,7.852C9.868,7.852,6.567,4.551,6.567,4.551C6.567,4.551,4.87,6.249,4.87,6.249C4.87,6.249,8.171,9.55,8.171,9.55C8.171,9.55,4.87,12.851,4.870,12.851C4.870,12.851,6.568,14.549,6.568,14.549C6.568,14.549,9.868,11.248,9.868,11.248C9.868,11.248,13.169,14.549,13.169,14.549C13.169,14.549,14.867,12.851,14.867,12.851";
            this.__deleteButton_BBox    = Raphael.pathBBox(this.__deleteButton_svgPath);

            // 2) proband arrow
            //this.__arrow_svgPath = "M7.589,20.935l-6.87,6.869l2.476,2.476l6.869-6.869l1.858,1.857l2.258-8.428l-8.428,2.258L7.589,20.935z";
            this.__arrow_svgPath = "M8.348,23.029C8.348,23.029,0.791,30.584,0.791,30.584C0.791,30.584,3.515,33.308,3.515,33.308C3.515,33.308,11.07,25.752,11.0704,25.752C11.07,25.752,13.114,27.795,13.114,27.795C13.114,27.795,15.598,18.524,15.598,18.524C15.598,18.524,6.327,21.008,6.327,21.008C6.327,21.008,8.348,23.029,8.348,23.0285C8.348,23.029,8.348,23.029,8.348,23.029";
            this.__probandArrowPath = Raphael.transformPath(this.__arrow_svgPath, ["s", 1.3, 1.3, 0, 0]);

            // 3) move node left/right buttons
            this.__moveNodeLeft_svgPath  = "M0,0 L-10,10 L0,20 z";
            this.__moveNodeLeft_BBox     = Raphael.pathBBox(this.__moveNodeLeft_svgPath);
            this.__moveNodeRight_svgPath = "M-10,0 L0,10 L-10,20 z";
            this.__moveNodeRight_BBox    = Raphael.pathBBox(this.__moveNodeRight_svgPath);
        },

        /**
         * Returns a map of node IDs to nodes
         *
         * @method getNodeMap
         * @return {Object}
         *
         {
            {nodeID} : {AbstractNode}
         }
         */
        getNodeMap: function() {
            return this._nodeMap;
        },

        /**
         * Returns a node with the given node ID
         *
         * @method getNode
         * @param {nodeId} id of the node to be returned
         * @return {AbstractNode}
         *
         */
        getNode: function(nodeId) {
            if (!this._nodeMap.hasOwnProperty(nodeId)) {
                console.log("ERROR: requesting non-existent node " + nodeId);
                throw "ERROR";
                return null;
            }
            return this._nodeMap[nodeId];
        },

        /**
         * Returns the person node containing x and y coordinates, or null if outside all person nodes
         *
         * @method getPersonNodeNear
         * @return {Object} or null
         */
        getPersonNodeNear: function(x, y) {
            for (var nodeID in this._nodeMap) {
                if (this._nodeMap.hasOwnProperty(nodeID)) {
                    var node = this.getNode(nodeID);
                    if ((node.getType() == "Person" || node.getType() == "PersonGroup") && node.getGraphics().containsXY(x,y))
                        return node;
                }
            }
            return null;
        },

        /**
         * Redraws the pedigree image in anonymized/non-anonymized way
         * @param {Object} anonymizeSettings a set of anonymization properties, currently "removePII" and "removeComments"
         *                                   When false anonymization status is reset to "do not anonymize"
         */
        setAnonymizeStatus: function(anonymizeSettings) {
            if (typeof anonymizeSettings !== 'object') {
                anonymizeSettings = {};
            }
            for (var nodeID in this._nodeMap) {
                if (this._nodeMap.hasOwnProperty(nodeID)) {
                    var node = this.getNode(nodeID);
                    node.getGraphics().setAnonymizedStatus(anonymizeSettings);
                }
            }
        },

        /**
         * Returns the node that is currently selected
         *
         * @method getCurrentHoveredNode
         * @return {AbstractNode}
         */
        getCurrentHoveredNode: function() {
            return this._currentHoveredNode;
        },

        /**
         * Returns the currently dragged element
         *
         * @method getCurrentDraggable
         * @return Either a handle from a hoverbox, or a PlaceHolder
         */
        getCurrentDraggable: function() {
            return this._currentDraggable;
        },

        /**
         * Returns the Object that is currently being dragged
         *
         * @method setCurrentDraggable
         * @param draggable A handle or a PlaceHolder
         */
        setCurrentDraggable: function(draggable) {
            this._currentDraggable = draggable;
        },

        /**
         * Removes given node from node index (Does not delete the node visuals).
         *
         * @method removeFromNodeMap
         * @param {nodeId} id of the node to be removed
         */
        removeFromNodeMap: function(nodeID) {
            delete this.getNodeMap()[nodeID];
        },

        /**
         * Creates a new set of raphael objects representing a curve from (xFrom, yFrom) trough (...,yTop) to (xTo, yTo).
         * The bend from (xTo,yTo) to vertical level yTop will happen "lastBend" pixels from xTo.
         * In case the flat part intersects any existing known lines a special crossing is drawn and added to the set.
         *
         * @method drawCurvedLineWithCrossings
         */
        drawCurvedLineWithCrossings: function ( id, xFrom, yFrom, yTop, xTo, yTo, lastBend, attr, twoLines, secondLineBelow, startSameX ) {
            //console.log("yFrom: " + yFrom + ", yTo: " + yTo + ", yTop: " + yTop);

            if (yFrom == yTop && yFrom == yTo) {
                return this.drawLineWithCrossings(id, xFrom, yFrom, xTo, yTo, attr, twoLines, secondLineBelow, false, startSameX);
            }

            var cornerRadius     = PedigreeEditorParameters.attributes.curvedLinesCornerRadius * 0.8;
            var goesRight        = ( xFrom > xTo );
            if (isFinite(lastBend)) {
                var xFinalBend       = goesRight ? xTo + lastBend                  : xTo - lastBend;
                var xFinalBendVert   = goesRight ? xTo + lastBend + cornerRadius   : xTo - lastBend - cornerRadius;
                var xBeforeFinalBend = goesRight ? xTo + lastBend + cornerRadius*2 : xTo - lastBend - cornerRadius*2;
            } else {
                var xBeforeFinalBend = xTo;
            }
            var xFromAndBit        = goesRight ? xFrom - cornerRadius/2        : xFrom + cornerRadius/2;
            var xFromAfterCorner   = goesRight ? xFromAndBit - cornerRadius    : xFromAndBit + cornerRadius;
            var xFromAfter2Corners = goesRight ? xFromAndBit - 2*cornerRadius  : xFromAndBit + 2 * cornerRadius;

            //console.log("XFinalBend: " + xFinalBend + ", xTo : " + xTo);

            if (yFrom <= yTop) {
                this.drawLineWithCrossings(id, xFrom, yFrom, xBeforeFinalBend, yFrom, attr, twoLines, !goesRight, true, startSameX);
            }
            else {
                this.drawLineWithCrossings(id, xFrom, yFrom, xFromAndBit, yFrom, attr, twoLines, !goesRight, true, startSameX);

                if (Math.abs(yFrom - yTop) >= cornerRadius*2) {
                    if (goesRight)
                        GraphicHelpers.drawCornerCurve( xFromAndBit, yFrom, xFromAfterCorner, yFrom-cornerRadius, true, attr, twoLines, -2.5, 2.5, 2.5, -2.5 );
                    else
                        GraphicHelpers.drawCornerCurve( xFromAndBit, yFrom, xFromAfterCorner, yFrom-cornerRadius, true, attr, twoLines, 2.5, 2.5, -2.5, -2.5 );
                    this.drawLineWithCrossings(id, xFromAfterCorner, yFrom-cornerRadius, xFromAfterCorner, yTop+cornerRadius, attr, twoLines, goesRight);
                    if (goesRight)
                        GraphicHelpers.drawCornerCurve( xFromAfterCorner, yTop+cornerRadius, xFromAfter2Corners, yTop, false, attr, twoLines, -2.5, 2.5, 2.5, -2.5 );
                    else
                        GraphicHelpers.drawCornerCurve( xFromAfterCorner, yTop+cornerRadius, xFromAfter2Corners, yTop, false, attr, twoLines, 2.5, 2.5, -2.5, -2.5 );
                } else {
                    // draw one continuous curve
                    if (goesRight)
                        GraphicHelpers.drawLevelChangeCurve( xFromAndBit, yFrom, xFromAfter2Corners, yTop, attr, twoLines, -2.5, 2.5, 2.5, -2.5 );
                    else
                        GraphicHelpers.drawLevelChangeCurve( xFromAndBit, yFrom, xFromAfter2Corners, yTop, attr, twoLines, 2.5, 2.5, -2.5, -2.5 );
                }
                this.drawLineWithCrossings(id, xFromAfter2Corners, yTop, xBeforeFinalBend, yTop, attr, twoLines, !goesRight, true);
            }

            if (xBeforeFinalBend != xTo) {
                // curve down to yTo level
                if (Math.abs(yTo - yTop) >= cornerRadius*2) {
                    // draw corner
                    if (goesRight)
                        GraphicHelpers.drawCornerCurve( xBeforeFinalBend, yTop, xFinalBendVert, yTop+cornerRadius, true, attr, twoLines, 2.5, 2.5, -2.5, -2.5 );
                    else
                        GraphicHelpers.drawCornerCurve( xBeforeFinalBend, yTop, xFinalBendVert, yTop+cornerRadius, true, attr, twoLines, 2.5, -2.5, -2.5, 2.5 );
                    this.drawLineWithCrossings(id, xFinalBendVert, yTop+cornerRadius, xFinalBendVert, yTo-cornerRadius, attr, twoLines, !goesRight);
                    if (goesRight)
                        GraphicHelpers.drawCornerCurve( xFinalBendVert, yTo-cornerRadius, xFinalBend, yTo, false, attr, twoLines, 2.5, 2.5, -2.5, -2.5 );
                    else
                        GraphicHelpers.drawCornerCurve( xFinalBendVert, yTo-cornerRadius, xFinalBend, yTo, false, attr, twoLines, 2.5, -2.5, -2.5, 2.5 );
                } else {
                    // draw one continuous curve
                    if (goesRight)
                        GraphicHelpers.drawLevelChangeCurve( xBeforeFinalBend, yTop, xFinalBend, yTo, attr, twoLines, 2.5, 2.5, -2.5, -2.5 );
                    else
                        GraphicHelpers.drawLevelChangeCurve( xBeforeFinalBend, yTop, xFinalBend, yTo, attr, twoLines, 2.5, -2.5, -2.5, 2.5 );
                }
                this.drawLineWithCrossings(id, xFinalBend, yTo, xTo, yTo, attr, twoLines, !goesRight);
            }
        },

        /**
         * Creates a new set of raphael objects representing a line segment from (x1,y1) to (x2,y2).
         * In case this line segment intersects any existing known segments a special crossing is drawn and added to the set.
         *
         * @method drawLineWithCrossings
         */
        drawLineWithCrossings: function(owner, x1, y1, x2, y2, attr, twoLines, secondLineBelow, bothEndsGoDown, sameXStart) {

            // make sure line goes from the left to the right (and if vertical from the top to the bottom):
            // this simplifies drawing the line piece by piece from intersection to intersection
            if (x1 > x2 || ((x1 == x2) && (y1 > y2))) {
                var tx = x1;
                var ty = y1;
                x1 = x2;
                y1 = y2;
                x2 = tx;
                y2 = ty;
            }

            var isHorizontal = (y1 == y2);
            var isVertical   = (x1 == x2);

            var intersections = this._lineSet.addLine( owner, x1, y1, x2, y2 );

            // sort intersections by distance from the start
            var compareDistanceToStart = function( p1, p2 ) {
                    var dist1 = (x1-p1.x)*(x1-p1.x) + (y1-p1.y)*(y1-p1.y);
                    var dist2 = (x1-p2.x)*(x1-p2.x) + (y1-p2.y)*(y1-p2.y);
                    return dist1 - dist2;
                };
            intersections.sort(compareDistanceToStart);
            //console.log("intersection points: " + Helpers.stringifyObject(intersections));

            for (var lineNum = 0; lineNum < (twoLines ? 2 : 1); lineNum++) {

                // TODO: this is a bit hairy, just a quick hack to make two nice parallel curves
                //       for consang. relationships: simple raphael.transform() does not work well
                //       because then the curves around crossings wont be exactly above the crossing
                if (twoLines) {
                    if (!bothEndsGoDown) {
                        x1 += (sameXStart && !secondLineBelow) ? 0 : (-2.5 + lineNum * 7.5);
                        x2 += (sameXStart && secondLineBelow)  ? 0 : (-2.5 + lineNum * 7.5);
                    } else {
                        x1 -= (sameXStart && secondLineBelow) ? 0 : 2.5;
                        x2 += (sameXStart && !secondLineBelow) ? 0 : 2.5;
                    }

                    if (secondLineBelow) {
                        y1 += ( 2.5 - lineNum * 7.5);
                        y2 += ( 2.5 - lineNum * 7.5);
                    }
                    else {
                        y1 += (-2.5 + lineNum * 7.5);
                        y2 += (-2.5 + lineNum * 7.5);
                    }
                }

                var raphaelPath = "M " + x1 + " " + y1;
                for (var i = 0; i < intersections.length; i++) {
                    var intersectPoint = intersections[i];

                    var distance = function(p1, p2) {
                        return (p1.x-p2.x)*(p1.x-p2.x) + (p1.y-p2.y)*(p1.y-p2.y);
                    };

                    var noCrossSymbolProximity = isHorizontal ? 20*20 : 9*9;

                    if (distance(intersectPoint, {"x": x1, "y": y1}) < noCrossSymbolProximity)
                        continue;
                    if (distance(intersectPoint, {"x": x2, "y": y2}) < noCrossSymbolProximity)
                        continue;

                    if (isHorizontal) {
                        if (twoLines) {
                            if (secondLineBelow)
                                intersectPoint.y += ( 2.5 - lineNum * 7.5);
                            else
                                intersectPoint.y += (-2.5 + lineNum * 7.5);
                        }
                        // a curve above the crossing
                        raphaelPath += " L " + (intersectPoint.x - 10) + " " + intersectPoint.y;
                        raphaelPath += " C " + (intersectPoint.x - 7)  + " " + (intersectPoint.y + 1) +
                                         " " + (intersectPoint.x - 7)  + " " + (intersectPoint.y - 7) +
                                         " " + (intersectPoint.x)      + " " + (intersectPoint.y - 7);
                        raphaelPath += " C " + (intersectPoint.x + 7)  + " " + (intersectPoint.y - 7) +
                                         " " + (intersectPoint.x + 7)  + " " + (intersectPoint.y + 1) +
                                         " " + (intersectPoint.x + 10) + " " + (intersectPoint.y);
                    } else if (isVertical) {
                        if (twoLines) {
                            intersectPoint.x += ( -2.5 + lineNum * 7.5);
                        }
                        // a curve on the right around crossing
                        raphaelPath += " L " + intersectPoint.x        + " " + (intersectPoint.y - 10);
                        raphaelPath += " C " + (intersectPoint.x - 1)  + " " + (intersectPoint.y - 7) +
                                         " " + (intersectPoint.x + 7)  + " " + (intersectPoint.y - 7) +
                                         " " + (intersectPoint.x + 7)  + " " + (intersectPoint.y);
                        raphaelPath += " C " + (intersectPoint.x + 7)  + " " + (intersectPoint.y + 7) +
                                         " " + (intersectPoint.x - 1)  + " " + (intersectPoint.y + 7) +
                                         " " + (intersectPoint.x)      + " " + (intersectPoint.y + 10);
                    }
                    // else: some diagonal line: presumably there should be none, if there are some
                    //       everything will be ok except there will be no special intersection graphic drawn
                }
                raphaelPath += " L " + x2 + " " + y2;
                editor.getPaper().path(raphaelPath).attr(attr).toBack();
            }
        },

        /**
         * Creates a new node in the graph and returns it. The node type is obtained from
         * editor.getGraph() and may be on of Person, Partnership or ... TODO. The position
         * of the node is also obtained form editor.getGraph()
         *
         * @method addPerson
         * @param {Number} [id] The id of the node
         * @return {Person}
         */
        addNode: function(id) {
            //console.log("add node");
            var positionedGraph = editor.getGraph();

            if (!positionedGraph.isValidID(id))
                throw "addNode(): Invalid id";

            var node;
            var properties = positionedGraph.getProperties(id);

            var graphPos = positionedGraph.getPosition(id);
            var position = editor.convertGraphCoordToCanvasCoord(graphPos.x, graphPos.y );

            if (positionedGraph.isRelationship(id)) {
                //console.log("-> add partnership");
                node = new Partnership(position.x, position.y, id, properties);
            }
            else if (positionedGraph.isPlaceholder(id)) {
                node = new PersonPlaceholder(position.x, position.y, id, properties);
            }
            else if (positionedGraph.isPersonGroup(id)) {
                //console.log("-> add person group");
                node = new PersonGroup(position.x, position.y, id, properties);
            }
            else if (positionedGraph.isPerson(id)) {
                //console.log("-> add person");
                node = new Person(position.x, position.y, id, properties);
            }
            else {
                throw "addNode(): unsupported node type";
            }

            this.getNodeMap()[id] = node;

            return node;
        },

        moveNode: function(id, animate) {
            var positionedGraph = editor.getGraph();
            var graphPos = positionedGraph.getPosition(id);
            var position = editor.convertGraphCoordToCanvasCoord(graphPos.x, graphPos.y );
            this.getNode(id).setPos(position.x, position.y, animate);
        },

        changeNodeIds: function( changedIdsSet ) {
            var newNodeMap = {};

            // change all IDs at once so that have both new and old references at the same time
            for (oldID in this._nodeMap) {
                var node  = this.getNode(oldID);

                var newID = changedIdsSet.hasOwnProperty(oldID) ? changedIdsSet[oldID] : oldID;
                node.setID( newID );

                newNodeMap[newID] = node;
            }

            this._nodeMap = newNodeMap;

            this._lineSet.replaceIDs(changedIdsSet);

            editor.getCancerLegend().replaceIDs(changedIdsSet);
            editor.getCandidateGeneLegend().replaceIDs(changedIdsSet);
            editor.getCausalGeneLegend().replaceIDs(changedIdsSet);
            editor.getHPOLegend().replaceIDs(changedIdsSet);
            editor.getDisorderLegend().replaceIDs(changedIdsSet);
        },

        /**
         * Enters hover-mode state, which is when a handle or a PlaceHolder is being dragged around the screen
         *
         * @method enterHoverMode
         * @param sourceNode The node whose handle is being dragged, or the placeholder that is being dragged
         * @param hoverType Enum includes 'parent', 'child', 'partner' or 'sibling'. Only nodes which can be in the correponding
         *                   relationship with sourceNode will be highlighted
         * dragged on top of them.
         */
        enterHoverMode: function(sourceNode, hoverType) {

            //var timer = new Helpers.Timer();

            var me = this;
            var validTargets = this.getValidDragTargets(sourceNode.getID(), hoverType);

            validTargets.each(function(target) {
                if (typeof target === 'object') {
                    var nodeID = target.nodeID;
                    var highlightColor = (target.hasOwnProperty("preferred") && target.preferred)
                                         ? null : PedigreeEditorParameters.attributes.secondaryGlowColor;
                } else {
                    var nodeID = target;
                    var highlightColor = null;
                }

                me._currentGrownNodes.push(nodeID);

                var node = me.getNode(nodeID);
                node.getGraphics().grow(highlightColor);

                var hoverModeZone = node.getGraphics().getHoverBox().getHoverZoneMask().clone().toFront();
                //var hoverModeZone = node.getGraphics().getHoverBox().getHoverZoneMask().toFront();
                hoverModeZone.hover(
                    function() {
                        me._currentHoveredNode = nodeID;
                        node.getGraphics().getHoverBox().setHighlighted(true);
                    },
                    function() {
                        me._currentHoveredNode = null;
                        node.getGraphics().getHoverBox().setHighlighted(false);
                    });

                me.hoverModeZones.push(hoverModeZone);
            });

            //timer.printSinceLast("=== Enter hover mode - highlight: ");
        },

        /**
         * Exits hover-mode state, which is when a handle or a PlaceHolder is being dragged around the screen
         *
         * @method exitHoverMode
         */
        exitHoverMode: function() {
            this._currentHoveredNode = null;

            this.hoverModeZones.remove();

            var me = this;
            this._currentGrownNodes.each(function(nodeID) {
                var node = me.getNode(nodeID)
                node.getGraphics().shrink();
                node.getGraphics().getHoverBox().setHighlighted(false);
            });

            this._currentGrownNodes = [];
        },

        unmarkAll: function() {
            for (var i = 0; i < this._currentMarkedNew.length; i++) {
                var node = this.getNode(this._currentMarkedNew[i]);
                node.getGraphics().unmark();
            }
            this._currentMarkedNew = [];
        },

        markNode: function(nodeID) {
            this.getNode(nodeID).getGraphics().markPermanently();
            this._currentMarkedNew.push(nodeID);
        },

        getValidDragTargets: function(sourceNodeID, hoverType) {
            var result = [];
            switch (hoverType) {
            case "sibling":
                result = editor.getGraph().getPossibleSiblingsOf(sourceNodeID);
                break;
            case "child":
                // all person nodes which are not ancestors of sourse node and which do not already have parents
                result = editor.getGraph().getPossibleChildrenOf(sourceNodeID);
                break;
            case "parent":
                result = editor.getGraph().getPossibleParentsOf(sourceNodeID);
                break;
            case "partnerR":
            case "partnerL":
                // allow dragging to anyone who is not already a partner of target not and who is not a fetus
                // ...but set "preferred" status only for nodes of the opposite or unknown gender
                result = editor.getGraph().getPossiblePartnersOf(sourceNodeID)
                //console.log("possible partners: " + Helpers.stringifyObject(result));
                break;
            case "PlaceHolder":
                // all nodes which can be this placehodler: e.g. all that can be child of it's parents &&
                // partners of it's partners
                throw "TODO";
            default:
                throw "Incorrect hoverType";
            }
            return result;
        },

        applyChanges: function( changeSet, markNew ) {
            // applies change set of the form {"new": {list of nodes}, "moved": {list of nodes} }
            console.log("Change set: " + Helpers.stringifyObject(changeSet));
            if (Helpers.isObjectEmpty(changeSet)) return;

            var timer = new Helpers.Timer();
            var timer2 = new Helpers.Timer();

            try {

            this.unmarkAll();

            // to simplify code which deals woith removed nodes making other mnodes to move
            if (!changeSet.hasOwnProperty("moved"))
                changeSet["moved"] = [];
            if (!changeSet.hasOwnProperty("removed"))
                changeSet["removed"] = [];
            if (!changeSet.hasOwnProperty("changedIDSet"))
                changeSet["changedIDSet"] = {};

            // 0. remove all removed
            //
            // 1. move all person nodes
            // 2. create all new person nodes
            //
            // 3. move all existing relationships - as all lines are attached to relationships we want to draw
            //                                      them after all person nodes are already in correct position
            // 4. create new relationships

            if (changeSet.hasOwnProperty("removed")) {
                var affectedByLineRemoval = {};

                for (var i = 0; i < changeSet.removed.length; i++) {
                    var nextRemoved = changeSet.removed[i];

                    this.getNodeMap()[nextRemoved].remove();
                    this.removeFromNodeMap(nextRemoved);

                    var affected = this._lineSet.removeAllLinesAffectedByOwnerMovement(nextRemoved);

                    for (var j = 0; j < affected.length; j++)
                        if (!Helpers.arrayContains(changeSet.removed, affected[j])) { // ignore nodes which are removed anyway
                            //console.log("adding due to line removal: " + affected[j]);
                            affectedByLineRemoval[affected[j]] = true;
                        }
                }

                // change all IDs at once so that have both new and old references at the same time
                this.changeNodeIds(changeSet.changedIDSet);

                //console.log("Affected by line removal: " + Helpers.stringifyObject(affectedByLineRemoval));
                //console.log("LineSet: " + Helpers.stringifyObject(this._lineSet));

                for (var node in affectedByLineRemoval) {
                    if (affectedByLineRemoval.hasOwnProperty(node)) {
                        var newID = changeSet["changedIDSet"].hasOwnProperty(node) ? changeSet["changedIDSet"][node] : node;
                        if (!Helpers.arrayContains(changeSet.moved, newID)) {
                            //console.log("moved due to line removal: oldID="+node + ", newID=" + newID);
                            changeSet.moved.push(newID);
                        }
                    }
                }
            }

            timer.printSinceLast("=== Removal runtime: ");


            var movedPersons       = [];
            var movedRelationships = [];
            var newPersons         = [];
            var newRelationships   = [];
            var animate            = {};

            /*
            // TODO: animations disabled because hoverboxes & labels behave strangely
            if (changeSet.hasOwnProperty("animate")) {
                for (var i = 0; i < changeSet.animate.length; i++) {
                    animate[changeSet.animate[i]] = true;
                }
            }*/

            //console.log("moved: " + Helpers.stringifyObject(changeSet.moved));

            if (changeSet.hasOwnProperty("moved")) {
                // remove all lines so that we start drawing anew
                for (var i = 0; i < changeSet.moved.length; i++) {
                    var nextMoved = changeSet.moved[i];
                    if (editor.getGraph().isRelationship(nextMoved)) {
                        var affected = this._lineSet.removeAllLinesAffectedByOwnerMovement(nextMoved);
                        for (var j = 0; j < affected.length; j++) {
                            var node = affected[j];
                            if (!Helpers.arrayContains(changeSet.moved, node))
                                changeSet.moved.push(node);
                        }
                    }
                }

                // move actual nodes
                for (var i = 0; i < changeSet.moved.length; i++) {
                    var nextMoved = changeSet.moved[i];
                    if (editor.getGraph().isRelationship(nextMoved))
                        movedRelationships.push(nextMoved);
                    else
                        movedPersons.push(nextMoved);
                }
            }
            console.log("moved: " + Helpers.stringifyObject(changeSet.moved));
            if (changeSet.hasOwnProperty("new")) {
                for (var i = 0; i < changeSet["new"].length; i++) {
                    var nextNew = changeSet["new"][i];
                    if (editor.getGraph().isRelationship(nextNew))
                        newRelationships.push(nextNew);
                    else
                        newPersons.push(nextNew);
                }
            }

            timer.printSinceLast("=== Bookkeeping/sorting runtime: ");


            for (var i = 0; i < movedPersons.length; i++) {
                this.moveNode(movedPersons[i], animate.hasOwnProperty(movedPersons[i]));
            }

            timer.printSinceLast("=== Move persons runtime: ");

            for (var i = 0; i < newPersons.length; i++) {
                var newPerson = this.addNode(newPersons[i]);
                if (markNew) {
                    this.markNode(newPerson.getID());
                }
            }

            timer.printSinceLast("=== New persons runtime: ");

            for (var i = 0; i < movedRelationships.length; i++)
                this.moveNode(movedRelationships[i]);

            timer.printSinceLast("=== Move rels runtime: ");

            for (var i = 0; i < newRelationships.length; i++)
                this.addNode(newRelationships[i]);

            timer.printSinceLast("=== New rels runtime: ");

            if (changeSet.hasOwnProperty("highlight")) {
                for (var i = 0; i < changeSet.highlight.length; i++) {
                    var nextHighlight = changeSet.highlight[i];
                    this.getNode(nextHighlight).getGraphics().markPermanently();
                    this._currentMarkedNew.push(nextHighlight);
                }
            }

            //timer.printSinceLast("=== highlight: ");

            if (!editor.isReadOnlyMode()) {
                // re-evaluate which buttons & handles are appropriate for the nodes (e.g. twin button appears/disappears)
                for (var nodeID in this._nodeMap) {
                    if (this._nodeMap.hasOwnProperty(nodeID)) {
                        if (editor.getGraph().isPerson(nodeID) && !this.getNode(nodeID).getGraphics().getHoverBox().isMenuToggled()) {
                            this.getNode(nodeID).getGraphics().getHoverBox().removeButtons();
                            this.getNode(nodeID).getGraphics().getHoverBox().removeHandles();
                        }
                    }
                }

                var checkNumberingEvent = { "memo": { "check": true, "noUndoRedo": true } };
                editor.getController().handleRenumber(checkNumberingEvent);

                // TODO: move the viewport to make changeSet.makevisible nodes visible on screen
                timer.printSinceLast("=== highlight & update handles runtime: ");
            }

            timer2.printSinceLast("=== Total apply changes runtime: ");

            } catch(err) {
                console.log("[view] update error: " + err);
            }
        }
    });
    return View;
});
