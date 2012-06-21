
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
            this._width = pedigree_node._graphics._radius * 4;
            this._xPos = pedigree_node._xPos;
            this._yPos = pedigree_node._yPos;
            this._paper = pedigree_node._graphics._paper;
            this._isOptionsToggled = false;

            //An invisible rectangle on top of the node. On hover, it triggers the appearance of the hoverBox elements
            this._hoverZoneMask = this._paper.rect(this._xPos-(this._width/2), this._yPos-(this._width/2),
                this._width, this._width).attr({fill: 'gray', opacity: 0});

            //The gray box that appears when you hover over the _hoverZoneMask
            this._boxOnHover = this._paper.rect(this._xPos-(this._width/2), this._yPos-(this._width/2),
                                              this._width, this._width, 5).attr({fill: "#CCCCCC", stroke: "#8F8F8F"});
            //The options button sitting in the top right corner of the hover zone
            this._optionsIcon = "M2.021,9.748L2.021,9.748V9.746V9.748zM2.022,9.746l5.771,5.773l-5.772,5.771l2.122,2.123l7.894-7.895L4.143,7.623L2.022,9.746zM12.248,23.269h14.419V20.27H12.248V23.269zM16.583,17.019h10.084V14.02H16.583V17.019zM12.248,7.769v3.001h14.419V7.769H12.248z";
            this._optionsBtnIcon = this._paper.path(this._optionsIcon).attr({fill: "#1F1F1F", stroke: "none"}).
                                            transform("t " + (this._xPos + this._width/3.1) +"," +
                                                     (this._yPos - this._width/1.95) + "s.6");
            this._optionsBtnMask = this._paper.rect(this._optionsBtnIcon.getBBox().x, this._optionsBtnIcon.getBBox().y,
                                                 this._optionsBtnIcon.getBBox().width,
                                                 this._optionsBtnIcon.getBBox().height, 1).attr({fill: 'gray', opacity: 0}).transform("s1.5");
            this._optionsBtnIcon.node.setAttribute('class', 'menu-trigger');
            this._optionsBtnMask.node.setAttribute('class', 'menu-trigger');
            this._optionsBtn = this._paper.set().push(this._optionsBtnMask, this._optionsBtnIcon);

            //The menu that appears when the options button is clicked
            this._menu = this._paper.set().push(this.generateMenu([{label: 'Gender', funct: this.menuSetGender},
                {label: 'Date of Birth', funct: this.menuSetDOB}, {label: 'Disorders', funct: this.menuEditDisorders},
                {label: 'Date of Death', funct: this.menuSetDOD},
                {label: 'Delete Person', funct: this.menuDeletePerson}]).hide());

            //Creates the Handles around the node
            this._handles = this.generateHandles();

            //Everything that is layered behind the node
            this._backElements = this._paper.set().push(this._boxOnHover, this._handles);
            //Everything that is layered in front of the node
            this._frontElements = this._paper.set().push(this._hoverZoneMask, this._optionsBtn, this._menu);

            this.hide();
            this.animateDrawHoverZone = this.animateDrawHoverZone.bind(this);
            this.animateHideHoverZone =  this.animateHideHoverZone.bind(this);

            this.enable();
            var me = this;

            //Add hover and click handlers to the options button
            this._optionsBtn.mousedown(function(){me.toggleOptions()});
            this._optionsBtn.hover(function() {me._optionsBtnMask.attr({opacity:.6, stroke: 'none'})},
                function() {me._optionsBtnMask.attr({opacity:0})});
            //Hide menu on mouse click outside the menu
            document.observe('click', function(event) {
                if (me._isOptionsToggled && ['menu-trigger', 'menu', 'menu-item', 'menu-label'].indexOf(event.element().getAttribute('class')) < 0) {
                    me._menu.hide();
                    me._isOptionsToggled =  !me._isOptionsToggled;
                    me.animateHideHoverZone();
                }
            })
       },

        /*
         * Creates a set with four Handles
         */
        generateHandles: function() {

            var outlineRight = this._paper.path("M" + this._xPos + ',' + (this._yPos - (this._node._graphics._radius/20)) + " h" +
                                           this._node._graphics._radius * 1.5 + " A" + this._node._graphics._radius/10 + "," +
                                           this._node._graphics._radius/10 + " 0 1,1 " + (this._xPos + (this._node._graphics._radius * 1.5))
                                           + ',' + (this._yPos + (this._node._graphics._radius/20)) + ' h-' + this._node._graphics._radius *
                                           1.5 + ' v-' + this._node._graphics._radius/10 + 'z'),

                    outlineLeft = this._paper.path("M" + this._xPos + ',' + (this._yPos - (this._node._graphics._radius/20)) + " h" +
                                            this._node._graphics._radius * (-1.5) + " A" + this._node._graphics._radius/10 + "," +
                                            this._node._graphics._radius/10 + " 0 1,0 " + (this._xPos + (this._node._graphics._radius * (-1.5)))
                                            + ',' + (this._yPos + (this._node._graphics._radius/20)) + ' h' + this._node._graphics._radius *
                                            1.5 + ' v-' + this._node._graphics._radius/10 + 'z'),

                    outlineDown = this._paper.path("M" + (this._xPos - (this._node._graphics._radius/20)) + ',' + this._yPos + " v" +
                                            this._node._graphics._radius * 1.5 + " A" + this._node._graphics._radius/10 + "," +
                                            this._node._graphics._radius/10 + " 0 1,0 " + (this._xPos + (this._node._graphics._radius/20))
                                            + ',' + (this._yPos + (this._node._graphics._radius * 1.5)) + ' v-' + this._node._graphics._radius *
                                            1.5 + ' h-' + this._node._graphics._radius/10 + 'z'),

                    outlineUp = this._paper.path("M" + (this._xPos - (this._node._graphics._radius/20)) + ',' + this._yPos + " v-" +
                                            this._node._graphics._radius * 1.5 + " A" + this._node._graphics._radius/10 + "," +
                                            this._node._graphics._radius/10 + " 0 1,1 " + (this._xPos + (this._node._graphics._radius/20))
                                            + ',' + (this._yPos - (this._node._graphics._radius * 1.5)) + ' v' + this._node._graphics._radius *
                                            1.5 + ' h-' + this._node._graphics._radius/10 + 'z'),

                    upCirc = this._paper.circle(this._xPos, this._yPos - (this._node._graphics._radius * 1.6), this._node._graphics._radius/10),
                    downCirc = upCirc.clone().attr({"cy": this._yPos + (this._node._graphics._radius * 1.6)}),
                    leftCirc = upCirc.clone().attr({"cx": this._xPos - (this._node._graphics._radius * 1.6), "cy": this._yPos}),
                    rightCirc = leftCirc.clone().attr({"cx": this._xPos + (this._node._graphics._radius * 1.6)}),

                    glossPath = "c 0.9306,-0.0189 -0.2605,-2.5861 -2.8509,-2.6009 -2.8277,-0.0147 -3.4054,2.332 -2.9559,2.374 0,0 0.7732,0.1071 1.5105,0.313 1.187,0.3341 2.6387,-0.2752 3.2101,-0.3634 0.6366,-0.0988 0.9958,0.3235 1.0862,0.2773z",
                    rightBtnGloss = this._paper.path("M" + (rightCirc.attr("cx") + this._node._graphics._radius/15) +"," + (rightCirc.attr("cy") - this._node._graphics._radius/40)  + glossPath),
                    leftBtnGloss = this._paper.path("M" + (leftCirc.attr("cx") + this._node._graphics._radius/15) +"," + (leftCirc.attr("cy") - this._node._graphics._radius/40)  + glossPath),
                    upBtnGloss = this._paper.path("M" + (upCirc.attr("cx") + this._node._graphics._radius/15) +"," + (upCirc.attr("cy") - this._node._graphics._radius/40)  + glossPath),
                    downBtnGloss = this._paper.path("M" + (downCirc.attr("cx") + this._node._graphics._radius/15) +"," + (downCirc.attr("cy") - this._node._graphics._radius/40)  + glossPath),

                    outlines = this._paper.set().push(outlineUp, outlineDown, outlineLeft, outlineRight),
                    circles = this._paper.set().push(upCirc, downCirc, rightCirc, leftCirc),
                    glosses = this._paper.set().push(rightBtnGloss, leftBtnGloss, upBtnGloss, downBtnGloss);

            outlines.attr({fill: 'gray', stroke: 'none'});
            circles.attr({fill:'#2B5783','fill-opacity':'1'});
            circles.attr({'stroke-width':'1.36','stroke-linecap':'round','stroke-linejoin':'round','stroke-miterlimit':'4','stroke':'none','stroke-opacity':'1'});
            glosses.attr({fill:'270.38154228141-#ffffff:0-#ffffff:100','fill-opacity':'0.0000000'});
            glosses.attr({'stroke':'none','stroke-width':'0.25000000pt','stroke-linecap':'butt','stroke-linejoin':'miter','stroke-opacity':'0.89999998'});


            var rightButton = this._paper.set().push(rightCirc, rightBtnGloss),
                leftButton = this._paper.set().push(leftCirc, leftBtnGloss),
                upButton = this._paper.set().push(upCirc, upBtnGloss),
                downButton = this._paper.set().push(downCirc, downBtnGloss),

                rightHandle = this._paper.set().push(outlineRight, rightButton),
                leftHandle = this._paper.set().push(outlineLeft, leftButton),
                upHandle = this._paper.set().push(outlineUp, upButton),
                downHandle = this._paper.set().push(outlineDown, downButton);

            return this._paper.set().push(rightHandle, leftHandle, upHandle, downHandle);
        },

        /*
         * Draws the hover box elements with a fade in animation
         */
        animateDrawHoverZone: function() {
            this._boxOnHover.stop().animate({opacity:0.7}, 300);
            this._optionsBtnIcon.stop().animate({opacity:1}, 300);
            for(var i = 0; i<this._handles.length; i++)
            {
                var button = this._handles[i][1];
                var handle = this._handles[i][0];
                handle.attr({opacity: 1});
                button[0].attr({opacity: 1});
                button[1].attr({fill:'270.38154228141-#ffffff:0-#ffffff:100', 'stroke':'none','stroke-width':'0.25000000pt','stroke-linecap':'butt','stroke-linejoin':'miter','stroke-opacity':'0.89999998'});
            }
        },

        /*
         * Hides the hover box elements with a fade out animation
         */
        animateHideHoverZone: function() {
            if(!this._isOptionsToggled) {
            this._boxOnHover.stop().animate({opacity:0}, 200);
            this._optionsBtnIcon.stop().animate({opacity:0}, 200);

                for(var i = 0; i<this._handles.length; i++)
                {
                    var button = this._handles[i][1];
                    button[1].attr({fill: '#FFFFFF', opacity: 0.3});
                }
                this._handles.attr({opacity:0});
            }
        },

        /*
         * Hides the hover zone without a fade out animation
         */
        hide: function() {
            this._boxOnHover.attr({opacity:0});
            this._optionsBtnIcon.attr({opacity:0});
            this._handles.attr({opacity:0});

            for(var i = 0; i<this._handles.length; i++)
            {
                var button = this._handles[i][1];
                button[1].attr({fill: '#FFFFFF', opacity:0});
            }
        },

        /*
         * Changes the toggle state of the options button and shows or hides the menu
         */
        toggleOptions: function() {
            var icon = this._optionsBtnIcon;
            this._isOptionsToggled = !this._isOptionsToggled;
            this._optionsBtnMask.attr({opacity:1});
            if(this._isOptionsToggled) {
                this._menu.show();
            }
            else {
                this._menu.hide();
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
            var date = prompt("Please type in the date of birth (DDMMYYYY)");
            node.setAge(date.substr(0,2),date.substr(2,2),date.substr(4));

        },

        /*
         * The menu function for changing the death date of the node
         */
        menuSetDOD: function(node) {
            var k = prompt("are you sure");
            alert(k);
        },

        /*
         * The menu function for managing disorders of the node
         */
        menuEditDisorders: function(node) {
            var k = prompt("are you sure");
            alert(k);
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
            this._frontElements.hover(this.animateDrawHoverZone, this.animateHideHoverZone);
        },

        /*
         * Creates an options menu and fills it up with labels from menuItmes
         * @params an array of objects with names and links to functions
         */
        generateMenu: function(menuItems) {

            var menu = this._paper.set();
            var optBBox = this._optionsBtnIcon.getBBox();
            var x = optBBox.x - optBBox.width;
            var y = optBBox.y2 + optBBox.height;
            var height = 1.3 * optBBox.height;
            var width = this._hoverZoneMask.getBBox().width/1.5;
            var i = 0;
            var me = this;
            var menuBox = this._paper.rect(x, y - optBBox.height/3, width, menuItems.length * height + (optBBox.height * 2/3),3);
            menu.push(menuBox.attr({stroke: 'gray', fill: 'white', 'stroke-width': '0.5px'}));
            menuItems.each(function(s){
                s.menuItem = me._paper.set();
                s.menuItemBox = me._paper.rect(x,y+i*height, width, height).attr({stroke: 'none', fill: 'white', 'stroke-width': 0});
                s.menuItemBox.node.setAttribute('class', 'menu-item');
                s.menuItem.push(s.menuItemBox);

                s.menuLabel = me._paper.text(optBBox.x, y+i*height+height/2, s.label);
                s.menuLabel.attr("x", s.menuLabel.attr("x") + s.menuLabel.getBBox().width/2);
                s.menuLabel.node.setAttribute('class', 'menu-label');
                s.menuItem.push(s.menuLabel);
                menu.push(s.menuItem);

                s.menuItem.hover(
                    function() {
                        s.menuItemBox.attr({'fill': 'blue'});
                        s.menuLabel.attr({fill: "white"});
                },  function() {
                        s.menuItemBox.attr('fill', 'white');
                        s.menuLabel.attr({fill: "black"});
                    }
                );
                s.menuItem.click(function() {s.funct(me._node);});
                i++;
            });
            return menu;
        }

    });
