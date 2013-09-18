function display_raw_graph(G, renderTo) {
    printObject(G);
    document.getElementById(renderTo).innerHTML =
        '<pre>'+
        'vertices:   ' + stringifyObject(G.nameToId)+'\n'+
        'edgesFromV: ' + stringifyObject(G.v)+'\n'+
        'weights:    ' + stringifyObject(G.weights) + '\n</pre>';
}


function addNewChild (form, positionedGraph, redrawRenderTo) {
    var inputs = form.getElementsByTagName('input');
    for (var index = 0; index < inputs.length; ++index) {
        console.log("Adding child to: " + inputs[index].value);
        positionedGraph.addNewChild(parseInt(inputs[index].value));
    }

    display_processed_graph(positionedGraph, redrawRenderTo);
    return false;
}

function addNewParents (form, positionedGraph, redrawRenderTo) {
    var inputs = form.getElementsByTagName('input');
    for (var index = 0; index < inputs.length; ++index) {
        console.log("Adding parents to: " + inputs[index].value);
        positionedGraph.addNewParents(parseInt(inputs[index].value));
    }

    display_processed_graph(positionedGraph, redrawRenderTo);
    return false;
}

function assignParents (form, positionedGraph, redrawRenderTo) {
    var inputs = form.getElementsByTagName('input');
    var child  = inputs[0].value;
    var parent = inputs[1].value;
    console.log("Assigning " + parent + " to be the parent of " + child);

    positionedGraph.assignParent(parent, child);

    display_processed_graph(positionedGraph, redrawRenderTo);
    return false;
}

function addNewRelationship (form, positionedGraph, redrawRenderTo) {
    var inputs = form.getElementsByTagName('input');
    for (var index = 0; index < inputs.length; ++index) {
        console.log("Adding new partner to: " + inputs[index].value);
        positionedGraph.addNewRelationship(parseInt(inputs[index].value));
    }

    display_processed_graph(positionedGraph, redrawRenderTo);
    return false;
}

function removeNode (positionedGraph, redrawRenderTo) {
    return false;
}

function repositionAll (positionedGraph, redrawRenderTo) {
    positionedGraph.repositionAll();
    display_processed_graph(positionedGraph, redrawRenderTo);
    return false;
}

function redrawAll (positionedGraph, redrawRenderTo) {
    positionedGraph.redrawAll();
    display_processed_graph(positionedGraph, redrawRenderTo);
    return false;
}

function showJSON (positionedGraph, redrawRenderTo) {
    positionedGraph.toJSON();
    return false;
}

function fromJSON (positionedGraph, redrawRenderTo) {
    var msg = prompt("Enter graph representation:");
    console.log("Read: " + msg);

    positionedGraph.fromJSON(msg);

    display_processed_graph(positionedGraph, redrawRenderTo);
    return false;
}

function checkNodeId (element) {
    //console.log("zzz2");
}

