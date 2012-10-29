/*
 * Workspace is responsible for initializing the Raphael canvas, the zoom/pan controls and the menu bar
 * on the top. It includes functions for managing the Raphael paper object and coordinate transformation methods
 * for taking pan and zoom levels into account.
 */

var Workspace = Class.create({

    initialize: function(graphics) {
        var me = this;
        this.canvas = new Element('div', {'id' : 'canvas'});
        this.workArea = new Element('div', {'id' : 'work-area'}).update(this.canvas);
        $('body').update(this.workArea);
        var screenDimensions = document.viewport.getDimensions();
        this.width = screenDimensions.width;
        this.height = screenDimensions.height - this.canvas.cumulativeOffset().top - 4;
        this._paper = Raphael("canvas",this.width, this.height);
        this.viewBoxX = 0;
        this.viewBoxY = 0;
        this.zoomCoefficient = 1;
        this.background = this.getPaper().rect(0,0, this.width, this.height).attr({fill: 'blue', stroke: 'none', opacity: 0}).toBack();
        this.background.node.setAttribute("class", "panning-background");

        this.adjustSizeToScreen = this.adjustSizeToScreen.bind(this);
        Event.observe (window, 'resize', me.adjustSizeToScreen);
        this.generateTopMenu();
        this.generateViewControls();

        //Initialize pan by dragging
        var start = function() {
            me.background.ox = me.background.attr("x");
            me.background.oy = me.background.attr("y");
            me.background.attr({cursor: 'url(https://mail.google.com/mail/images/2/closedhand.cur)'});
        };
        var move = function(dx, dy) {
            var  deltax = me.viewBoxX - dx/me.zoomCoefficient;
            var deltay = me.viewBoxY - dy/me.zoomCoefficient;

            me.background.attr({x: me.background.ox - dx/me.zoomCoefficient, y: me.background.oy - dy/me.zoomCoefficient});
            me.getPaper().setViewBox(deltax, deltay, me.width/me.zoomCoefficient, me.height/me.zoomCoefficient);
            me.background.ox = deltax;
            me.background.oy = deltay;
            me.background.attr({x: me.background.ox, y: me.background.oy });
        };
        var end = function() {
            me.viewBoxX = me.background.ox;
            me.viewBoxY = me.background.oy;
            me.background.attr({cursor: 'default'});
        };
        me.background.drag(move, start, end);
    },

    /*
     * Returns the Raphael paper object.
     */
    getPaper: function() {
        return this._paper;
    },

    /*
     * Returns the div element containing the Workspace elements.
     */
    getWorkArea: function() {
        return this.workArea;
    },

    /*
     * Returns the width of the canvas
     */
    getWidth: function() {
        return this.width;
    },

    /*
     * Returns the height of the canvas
     */
    getHeight: function() {
        return this.height;
    },

    /*
     * Adjust the canvas viewbox by the given zoom coefficient
     *
     * @param zoomCoefficient the fraction which will be applied to the original zoom level
     */
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

    /*
     * Creates the menu on the top
     */
    generateTopMenu: function() {
        var menu = new Element('div', {'id' : 'editor-menu'});
        this.getWorkArea().insert({before : menu});
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
            if (data.callback && typeof(this[data.callback]) == 'function') {
                mi.observe('click', function() {
                    this[data.callback]();
                });
            }
            return mi;
        };
        submenus.each(_createSubmenu);
    },

    /*
     * Creates the controls for panning and zooming
     */
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
                    _this.panTo(_this.viewBoxX, _this.viewBoxY - 400);
                }
                else if(direction == 'down') {
                    _this.panTo(_this.viewBoxX, _this.viewBoxY + 400);
                }
                else if(direction == 'left') {
                    _this.panTo(_this.viewBoxX - 400, _this.viewBoxY);
                }
                else {
                    _this.panTo(_this.viewBoxX + 400, _this.viewBoxY);
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
        this.getWorkArea().insert(this.__controls);
    },

    /*
     * Converts the coordinates relative to the Raphael canvas to coordinates relative to the canvas div
     * and returns them
     *
     * @param canvasX the x coordinate relative to the Raphael canvas (ie with pan/zoom transformations)
     * @param canvasY the y coordinate relative to the Raphael canvas (ie with pan/zoom transformations)
     */
    canvasToDiv: function(canvasX,canvasY) {
        return {
            x: this.zoomCoefficient * (canvasX - this.viewBoxX),
            y: this.zoomCoefficient * (canvasY - this.viewBoxY)
        }
    },

    /*
     * Converts the coordinates relative to the canvas div to coordinates relative to the Raphael canvas
     * by applying zoom/pan transformations and returns them.
     *
     * @param divX the x coordinate relative to the canvas div (ie without pan/zoom transformations)
     * @param divY the y coordinate relative to the canvas div (ie without pan/zoom transformations)
     */
    divToCanvas: function(divX,divY) {
        return {
            x: divX/this.zoomCoefficient + this.viewBoxX,
            y: divY/this.zoomCoefficient + this.viewBoxY
        }
    },

    /*
     * Converts the coordinates relative to the browser viewport to coordinates relative to the canvas div,
     * and returns them
     *
     * @param absX the x coordinate relative to the browser viewport
     * @param absY the y coordinate relative to the browser viewport
     */
    viewportToDiv : function (absX, absY) {
        return {
            x : + absX - this.canvas.cumulativeOffset().left,
            y : absY - this.canvas.cumulativeOffset().top
        };
    },

    /*
     * Animates a transformation of the viewbox to the given coordinate
     *
     * @param x the x coordinate (relative to the Raphael canvas)
     * @param y the y coordinate (relative to the Raphael canvas)
     */
    panTo: function(x, y) {
        var me = this,
            oX = this.viewBoxX,
            oY = this.viewBoxY,
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
                    me.viewBoxX += xStep;
                    me.viewBoxY += yStep;
                    me.getPaper().setViewBox(me.viewBoxX, me.viewBoxY, me.width/me.zoomCoefficient, me.height/me.zoomCoefficient);
                    draw();
                }
            }, 1000 / fps);
        }
        draw();
    },

    /*
     * Adjusts the canvas size to the current viewport dimensions.
     */
    adjustSizeToScreen : function() {
        var screenDimensions = document.viewport.getDimensions();
        this.width = screenDimensions.width;
        this.height = screenDimensions.height - this.canvas.cumulativeOffset().top - 4;
        this.getPaper().setSize(this.width, this.height);
        this.getPaper().setViewBox(this.viewBoxX, this.viewBoxY, this.width/this.zoomCoefficient, this.height/this.zoomCoefficient);
        this.background && this.background.attr({"width": this.width, "height": this.height});
        if (editor.getNodeMenu()) {
            editor.getNodeMenu().reposition();
        }
    }
});