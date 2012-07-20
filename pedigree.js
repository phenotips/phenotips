var PedigreeEditor = Class.create({
    initialize: function(graphics) {

        //create canvas
        var screenDimensions = document.viewport.getDimensions();
        var titleHeight = $('document-title');

        this.width = screenDimensions.width;
        this.height = screenDimensions.height;
        this.paper = Raphael("canvas", this.width, this.height);
        this.nodes = [[],[]];
        this.idCount = 1;
        this.hoverModeZones = this.paper.set();
        this.graphics = new NodeVisuals(this.paper);
        this.globalSet = [];

        this.initMenu();
        this.nodeMenu = this.generateNodeMenu();
        this._legend = new Legend();



        //TODO: for each connection: draw, for each node: draw

        //TODO: capture resize events

        //Generate a map of Disorders and patterns
        disorderMap = {};
        this._disorderMap = disorderMap;
        this.currentHoveredNode = null;
        this.currentDraggable = {
            handle: null,
            placeholder: null
        };
    },

    getLegend: function() {
        return this._legend;
    },

    generateID: function() {
        return this.idCount++;
    },
    generateNodeMenu: function() {
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
                'type' : 'disease-picker'
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

    addNode: function(x, y, gender)
    {
        var node = new Person(x, y, gender);
        this.nodes[0].push(node);
        return node;
    },

    addPlaceHolder: function(x,y, gender)
    {
        var ph = new PlaceHolder(x,y, gender);
        this.nodes[1].push(ph);
        return ph;
    },

    removeNode: function(node)
    {
        //TODO: optimize (check whether node is a placeholder or a person)
        this.nodes[0] = this.nodes[0].without(node);
        this.nodes[1] = this.nodes[1].without(node);
        this._graphics.remove(node);
    },


    addPartnerConnection : function(node1, node2)
    {
        if(node1._partnerConnections.indexOf(node2) == -1)
        {
            var connection = new Connection("partner", node1, node2);
            node1._partnerConnections.push(node2);
            node2._partnerConnections.push(node1);
        }
    },

    addParentsConnection : function(node, leftParent, rightParent)
    {
        leftParent._partnerConnections.push(rightParent);
        rightParent._partnerConnections.push(leftParent);
        leftParent._children.push(node);
        node._parents.push(leftParent,rightParent);
        var connection = new Connection("partner", leftParent, rightParent);
    },

    enterHoverMode: function(sourceNode){
        var hoverNodes = this.nodes[0].without(sourceNode);
        var me = this;
        hoverNodes.each(function(s) {
            var hoverModeZone = s.getHoverBox().getHoverZoneMask().clone().toFront();
            hoverModeZone.hover(
                function() {
                    me.currentHoveredNode = s;
                    s.getHoverBox().setHovered(true);
                    if(me.currentDraggable.placeholder != null)
                    {
                        if(me.currentDraggable.placeholder.canMergeWith(s)) {
                            s.getHoverBox().getBoxOnHover().attr(me.graphics._attributes.boxOnHover);
                            s.getHoverBox().getBoxOnHover().attr({"fill": "green", opacity: 1, "fill-opacity": 1});
                            me.validPlaceholderNode = true;
                        }
                        else
                        {
                            s.getHoverBox().getBoxOnHover().attr(me.graphics._attributes.boxOnHover);
                            s.getHoverBox().getBoxOnHover().attr("fill", "red");
                        }
                    }
                    else if(me.currentDraggable.handle == "partner" && ((sourceNode.getGender() && sourceNode.getGender() == "male" && s.getGender() == "female") ||
                        (sourceNode.getGender() == "female" && s.getGender() == "male")))
                    {
                        s.validPartnerSelected = true;
                        s.getHoverBox().getBoxOnHover().attr(me.graphics._attributes.boxOnHover);
                    }
                    else
                    {
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
    //alert(Raphael.color('blue'));


    var patientNode = editor.addNode(editor.width/2, editor.height/2, 'M');
    patientNode.setBirthDate(new Date(1999,9,2));
    patientNode.setDeceased();
//    patientNode.setAlive();
//    patientNode.setAborted();
//   patientNode.setAlive();
//    patientNode.setSB();
   patientNode.setAlive();
//    patientNode.setFetus(true);
    patientNode.setFirstName("peter");
    patientNode.setLastName("panovitch");
    patientNode.updateNameLabel();
//    patientNode.setSB();
//    patientNode.setConceptionDate(new Date(2002,8,2));
    patientNode.setDeathDate(new Date(2002,9,2));
//




      patientNode.addDisorder("DS1","1 Syndrome");

        patientNode.addDisorder("DS2","2 Syndrome");
    patientNode.addDisorder("DS3","3 Syndrome");
    patientNode.addDisorder("DS4","4 Syndrome");
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
