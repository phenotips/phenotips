
    /**
     * A box with options for a PedigreeNode
     */
    var Hoverbox = Class.create( {
        
        /**
         * Constructor for Hoverbox
         * @param pedigree_node the PedigreeNode around which the box is drawn
         */
        initialize: function(pedigree_node) {
            this._node = pedigree_node;
            this._width = editor.graphics.getRadius() * 4;
            this._isOptionsToggled = false;
            var me = this;
            this._optionsBtn = this.generateOptionsBtn();
            this._handles = this.generateHandles();
            this._backElements = this.generateBackElements();
            this._frontElements = this.generateFrontElements();
            this._isHovered = false;

            this.animateDrawHoverZone = this.animateDrawHoverZone.bind(this);
            this.animateHideHoverZone =  this.animateHideHoverZone.bind(this);
            this.hide();
            this.enable();

            document.observe('click', function(event) {
                if (me._isOptionsToggled && ['menu-trigger', 'menu', 'menu-item', 'menu-label'].indexOf(event.element().getAttribute('class')) < 0) {
                    editor.nodeMenu.hide();
                    me._isOptionsToggled =  !me._isOptionsToggled;
                    me.animateHideHoverZone();
                }
            });

       },
        getNode: function() {
            return this._node;
        },
        getHandles: function() {
            return this._handles;
        },

        getOptionsBtn: function() {
            return this._optionsBtn;
        },
        getBoxOnHover: function() {
            return this.getBackElements()[0];
        },

        isHovered: function() {
            return this._isHovered;
        },

        setHovered: function(isHovered) {
            this._isHovered = isHovered;
        },

        generateFrontElements: function() {
            var mask = editor.paper.rect(this.getNode().getX()-(this._width/2),
                                         this.getNode().getY()-(this._width/2),this._width, this._width);
            mask.attr({fill: 'gray', opacity: 0});
            var me = this,
                frontElements = editor.paper.set().push(mask, this.getOptionsBtn(), this.handleOrbs);
            return frontElements.hover(function() {me.setHovered(true)}, function() {me.setHovered(false)});
        },

        getHoverZoneMask: function() {
            return this.getFrontElements()[0];
        },

        getFrontElements: function() {
            return this._frontElements;
        },

        generateBackElements: function() {
            var boxOnHover = editor.paper.rect(this.getNode().getX()-(this._width/2), this.getNode().getY()-(this._width/2),
                this._width, this._width, 5).attr(editor.graphics._attributes.boxOnHover);
            return editor.paper.set().push(boxOnHover, this.getHandles());
        },

        getBackElements: function() {
            return this._backElements;
        },

        //The options button sitting in the top right corner of the hover zone
        generateOptionsBtn: function() {
            var me = this,
                path = "M2.021,9.748L2.021,9.748V9.746V9.748zM2.022,9.746l5.771,5.773l-5.772,5.771l2.122,2.123l7.894-7.895L4.143,7.623L2.022,9.746zM12.248,23.269h14.419V20.27H12.248V23.269zM16.583,17.019h10.084V14.02H16.583V17.019zM12.248,7.769v3.001h14.419V7.769H12.248z",
                iconX = this.getNode().getX() + editor.graphics.getRadius() * 1.45,
                iconY = this.getNode().getY() - editor.graphics.getRadius() * 1.9,
                iconScale = editor.graphics.getRadius() * 0.014,
                optionsBtnIcon = editor.paper.path(path);

            optionsBtnIcon.attr(editor.graphics._attributes.optionsBtnIcon);
            optionsBtnIcon.transform(["t" , iconX , iconY, "s", iconScale, iconScale, 0, 0]);
            var optionsBtnMask = editor.paper.rect(optionsBtnIcon.getBBox().x, optionsBtnIcon.getBBox().y,
                optionsBtnIcon.getBBox().width, optionsBtnIcon.getBBox().height, 1);
            optionsBtnMask.attr({fill: 'gray', opacity: 0}).transform("s1.5");

            optionsBtnIcon.node.setAttribute('class', 'menu-trigger');
            optionsBtnMask.node.setAttribute('class', 'menu-trigger');

            var button = editor.paper.set(optionsBtnMask, optionsBtnIcon).click(function(){me.toggleOptions()});
            button.mousedown(function(){optionsBtnMask.attr(editor.graphics._attributes.optionsBtnMaskClick)});
            button.hover(function() {
                            optionsBtnMask.attr(editor.graphics._attributes.optionsBtnMaskHoverOn)
                        },
                        function() {
                            optionsBtnMask.attr(editor.graphics._attributes.optionsBtnMaskHoverOff)
                        });
            return button;
        },

        getOptionsBtnIcon: function() {
            return this.getOptionsBtn()[1];
        },

        /*
         * Creates a set with four Handles
         */
        generateHandles: function() {
            var rightPath = [["M", this.getNode().getX(), this.getNode().getY()],["L", this.getNode().getX() + (editor.graphics.getRadius() * 1.6), this.getNode().getY()]],
                leftPath = [["M", this.getNode().getX(), this.getNode().getY()],["L", this.getNode().getX() - (editor.graphics.getRadius() * 1.6), this.getNode().getY()]],
                upPath = [["M", this.getNode().getX(), this.getNode().getY()],["L", this.getNode().getX(), this.getNode().getY() - (editor.graphics.getRadius() * 1.6)]],
                downPath = [["M", this.getNode().getX(), this.getNode().getY()],["L", this.getNode().getX(), this.getNode().getY() + (editor.graphics.getRadius() * 1.6)]],

                connectionAttr = {"stroke-width": 4, stroke: "gray"},
                rightConnection = editor.paper.path(rightPath).attr(connectionAttr),
                leftConnection = editor.paper.path(leftPath).attr(connectionAttr),
                downConnection = editor.paper.path(downPath).attr(connectionAttr),
                upConnection = editor.paper.path(upPath).attr(connectionAttr),

                orbRadius = editor.graphics._attributes.orbRadius,
                orbHue = editor.graphics._attributes.orbHue,
                upCirc = editor.graphics.generateOrb(upPath[1][1], upPath[1][2], orbRadius, orbHue),
                downCirc = editor.graphics.generateOrb(downPath[1][1], downPath[1][2], orbRadius, orbHue),
                leftCirc = editor.graphics.generateOrb(leftPath[1][1], leftPath[1][2], orbRadius, orbHue),
                rightCirc = editor.graphics.generateOrb(rightPath[1][1], rightPath[1][2], orbRadius, orbHue),

                rightHandle = editor.paper.set().push(rightConnection, rightCirc),
                leftHandle = editor.paper.set().push(leftConnection, leftCirc),
                upHandle = editor.paper.set().push(upConnection, upCirc),
                downHandle = editor.paper.set().push(downConnection, downCirc);

            rightHandle.type = leftHandle.type = 'partner';
            upHandle.type = 'parent';
            downHandle.type = 'child';


            rightConnection.oPath = rightPath;
            leftConnection.oPath = leftPath;
            upConnection.oPath = upPath;
            downConnection.oPath = downPath;
            this.enable =  this.enable.bind(this);
            this.disable =  this.disable.bind(this);
            this.handleOrbs = editor.paper.set().push(rightCirc, leftCirc, upCirc, downCirc);
            var me = this,
                orb,
                connection,
                isDrag,
                movingHandles = 0;

            var start = function(handle) {
                me.disable();
                orb = handle[1];
                connection = handle[0];
                orb.ox = orb[0].attr("cx");
                orb.oy = orb[0].attr("cy");
                connection.ox = connection.oPath[1][1];
                connection.oy = connection.oPath[1][2];
                movingHandles++;
                editor.currentDraggable.node = me.node;
                editor.currentDraggable.handle = handle.type;
                isDrag = false;
                me.getNode().draw();
                editor.enterHoverMode(me.getNode());
                //TODO: right click behavior
//                document.observe('contextmenu',
//                    function(ev) {
//                            ev.preventDefault();
//                            //alert("rclick");
//                            isDrag = true;
//                            end();
//                            ev.stop();
//                            }
//                    , false);
            };
            var move = function(dx, dy) {
                orb.attr("cx", orb.ox + dx);
                orb.attr("cy", orb.oy + dy);
                connection.oPath[1][1] = connection.ox + dx;
                connection.oPath[1][2] = connection.oy + dy;
                connection.attr("path", connection.oPath);
                if(dx > 5 || dx < -5 || dy > 5 || dy < -5 )
                {
                    isDrag = true;
                }
            };
            var end = function() {
                orb.animate({"cx": orb.ox, "cy": orb.oy}, +isDrag * 1000, "elastic",
                    function() {
                        movingHandles--;
                        if(movingHandles == 0)
                        {
                            me.enable();
                            me.animateHideHoverZone();
                        }
                    });
                editor.exitHoverMode();
                if(editor.currentHoveredNode && editor.currentHoveredNode.validPartnerSelected)
                {
                    editor.addPartnerConnection(me.getNode(), editor.currentHoveredNode);
                    editor.currentHoveredNode = null;
                }
                editor.currentHoveredNode && editor.currentHoveredNode.getHoverBox().getBoxOnHover().attr(editor.graphics._attributes.boxOnHover);
                connection.oPath[1][1] = connection.ox;
                connection.oPath[1][2] = connection.oy;
                connection.animate({"path": connection.oPath},1000, "elastic");

                if(isDrag == false)
                {
                    me.handleClickAction(editor.currentDraggable.handle);
                }
                editor.currentDraggable.node = null;
                editor.currentDraggable.handle = null;
            };

            rightCirc.drag(move, function() {start(rightHandle)},end);
            leftCirc.drag(move, function() {start(leftHandle)},end);
            upCirc.drag(move, function() {start(upHandle)},end);
            downCirc.drag(move, function() {start(downHandle)},end);

            return editor.paper.set().push(rightHandle, leftHandle, upHandle, downHandle);
        },

        handleClickAction : function(relationship)
        {
            if(relationship == "partner")
            {
                var partner = editor.addNode(this.getNode().getX() + 300, this.getNode().getY(), editor.getOppositeGender(this.getNode()));
                editor.addPartnerConnection(this.getNode(), partner);
            }
            else if(relationship == "child")
            {
                var child = editor.addNode(this.getNode().getX() + 150, this.getNode().getY() + 200, 'unknown');
                var otherParent = editor.addPlaceHolder(this.getNode().getX() + 300, this.getNode().getY(), editor.getOppositeGender(this.getNode()));
                this.getNode().addChild(child);
                child.addParent(this.getNode());
                child.addParent(otherParent);
                otherParent.addChild(child);
                this.getNode().addPartner(otherParent);
                otherParent.addPartner(this.getNode());
            }
            else if(relationship == "parent") {
                if(this.getNode().getMother() == null) {
                    var mother = editor.addNode(this.getNode().getX() + 100, this.getNode().getY() - 260, "female");
                    this.getNode().addParent(mother);
                }
                if(this.getNode().getFather() == null) {
                    var father = editor.addNode(this.getNode().getX() - 100, this.getNode().getY() - 260, "male");
                    this.getNode().addParent(father);
                }
            }
        },

        /*
         * Draws the hover box elements with a fade in animation
         */
        animateDrawHoverZone: function() {
            this.getBoxOnHover().stop().animate({opacity:0.7}, 300);
            this.getOptionsBtnIcon().stop().animate({opacity:1}, 300);
            this._handles.show();
            var labels = this.getNode()._labels;
            for (var i = 0; i < labels.length; i++)
            {
                labels[i].stop().animate({"y": labels[i].oy + 50}, 300,">");
            }
        },

        /*
         * Hides the hover box elements with a fade out animation
         */
        animateHideHoverZone: function() {
            if(!this._isOptionsToggled && !this.isHovered()) {
            this.getBoxOnHover().stop().animate({opacity:0}, 200);
            this.getOptionsBtnIcon().stop().animate({opacity:0}, 200);
            this._handles.hide();
            var labels = this.getNode()._labels;
            for (var i = 0; i < labels.length; i++)
            {
                labels[i].stop().animate({"y": labels[i].oy}, 300,">");
            }
            }
        },

        /*
         * Hides the hover zone without a fade out animation
         */
        hide: function() {
            this.getBoxOnHover().attr({opacity:0});
            this.getOptionsBtnIcon().attr({opacity:0});
            this._handles.hide();
        },

        /*
         * Changes the toggle state of the options button and shows or hides the menu
         */
        toggleOptions: function() {
            this._isOptionsToggled = !this._isOptionsToggled;
            this.getOptionsBtn()[0].attr(editor.graphics._attributes.optionsBtnMaskHoverOn);
            if(this._isOptionsToggled) {
                var optBBox = this.getBoxOnHover().getBBox();
                var x = optBBox.x2;
                var y = optBBox.y;
                editor.nodeMenu.show({}, x+5,y);
            }
            else {
                editor.nodeMenu.hide();
            }
        },

        /*
         * The menu function for removing a node
         */
        menuDeletePerson: function(node) {
            confirm("are you sure") && alert("Person Removed");
        },

        /*
         * The menu function for changing gender of the node
         */
        menuSetGender: function(node) {
            var gender = prompt("Please input gender (male, female or unknown)");
            node.setGender(gender);
        },

        /*
         * The menu function for changing the birth date of the node
         */
        menuSetDOB: function(node) {
            var input = prompt("Please type in the date of birth (DDMMYYYY)");
            var birthDate = new Date(input.substr(4), input.substr(2,2), input.substr(0,2));
            node.setAge(birthDate);
        },

        /*
         * The menu function for changing the death date of the node
         */
        menuSetDOD: function(node) {
            var input = prompt("Please type in the date of death (DDMMYYYY)");
            var deathDate = new Date(input.substr(4), input.substr(2,2), input.substr(0,2));
            var isDead;
            deathDate && (isDead = true);
            node.setDead(isDead, deathDate);
        },

        menuToggleDead: function(node)  {
            node.setDead(!node.isDead());
        },

        /*
         * The menu function for managing disorders of the node
         */
        menuEditDisorders: function(node) {
            var k = prompt("are you sure");
            alert(k);
        },

        menuToggleAdopt: function(node) {
            node.toggleAdoption();
        },

        menuToggleSB: function(node) {
            node.toggleSB();
        },

        /*
         * Removes the hover properties of the hover box
         */
        disable: function() {
            this._frontElements.unhover(this.animateDrawHoverZone, this.animateHideHoverZone);
        },

        /*
         * Enables the hover properties of the hover box
         */
        enable: function() {
            var me = this;
            this._frontElements.hover(function() {

                if(editor.currentDraggable.handle != null && editor.currentDraggable.node == me.getNode())
                {
                    alert("drag with handle");
                }
                else
                {
                    //alert(editor.currentDraggable.handle);
                    me.animateDrawHoverZone()
                }
            }, me.animateHideHoverZone);
        }
    });
