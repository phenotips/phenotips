/**
 * Returns a raphael element representing a Pi-Chart-like slice of the icon representing the given gender
 *
 * @param canvas Raphael paper object
 * @param {Number} xPosition
 * @param {Number} yPosition
 * @param {Number} radius Radius of the associated shape
 * @param {String} gender Can be "M", "F" or "U"
 * @param {Number} startAngle Has to be equal or greater than 0
 * @param {Number} endAngle
 * @param {String} color CSS color
 * @return {Raphael.el}
 */
 define([], function(){
    var GraphicHelpers = {};

    GraphicHelpers.sector = function(canvas, xPosition, yPosition, radius, gender, startAngle, endAngle, color) {
        var sectorPath,
            gen = gender,
            cx = xPosition,
            cy = yPosition,
            r = radius,
            paper = canvas,
            rad = Math.PI / 180,
            shapeAttributes = {fill: color, 'stroke-width':.0 };

        //returns coordinates of the point on the circle (with radius = _radius) at angle alpha
        var circleCoordinate = function(alpha) {
                var x = cx + r * Math.cos(-alpha * rad),
                    y = cy + r * Math.sin(-alpha * rad);
                return [x,y];
            };
       
        if (gen === 'F') {
            if(endAngle-startAngle == 360)
            {
                return paper.circle(cx, cy, r).attr(shapeAttributes);
            }
            var x1 = circleCoordinate(startAngle)[0],
                x2 = circleCoordinate(endAngle)[0],
                y1 = circleCoordinate(startAngle)[1],
                y2 = circleCoordinate(endAngle)[1];

            return paper.path(["M", cx, cy, "L", x1, y1, "A", r, r, 0, +(endAngle - startAngle > 180), 0, x2, y2, "z"]).attr(shapeAttributes);
        }
        else if(gen === 'M') {
            //returns the side of the square on which the coordinate exists. Sides are numbered 0-3 counter-clockwise,
            //starting with the right side
            function sideAtAngle(angle) {
                return (((angle + 45)/90).floor()) % 4;
            }

            //returns the tangent value of the parameter degrees
            function tanOfDegrees(degrees) {
                var radians = degrees * Math.PI/180;
                return Math.tan(radians);
            }

            //returns the coordinate of point at angle alpha on the square
            function getCoord(alpha) {
                var side = sideAtAngle(alpha);
                var result = {};
                var xFactor = (side % 2);
                var yFactor = (1 - side % 2);
                var sideFactor = side % 3 ? -1 : 1;

                result.angle = (alpha - side * 90 - ((side == 0 && alpha > 45) ? 360 : 0)) * (side < 2 ? -1 : 1);
                // Find the distance from the middle of the line
                var d = r * tanOfDegrees(result.angle);
                // Compute the coordinates
                result.x = cx + xFactor * d + yFactor * sideFactor * r;
                result.y = cy + yFactor * d + xFactor * sideFactor * r;
                return result;
            }

            //returns the coordinate of the next corner (going counter-clockwise, and starting with side given in the
            //parameter
            function getNextCorner(side) {
                var factorA = (side % 3) ? -1: 1,
                    factorB = (side < 2) ? -1: 1,
                    result = {};
                result.x = cx + factorA * r;
                result.y = cy + factorB * r;
                return result;
            }

            var startSide = sideAtAngle(startAngle),
                endSide = sideAtAngle(endAngle);
                if(endSide == 0 && endAngle > startAngle) {
                    endSide = (startAngle >= 315) ? 0 : 4;
                }
                var numSides = endSide - startSide;

            var startCoord = getCoord(startAngle),
                endCoord = getCoord(endAngle),
                sectorPathData = ["M", endCoord.x, endCoord.y, "L", cx, cy, "L", startCoord.x, startCoord.y],
                currentSide = startSide;

            while(numSides > 0)
            {
                sectorPathData.push("L", getNextCorner(currentSide).x + " " + getNextCorner(currentSide).y);
                currentSide = (++currentSide) % 4;
                numSides--;
            }
            sectorPathData.push("L",endCoord.x, endCoord.y, "z");
            return paper.path(sectorPathData).attr(shapeAttributes);
        }
        else {
            var shape = GraphicHelpers.sector(paper, cx, cy, r* (Math.sqrt(3)/2), "M", startAngle, endAngle, color);
            shape.transform(["...r-45,", cx , cy]).attr(shapeAttributes);
            return shape;
        }
    }

    /**
     * Creates a 3D looking orb
     *
     * @method generateOrb
     * @param canvas Raphael paper
     * @param {Number} x X coordinate for the orb
     * @param {Number} y Y coordinate for the orb
     * @param {Number} r Radius of the orb
     * @param {Number} hue Hue value between 0 and 1
     * @return {Raphael.st}
     */
    GraphicHelpers.generateOrb = function (canvas, x, y, r, gender) {
        if (!gender || gender == 'F') {
            return canvas.set(
                    canvas.ellipse(x, y, r, r),
                    canvas.ellipse(x, y, r - r / 5, r - r / 20).attr({stroke: "none", fill: "r(.5,.1)#ccc-#ccc", opacity: 0})
                );        
        }        
        
        if (gender == "M") {
            var rr = r - 1;
            return canvas.set(                                
                    canvas.rect(x-rr, y-rr, rr*2, rr*2, 0),
                    canvas.rect(x-rr, y-rr, rr*2, rr*2, 1).attr({stroke: "none", fill: "330-#ccc-#ccc", opacity: 0})
                );
        }
        
        if (gender == "U") {
            var rr = (r-1) * 0.9;
            return canvas.set(                                
                    canvas.rect(x-rr, y-rr, rr*2, rr*2, 0).attr({transform: "r45"}),
                    canvas.rect(x-rr, y-rr, rr*2, rr*2, 1).attr({stroke: "none", fill: "330-#ccc-#ccc", opacity: 0}).attr({transform: "r45"})
                );        
        }   
    }

    /**
     * Draws a quarter-circle-like curve connecting xFrom,Yfrom and xTo,yTo
     * with the given attributes and bend (upwars or downwards)
     * 
     * Iff "doubleCurve" is true, cones the curve and shifts one curve by (shiftx1, shifty1) and the other by (shiftx2, shifty2)
     */
    GraphicHelpers.drawCornerCurve = function (xFrom, yFrom, xTo, yTo, bendDown, attr, doubleCurve, shiftx1, shifty1, shiftx2, shifty2 ) {
        var xDistance = xTo - xFrom;
        var yDistance = yFrom - yTo;
        
        var dist1x = xDistance/2;
        var dist2x = xDistance/10;
        var dist1y = yDistance/2;
        var dist2y = yDistance/10;
            
        var curve;
        
        if (bendDown) {
            var raphaelPath =  "M " + (xFrom)          + " " + (yFrom) +
                              " C " + (xFrom + dist1x) + " " + (yFrom + dist2y) +
                                " " + (xTo   + dist2x) + " " + (yTo   + dist1y) +
                                " " + (xTo)            + " " + (yTo);
            curve = editor.getPaper().path(raphaelPath).attr(attr).toBack();                            
        } else {
            var raphaelPath =   "M " + (xFrom)          + " " + (yFrom) +
                               " C " + (xFrom - dist2x) + " " + (yFrom - dist1y) +
                                 " " + (xTo   - dist1x) + " " + (yTo   - dist2y) +
                                 " " + (xTo)            + " " + (yTo);
            curve = editor.getPaper().path(raphaelPath).attr(attr).toBack();
        }
        
        if (doubleCurve) {
            var curve2 = curve.clone().toBack();
            curve .transform("t " + shiftx1  + "," + shifty1 + "...");
            curve2.transform("t " + shiftx2 + "," + shifty2 + "..."); 
        }
    }

    GraphicHelpers.drawLevelChangeCurve = function (xFrom, yFrom, xTo, yTo, attr, doubleCurve, shiftx1, shifty1, shiftx2, shifty2 )
    {
        var xDistance = xTo - xFrom;   
        var dist1x    = xDistance/2;
        
        var raphaelPath = " M " + (xFrom)           + " " + yFrom;                    
        raphaelPath    += " C " + (xFrom + dist1x)  + " " + (yFrom) +
                            " " + (xTo   - dist1x)  + " " + (yTo) +
                            " " + (xTo)             + " " + (yTo);

        var curve = editor.getPaper().path(raphaelPath).attr(attr).toBack();
        if (doubleCurve) {
            var curve2 = curve.clone().toBack();
            curve .transform("t " + shiftx1  + "," + shifty1 + "...");
            curve2.transform("t " + shiftx2 + "," + shifty2 + "..."); 
        }    
    }

    /**
     * Computes the intersection point between a horizontal line @ y == crossY and a line from x1,y1 to x2,y2
     */
    GraphicHelpers.findXInterceptGivenLineAndY = function(crossY, x1, y1, x2, y2) {
        // y = ax + b   
        if (x1 == x2) return x1;
        var a = (y1 - y2)/(x1 - x2);
        var b = y1 - a*x1;    
        var interceptX = (crossY - b)/a;
        return interceptX;
    }

    GraphicHelpers.getElementHalfHeight = function(t) {
        return Math.floor(t.getBBox().height/2);
    }

    GraphicHelpers.getCaretPosition = function(elem) {
        if (typeof elem.selectionEnd !== "undefined") {
            return elem.selectionEnd;
        } else if (elem.createTextRange && document.selection) {
            var r = document.selection.createRange();
            r.moveStart('character', -elem.value.length);
            return r.text.length;
        } else return null;
    }

    GraphicHelpers.setCaretPosition = function(ctrl, pos)
    {
        if (pos == null || pos <= 0) {
            return;
        }
        if(ctrl.setSelectionRange)
        {
            ctrl.focus();
            ctrl.setSelectionRange(pos,pos);
        }
        else if (ctrl.createTextRange) {
            var range = ctrl.createTextRange();
            range.collapse(true);
            range.moveEnd('character', pos);
            range.moveStart('character', pos);
            range.select();
        }
    }

    /**
     * Joins all the subsets into one set and returns it.
     * @return {Raphael.st}
     */
    Raphael.st.flatten = function () {
        var flattenedSet = new Raphael.st.constructor();
            this.forEach(function(element) {
                flattenedSet = flattenedSet.concat(element.flatten());
            });
        return flattenedSet;
    };

    /**
     * Returns set containing the given element
     * @return {Raphael.st}
     */
    Raphael.el.flatten = function () {
        return this.paper.set(this);
    };

    /**
     * Returns a set containing the elements of this set and the given set. Doesn't modify the original sets.
     * @param {Raphael.st} set
     * @return {Raphael.st}
     */
    Raphael.st.concat = function (set) {
        var newSet = this.copy();
        if(typeof(set.forEach) == 'function') {
            set.forEach(function(element) {
                newSet.push(element);
            });
        }
        else {
            newSet.push(set);
        }
        return newSet;
    };

    /**
     * Returns True if this set contains target. Target has to be directly in this set, and not in a subset.
     *
     * @param {Raphael.st|Raphael.el} target
     * @return {boolean}
     */
    Raphael.st.contains = function (target) {
        var found = false;
        this.forEach(function(element) {
            if(element == target) {
                found = true;
            }
        });
        return found;
    };

    /**
     * Returns a new set containing the same elements as this set
     * @return {Raphael.st}
     */
    Raphael.st.copy = function() {
        var newSet = new Raphael.st.constructor();
        this.forEach(function(element) {
            newSet.push(element);
        });
        return newSet;
    };

    /**
     * Attempts to make resizableElement in the dialog modal fit on screen by adjusting the resizableElement height
     *
     * @param {String} dialogSelectorClass
     * @param {String} resizableElementId
     * @param {Number} minResizableElementHeight
     * @param {Number} maxResizableElementHeight
     */
    GraphicHelpers.adjustPreviewWindowHeight = function(dialogSelectorClass, resizableElementId, minResizableElementHeight, maxResizableElementHeight) {
        var pedigreeDialogue = $$('.'+dialogSelectorClass)[0];
        var resizableElement = $(resizableElementId);
        if (!pedigreeDialogue || !resizableElement) {
            return;
        }
        var screenHeight = window.innerHeight - 10;
        var dialogueHeight = pedigreeDialogue.getHeight();
        var freeSpace = screenHeight - dialogueHeight;
        var resizableElementHeight = resizableElement.getHeight();
        if (freeSpace < 0) {
            var newHeight = Math.max(minResizableElementHeight, resizableElementHeight + freeSpace);
            resizableElement.style.height = newHeight + "px";
        }
        if (freeSpace > 0 && resizableElementHeight < maxResizableElementHeight && resizableElementHeight < resizableElement.scrollHeight) {
            var newHeight = Math.min(maxResizableElementHeight, resizableElementHeight + freeSpace);
            resizableElement.style.height = newHeight + "px";
        }
    },

    //Animation helpers
    window.requestAnimFrame = (function(){
        return  window.requestAnimationFrame   ||
            window.webkitRequestAnimationFrame ||
            window.mozRequestAnimationFrame    ||
            window.oRequestAnimationFrame      ||
            window.msRequestAnimationFrame     ||
            function( callback ){
                window.setTimeout(callback, 1000 / 60);
            };
    })();

    return GraphicHelpers;
});
