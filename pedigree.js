
var PedigreeEditor = Class.create({

    attributes: {
        radius: 40,
        unbornShape: {'font-size': 50, 'font-family': 'Cambria'},
        nodeShape: {fill: "0-#ffffff:0-#B8B8B8:100", stroke: "#595959"},
        boxOnHover : {fill: "gray", stroke: "none",opacity: 1, "fill-opacity":.25},
        menuBtnIcon : {fill: "#1F1F1F", stroke: "none"},
        deleteBtnIcon : {fill: "#990000", stroke: "none"},
        btnMaskHoverOn : {opacity:.6, stroke: 'none'},
        btnMaskHoverOff : {opacity:0},
        btnMaskClick: {opacity:1},
        orbHue : .53,
        phShape: {fill: "white","fill-opacity": 0, "stroke": 'black', "stroke-dasharray": "- "},
        dragMeLabel: {'font-size': 14, 'font-family': 'Tahoma'},
        label: {'font-size': 18, 'font-family': 'Cambria'},
        disorderShapes: {}
    },

    initialize: function(graphics) {
        window.editor = this;
        this._paper = Raphael("canvas", this.width, this.height);
        this.viewBoxX = 0;
        this.viewBoxY = 0;
        var nodeIndex = this.nodeIndex = new NodeIndex();
        this.generateViewControls();
        (this.adjustSizeToScreen = this.adjustSizeToScreen.bind(this))();

        this.adjustSizeToScreen();
        this.nodes = [[/*Person*/],[/*PlaceHolder*/],[/*Partnership*/]];
        this.idCount = 1;
        this.hoverModeZones = this._paper.set();

        this.initMenu();
        this.nodeMenu = this.generateNodeMenu();
        this._legend = new Legend();

        // Capture resize events
        Event.observe (window, 'resize', this.adjustSizeToScreen);

        this.currentHoveredNode = null;
        this.currentDraggable = {
            handle: null,
            placeholder: null
        };
        Droppables.add($('canvas'), {accept: 'disorder', onDrop: this._onDropDisorder.bind(this)});
        this._proband = this.addNode(this.width/2, this.height/2, 'M', false);
	
	
	document.observe('pedigree:child:added', function(event){
	  if (event && event.memo && event.memo.node) {
	    nodeIndex._childAdded(event.memo.node);
	  }
	});
    },

    getPaper: function() {
        return this._paper;
    },
    getProband: function() {
        return this._proband;
    },

    adjustSizeToScreen : function() {
        var canvas = $('canvas');
        var screenDimensions = document.viewport.getDimensions();
        this.width = screenDimensions.width;
        this.height = screenDimensions.height - canvas.cumulativeOffset().top - 4;
        if (this.getPaper()) {
            // TODO : pan to center?... set viewbox instead of size?
            this.getPaper().setSize(this.width, this.height);
        }
        if (this.nodeMenu) {
            this.nodeMenu.reposition();
        }
    },

    generateViewControls : function() {
        var _this = this;
        this.__controls = new Element('div', {'class' : 'view-controls'});
        // Pan controls
        this.__pan = new Element('div', {'class' : 'view-controls-pan', title : 'Pan'});
        this.__controls.insert(this.__pan);
        ['up', 'right', 'down', 'left'].each(function (direction) {
            _this.__pan[direction] = new Element('span', {'class' : 'view-control-pan pan-' + direction, 'title' : 'Pan ' + direction});
            _this.__pan.insert(_this.__pan[direction]);
            _this.__pan[direction].observe('click', function(event) {
                var duration = 300;
                if(direction == 'up') {
                    editor.panTo(editor.viewBoxX, editor.viewBoxY - 200, duration);
                }
                else if(direction == 'down') {
                    editor.panTo(editor.viewBoxX, editor.viewBoxY + 200, duration);
                }
                else if(direction == 'left') {
                    editor.panTo(editor.viewBoxX - 200, editor.viewBoxY, duration);
                }
                else {
                    editor.panTo(editor.viewBoxX + 200, editor.viewBoxY, duration);
                }
            })
        });
        // Zoom controls
        var trackLength = 200;
        this.__zoom = new Element('div', {'class' : 'view-controls-zoom', title : 'Zoom'});
        this.__controls.insert(this.__zoom);
        this.__zoom.track = new Element('div', {'class' : 'zoom-track'});
        this.__zoom.handle = new Element('div', {'class' : 'zoom-handle', title : 'Drag to zoom'});
        this.__zoom.in = new Element('div', {'class' : 'zoom-button zoom-in', title : 'Zoom in'});
        this.__zoom.out = new Element('div', {'class' : 'zoom-button zoom-out', title : 'Zoom out'});
        this.__zoom.label = new Element('div', {'class' : 'zoom-crt-value'});
        this.__zoom.insert(this.__zoom.in);
        this.__zoom.insert(this.__zoom.track);
        this.__zoom.track.insert(this.__zoom.handle);
        this.__zoom.track.style.height = trackLength + 'px';
        this.__zoom.insert(this.__zoom.out);
        this.__zoom.insert(this.__zoom.label);
        // Scriptaculous slider
        // see also http://madrobby.github.com/scriptaculous/slider/
        this.__zoom.__crtValue = 0;
        this.zoomSlider = new Control.Slider(this.__zoom.handle, this.__zoom.track, {
            axis:'vertical',
            minimum: 60,
            maximum: trackLength + 60,
            increment : trackLength / 100,
            alignY: 6,
            onSlide : function (value) {
                // Called whenever the Slider is moved by dragging.
                // The called function gets the slider value (or array if slider has multiple handles) as its parameter.
                _this.__zoom.__crtValue = 1 - value;
                _this.__zoom.label.update(new Number(_this.__zoom.__crtValue  * 100).toPrecision(3) + "%");
                // TODO: Zoom
            },
            onChange : function (value) {
                // Called whenever the Slider has finished moving or has had its value changed via the setSlider Value function.
                // The called function gets the slider value (or array if slider has multiple handles) as its parameter.
                _this.__zoom.__crtValue = 1 - value;
                _this.__zoom.label.update(new Number(_this.__zoom.__crtValue  * 100).toPrecision(3) + "%");
                // TODO: Zoom
            }
        });
        this.zoomSlider.setValue(.5); // TODO : set initial value
        this.__zoom.in.observe('click', function(event) {
            _this.zoomSlider.setValue(1 - (_this.__zoom.__crtValue + .01))
        });
        this.__zoom.out.observe('click', function(event) {
            _this.zoomSlider.setValue(1 - (_this.__zoom.__crtValue - .01))
        });
        // Insert all controls in the document
        $('canvas').insert({'after' : this.__controls});
    },

    // VIEWBOX RELATED FUNCTIONS
    getPositionInViewBox: function(x,y) {
        return {
            x: x - editor.viewBoxX,
            y: y - editor.viewBoxY
        }
    },

    panTo: function(x, y, duration, callback, callbackObject, args) {
        var oX = editor.viewBoxX,
            oY = editor.viewBoxY,
            xDisplacement = x - oX,
            yDisplacement = y - oY,
            start = Date.now();

        function step(timestamp) {
            var timePassed = (timestamp - start);
            var progress = (timePassed)/duration;
            editor.viewBoxX = oX + xDisplacement * (progress);
            editor.viewBoxY = oY + yDisplacement * (progress);
            console.log("x: " + editor.viewBoxX + " y: " + editor.viewBoxY);
            editor.getPaper().setViewBox(editor.viewBoxX, editor.viewBoxY, editor.width, editor.height);
            if (timePassed < duration) {
                window.requestAnimFrame(step);
            }
            else {
                callback && callback.apply(callbackObject, args);
            }
        }
        window.requestAnimFrame(step);
    },

    // EDITOR TOOLS
    getLegend: function() {
        return this._legend;
    },

    generateID: function() {
        return this.idCount++;
    },
    generateNodeMenu: function() {
        var _this = this;
        document.observe('click', function(event) {
                    if (_this.nodeMenu && _this.nodeMenu.isActive()) {
                        if (event.element().getAttribute('class') != 'menu-trigger' &&
                            (!event.element().up || !event.element().up('.menu-box, .calendar_date_select') && event.element().up('body'))) {
                            _this.nodeMenu.hide();
                        }
                    }
        });
        document.observe('nodemenu:hiding', function(event) {
            if (event.memo && event.memo.node) {
                var nodeBox = event.memo.node.getGraphics().getHoverBox();
                nodeBox._isMenuToggled = false;
                !nodeBox.isHovered() && nodeBox.animateHideHoverZone();
                nodeBox.enable();
            }
        });
        return new NodeMenu([
            {
                'name' : 'identifier',
                'label' : '',
                'type'  : 'hidden'
            },
            {
                'name' : 'first_name',
                'label': 'First name',
                'type' : 'text',
                'function' : 'setFirstName'
            },
            {
                'name' : 'last_name',
                'label': 'Last name',
                'type' : 'text',
                'function' : 'setLastName'
            },
            {
                'name' : 'gender',
                'label' : 'Gender',
                'type' : 'radio',
                'values' : [
                    { 'actual' : 'M', 'displayed' : 'Male' },
                    { 'actual' : 'F', 'displayed' : 'Female' },
                    { 'actual' : 'U', 'displayed' : 'Unknown' }
                ],
                'default' : 'U',
                'function' : 'setGender'
            },
            {
                'name' : 'date_of_birth',
                'label' : 'Date of birth',
                'type' : 'date-picker',
                'format' : 'dd/MM/yyyy',
                'function' : 'setBirthDate'
            },

            {
                'name' : 'disorders',
                'label' : 'Known disorders of this individual',
                'type' : 'disease-picker',
                'function' : 'updateDisorders'
            },
            {
                'name' : 'adopted',
                'label' : 'Adopted',
                'type' : 'checkbox',
                'function' : 'setAdopted'
            },
            {
                'name' : 'state',
                'label' : 'Individual is',
                'type' : 'radio',
                'values' : [
                    { 'actual' : 'alive', 'displayed' : 'Alive' },
                    { 'actual' : 'deceased', 'displayed' : 'Deceased' },
                    { 'actual' : 'stillborn', 'displayed' : 'Stillborn' },
                    { 'actual' : 'aborted', 'displayed' : 'Aborted' },
                    { 'actual' : 'unborn', 'displayed' : 'Unborn' }
                ],
                'default' : 'alive',
                'function' : 'setLifeStatus'
            },
            {
                'name' : 'gestation_age',
                'label' : 'Gestation age',
                'type' : 'select',
                'range' : {'start': 0, 'end': 50, 'item' : ['week', 'weeks']},
                'function' : 'setGestationAge'
            },
            {
                'name' : 'date_of_death',
                'label' : 'Date of death',
                'type' : 'date-picker',
                'format' : 'dd/MM/yyyy',
                'function' : 'setDeathDate'
            }
        ],$('canvas'));
    },

    initMenu : function() {
        var el = $("action-new");
        el.observe("click", function() {alert("new node has been created! WHOAH!")});
    },

    addPartnership : function(x, y, node1, node2) {
        var partnership = new Partnership(x, y, node1, node2);
        this.nodeIndex.add(partnership);
        this.nodes[2].push(partnership);
        return partnership;
    },

    removePartnership: function(partnership) {
        this.nodes[2] = this.nodes[2].without(partnership);
    },

    addNode: function(x, y, gender, isPlaceHolder, id) {
        !isPlaceHolder && (isPlaceHolder = false);
        var isProband = (!isPlaceHolder && this.nodes[0].length == 0);
        var node = (isPlaceHolder) ? (new PlaceHolder(x, y, gender)) : (new Person(x, y, gender, id, isProband));
        this.nodes[+(isPlaceHolder)].push(node);
	this.nodeIndex.add(node);
        return node;
    },

    removeNode: function(node) {
        //TODO: optimize (check whether node is a placeholder or a person)
        this.nodes[0] = this.nodes[0].without(node);
        this.nodes[1] = this.nodes[1].without(node);
    },

    _onDropDisorder: function(disorder, target, event) {
      var position = {};
      var x = event.pointerX() - target.cumulativeOffset().left;
      var y = event.pointerY() - target.cumulativeOffset().top;
      // TODO transform coordinates to real coordinates on the raphael paper once zoom & pan are implemented
      var node = this.nodeIndex.getNodeNear(x, y);
      if (node && node.getType() == 'pn') {
	var disorderObj = {};
        disorderObj.id = disorder.id.substring( disorder.id.indexOf('-') + 1);
        disorderObj.value = disorder.down('.disorder-name').firstChild.nodeValue;
        node.addDisorder(disorderObj, true);
      }
    },
        
    // WRAPPERS FOR THE NODE INDEX & GRID FUNCITONS
    getGridUnitX : function () {
      return this.nodeIndex.gridUnit.x;
    },
    
    getGridUnitY : function () {
      return this.nodeIndex.gridUnit.y;
    },
    findPosition : function (relativeNodePosition, ids) {
      return this.nodeIndex.findPosition(relativeNodePosition, ids);
    },
    
    // HOVER MODE

    enterHoverMode: function(sourceNode) {
        var hoverNodes = this.nodes[0].without(sourceNode);
        var me = this,
            color;
        hoverNodes.each(function(s) {
            var hoverModeZone = s.getGraphics().getHoverBox().getHoverZoneMask().clone().toFront();
            hoverModeZone.attr("cursor", "pointer");
            hoverModeZone.hover(
                function() {
                    me.currentHoveredNode = s;
                    s.getGraphics().getHoverBox().setHovered(true);
                    s.getGraphics().getHoverBox().getBoxOnHover().attr(editor.attributes.boxOnHover);

                    if(me.currentDraggable.placeholder && me.currentDraggable.placeholder.canMergeWith(s)) {
                        me.validPlaceholderNode = true;
                        color = "green";
                    }
                    else if(me.currentDraggable.handle == "partner" && sourceNode.canPartnerWith(s)) {
                        s.validPartnerSelected = true;
                        color = "green";
                    }
                    else if(me.currentDraggable.handle == "child" && sourceNode.canBeParentOf(s)) {
                        s.validChildSelected = true;
                        color = "green";
                    }
                    else if(me.currentDraggable.handle == "parent" && s.canBeParentOf(sourceNode)) {
                        s.validParentSelected = true;
                        color = "green";
                    }
                    else {
                        color = "red";
                    }
                    s.getGraphics().getHoverBox().getBoxOnHover().attr("fill", color);
                },
                function() {
                    s.getGraphics().getHoverBox().setHovered(false);
                    s.getGraphics().getHoverBox().getBoxOnHover().attr(me.attributes.boxOnHover).attr('opacity', 0);
                    me.currentHoveredNode = null;
                    me.validPlaceholderNode = false;
                });
            me.hoverModeZones.push(hoverModeZone);
        });
    },

    exitHoverMode: function() {
        this.hoverModeZones.remove();
    }
});

var editor;

document.observe("dom:loaded",function() {
    editor = new PedigreeEditor();
});
