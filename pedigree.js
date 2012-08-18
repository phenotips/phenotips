
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
        this.nodes = [[],[]];
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

    addNode: function(x, y, gender, isPlaceHolder) {
        !isPlaceHolder && (isPlaceHolder = false);
        var isProband = (!isPlaceHolder && this.nodes[0].length == 0);
        var node = (isPlaceHolder) ? (new PlaceHolder(x, y, gender)) : (new Person(x, y, gender, isProband));
        this.nodes[+(isPlaceHolder)].push(node);
	this.nodeIndex.add(node);
        return node;
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

    addPlaceHolder: function(x,y, gender) {
        var ph = new PlaceHolder(x,y, gender);
        this.nodes[1].push(ph);
        return ph;
    },

    removeNode: function(node) {
        //TODO: optimize (check whether node is a placeholder or a person)
        this.nodes[0] = this.nodes[0].without(node);
        this.nodes[1] = this.nodes[1].without(node);
    },


    addPartnership : function(node1, node2) {
      var joinPosition = editor.findPosition({join : [node1.getID(), node2.getID()]});
      var partnership = new Partnership(joinPosition.x, joinPosition.y, node1, node2);
      this.nodeIndex.add(partnership);
      return partnership;
    },

    addParentsPartnership : function(node, leftParent, rightParent) {
        //TODO:
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
    var p = editor.getProband();
    p.setPos(500,500, true, function() {alert('sweet peanuts')});
//    node.getGraphics().getShapes().flatten().insertBefore(editor.getProband().getGraphics().getAllGraphics().flatten());
//    var el1 = editor.getPaper().circle(20,20,20).attr("fill", "pink");
//    var el3 = editor.getPaper().circle(30,20,20).attr("fill", "blue");
//
//    var el2 = editor.getPaper().circle(20,20,20).attr("fill", "green");
//    var el4 = editor.getPaper().circle(40,20,20).attr("fill", "yellow");
//    var el1 = editor.getPaper().circle(10,20,20).attr("fill", "pink");
    //var set2 = editor.getPaper().set(el4).insertAfter(set1);
//    set1.push(set2);


//    var iconPath = Raphael.pathToRelative("M16,1.466C7.973,1.466,1.466,7.973,1.466,16c0,8.027,6.507,14.534,14.534,14.534c8.027,0,14.534-6.507,14.534-14.534C30.534,7.973,24.027,1.466,16,1.466zM16,28.792c-1.549,0-2.806-1.256-2.806-2.806s1.256-2.806,2.806-2.806c1.55,0,2.806,1.256,2.806,2.806S17.55,28.792,16,28.792zM16,21.087l-7.858-6.562h3.469V5.747h8.779v8.778h3.468L16,21.087z");
//    iconPath[0][1] = 0;
//    iconPath[0][2] = 0;
//    var duude = editor.getPaper().path(iconPath);

//    var a = editor.getPaper().circle(0,20,20);
//    a.transform("t150");
//    a.translate(10);
////
////    var b = editor.getPaper().rect(100,100,100);
////    var g = editor.getPaper().set(a,b);
////    a.transform('t400');
////    b.transform('t0,33');
////    var k = g.transform();
////    var b = editor.getPaper().rect(120,20,20,20);
////    var arr = [a,b];
////    var se2 = editor.getPaper().set(editor.getPaper().circle(20,120,40));
////    var se = editor.getPaper().set(arr);
////    se.push(se2);
////
////    se.hide();
////    se.show();
    //alert(Raphael.color('blue'));

//    var patientNode = editor.getProband();
//    var patientNodesFriend = editor.addNode(patientNode.getX() + 200, patientNode.getY(), 'F', false);
//    var nodesSon = editor.addNode(patientNode.getX() + 100, patientNode.getY() + 200, 'F', false);
//    patientNode.setBirthDate(new Date(1999,9,2), true);
//    var partnership = patientNode.addPartner(patientNodesFriend);
//    partnership.addChild(nodesSon);
//
//    patientNode.addDisorder({id: "190685",value: "Down syndrome"}, true);

    //var randomNode = editor.addNode(300, 500, 'M', false);
    //patientNode.setPos(patientNode.getX(),patientNode.getY()-200,true);
    //patientNode.setPos(20,20,true);
   // patientNode.remove(true,true);
   // nodesSon.removeParents();
//    patientNode.setGender("F", true);
//patientNode.setAlive(true);
//    patientNode.setAborted(true);
//  //patientNode.setAlive(true);
//    //patientNode.setSB(true);
//   // patientNode.setAlive(true);
//    patientNode.setFetus(true, true);
//    patientNode.setFirstName("peter", true);
//  patientNode.setLastName("panovitch", true);
//    patientNode.setAdopted(true,true);

   // patientNode.setSB(true);
////    patientNode.setConceptionDate(new Date(2002,8,2));
//    patientNode.setDeathDate(new Date(2002,9,2));
////
//

//
//

    //patientNode.updateDisorders([{id: "DS1",value: "1 Syndrome"},{id: "DS2",value: "2 Syndrome"}], true);

//
//
//    patientNode.addDisorder({id: "DS2",value: "1 Syndrome"}, true);
//    patientNode.addDisorder({id: "DS3",value: "1 Syndrome"}, true);
//    patientNode.addDisorder({id: "DS4",value: "1 Syndrome"}, true);
//
//    patientNode.setAborted(true);
//    patientNode.setAlive(true);
//    patientNode.setDeceased(true);
//
//






//    //patientNode.getGraphics().move(20, 20);
//
//
//    patientNode.setDeathDate(new Date(2002, 9, 2));
//
//    patientNode.setGender("M");
    //alert(Object.keys(editor.getLegend().getDisorders());
//        patientNode.addDisorder("Left Disorder");
//        patientNode.setAdopted(false);
    //patientNode.setGender('F');
    //   patientNode.removeDisorder("DS1");
//        //patientNode.addDisorder("Down Syndrome");
//        patientNode.setGender('F');
    //var nodeElement = patientNode._graphics.draw(patientNode);
    //alert(nodeElement.transform());



//    var ph = new PlaceHolder(editor.width/2, editor.height/2, editor.graphics, 'F');
//    var dad = editor.addNode(editor.width/2, editor.height/2, 'U');
//    var mom = editor.addNode(editor.width/2, editor.height/2, 'U');
//    var son = editor.addNode(editor.width/2, editor.height/2, 'F');
//    ph._father = dad;
//    son._mother = mom;

    //var pn = new Person(0,0,'F');



});