function display_controls(positionedGraph, controlRenderTo, redrawRenderTo) {
    var str = "'" + redrawRenderTo + "'";
    document.getElementById(controlRenderTo).innerHTML = "\
        <form method=\"post\" action=\"\" onsubmit=\"return assignParents(this, " + positionedGraph + ", " + str  + ")\">\
            child: <input type=\"text\" style=\"width: 40px\" name=\"nodeid1\" id=\"nodeid1\" onkeyup=\"checkNodeId(this)\"/>\
            relationship or person: <input type=\"text\" style=\"width: 40px\" name=\"nodeid2\" id=\"nodeid2\" onkeyup=\"checkNodeId(this)\"/>\
            <button style=\"width: 120px\" type=\"submit\" id=\"submitParents\">Assign Parents</button>\
        </form>\
        <form method=\"post\" action=\"\" onsubmit=\"return addNewChild(this, " + positionedGraph + ", " + str  + ")\">\
            relationship or childhub: <input type=\"text\" style=\"width: 40px\" name=\"nodeid\" id=\"nodeid\" onkeyup=\"checkNodeId(this)\"/>\
            <button style=\"width: 120px\" type=\"submit\" id=\"submitNewChild\">Add New Child</button>\
        </form>\
        <form method=\"post\" action=\"\" onsubmit=\"return addNewParents(this, " + positionedGraph + ", " + str  + ")\">\
            person: <input type=\"text\" style=\"width: 40px\" name=\"nodeid\" id=\"nodeid\" onkeyup=\"checkNodeId(this)\"/>\
            <button style=\"width: 150px\" type=\"submit\" id=\"submitNewParents\">Add New Parents</button>\
        </form>\
        <form method=\"post\" action=\"\" onsubmit=\"return addNewRelationship(this, " + positionedGraph + ", " + str  + ")\">\
            person: <input type=\"text\" style=\"width: 40px\" name=\"nodeid\" id=\"nodeid\" onkeyup=\"checkNodeId(this)\"/>\
            <button style=\"width: 150px\" type=\"submit\" id=\"submitNewRel\">Add New Relationship</button>\
        </form>\
        <form method=\"post\" action=\"\" onsubmit=\"return removeNode(this, " + positionedGraph + ", " + str  + ")\">\
            nodeId: <input type=\"text\" style=\"width: 40px\" name=\"nodeid\" id=\"nodeid\" onkeyup=\"checkNodeId(this)\"/>\
            <button style=\"width: 150px\" type=\"submit\" id=\"submitRemove\">Remove Node</button>\
        </form>\
        <form method=\"post\" action=\"\" onsubmit=\"return repositionAll(" + positionedGraph + ", " + str  + ")\">\
            <button type=\"submit\" id=\"submitReposition\">Reposition all (change X coordinate, keep order)</button>\
        </form>\
        <form method=\"post\" action=\"\" onsubmit=\"return redrawAll(" + positionedGraph + ", " + str  + ")\">\
            <button type=\"submit\" id=\"submitRedraw\">Redraw all (recompute layout)</button>\
        </form>\
        <form method=\"post\" action=\"\" onsubmit=\"return showJSON(" + positionedGraph + ", " + str  + ")\">\
            <button type=\"submit\" id=\"submitJSON\">To JSON</button>\
        </form>\
        <form method=\"post\" action=\"\" onsubmit=\"return fromJSON(" + positionedGraph + ", " + str  + ")\">\
            <button type=\"submit\" id=\"readJSON\">From JSON</button>\
        </form>"
}

function drawPersonBox ( canvas, scale, x, scaledY, width, label, sex ) {
    // x: middle of node
    // y: top of node

    var cornerRadius = 0;
    if (sex == "F")
        cornerRadius = scale.yscale * scale.yLevelSize/2;
    var fill = "#ccc";
    if (sex == "U")
        fill = "#ddd";

    var box = canvas.rect( scale.xshift + (x - width/2)*scale.xscale, scaledY, width*scale.xscale, scale.yLevelSize * scale.yscale, cornerRadius );
    box.attr({fill: fill});

    var text = canvas.text( scale.xshift + x*scale.xscale, scaledY + (scale.yLevelSize/2)*scale.yscale, label );
    //var text = canvas.text( scale.xshift + x*scale.xscale, scaledY + (scale.yLevelSize/2)*scale.yscale, x.toString() );
}

function computeChildhubHorizontalY ( scale, scaledY, targetLevel ) {
    return scaledY + (scale.yLevelSize + scale.yInterLevelGap)*scale.yscale + (targetLevel-1) * scale.yExtraPerHorizontalLevel;
}

function drawRelationshipChildhubEdge( canvas, scale, x, scaledY, targetLevel ) {
    var xx1 = scale.xshift + x*scale.xscale;
    var yy1 = scaledY + (scale.yLevelSize/2)*scale.yscale;
    var xx2 = xx1;
    var yy2 = computeChildhubHorizontalY( scale, yy1, targetLevel);
    var line = canvas.path("M " + xx1 + " " + yy1 + " L " + xx2 + " " + yy2);
    line.attr({"stroke":"#000"});
    line.translate(0.5, 0.5);
}

