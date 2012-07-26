var PedigreeEditor = Class.create({
    initialize: function(graphics) {
        this.generateViewControls();
        (this.adjustSizeToScreen = this.adjustSizeToScreen.bind(this))();

        //create canvas
        this.paper = Raphael("canvas", this.width, this.height);
        this.adjustSizeToScreen();
        this.nodes = [[],[]];
        this.idCount = 1;
        this.hoverModeZones = this.paper.set();
        this.graphics = new NodeVisuals(this.paper);
        this.globalSet = [];

        this.initMenu();
        this.nodeMenu = this.generateNodeMenu();
        this._legend = new Legend();

        //TODO: for each connection: draw, for each node: draw

        // Capture resize events
        Event.observe (window, 'resize', this.adjustSizeToScreen);

        //Generate a map of Disorders and patterns
        disorderMap = {};
        this._disorderMap = disorderMap;
        this.currentHoveredNode = null;
        this.currentDraggable = {
            handle: null,
            placeholder: null
        };
    },

    adjustSizeToScreen : function() {
        var canvas = $('canvas');
        var screenDimensions = document.viewport.getDimensions();
        this.width = screenDimensions.width;
        this.height = screenDimensions.height - canvas.cumulativeOffset().top - 4;
        if (this.paper) {
            // TODO : pan to center?... set viewbox instead of size?
            this.paper.setSize(this.width, this.height);
        }
        if (!this.__sizeLabel) {
            this.__sizeLabel = new Element('span', {'class' : 'size-label'});
            canvas.insert({'after' : this.__sizeLabel});
        }
        this.__sizeLabel.update(this.width + " × " + this.height);
        if (this.__controls) {
            this.__controls.style.top = canvas.cumulativeOffset().top + 10 + "px";
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
                // TODO : Pan
                alert('Panning ' + direction + '!');
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
                nodeBox.animateHideHoverZone();
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
                'format' : 'dd/MM/yyyy'
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
                'name' : 'fetus',
                'label' : 'Fetus',
                'type' : 'checkbox',
                'function' : 'setFetus'
            },
            {
                'name' : 'state',
                'label' : 'Individual is',
                'type' : 'radio',
                'values' : [
                    { 'actual' : 'alive', 'displayed' : 'Alive' },
                    { 'actual' : 'deceased', 'displayed' : 'Deceased' },
                    { 'actual' : 'stillborn', 'displayed' : 'Stillborn' },
                    { 'actual' : 'aborted', 'displayed' : 'Aborted' }
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
                'format' : 'dd/MM/yyyy'
            }
        ],$('canvas'));
    },

    initMenu : function() {
        var el = $("action-new");
        el.observe("click", function() {alert("new node has been created! WHOAH!")});
    },

    addNode: function(x, y, gender) {
        var node = new Person(x, y, gender);
        this.nodes[0].push(node);
        return node;
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
        this._graphics.remove(node);
    },


    addPartnerConnection : function(node1, node2) {
        if(node1._partnerConnections.indexOf(node2) == -1) {
            var connection = new Connection("partner", node1, node2);
            node1._partnerConnections.push(node2);
            node2._partnerConnections.push(node1);
        }
    },

    addParentsConnection : function(node, leftParent, rightParent) {
        leftParent._partnerConnections.push(rightParent);
        rightParent._partnerConnections.push(leftParent);
        leftParent._children.push(node);
        node._parents.push(leftParent,rightParent);
        var connection = new Connection("partner", leftParent, rightParent);
    },

    enterHoverMode: function(sourceNode) {
        var hoverNodes = this.nodes[0].without(sourceNode);
        var me = this;
        hoverNodes.each(function(s) {
            var hoverModeZone = s.getHoverBox().getHoverZoneMask().clone().toFront();
            hoverModeZone.hover(
                function() {
                    me.currentHoveredNode = s;
                    s.getHoverBox().setHovered(true);
                    if(me.currentDraggable.placeholder != null) {
                        if(me.currentDraggable.placeholder.canMergeWith(s)) {
                            s.getHoverBox().getBoxOnHover().attr(me.graphics._attributes.boxOnHover);
                            s.getHoverBox().getBoxOnHover().attr({"fill": "green", opacity: 1, "fill-opacity": 1});
                            me.validPlaceholderNode = true;
                        }
                        else {
                            s.getHoverBox().getBoxOnHover().attr(me.graphics._attributes.boxOnHover);
                            s.getHoverBox().getBoxOnHover().attr("fill", "red");
                        }
                    }
                    else if(me.currentDraggable.handle == "partner" && ((sourceNode.getGender() && sourceNode.getGender() == "male" && s.getGender() == "female") ||
                        (sourceNode.getGender() == "female" && s.getGender() == "male"))) {
                        s.validPartnerSelected = true;
                        s.getHoverBox().getBoxOnHover().attr(me.graphics._attributes.boxOnHover);
                    }
                    else {
                        s.getHoverBox().getBoxOnHover().attr(me.graphics._attributes.boxOnHover);
                        s.getHoverBox().getBoxOnHover().attr("fill", "red");
                    }
                },
                function() {
                    s.getHoverBox().setHovered(false);
                    s.getHoverBox().getBoxOnHover().attr(me.graphics._attributes.boxOnHover).attr('opacity', 0);
                    me.currentHoveredNode.validPartnerSelected = false;
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

var editor,
    disorderMap;

document.observe("dom:loaded",function() {

    editor = new PedigreeEditor();
//    var a = editor.paper.circle(0,20,20);
//    a.transform("t150");
//    a.translate(10);
////
////    var b = editor.paper.rect(100,100,100);
////    var g = editor.paper.set(a,b);
////    a.transform('t400');
////    b.transform('t0,33');
////    var k = g.transform();
////    var me = "hahaha";
////    var b = editor.paper.rect(120,20,20,20);
////    var arr = [a,b];
////    var se2 = editor.paper.set(editor.paper.circle(20,120,40));
////    var se = editor.paper.set(arr);
////    se.push(se2);
////
////    se.hide();
////    se.show();
    //alert(Raphael.color('blue'));

    var patientNode = editor.addNode(editor.width/2, editor.height/2, 'M');
    var patientNodesFriend = editor.addNode(editor.width/3, editor.height/2, 'F');
  patientNode.setBirthDate(new Date(1999,9,2), true);
//   patientNode.setDeceased(true);
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



    patientNode.addDisorder({id: "DS1",value: "1 Syndrome"}, true);
   patientNode.getGraphics().move(20, 20);
//
//    patientNode.addDisorder("DS2","2 Syndrome", true);
//    patientNode.addDisorder("DS3","3 Syndrome", true);
//    patientNode.addDisorder("DS4","4 Syndrome", true);
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

    var ani = function() {
        patientNode._hoverBox.disable();
        patientNode._xPos += 100;
        patientNode._yPos += 100;
        nodeElement.stop().animate({'transform': "t " + 100 + "," + 100+"..."}, 2000, "linear", patientNode._hoverBox.enable.bind(patientNode._hoverBox));
    };
    // ani();

//    var ph = new PlaceHolder(editor.width/2, editor.height/2, editor.graphics, 'F');
//    var dad = editor.addNode(editor.width/2, editor.height/2, 'U');
//    var mom = editor.addNode(editor.width/2, editor.height/2, 'U');
//    var son = editor.addNode(editor.width/2, editor.height/2, 'F');
//    ph._father = dad;
//    son._mother = mom;

    //var pn = new Person(0,0,'F');




});
