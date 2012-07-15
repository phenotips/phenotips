var PedigreeEditor = Class.create({
    initialize: function(graphics) {

        //create canvas
        var screenDimensions = document.viewport.getDimensions();
        var titleHeight = $('document-title');

        this.width = screenDimensions.width;
        this.height = screenDimensions.height;
        this.paper = Raphael("canvas", this.width, this.height);
        var myrect = this.paper.rect(0,0,this.width,this.height);
        myrect.node.setAttribute('class', 'background');
        myrect.attr({fill: "r(.5,.9)hsb(" +.5 + ", 1, .75)-hsb(" +.3 + ", .5, .25)"});
        this.paper.circle(this.width/2, this.height/2, (Math.sqrt(this.height * this.height + this.width * this.width))/2).attr({fill: "r(.5,.5)hsb(" +.4 + ", 0, 1)-hsb(" +.2 + ", 0, .75)", stroke: "none"});

        this.nodes = [[],[]];
        this.idCount = 1;
        this.hoverModeZones = this.paper.set();
        this.graphics = new NodeVisuals(this.paper);
        this.globalSet = [];

        this.initMenu();
        this.nodeMenu = this.generateNodeMenu();



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
                'name' : 'gender',
                'label' : 'Gender',
                'type' : 'radio',
                'values' : [
                    { 'actual' : 'M', 'displayed' : 'Male' },
                    { 'actual' : 'F', 'displayed' : 'Female' },
                    { 'actual' : 'U', 'displayed' : 'Unknown' }
                ],
                'default' : 'U'
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
                'type' : 'checkbox'
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
                'default' : 'alive'
            },
            {
                'name' : 'date_of_death',
                'label' : 'Date of death',
                'type' : 'date-picker',
                'format' : 'dd/MM/yyyy'
            }
        ],$('canvas'));
    },
    getOppositeGender : function(node){
        if (node.getGender() == "unknown") {
            return "unknown"
        }
        else if(node.getGender() == "male") {
            return "female"
        }
        else {
            return "male"
        }
    },

    initMenu : function() {
        var el = $("action-new");
        el.observe("click", function() {alert("new node has been created! WHOAH!")});
    },

    addNode: function(x, y, gender)
    {
        var node = new PedigreeNode(x, y, gender);
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
        if(node1._partners.indexOf(node2) == -1)
        {
            var connection = new Connection("partner", node1, node2);
            node1._partners.push(node2);
            node2._partners.push(node1);
        }
    },

    addParentsConnection : function(node, leftParent, rightParent)
    {
        leftParent._partners.push(rightParent);
        rightParent._partners.push(leftParent);
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

    var patientNode = editor.addNode(editor.width/2, editor.height/2, 'male');
    var otherPatient = editor.addNode(editor.width/3, editor.height/3, 'male');

    patientNode.setBirthDate(10, 6, 2011);
    patientNode.setDead(true);

//        patientNode.setGender('male');
//        patientNode.addDisorder("Down Syndrome");
//        patientNode.addDisorder("Up Syndrome");
//        patientNode.addDisorder("Left Disorder");
//        patientNode.setAdopted(false);
      //patientNode.setGender('female');
//        //patientNode.removeDisorder("Down Syndrome");
//        //patientNode.addDisorder("Down Syndrome");
//        patientNode.setGender('female');
        //var nodeElement = patientNode._graphics.draw(patientNode);
        //alert(nodeElement.transform());

        var ani = function() {
            patientNode._hoverBox.disable();
            patientNode._xPos += 100;
            patientNode._yPos += 100;
            nodeElement.stop().animate({'transform': "t " + 100 + "," + 100+"..."}, 2000, "linear", patientNode._hoverBox.enable.bind(patientNode._hoverBox));
        };
   // ani();

//    var ph = new PlaceHolder(editor.width/2, editor.height/2, editor.graphics, 'female');
//    var dad = editor.addNode(editor.width/2, editor.height/2, 'unknown');
//    var mom = editor.addNode(editor.width/2, editor.height/2, 'unknown');
//    var son = editor.addNode(editor.width/2, editor.height/2, 'female');
//    ph._father = dad;
//    son._mother = mom;

      //var pn = new PedigreeNode(0,0,'female');




});