function drawVerticalChildLine( canvas, scale, childX, scaledY, targetLevel, scaledChildY) {
    var yy1 = scaledY + (scale.yLevelSize/2)*scale.yscale;
    var yy1 = computeChildhubHorizontalY( scale, yy1, targetLevel);
    var yy2 = scaledChildY;
    var xx1 = scale.xshift + childX*scale.xscale;
    var xx2 = xx1;
    var line = canvas.path("M " + xx1 + " " + yy1 + " L " + xx2 + " " + yy2);
    line.attr({"stroke":"#000"});
    line.translate(0.5, 0.5);
    var line2 = canvas.path("M " + (xx1+1) + " " + yy1 + " L " + (xx2+1) + " " + yy2);
    line2.attr({"stroke":"#FFF"});
    line2.translate(0.5, 0.5);
    var line3 = canvas.path("M " + (xx1-1) + " " + yy1 + " L " + (xx2-1) + " " + yy2);
    line3.attr({"stroke":"#FFF"});
    line3.translate(0.5, 0.5);
    var line4 = canvas.path("M " + (xx1+2) + " " + yy1 + " L " + (xx2+2) + " " + yy2);
    line4.attr({"stroke":"#FFF"});
    line4.translate(0.5, 0.5);
    var line5 = canvas.path("M " + (xx1-2) + " " + yy1 + " L " + (xx2-2) + " " + yy2);
    line5.attr({"stroke":"#FFF"});
    line5.translate(0.5, 0.5);
}

function drawHorizontalChildLine( canvas, scale, leftmostX, rightmostX, scaledY, targetLevel) {
    var xx1 = scale.xshift + leftmostX*scale.xscale;
    var xx2 = scale.xshift + rightmostX*scale.xscale;
    var yy1 = (scale.yLevelSize/2)*scale.yscale + computeChildhubHorizontalY( scale, scaledY, targetLevel);
    var yy2 = yy1;
    var line = canvas.path("M " + xx1 + " " + yy1 + " L " + xx2 + " " + yy2);
    line.attr({"stroke":"#000"});
    line.translate(0.5, 0.5);
    var line2 = canvas.path("M " + xx1 + " " + (yy1-1) + " L " + xx2 + " " + (yy2-1));
    line2.attr({"stroke":"#FFF"});
    line2.translate(0.5, 0.5);
    var line3 = canvas.path("M " + xx1 + " " + (yy1-2) + " L " + xx2 + " " + (yy2-2));
    line3.attr({"stroke":"#FFF"});
    line3.translate(0.5, 0.5);
}

function drawNeighbourRelationshipEdge( canvas, scale, x, scaledY, width, u_x, isBetweenRelatives ) {
    var stroke = "#000";
    if (isBetweenRelatives)
        stroke = "#A00";

    var yy1 = scaledY + (scale.yLevelSize/2)*scale.yscale;
    var yy2 = yy1;
    var xx1 = undefined;
    var xx2 = scale.xshift + u_x * scale.xscale;

    if ( u_x > x )
        xx1 = scale.xshift + (x + width/2)*scale.xscale;
    else
        xx1 = scale.xshift + (x - width/2)*scale.xscale;

    var line = canvas.path("M " + xx1 + " " + yy1 + " L " + xx2 + " " + yy2);
    line.attr({"stroke":stroke});
    line.translate(0.5, 0.5);
}

function drawVerticalRelationshipLine( canvas, scale, x, scaledY, u_x, u_scaledY, isBetweenRelatives ) {
    var stroke = "#000";
    if (isBetweenRelatives)
        stroke = "#A00";

    var xx1 = scale.xshift +   x * scale.xscale;
    var xx2 = scale.xshift + u_x * scale.xscale;

    var yy1 = scaledY;
    var yy2 = u_scaledY;

    var line2 = canvas.path("M " + (xx1+1) + " " + yy1 + " L " + (xx2+1) + " " + yy2);
    line2.attr({"stroke":"#FFF"});
    line2.translate(0.5, 0.5);
    var line3 = canvas.path("M " + (xx1-1) + " " + yy1 + " L " + (xx2-1) + " " + yy2);
    line3.attr({"stroke":"#FFF"});
    line3.translate(0.5, 0.5);
    var line4 = canvas.path("M " + (xx1+2) + " " + yy1 + " L " + (xx2+2) + " " + yy2);
    line4.attr({"stroke":"#FFF"});
    line4.translate(0.5, 0.5);
    var line5 = canvas.path("M " + (xx1-2) + " " + yy1 + " L " + (xx2-2) + " " + yy2);
    line5.attr({"stroke":"#FFF"});
    line5.translate(0.5, 0.5);
    var line = canvas.path("M " + xx1 + " " + yy1 + " L " + xx2 + " " + yy2);
    line.attr({"stroke":stroke});
    line.translate(0.5, 0.5);
}

