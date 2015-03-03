/**
 * Workspace contains the Raphael canvas, the zoom/pan controls and the menu bar
 * on the top. The class includes functions for managing the Raphael paper object and coordinate transformation methods
 * for taking pan and zoom levels into account.
 *
 * @class Workspace
 * @constructor
 */

var Workspace = Class.create({

    initialize: function() {
        var me = this;
        this.canvas = new Element('div', {'id' : 'canvas'});
        this.workArea = new Element('div', {'id' : 'work-area'}).update(this.canvas);
        $('body').update(this.workArea);
        var screenDimensions = document.viewport.getDimensions();
        this.generateTopMenu();
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
        this.generateViewControls();

        //Initialize pan by dragging
        var start = function() {
            if (editor.isAnyMenuVisible()) {
                return;
            }
            me.background.ox = me.background.attr("x");
            me.background.oy = me.background.attr("y");
            //me.background.attr({cursor: 'url(https://mail.google.com/mail/images/2/closedhand.cur)'});
            me.background.attr({cursor: 'move'});
        };
        var move = function(dx, dy) {
            var deltax = me.viewBoxX - dx/me.zoomCoefficient;
            var deltay = me.viewBoxY - dy/me.zoomCoefficient;

            me.getPaper().setViewBox(deltax, deltay, me.width/me.zoomCoefficient, me.height/me.zoomCoefficient);
            me.background.ox = deltax;
            me.background.oy = deltay;
            me.background.attr({x: deltax, y: deltay });
        };
        var end = function() {
            me.viewBoxX = me.background.ox;
            me.viewBoxY = me.background.oy;
            me.background.attr({cursor: 'default'});
        };
        me.background.drag(move, start, end);

        if (document.addEventListener) {
            // adapted from from raphaelZPD
            me.handleMouseWheel = function(evt) {
                if (evt.preventDefault)
                    evt.preventDefault();
                else
                    evt.returnValue = false;

                // disable while menu is active - too easy to scroll and get the active node out of sight, which is confusing
                if (editor.isAnyMenuVisible()) {
                    return;
                }

                var delta;
                if (evt.wheelDelta)
                    delta = -evt.wheelDelta; // Chrome/Safari
                else
                    delta = evt.detail; // Mozilla

                //console.log("Mouse wheel: " + delta);
                if (delta > 0) {
                    var x = $$('.zoom-out')[0];
                    $$('.zoom-out')[0].click();
                } else {
                    $$('.zoom-in')[0].click();
                }
            }

            if (navigator.userAgent.toLowerCase().indexOf('webkit') >= 0) {
                this.canvas.addEventListener('mousewheel', me.handleMouseWheel, false); // Chrome/Safari
            } else {
                this.canvas.addEventListener('DOMMouseScroll', me.handleMouseWheel, false); // Others
            }
        } 
    },

    /**
     * Returns the Raphael paper object.
     *
     * @method getPaper
     * @return {Object} Raphael Paper element
     */
    getPaper: function() {
        return this._paper;
    },

    /**
     * Returns the div element containing everything except the top menu bar
     *
     * @method getWorkArea
     * @return {HTMLElement}
     */
    getWorkArea: function() {
        return this.workArea;
    },

    /**
     * Returns width of the work area
     *
     * @method getWidth
     * @return {Number}
     */
    getWidth: function() {
        return this.width;
    },

    /**
     * Returns height of the work area
     *
     * @method getHeight
     * @return {Number}
     */
    getHeight: function() {
        return this.height;
    },

    /**
     * Creates the menu on the top
     *
     * @method generateTopMenu
     */
    generateTopMenu: function() {
        var menu = new Element('div', {'id' : 'editor-menu'});
        this.getWorkArea().insert({before : menu});
        var submenus = [];

        if (editor.isUnsupportedBrowser()) {
            submenus = [{
                name : 'input',
                items: [
                    { key : 'readonlymessage', label : 'Unsuported browser mode', icon : 'exclamation-triangle'}
                ]
              }, {
                name : 'output',
                items: [
                    { key : 'export',    label : 'Export', icon : 'download'},
                    { key : 'reload',    label : 'Reload', icon : 'refresh'},
                    { key : 'close',     label : 'Close', icon : 'sign-out'}
                ]
            }];
        } else {
            submenus = [{
                name : 'input',
                items: [
                    { key : 'templates', label : 'Templates', icon : 'copy'},
                    { key : 'import',    label : 'Import', icon : 'upload'}
                ]
              }, {
                name : 'edit',
                items: [
                    { key : 'undo',   label : 'Undo', icon : 'undo'},
                    { key : 'redo',   label : 'Redo', icon : 'repeat'},
                    { key : 'layout', label : 'Automatic layout', icon : 'sitemap'},
                    { key : 'number', label : 'Renumber', icon : 'sort-numeric-asc'}
                ]
              }, {
                name : 'reset',
                items: [
                    { key : 'clear',  label : 'Clear all', icon : 'times-circle'},
                    { key : 'reload',    label : 'Reload', icon : 'refresh'}
                ]
              }, {
                name : 'output',
                items: [
                    { key : 'save',      label : 'Save', icon : 'check'},
                    { key : 'export',    label : 'Export', icon : 'download'},
                    //{ key : 'print',     label : 'Print', icon : 'print'},
                    { key : 'close',     label : 'Close', icon : 'sign-out'}
                ]
            }];
        }
        var _createSubmenu = function(data) {
            var submenu = new Element('div', {'class' : data.name + '-actions action-group'});
            menu.insert(submenu);
            data.items.each(function (item) {
                submenu.insert(_createMenuItem(item));
            });
        };
        var _createMenuItem = function(data) {
            var mi = new Element('span', {'id' : 'action-' + data.key, 'class' : 'field-no-user-select menu-item ' + data.key}).insert(new Element('span', {'class' : 'fa fa-' + data.icon})).insert(' ').insert(data.label);
            if (data.callback && typeof(this[data.callback]) == 'function') {
                mi.observe('click', function() {
                    this[data.callback]();
                });
            }
            return mi;
        };
        submenus.each(_createSubmenu);
    },

    /**
     * Adjusts the canvas viewbox to the given zoom coefficient
     *
     * @method zoom
     * @param {Number} zoomCoefficient The zooming ratio
     */
    zoom: function(zoomCoefficient) {
        if (zoomCoefficient < 0.15) zoomCoefficient = 0.15;
        if (zoomCoefficient > 0.15 && zoomCoefficient < 0.25) zoomCoefficient = 0.25;
        zoomCoefficient = Math.round(zoomCoefficient/0.05)/20;
        //console.log("zoom: " + zoomCoefficient);
        var newWidth  = this.width/zoomCoefficient;
        var newHeight = this.height/zoomCoefficient;
        this.viewBoxX = this.viewBoxX + (this.width/this.zoomCoefficient - newWidth)/2;
        this.viewBoxY = this.viewBoxY + (this.height/this.zoomCoefficient - newHeight)/2;
        this.getPaper().setViewBox(this.viewBoxX, this.viewBoxY, newWidth, newHeight);
        this.zoomCoefficient = zoomCoefficient;
        this.background.attr({x: this.viewBoxX, y: this.viewBoxY, width: newWidth, height: newHeight});
    },

    /**
     * Creates the controls for panning and zooming
     *
     * @method generateViewControls
     */
    generateViewControls : function() {
        var _this = this;
        this.__controls = new Element('div', {'class' : 'view-controls'});
        // Pan controls
        this.__pan = new Element('div', {'class' : 'view-controls-pan', title : 'Pan'});
        this.__controls.insert(this.__pan);
        ['up', 'right', 'down', 'left', 'home'].each(function (direction) {
            var faIconClass = (direction == 'home') ? "fa-user" : "fa-arrow-" + direction;
            _this.__pan[direction] = new Element('span', {'class' : 'view-control-pan pan-' + direction + ' fa fa-fw ' + faIconClass, 'title' : 'Pan ' + direction});
            _this.__pan.insert(_this.__pan[direction]);
            _this.__pan[direction].observe('click', function(event) {
                if (direction == 'home') {
                    _this.centerAroundNode(0);
                }
                else if(direction == 'up') {
                    _this.panTo(_this.viewBoxX, _this.viewBoxY - 150);
                }
                else if(direction == 'down') {
                    _this.panTo(_this.viewBoxX, _this.viewBoxY + 150);
                }
                else if(direction == 'left') {
                    _this.panTo(_this.viewBoxX - 150, _this.viewBoxY);
                }
                else {
                    _this.panTo(_this.viewBoxX + 150, _this.viewBoxY);
                }
            })
        });
        // Zoom controls
        var trackLength = 200;
        this.__zoom = new Element('div', {'class' : 'view-controls-zoom', title : 'Zoom'});
        this.__controls.insert(this.__zoom);
        this.__zoom.track  = new Element('div', {'class' : 'zoom-track'});
        this.__zoom.handle = new Element('div', {'class' : 'zoom-handle', title : 'Drag to zoom'});
        this.__zoom['in']  = new Element('div', {'class' : 'zoom-button zoom-in fa fa-fw fa-search-plus', title : 'Zoom in'});
        this.__zoom['out'] = new Element('div', {'class' : 'zoom-button zoom-out fa fa-fw fa-search-minus', title : 'Zoom out'});
        this.__zoom.label  = new Element('div', {'class' : 'zoom-crt-value'});
        this.__zoom.insert(this.__zoom['in']);
        this.__zoom.insert(this.__zoom.track);
        this.__zoom.track.insert(this.__zoom.handle);
        this.__zoom.track.style.height = trackLength + 'px';
        this.__zoom.insert(this.__zoom.out);
        this.__zoom.insert(this.__zoom.label);
        // Scriptaculous slider
        // see also http://madrobby.github.com/scriptaculous/slider/
        //
        // Here a non-linear scale is used: slider positions form [0 to 0.9] correspond to
        // zoom coefficients from 1.25x to 0.25x, and zoom positions from (0.9 to 1]
        // correspond to single deepest zoom level 0.15x
        this.zoomSlider = new Control.Slider(this.__zoom.handle, this.__zoom.track, {
            axis:'vertical',
            minimum: 0,
            maximum: trackLength,
            increment : 1,
            alignY: 6,
            onSlide : function (value) {
                // Called whenever the Slider is moved by dragging.
                // The called function gets the slider value (or array if slider has multiple handles) as its parameter.
                //console.log("new val: " + value + " current coeff: " + _this.zoomCoefficient );
                if (value <= 0.9) {
                    _this.zoom(-value/0.9 + 1.25);
                } else {
                    _this.zoom(0.15);
                }
            },
            onChange : function (value) {
                // Called whenever the Slider has finished moving or has had its value changed via the setSlider Value function.
                // The called function gets the slider value (or array if slider has multiple handles) as its parameter.
                if (value <= 0.9) {
                    _this.zoom(-value/0.9 + 1.25);
                } else {
                    _this.zoom(0.15);
                }
            }
        });
        if (editor.isUnsupportedBrowser()) {
            this.zoomSlider.setValue(0.25 * 0.9); // 0.25 * 0.9 corresponds to zoomCoefficient of 1, i.e. 1:1
                                                  // - for best chance of decent looks on non-SVG browsers like IE8
        } else {
            this.zoomSlider.setValue(0.5 * 0.9);  // 0.5 * 0.9 corresponds to zoomCoefficient of 0.75x 
        }
        this.__zoom['in'].observe('click', function(event) {
            if (_this.zoomCoefficient < 0.25)
                _this.zoomSlider.setValue(0.9);   // zoom in from the any value below 0.25x goes to 0.25x (which is 0.9 on the slider) 
            else
                _this.zoomSlider.setValue(-(_this.zoomCoefficient - 1)*0.9);     // +0.25x
        });
        this.__zoom['out'].observe('click', function(event) {
            if (_this.zoomCoefficient <= 0.25)
                _this.zoomSlider.setValue(1);     // zoom out from 0.25x goes to the final slider position
            else
                _this.zoomSlider.setValue(-(_this.zoomCoefficient - 1.5)*0.9);   // -0.25x
        });
        // Insert all controls in the document
        this.getWorkArea().insert(this.__controls);
    },    
    
    /* To work around a bug in Raphael or Raphaelzpd (?) which creates differently sized lines
     * @ different zoom levels given the same "stroke-width" in pixels this function computes
     * the pixel size to be used at this zoom level to create a line of the correct size.
     * 
     * Returns the pixel value to be used in stoke-width
     */
    getSizeNormalizedToDefaultZoom: function(pixelSizeAtDefaultZoom) {
        return pixelSizeAtDefaultZoom;
    },

    /**
     * Returns the current zoom level (not normalized to any value, larger numbers mean deeper zoom-in)
     */
    getCurrentZoomLevel: function(pixelSizeAtDefaultZoom) {
        return this.zoomCoefficient;
    },

    /**
     * Converts the coordinates relative to the Raphael canvas to coordinates relative to the canvas div
     * and returns them
     *
     * @method canvasToDiv
     * @param {Number} canvasX The x coordinate relative to the Raphael canvas (ie with pan/zoom transformations)
     * @param {Number} canvasY The y coordinate relative to the Raphael canvas (ie with pan/zoom transformations)
     * @return {{x: number, y: number}} Object with coordinates
     */
    canvasToDiv: function(canvasX,canvasY) {
        return {
            x: this.zoomCoefficient * (canvasX - this.viewBoxX),
            y: this.zoomCoefficient * (canvasY - this.viewBoxY)
        }
    },

    /**
     * Converts the coordinates relative to the canvas div to coordinates relative to the Raphael canvas
     * by applying zoom/pan transformations and returns them.
     *
     * @method divToCanvas
     * @param {Number} divX The x coordinate relative to the canvas
     * @param {Number} divY The y coordinate relative to the canvas
     * @return {{x: number, y: number}} Object with coordinates
     */
    divToCanvas: function(divX,divY) {
        return {
            x: divX/this.zoomCoefficient + this.viewBoxX,
            y: divY/this.zoomCoefficient + this.viewBoxY
        }
    },

    /**
     * Converts the coordinates relative to the browser viewport to coordinates relative to the canvas div,
     * and returns them.
     *
     * @method viewportToDiv
     * @param {Number} absX The x coordinate relative to the viewport
     * @param {Number} absY The y coordinate relative to the viewport
     * @return {{x: number, y: number}} Object with coordinates
     */
    viewportToDiv : function (absX, absY) {
        return {
            x : + absX - this.canvas.cumulativeOffset().left,
            y : absY - this.canvas.cumulativeOffset().top
        };
    },

    /**
     * Animates a transformation of the viewbox to the given coordinate
     *
     * @method panTo
     * @param {Number} x The x coordinate relative to the Raphael canvas
     * @param {Number} y The y coordinate relative to the Raphael canvas
     */
    panTo: function(x, y, instant) {
        var me = this,
            oX = this.viewBoxX,
            oY = this.viewBoxY,
            xDisplacement = x - oX,
            yDisplacement = y - oY;
        
        if (editor.isUnsupportedBrowser()) {
            instant = true;
        }
        
        var numSeconds = instant ? 0 : .4;
        var frames     = instant ? 1 : 11;
        
        var xStep = xDisplacement/frames,
            yStep = yDisplacement/frames;
        
        if (xStep == 0 && yStep == 0) return;
        
        var progress = 0;

        (function draw() {
            setTimeout(function() {
                if(progress++ < frames) {
                    me.viewBoxX += xStep;
                    me.viewBoxY += yStep;
                    me.getPaper().setViewBox(me.viewBoxX, me.viewBoxY, me.width/me.zoomCoefficient, me.height/me.zoomCoefficient);
                    me.background.attr({x: me.viewBoxX, y: me.viewBoxY });
                    draw();        
                }
            }, 1000 * numSeconds / frames);
        })();
    },

    /**
     * Animates a transformation of the viewbox by the given delta in the X direction
     *
     * @method panTo
     * @param {Number} deltaX The move size
     */
    panByX: function(deltaX, instant) {
        this.panTo(this.viewBoxX + Math.floor(deltaX/this.zoomCoefficient), this.viewBoxY, instant);
    },

    /**
     * Adjusts the canvas size to the current viewport dimensions.
     *
     * @method adjustSizeToScreen
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
    },

    /**
     * Pans the canvas to put the node with the given id at the center.
     * 
     * When (xCenterShift, yCenterShift) are given positions the node with the given shift relative
     * to the center instead of exact center of the screen
     * 
     * @method centerAroundNode
     * @param {Number} nodeID The id of the node
     */
    centerAroundNode: function(nodeID, instant, xCenterShift, yCenterShift) {
        var node = editor.getNode(nodeID);
        if(node) {
            var x = node.getX(),
                y = node.getY();
            if (!xCenterShift) xCenterShift = 0;
            if (!yCenterShift) yCenterShift = 0;
            var xOffset = this.getWidth()/this.zoomCoefficient;
            var yOffset = this.getHeight()/this.zoomCoefficient;
            this.panTo(x - xOffset/2 - xCenterShift, y - yOffset/2 - yCenterShift, instant);
        }
    }
});
