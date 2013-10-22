function display_raw_graph(G, renderTo) {
    printObject(G);
    document.getElementById(renderTo).innerHTML =
        '<pre>'+
        'edgesFromV: ' + stringifyObject(G.v) + '\n' +
        'weights:    ' + stringifyObject(G.weights) +'\n' +
        'properties: ' + stringifyObject(G.properties) + '\n</pre>';
}


function addNewChild (form, positionedGraph, redrawRenderTo) {
    var inputs = form.getElementsByTagName('input');
    var parent = parseInt(inputs[0].value);

    if (positionedGraph.isPerson(parent))
        positionedGraph.addNewRelationship(parent);
    else
        positionedGraph.addNewChild(parent);

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

function assignPartner (form, positionedGraph, redrawRenderTo) {
    var inputs = form.getElementsByTagName('input');
    var person1 = inputs[0].value;
    var person2 = inputs[1].value;
    console.log("Assigning " + person1 + " to be the partner of " + person2);

    positionedGraph.assignPartner(person1, person2, {});

    display_processed_graph(positionedGraph, redrawRenderTo);
    return false;
}

function addTwin (form, positionedGraph, redrawRenderTo) {
    var inputs = form.getElementsByTagName('input');
    var person = inputs[0].value;
    console.log("Adding a twin to " + person);

    positionedGraph.addTwin(person, { "gender": "U" });

    display_processed_graph(positionedGraph, redrawRenderTo);
    return false;
}

function addNewRelationship (form, positionedGraph, redrawRenderTo) {
    if (!preferLeft) preferLeft = false;
    var inputs = form.getElementsByTagName('input');
    var person     = parseInt(inputs[0].value);
    var preferLeft = (parseInt(inputs[1].value) == 1);
    console.log("Adding new partner to: " + person + ", left: " + inputs[1].value);
    positionedGraph.addNewRelationship(person, {}, preferLeft);

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

function clearAll (positionedGraph, redrawRenderTo) {
    positionedGraph.clearAll();
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
        <form method=\"post\" action=\"\" onsubmit=\"return assignPartner(this, " + positionedGraph + ", " + str  + ")\">\
            person1: <input type=\"text\" style=\"width: 40px\" name=\"nodeid1\" id=\"nodeid1\" onkeyup=\"checkNodeId(this)\"/>\
            person2: <input type=\"text\" style=\"width: 40px\" name=\"nodeid2\" id=\"nodeid2\" onkeyup=\"checkNodeId(this)\"/>\
            <button style=\"width: 120px\" type=\"submit\" id=\"submitPartner\">Assign Partner</button>\
        </form>\
        <form method=\"post\" action=\"\" onsubmit=\"return assignParents(this, " + positionedGraph + ", " + str  + ")\">\
            child: <input type=\"text\" style=\"width: 40px\" name=\"nodeid1\" id=\"nodeid1\" onkeyup=\"checkNodeId(this)\"/>\
            relationship or person: <input type=\"text\" style=\"width: 40px\" name=\"nodeid2\" id=\"nodeid2\" onkeyup=\"checkNodeId(this)\"/>\
            <button style=\"width: 120px\" type=\"submit\" id=\"submitParents\">Assign Parents</button>\
        </form>\
        <form method=\"post\" action=\"\" onsubmit=\"return addNewChild(this, " + positionedGraph + ", " + str  + ")\">\
            any node: <input type=\"text\" style=\"width: 40px\" name=\"nodeid\" id=\"nodeid\" onkeyup=\"checkNodeId(this)\"/>\
            <button style=\"width: 120px\" type=\"submit\" id=\"submitNewChild\">Add New Child</button>\
        </form>\
        <form method=\"post\" action=\"\" onsubmit=\"return addTwin(this, " + positionedGraph + ", " + str  + ")\">\
            person: <input type=\"text\" style=\"width: 40px\" name=\"nodeid\" id=\"nodeid\" onkeyup=\"checkNodeId(this)\"/>\
            <button style=\"width: 120px\" type=\"submit\" id=\"submitNewChild\">Add Twin</button>\
        </form>\
        <form method=\"post\" action=\"\" onsubmit=\"return addNewParents(this, " + positionedGraph + ", " + str  + ")\">\
            person: <input type=\"text\" style=\"width: 40px\" name=\"nodeid\" id=\"nodeid\" onkeyup=\"checkNodeId(this)\"/>\
            <button style=\"width: 150px\" type=\"submit\" id=\"submitNewParents\">Add New Parents</button>\
        </form>\
        <form method=\"post\" action=\"\" onsubmit=\"return addNewRelationship(this, " + positionedGraph + ", " + str  + ")\">\
            person: <input type=\"text\" style=\"width: 40px\" name=\"nodeid\" id=\"nodeid\" onkeyup=\"checkNodeId(this)\"/>\
            <input type=\"hidden\" name=\"left\" id=\"left\" value=\"0\"/>\
            <button style=\"width: 150px\" type=\"submit\" id=\"submitNewRel\" onclick=\"this.form.left.value=0;\">Add New Relationship</button>\
            <button style=\"width: 150px\" type=\"submit\" id=\"submitNewRelLeft\" onclick=\"this.form.left.value=1;\">(on the left)</button>\
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
        </form>\
        <form method=\"post\" action=\"\" onsubmit=\"return clearAll(" + positionedGraph + ", " + str  + ")\">\
            <button type=\"submit\" id=\"clear\">Clear</button>\
        </form>";
}


function drawNodeBox ( canvas, scale, x, scaledY, width, heightPercent, label ) {

    var fill = "#ddd";
    var skip = scale.yLevelSize * scale.yscale * (1 - heightPercent)/2;

    var box = canvas.rect( scale.xshift + (x - width/2)*scale.xscale, scaledY + skip, width*scale.xscale, scale.yLevelSize * scale.yscale * heightPercent ).toFront();
    box.attr({fill: fill});

    var text = canvas.text( scale.xshift + x*scale.xscale, scaledY + (scale.yLevelSize/2)*scale.yscale, label ).toFront();
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

    var box = canvas.rect( scale.xshift + (x - width/2)*scale.xscale, scaledY, width*scale.xscale, scale.yLevelSize * scale.yscale, cornerRadius ).toFront();
    box.attr({fill: fill});

    var text = canvas.text( scale.xshift + x*scale.xscale, scaledY + (scale.yLevelSize/2)*scale.yscale, label ).toFront();
    //var text = canvas.text( scale.xshift + x*scale.xscale, scaledY + (scale.yLevelSize/2)*scale.yscale, x.toString() );
}

function computeChildhubHorizontalY ( scale, scaledY, targetLevel ) {
    return scaledY + (scale.yLevelSize + scale.yInterLevelGap)*scale.yscale + (targetLevel-1) * scale.yExtraPerHorizontalLevel *scale.yscale;
}

function drawRelationshipChildhubEdge( canvas, scale, x, scaledY, targetLevel ) {
    var xx1 = scale.xshift + x*scale.xscale;
    var yy1 = scaledY + (scale.yLevelSize/2)*scale.yscale;
    var xx2 = xx1;
    var yy2 = computeChildhubHorizontalY( scale, yy1, targetLevel);
    var line = canvas.path("M " + xx1 + " " + yy1 + " L " + xx2 + " " + yy2).toBack();
    line.attr({"stroke":"#000"});
    line.translate(0.5, 0.5);
}

function drawVerticalChildLine( canvas, scale, childX, scaledY, targetLevel, scaledChildY) {
    var yy1 = scaledY + (scale.yLevelSize/2)*scale.yscale;
    var yy1 = computeChildhubHorizontalY( scale, yy1, targetLevel);
    var yy2 = scaledChildY;
    var xx1 = scale.xshift + childX*scale.xscale;
    var xx2 = xx1;
    var line = canvas.path("M " + xx1 + " " + yy1 + " L " + xx2 + " " + yy2).toBack();
    line.attr({"stroke":"#000"});
    line.translate(0.5, 0.5);
}

function drawHorizontalChildLine( canvas, scale, leftmostX, rightmostX, scaledY, targetLevel) {
    var xx1 = scale.xshift + leftmostX*scale.xscale;
    var xx2 = scale.xshift + rightmostX*scale.xscale;
    var yy1 = (scale.yLevelSize/2)*scale.yscale + computeChildhubHorizontalY( scale, scaledY, targetLevel);
    var yy2 = yy1;
    var line = canvas.path("M " + xx1 + " " + yy1 + " L " + xx2 + " " + yy2).toBack();
    line.attr({"stroke":"#000"});
    line.translate(0.5, 0.5);
}

function drawRelationshipLine( canvas, scale, x1, scaledY1, x2, scaledY2, isBetweenRelatives ) {
    var stroke = "#000";
    if (isBetweenRelatives)
        stroke = "#A00";

    var xx1 = scale.xshift + x1 * scale.xscale
    var xx2 = scale.xshift + x2 * scale.xscale;

    var line = canvas.path("M " + xx1 + " " + scaledY1 + " L " + xx2 + " " + scaledY2).toBack();
    line.attr({"stroke":stroke});
    line.translate(0.5, 0.5);
}

function browserVisibleWidth(){
   return window.innerWidth||document.documentElement.clientWidth||document.body.clientWidth||0;
}

function display_processed_graph(positionedGraph, renderTo, debugPrint, debugMsg) {

    var DISPLAYDEBUG = false;

    positionedGraph.DG.displayDebug = true;
    positionedGraph.DG.displayGraph(positionedGraph.DG.positions, "after improvement");
    positionedGraph.DG.displayDebug = false;

    //if (!debugPrint) printObject(renderPackage);

    var G         = positionedGraph.DG.GG;
    var ranks     = positionedGraph.DG.ranks;
    var ordering  = positionedGraph.DG.order;
    var positions = positionedGraph.DG.positions;
    var consangr  = positionedGraph.DG.consangr;
    var vertLevel = positionedGraph.DG.vertLevel;
    var rankYraw  = positionedGraph.DG.rankY;

    var scale = { xscale: 4.0, yscale: 2.0, xshift: 5, yshift: 5, yLevelSize: 15, yInterLevelGap: 2, yExtraPerHorizontalLevel: 4 };

    if (debugMsg) canvas.text(50,10,debugMsg);

    // precompute Y coordinate for different ranks
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
                if (DISPLAYDEBUG)
                    drawNodeBox(canvas, scale, x, y, width, 0.75, v.toString() + "/" + positions[v].toString());

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
                    var destination = G.downTheChainUntilNonVirtual(u);
                    if (consangr.hasOwnProperty(destination))
                        consangrRelationship = true;

                    var xprev = x;
                    var yprev = y + (scale.yLevelSize/2)*scale.yscale;  // line goes in the middle of a node

                    if ( rankU == r ) {
                        // draw horizontal relationship edge (which may go at different levels depending on attachmentPort)
                        var relLineInfo = positionedGraph.getRelationshipLineInfo( destination, v );
                        var yShift = relLineInfo.attachmentPort*5;

                        drawRelationshipLine( canvas, scale, x, yprev-yShift, u_x, yprev-yShift, consangrRelationship );
                        if (yShift != 0)
                            drawRelationshipLine( canvas, scale, u_x, yprev-yShift, u_x, yprev, consangrRelationship );

                        if (DISPLAYDEBUG && G.isVirtual(u))
                            drawNodeBox(canvas, scale, u_x, y, 2, 0.4, u.toString() + "/" + u_x.toString());

                        xprev = u_x;
                        u     = G.getOutEdges(u)[0];
                        u_x   = positions[u];
                        u_y   = rankYcoord[ranks[u]];
                    }

                    if (u <= G.getMaxRealVertexId()) continue;

                    // draw "long" (multi-rank) vetrtical relationship edge
                    do {
                        if (DISPLAYDEBUG)
                            drawNodeBox(canvas, scale, u_x, u_y, 2, 0.4, u.toString() + "/" + u_x.toString());

                        if (!G.isVirtual(G.getOutEdges(u)[0])) { u_y += (scale.yLevelSize/2)*scale.yscale; }

                        drawRelationshipLine( canvas, scale, xprev, yprev, u_x, u_y, consangrRelationship );

                        xprev = u_x;
                        yprev = u_y;

                        u     = G.getOutEdges(u)[0];
                        u_x   = positions[u];
                        u_y   = rankYcoord[ranks[u]];
                    }
                    while (G.isVirtual(u));

                    u_y += (scale.yLevelSize/2)*scale.yscale;
                    drawRelationshipLine( canvas, scale, xprev, yprev, u_x, u_y, consangrRelationship );
                }

                //drawPersonBox( canvas, scale, x, y, width, G.getVertexNameById(v) + "/" + v.toString(), G.properties[v]["sex"]);
                drawPersonBox( canvas, scale, x, y, width, v.toString() + "/" + positions[v].toString() , G.properties[v]["gender"]);
                continue;
            }
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
    var canvas = Raphael(renderTo, Math.max(browserVisibleWidth(), maxX), 600);

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
                var currentV = v;
                var u        = outEdges[j];

                var leftTargetX  = positions[u] - G.getVertexHalfWidth(u);
                var rightTargetX = positions[u] + G.getVertexHalfWidth(u);
                var midTargetX  = 5 + leftTargetX * xScale + (G.getVertexWidth(u) * xScale)/2;

                var stroke = "#000";
                var destination = u;
                while (destination > G.getMaxRealVertexId())
                    destination = G.getOutEdges(destination)[0];
                if (consangr.hasOwnProperty(destination))
                    stroke = "#F00";

                //console.log("edge " + v + " to " + u);
                if ( ranks[u] == ranks[currentV] )  // edge across
                {
                    // note: only possible with "relationship" nodes on the same rank
                    if ( ordering.vOrder[u] < ordering.vOrder[currentV] ) {   // edge to the left
                        var line = canvas.path("M " + (5+leftX*xScale) + " " + (topY + 15) + " L " + (5+rightTargetX*xScale) + " " + (topY + 15));
                        line.attr({"stroke":stroke});
                    }
                    else {                                             // edge to the right
                        var line = canvas.path("M " + (5+rightX*xScale) + " " + (topY + 15) + " L " + (5+leftTargetX*xScale) + " " + (topY + 15));
                        line.attr({"stroke":stroke});
                    }

                    if (u > G.getMaxRealVertexId())
                    {
                        // TODO
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

                            if (u > G.getMaxRealVertexId() || ranks[u] != ranks[currentV]) {
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
                                if ( ordering.vOrder[u] < ordering.vOrder[currentV] ) {   // edge to the left
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

                            currentV = u;
                            u        = G.getOutEdges(u)[0];

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
    if (level > 10) return "...[too deep]...";

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
    else if (typeof o == 'string') {
        output = "'" + o + "'";
    }
    else
        output = ''+o;

    return output;
}