function browserVisibleWidth(){
   return window.innerWidth||document.documentElement.clientWidth||document.body.clientWidth||0;
}

function display_processed_graph(positionedGraph, renderTo, debugPrint, debugMsg) {

    //if (!debugPrint) printObject(renderPackage);

    var G         = positionedGraph.DG.GG;
    var ranks     = positionedGraph.DG.ranks;
    var ordering  = positionedGraph.DG.order;
    var positions = positionedGraph.DG.positions;
    var consangr  = positionedGraph.DG.consangr;
    var vertLevel = positionedGraph.DG.vertLevel;
    var rankYraw  = positionedGraph.DG.rankY;

    var scale = { xscale: 4.0, yscale: 1.0, xshift: 5, yshift: 5, yLevelSize: 30, yInterLevelGap: 6, yExtraPerHorizontalLevel: 8 };

    if (debugMsg) canvas.text(50,10,debugMsg);

    // precompute Y coordinate for different ranks (so that it is readily available when drawing multi-rank edges)
    var rankYcoord = [0];
    for ( var r = 1; r < rankYraw.length; r++ ) {
        rankYcoord[r] = scale.yshift + rankYraw[r] * scale.yscale;
    }

    var maxY = (rankYcoord[ordering.order.length-1] + 200);
    var maxX = scale.xshift + (Math.max.apply(Math, positions) + Math.max.apply(Math, G.vWidth))*scale.xscale;

    var canvas = Raphael(renderTo, Math.max(browserVisibleWidth(), maxX), Math.max(100, maxY));

    // rank 0 has removed virtual nodes
    for ( var r = 1; r < ordering.order.length; r++ ) {

        var len = ordering.order[r].length;
        for ( var orderV = 0; orderV < len; orderV++ ) {
            var v = ordering.order[r][orderV];

            if (v > G.getMaxRealVertexId() || G.isChildhub(v)) continue;

            var width = G.getVertexWidth(v);
            var x     = positions[v];            // note: position has middle coordinates
            var y     = rankYcoord[r];

            if ( G.isRelationship(v) ) {
                var targetChildhub = G.getOutEdges(v)[0];

                // draw child edges from childhub
                var childEdges = G.getOutEdges(targetChildhub);

                var leftmostX  = x;
                var rightmostX = x;
                for ( var j = 0; j < childEdges.length; j++ ) {
                    var child  = childEdges[j];
                    var childX =  positions[child];
                    if (childX > rightmostX)
                        rightmostX = childX;
                    if (childX < leftmostX)
                        leftmostX = childX;

                    drawVerticalChildLine( canvas, scale, childX, y, vertLevel.childEdgeLevel[targetChildhub], rankYcoord[r+2] );
                }

                drawHorizontalChildLine( canvas, scale, leftmostX, rightmostX, y, vertLevel.childEdgeLevel[targetChildhub]);

                // only one outedge to childhub - and it is guaranteed to be a one-rank long vertical edge
                drawRelationshipChildhubEdge( canvas, scale, x, y, vertLevel.childEdgeLevel[targetChildhub] );

                continue;
            }

            if ( G.isPerson(v) ) {
                var outEdges = G.getOutEdges(v);
                for ( var j = 0; j < outEdges.length; j++ ) {
                    var u      = outEdges[j];
                    var orderU = ordering.vOrder[u];
                    var rankU  = ranks[u];

                    var u_x = positions[u];            // note: position has middle coordinates
                    var u_y = rankYcoord[rankU];

                    var consangrRelationship = false;
                    var destination = u;
                    while (destination > G.getMaxRealVertexId())
                        destination = G.getOutEdges(destination)[0];
                    if (consangr.hasOwnProperty(destination))
                        consangrRelationship = true;

                    if ( rankU == r ) {
                        if (orderU == orderV+1 || orderU == orderV-1) {
                            // draw relationship edge directly
                            drawNeighbourRelationshipEdge( canvas, scale, x, y, width, u_x, consangrRelationship );
                        }
                        else
                        {
                            // draw "long" horizontal relationship edge (which goes above some other nodes)
                            // TODO

                        }
                    }
                    else {
                        // draw "long" (multi-rank) vetrtical relationship edge
                        // TODO: collec the entire path and draw a curve instead of line segments
                        // note: always have a small horizontal part before connecting to middle of relationship node
                        var xx = x;
                        var yy = y + (scale.yLevelSize/2)*scale.yscale;
                        do {
                            drawVerticalRelationshipLine( canvas, scale, xx, yy, u_x, u_y, consangrRelationship );
                            u     = G.getOutEdges(u)[0];
                            rankU = ranks[u];
                            xx    = u_x;
                            yy    = u_y;
                            u_x   = positions[u];
                            u_y   = rankYcoord[rankU];
                            if (!G.isVirtual(G.getOutEdges(u)[0])) { u_y += (scale.yLevelSize/2)*scale.yscale; }
                        }
                        while (G.isVirtual(u))
                        yy -= (scale.yLevelSize/2)*scale.yscale;
                        //drawVerticalRelationshipLine( canvas, scale, xx, yy, u_x, u_y + (scale.yLevelSize/2)*scale.yscale, consangrRelationship );
                        drawNeighbourRelationshipEdge( canvas, scale, xx, yy, 0, u_x, consangrRelationship );
                    }
                }

                //drawPersonBox( canvas, scale, x, y, width, G.getVertexNameById(v) + "/" + v.toString() /*positions[v].toString()*/ , G.properties[v]["sex"]);
                drawPersonBox( canvas, scale, x, y, width, v.toString() + "/" + positions[v].toString() , G.properties[v]["gender"]);
                continue;
            }

                    /*
                    else {
                        // the entire long edge is handled here so that it is easie rot replace by splines or something else later on
                        var yy      = topY + 30;
                        var targetY = topY + 50;
                        var prevX   = midX;

                        while (true) {
                            var leftTargetX = positions[u] - G.getVertexHalfWidth(u);
                            var midTargetX  = 5 + leftTargetX * xScale + (G.getVertexWidth(u) * xScale)/2;

                            if (u > G.getMaxRealVertexId()) {
                                var line = canvas.path("M " + (prevX) + " " + yy + " L " + (midTargetX) +
                                                       " " + targetY);
                                line.attr({"stroke":stroke});

                                if (G.getOutEdges(u)[0] > G.getMaxRealVertexId() ) {
                                    // draw a line across the node itself (instead of a box as for real nodes)
                                    var line2 = canvas.path("M " + (midTargetX) + " " + targetY + " L " + (midTargetX) +
                                                           " " + (targetY+30));
                                    line2.attr({"stroke":stroke});
                                }
                                else { yy -= 30; }
                            }
                            else {
                                var leftTargetX  = positions[u] - G.getVertexHalfWidth(u);
                                var rightTargetX = positions[u] + G.getVertexHalfWidth(u);
                                var midTargetX  = 5 + leftTargetX * xScale + (G.getVertexWidth(u) * xScale)/2;
                                // final piece - this one goes across to the right or to the left (since multi-rank edges only connect relationship nodes)
                                // note: only possible with "relationship" nodes on the same rank
                                if ( ordering.vOrder[u] < ordering.vOrder[v] ) {   // edge to the left
                                    var line = canvas.path("M " + prevX + " " + (yy) + " L " + midTargetX + " " + (yy + 15));
                                    line.attr({"stroke":stroke});
                                }
                                else                                               // edge to the right
                                {
                                    var line = canvas.path("M " + prevX + " " + (yy) + " L " + midTargetX + " " + (yy + 15));
                                    line.attr({"stroke":stroke});
                                }
                                break;
                            }

                            v = u;
                            u = G.getOutEdges(u)[0];

                            yy      += 50;
                            targetY += 50;

                            prevX = midTargetX;
                        }
                    }
                }
            }*/
        }
    }
}


