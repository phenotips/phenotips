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
        disorderShapes: {},
        junctionRadius: 6
    },

    initialize: function(graphics) {
        // Workspace
        this.DEBUG_MODE = false;
        window.editor = this;
        this.initWorkspace();

        // Canvas controls
        this.viewBoxX = 0;
        this.viewBoxY = 0;
        this.zoomCoefficient = 1;
        this.background = editor.getPaper().rect(0,0, 200, 200).attr({fill: 'blue', stroke: 'none', opacity:0}).toBack();

        (this.adjustSizeToScreen = this.adjustSizeToScreen.bind(this))();
        this.generateViewControls();

        var me = this;
        var start = function() {
            me.background.ox = me.background.attr("x");
            me.background.oy = me.background.attr("y");
            me.background.attr({cursor: 'url(https://mail.google.com/mail/images/2/closedhand.cur)'});
        };
        var move = function(dx, dy) {
            var  deltax = editor.viewBoxX - dx/me.zoomCoefficient;
            var deltay = editor.viewBoxY - dy/me.zoomCoefficient;

            me.background.attr({x: me.background.ox - dx/me.zoomCoefficient, y: me.background.oy - dy/me.zoomCoefficient});
            editor.getPaper().setViewBox(deltax, deltay, editor.width/me.zoomCoefficient, editor.height/me.zoomCoefficient);
            me.background.ox = deltax;
            me.background.oy = deltay;
            me.background.attr({x: me.background.ox, y: me.background.oy });
        };
        var end = function() {
            editor.viewBoxX = me.background.ox;
            editor.viewBoxY = me.background.oy;
            me.background.attr({cursor: 'default'});
        };
        me.background.drag(move, start, end);

        // Capture resize events
        Event.observe (window, 'resize', this.adjustSizeToScreen);

        // Nodes
        this.nodes = [[/*Person*/],[/*PlaceHolder*/],[/*Partnership*/]];
        var nodeIndex = this.nodeIndex = new NodeIndex();
        this.idCount = 1;
        this._proband = this.addNode(this.width/2, this.height/2, 'M', false);

        document.observe('pedigree:child:added', function(event){
          if (event && event.memo && event.memo.node) {
            nodeIndex._childAdded(event.memo.node, event.memo.sourceNode);
          }
        });
        document.observe('pedigree:partner:added', function(event){
          if (event && event.memo && event.memo.node) {
            nodeIndex._partnerAdded(event.memo.node, event.memo.sourceNode);
          }
        });
        document.observe('pedigree:parents:added', function(event){
          if (event && event.memo && event.memo.node) {
            nodeIndex._parentsAdded(event.memo.node, event.memo.sourceNode);
          }
        });
        document.observe('pedigree:partnership:added', function(event){
          if (event && event.memo && event.memo.node) {
            nodeIndex._partnershipAdded(event.memo.node, event.memo.sourceNode);
          }
        });
        document.observe('pedigree:node:upgraded', function(event){
          if (event && event.memo && event.memo.node) {
            nodeIndex._nodeUpgraded(event.memo.node, event.memo.sourceNode);
          }
        });
    },

    zoom: function(zoomCoefficient) {
        zoomCoefficient += .6;
        var newWidth = this.width/zoomCoefficient;
        var newHeight = this.height/zoomCoefficient;
        this.viewBoxX = this.viewBoxX + (this.width/this.zoomCoefficient - newWidth)/2;
        this.viewBoxY = this.viewBoxY + (this.height/this.zoomCoefficient - newHeight)/2;
        this.getPaper().setViewBox(this.viewBoxX, this.viewBoxY, newWidth, newHeight);
        this.zoomCoefficient = zoomCoefficient;
        this.background.attr({x: this.viewBoxX, y: this.viewBoxY, width: newWidth, height: newHeight});
    },

    getPaper: function() {
        return this._paper;
    },
    getProband: function() {
        return this._proband;
    },

    adjustSizeToScreen : function() {
        var screenDimensions = document.viewport.getDimensions();
        this.width = screenDimensions.width;
        this.height = screenDimensions.height - this.canvas.cumulativeOffset().top - 4;
        if (this.getPaper()) {
            // TODO : pan to center?... set viewbox instead of size?
            this.getPaper().setSize(this.width, this.height);
            this.background && this.background.attr({"width": this.width, "height": this.height});
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
                if(direction == 'up') {
                    editor.panTo(editor.viewBoxX, editor.viewBoxY - 400);
                }
                else if(direction == 'down') {
                    editor.panTo(editor.viewBoxX, editor.viewBoxY + 400);
                }
                else if(direction == 'left') {
                    editor.panTo(editor.viewBoxX - 400, editor.viewBoxY);
                }
                else {
                    editor.panTo(editor.viewBoxX + 400, editor.viewBoxY);
                }
            })
        });
        // Zoom controls
        var trackLength = 200;
        this.__zoom = new Element('div', {'class' : 'view-controls-zoom', title : 'Zoom'});
        this.__controls.insert(this.__zoom);
        this.__zoom.track = new Element('div', {'class' : 'zoom-track'});
        this.__zoom.handle = new Element('div', {'class' : 'zoom-handle', title : 'Drag to zoom'});
        this.__zoom['in'] = new Element('div', {'class' : 'zoom-button zoom-in', title : 'Zoom in'});
        this.__zoom.out = new Element('div', {'class' : 'zoom-button zoom-out', title : 'Zoom out'});
        this.__zoom.label = new Element('div', {'class' : 'zoom-crt-value'});
        this.__zoom.insert(this.__zoom['in']);
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
                var zoomValue = (_this.__zoom.__crtValue  * 2);
                _this.zoom(zoomValue);
            },
            onChange : function (value) {
                // Called whenever the Slider has finished moving or has had its value changed via the setSlider Value function.
                // The called function gets the slider value (or array if slider has multiple handles) as its parameter.

                _this.__zoom.__crtValue = 1 - value;
                var zoomValue = (_this.__zoom.__crtValue  * 2);
                _this.zoom(zoomValue)
            }
        });
        this.zoomSlider.setValue(.70); // TODO : set initial value
        this.__zoom['in'].observe('click', function(event) {
            _this.zoomSlider.setValue(1 - (_this.__zoom.__crtValue + .2))
        });
        this.__zoom.out.observe('click', function(event) {
            _this.zoomSlider.setValue(1 - (_this.__zoom.__crtValue - .2))
        });
        // Insert all controls in the document
        this.workspace.insert(this.__controls);
    },

    // VIEWBOX RELATED FUNCTIONS
    canvasToDiv: function(canvasX,canvasY) {
        return {
            x: this.zoomCoefficient * (canvasX - editor.viewBoxX),
            y: this.zoomCoefficient * (canvasY - editor.viewBoxY)
        }
    },

    divToCanvas: function(divX,divY) {
        return {
            x: divX/this.zoomCoefficient + editor.viewBoxX,
            y: divY/this.zoomCoefficient + editor.viewBoxY
        }
    },
    
    viewportToDiv : function (absX, absY) {
      return {
        x : this.canvas.cumulativeOffset().left + absX,
        y : this.canvas.cumulativeOffset().top + absY
      };
    },

    panTo: function(x, y) {
        var oX = editor.viewBoxX,
            oY = editor.viewBoxY,
            xDisplacement = x - oX,
            yDisplacement = y - oY,
            numSeconds = .5,
            fps = 50,
            xStep = xDisplacement/(fps*numSeconds),
            yStep = yDisplacement/(fps*numSeconds);
        var progress = 0;
        function draw() {
            setTimeout(function() {
                if(progress++ < fps * numSeconds) {
                    editor.viewBoxX += xStep;
                    editor.viewBoxY += yStep;
                    editor.getPaper().setViewBox(editor.viewBoxX, editor.viewBoxY, editor.width/editor.zoomCoefficient, editor.height/editor.zoomCoefficient);
                    //this.background.attr({x: editor.viewBoxX, y: editor.viewBoxY});
                    draw();
                }
            }, 1000 / fps);

        }
        draw();
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
        document.observe('mousedown', function(event) {
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
                'name' : 'date_of_birth',
                'label' : 'Date of birth',
                'type' : 'date-picker',
                'format' : 'dd/MM/yyyy',
                'function' : 'setBirthDate'
            },
            {
                'name' : 'date_of_death',
                'label' : 'Date of death',
                'type' : 'date-picker',
                'format' : 'dd/MM/yyyy',
                'function' : 'setDeathDate'
            },
            {
                'name' : 'disorders',
                'label' : 'Known disorders of this individual',
                'type' : 'disease-picker',
                'function' : 'updateDisorders'
            },
            {
                'name' : 'gestation_age',
                'label' : 'Gestation age',
                'type' : 'select',
                'range' : {'start': 0, 'end': 50, 'item' : ['week', 'weeks']},
                'function' : 'setGestationAge'
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
                'name' : 'adopted',
                'label' : 'Adopted',
                'type' : 'checkbox',
                'function' : 'setAdopted'
            }
        ]);
    },

    initWorkspace : function() {
      this.canvas = new Element('div', {'id' : 'canvas'});
      this.workspace = new Element('div', {'id' : 'workspace'}).update(this.canvas);
      $('body').update(this.workspace);
      this._paper = Raphael("canvas", this.width, this.height);

      this.nodeMenu = this.generateNodeMenu();
      // TODO: create options bubble
      // this.nodeTypeOptions = new NodeTypeOptions();
      this._legend = new Legend();
      this.initMenu();
      // TODO: add header & title


      this.hoverModeZones = this._paper.set();
      this.currentHoveredNode = null;
      this.currentDraggable = {
          handle: null,
          placeholder: null
      };
      Droppables.add(this.canvas, {accept: 'disorder', onDrop: this._onDropDisorder.bind(this)});
    },

    initMenu : function() {
      var menu = new Element('div', {'id' : 'editor-menu'});
      editor.workspace.insert({before : menu});
      var submenus = [{
        name : 'internal',
        items: [
          { key : 'new',    label : 'New node'},
          { key : 'undo',   label : 'Undo'},
          { key : 'redo',   label : 'Redo'},
          { key : 'layout', label : 'Adjust layout'},
          { key : 'clear',  label : 'Clear all'}
        ]
      }, {
        name : 'external',
        items: [
          { key : 'print',  label : 'Printable version'},
          { key : 'save',   label : 'Save'},
          { key : 'close',  label : 'Close'}
        ]
      }];
      var _createSubmenu = function(data) {
        var submenu = new Element('div', {'class' : data.name + '-actions action-group'});
        menu.insert(submenu);
        data.items.each(function (item) {
          submenu.insert(_createMenuItem(item));
        });
      };
      var _createMenuItem = function(data) {
        var mi = new Element('span', {'id' : 'action-' + data.key, 'class' : 'menu-item ' + data.key}).update(data.label);
        if (data.callback && typeof(editor[data.callback]) == 'function') {
          mi.observe('click', function() {
            editor[data.callback]();
          });
        }
        return mi;
      };
      submenus.each(_createSubmenu);
    },

    addPartnership : function(x, y, node1, node2) {
        var partnership = new Partnership(x, y, node1, node2);
        this.nodeIndex._addNode(partnership);
        this.nodes[2].push(partnership);
        return partnership;
    },

    removePartnership: function(partnership) {
        this.nodes[2] = this.nodes[2].without(partnership);
    },

    addNode: function(x, y, gender, isPlaceHolder, id) {
        !isPlaceHolder && (isPlaceHolder = false);
        var isProband = (!isPlaceHolder && this.nodes[0].length == 0);
        var node = (isPlaceHolder) ? (new PlaceHolder(x, y, gender, id)) : (new Person(x, y, gender, id, isProband));
        this.nodes[+(isPlaceHolder)].push(node);
        this.nodeIndex._addNode(node, isProband);
        return node;
    },

    removeNode: function(node) {
        //TODO: optimize (check whether node is a placeholder or a person)
        this.nodes[0] = this.nodes[0].without(node);
        this.nodes[1] = this.nodes[1].without(node);
    },

    _onDropDisorder: function(disorder, target, event) {
        var divPos = editor.viewportToDiv(event.pointerX(), event.pointerY());
        var pos = editor.divToCanvas(divPos.x,divPos.y);
        var node = this.nodeIndex.getNodeNear(pos.x, pos.y);
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
        if(this.currentDraggable.handle == "parent") {
            this._junctions = this.nodes[2].without(sourceNode);
            hoverNodes = hoverNodes.concat(this._junctions);
            this._junctions.each(function(bubble) {
                bubble.getGraphics().grow();
            })
        }
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
                        if(s.getType() == 'pn') {
                            s.validParentSelected = true;
                        }
                        else {
                            s.validParentsSelected = true;
                        }
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
        this._junctions && this._junctions.each(function(bubble) {
            bubble.getGraphics().area && bubble.getGraphics().area.remove();
        });
        this._junctions = null;
    }
});

var editor;

document.observe("dom:loaded",function() {
    editor = new PedigreeEditor();
});
