
function sector(canvas, xPosition, yPosition, radius, gender, startAngle, endAngle, color) {
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
        var shape = sector(paper, cx, cy, r* (Math.sqrt(3)/2), "M", startAngle, endAngle, color);
        shape.transform(["...r-45,", cx , cy]).attr(shapeAttributes);
        return shape;
    }
}

function generateOrb(canvas, x, y, r, hue) {
    hue = hue || 0;
    return canvas.set(
        canvas.ellipse(x, y, r, r).attr({fill: "r(.5,.9)hsb(" + hue + ", 1, .75)-hsb(" + hue + ", .5, .25)", stroke: "none"}),
        canvas.ellipse(x, y, r - r / 5, r - r / 20).attr({stroke: "none", fill: "r(.5,.1)#ccc-#ccc", opacity: 0})
    );
}


Raphael.st.flatten = function () {
    var flattenedSet = new Raphael.st.constructor();
        this.forEach(function(element) {
            flattenedSet.concat(element.flatten());
        });
    return flattenedSet;
};

Raphael.el.flatten = function () {
    return this.paper.set(this);
};

Raphael.st.concat = function (set) {
    var _this = this;
    if(typeof(set.forEach) == 'function') {
        set.forEach(function(element) {
            _this.push(element);
        });
    }
    else {
        _this.push(set);
    }
};

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