// old version which does not make any assumptions about the structure (e.g. what is on what
// rank, that relationship nodes ar eon the same rank with person nodes, etc) and thus
// is good for debugging when certain parts of the algorithm are disabled
function debug_display_processed_graph(renderPackage, renderTo, debugPrint, debugMsg) {

    //if (!debugPrint) printObject(renderPackage);

    var G         = renderPackage.convertedG;
    var ranks     = renderPackage.ranks;
    var ordering  = renderPackage.ordering;
    var positions = renderPackage.positions;
    var consangr  = renderPackage.consangr;

    var xScale = 4.0;

    var maxX = 50 + (Math.max.apply(Math, positions) + Math.max.apply(Math, G.vWidth))*xScale;
    var canvas = Raphael(renderTo, Math.max(browserVisibleWidth(), maxX), 1200);

    var curY = 20;

    if (debugMsg) canvas.text(50,10,debugMsg);

    // rank 0 has removed virtual nodes
    for ( var r = 1; r < ordering.order.length; r++ ) {

        var len = ordering.order[r].length;
        for ( var i = 0; i < len; i++ ) {
            var v = ordering.order[r][i];

            var topY   = curY;
            var leftX  = positions[v] - G.getVertexHalfWidth(v);
            var rightX = positions[v] + G.getVertexHalfWidth(v);
            var midX   = 5 + leftX * xScale + (G.getVertexWidth(v) * xScale)/2;

            if (v > G.getMaxRealVertexId()) {
                var box = canvas.rect( 5 + leftX * xScale, topY+10, 2 * xScale, 10 );
                box.attr({fill: "#ccc"});
                var text = canvas.text( midX, topY + 15, v.toString() );
                continue;
            }

            if ( v <= G.getMaxRealVertexId() ) {
                var box = canvas.rect( 5 + leftX * xScale, topY, G.getVertexWidth(v) * xScale, 30 );
                box.attr({fill: "#ccc"});
            }

            if ( v <= G.getMaxRealVertexId() || debugPrint )
            {
                //var text = canvas.text( midX, topY + 15, G.getVertexNameById(v) );
                var text = canvas.text( midX, topY + 15, v.toString() + "/" + positions[v].toString() );
                //var text = canvas.text( midX, topY + 15, midX.toString() );
            }

            var outEdges = G.getOutEdges(v);

            for ( var j = 0; j < outEdges.length; j++ ) {
                var u = outEdges[j];

                var leftTargetX  = positions[u] - G.getVertexHalfWidth(u);
                var rightTargetX = positions[u] + G.getVertexHalfWidth(u);
                var midTargetX  = 5 + leftTargetX * xScale + (G.getVertexWidth(u) * xScale)/2;

                var stroke = "#000";
                var destination = u;
                while (destination > G.getMaxRealVertexId())
                    destination = G.getOutEdges(destination)[0];
                if (consangr.hasOwnProperty(destination))
                    stroke = "#F00";

                if ( ranks[u] == ranks[v] )  // edge across
                {
                    // note: only possible with "relationship" nodes on the same rank
                    if ( ordering.vOrder[u] < ordering.vOrder[v] ) {   // edge to the left
                        var line = canvas.path("M " + (5+leftX*xScale) + " " + (topY + 15) + " L " + (5+rightTargetX*xScale) + " " + (topY + 15));
                        line.attr({"stroke":stroke});
                    }
                    else {                                             // edge to the right
                        var line = canvas.path("M " + (5+rightX*xScale) + " " + (topY + 15) + " L " + (5+leftTargetX*xScale) + " " + (topY + 15));
                        line.attr({"stroke":stroke});
                    }
                }
                else                         // edge below
                {
                    if (u <= G.getMaxRealVertexId()){
                        var startX = midX;
                        if (midTargetX < midX) { midTargetX += 2; startX -= 2; }
                        if (midTargetX > midX) { midTargetX -= 2; startX += 2; }
                        var initLineY = topY+30;
                        var line = canvas.path("M " + (startX) + " " + initLineY + " L " + (midTargetX) +
                                               " " + (topY + 50));
                        line.attr({"stroke":stroke});
                    }
                    else {
                        // the entire long edge is handled here so that it is easie rot replace by splines or something else later on
                        var yy      = topY + 30;
                        var targetY = topY + 50;
                        var prevX   = midX;

                        while (true) {
                            var leftTargetX = positions[u] - G.getVertexHalfWidth(u);
                            var midTargetX  = 5 + leftTargetX * xScale + (G.getVertexWidth(u) * xScale)/2;

                            if (u > G.getMaxRealVertexId() || ranks[u] != ranks[v]) {
                                var line = canvas.path("M " + (prevX) + " " + yy + " L " + (midTargetX) +
                                                       " " + targetY);
                                line.attr({"stroke":stroke});

                                if (u > G.getMaxRealVertexId() && G.getOutEdges(u)[0] > G.getMaxRealVertexId() ) {
                                    // draw a line across the node itself (instead of a box as for real nodes)
                                    var line2 = canvas.path("M " + (midTargetX) + " " + targetY + " L " + (midTargetX) +
                                                           " " + (targetY+30));
                                    line2.attr({"stroke":stroke});
                                }
                                else { yy -= 30; }

                                if (u <= G.getMaxRealVertexId()) break;
                            }
                            else {
                                var leftTargetX  = positions[u] - G.getVertexHalfWidth(u);
                                var rightTargetX = positions[u] + G.getVertexHalfWidth(u);
                                var midTargetX  = 5 + leftTargetX * xScale + (G.getVertexWidth(u) * xScale)/2;
                                // final piece - this one goes across to the right or to the left (since multi-rank edges only connect relationship nodes)
                                // note: only possible with "relationship" nodes on the same rank
                                if ( ordering.vOrder[u] < ordering.vOrder[v] ) {   // edge to the left
                                    var line = canvas.path("M " + prevX + " " + (yy) + " L " + midTargetX + " " + (yy + 15));
                                    line.attr({"stroke":stroke});
                                }
                                else                                               // edge to the right
                                {
                                    var line = canvas.path("M " + prevX + " " + (yy) + " L " + midTargetX + " " + (yy + 15));
                                    line.attr({"stroke":stroke});
                                }
                                break;
                            }

                            v = u;
                            u = G.getOutEdges(u)[0];

                            yy      += 50;
                            targetY += 50;

                            prevX = midTargetX;
                        }
                    }
                }
            }
        }

        curY += 50;
    }
}
//-----------------------------------------------

function stringifyObject(obj) {
    return _printObjectInternal(obj, 1);
}

function printObject(obj) {
    console.log( _printObjectInternal(obj, 0) );
}

function _printObjectInternal(o, level) {
    var output = '';

    if (typeof o == 'object')
    {

        if (Object.prototype.toString.call(o) === '[object Array]' ||
            o instanceof Uint32Array)
        {
            output = '[';
            for (var i = 0; i < o.length; i++) {
                if (i > 0) output += ', ';
                output += _printObjectInternal(o[i], level+1);
            }
            output += ']';
        }
        else
        {
            output = '{';
            var idx = 0;
            if (level == 0) output += '\n';
            for (property in o) {
                if (!o.hasOwnProperty(property)) continue;

                if (level != 0 && idx != 0 )
                    output += ', ';
                output += property + ': ' + _printObjectInternal(o[property], level+1);

                if (level == 0)
                    output += '\n';
                idx++;
            }
            output += '}';
        }
    }
    else
        output = ''+o;

    return output;
}

