lineD3 = function module() {

  var dispatch = d3.dispatch("d3brush", "d3rendered");

  var debug = false;

  var KIND_LINE = "line";
  var KIND_AREA = "area";

  var kind = KIND_LINE;

  var x = null;
  var y = null;
  var maxDepth = null;
  var container = null;

  var formatter = d3.format(',');


  var theData = null;

  var pos    = function(d) { return d.pos };
  var depth  = function(d) { return d.depth };

  var getScreenCoords = function(x, y, ctm) {
    var xn = ctm.e + x*ctm.a;
    var yn = ctm.f + y*ctm.d;
    return { x: xn, y: yn };
  }

  var formatCircleText = function(pos, depth) {
        return pos + ',' + depth;
  }
  var showCircle = function(start, theDepth) {
    if (container == null) {
      return;
    }
    // Find the closest position in the data
    d = null;
    if (theData) {
      for (var i = 0; i < theData.length - 1; i++) {
        if (start >= pos(theData[i])  &&  start <= pos(theData[i+1])) {
          d = theData[i];
          break;
        }
      }
    }


    // Get the x for this position
    if (d) {
      var mousex = d3.round(x(pos(d)));
      var mousey = d3.round(y(depth(d)));
      var posx = d3.round(pos(d));
      var depthy = d3.round(depth(d));

      var invertedx = x.invert(mousex); 
      var invertedy = y.invert(mousey); 

      if (theDepth == null || theDepth == "") {
        theDepth = depthy.toString();
      }
      var circleText = formatCircleText(posx, theDepth);
      if (debug) {
        circleText += ' ' + posx + ':' + depthy + ' ' + invertedx + ':' + invertedy;
      }

      var label = container.select(".circle-label");
      label.transition()
           .duration(200)
          .style("opacity", 1);
      label.attr("x", 0)
           .attr("y", margin.top + 5)
           .attr("class", "circle-label")           
           .text(circleText);

      container.select(".circle-label")
               .attr( "x", function (d,i) {
                  var w = this.getBBox().width;
                  var x = mousex + margin.left - (w/2) + 3;
    
                  if (x + (w/2) > innerWidth) {
                    // If the circle label is too far to the right,
                    // position it as far right as possible without
                    // truncating the text.
                    x  = innerWidth - (w/2);
                  } else if (x - (w/2) < 0) {
                    // If the circle label is position out-of-bounds
                    // from the area, position the label to
                    // start at x position 0;
                    x = 0;
                  }
                  return x;
               });

      var circle = container.select(".circle");
      circle.transition()
            .duration(200)
            .style("opacity", .7);
      circle.attr("cx", mousex + margin.left + 2 )
            .attr("cy", mousey + margin.top )
            .attr("r", 3)
              
    }
  };

  var hideCircle = function() {
    if (container == null) {
      return;
    }
    container.select(".circle").transition()        
                 .duration(500)      
                 .style("opacity", 0); 
    
    container.select(".circle-label").transition()        
                 .duration(500)      
                 .style("opacity", 0);                    
    
    container.select(".tooltip").transition()
             .duration(500)
             .style("opacity", 0); 
  }


  var formatXTick = null;


  var margin = {left: 50, right: 20, top: 10, bottom: 30};

  var width = 600 - margin.left - margin.right;
  var height = 220 - margin.top - margin.bottom;
  var widthPercent  = "95%";
  var heightPercent = "95%";

  var showTooltip = true;
  var showBrush = false;
  var showXAxis = true;
  var showYAxis = true;
  var showTransition = true;
  var showGradient = true;
  var brushHeight = null;
  var xStart = null;
  var xEnd = null;



      
  function exports(selection, cb) {

   
    selection.each(function(data) {
      theData = data;

      container = d3.select(this);

      // add tooltip div
      var tooltip = container.selectAll(".tooltip").data([0])
        .enter().append('div')
          .attr("class", "tooltip")               
          .style("opacity", 0);


      var svg = d3.select(this)
                  .selectAll("svg")
                  .data([data]);

      svg.enter()
        .append("svg")
        .attr("width", widthPercent)
        .attr("height", heightPercent)
        .attr('viewBox', "0 0 " + (parseInt(width) + margin.left + margin.right) + " " + parseInt(height))
        .attr("preserveAspectRatio", "none");

      // The chart dimensions could change after instantiation, so update viewbox dimensions
      // every time we draw the chart.
      d3.select(this).selectAll("svg")
        .filter(function() { 
           return this.parentNode === container.node();
         })
        .attr('viewBox', "0 0 " + (parseInt(width) + margin.left + margin.right) + " " + parseInt(height));

      // add a circle and label
      var circle = svg.selectAll(".circle").data([0])
        .enter().append('circle')
          .attr("class", "circle")
          .attr("r", 3)
          .style("opacity", 0);
      var circleLabel = svg.selectAll(".circle-label").data([0])
        .enter().append('text')
          .attr("class", "circle-label")
          .attr("x", 0)
          .attr("y", 0)
          .style("opacity", 0);

      if (kind == KIND_AREA && showGradient) {
          var defs = svg.selectAll("defs").data([data]).enter()
                        .append("defs");

          var lg = defs.selectAll("linearGradient").data([data]).enter()
                 .append("linearGradient")
                 .attr("id", "area-chart-gradient")
                 .attr("x1", "0")
                 .attr("x2", "0")
                 .attr("y1", "0")
                 .attr("y2", "1");

          lg.selectAll("stop.area-chart-gradient-top").data([data]).enter()
             .append("stop")
             .attr("class", "area-chart-gradient-top")
             .attr("offset", "60%");

          lg.selectAll("stop.area-chart-gradient-bottom").data([data]).enter()
             .append("stop")
             .attr("class", "area-chart-gradient-bottom")
             .attr("offset", "100%");
      }
  
        
      var svgGroup =  svg.selectAll("g.group").data([data]).enter()
        .append("g")
        .attr("class", "group")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

      // Tooltip     
      svgGroup.on("mouseover", function(d) {
        var tooltipText = '';
        mousex = d3.mouse(this)[0];
        mousey = d3.mouse(this)[1];
        var invertedx = x.invert(mousex);
        var invertedy = y.invert(mousey);

        tooltipText += 'position ' + formatter(parseInt(invertedx));
        tooltipText += '<br>depth ' + parseInt(invertedy);

        var width = 130;
        var height = 40;
        var left = sidebarAdjustX(d3.event.pageX - 130);
        var top = d3.event.pageY - 42;

        // if the tooltip approaches the left sidebar, flip it to the right side
        if (left < 50) {
          left = d3.round(left + width);
        }

        var tooltip = container.select('.tooltip');
        tooltip.transition()
          .duration(200)
          .style("opacity", .9)
          .style("display", "block");
        tooltip.html(tooltipText)
          .style("width", width + "px")
          .style("height", height + "px")
          .style("left",  left + "px")
          .style("text-align", 'left')
          .style("top",  top + "px");
      })
      .on("mouseout", function(d) {
        var tooltip = container.select('.tooltip');
        tooltip.transition()
          .duration(500)
          .style("opacity", 0);

        tooltip.transition()
          .delay(500)
          .style("display", "none");
      });

      var innerWidth = width - margin.left - margin.right;
      x = d3.scale.linear()
          .range([0, innerWidth]);

      var innerHeight = height - margin.top - margin.bottom;
      y = d3.scale.linear()
          .range([innerHeight, 0]);


      if (xStart && xEnd) {
        x.domain([xStart, xEnd]);
      } else {
        x.domain(d3.extent(data, pos));
      }
      var theMaxDepth = maxDepth ? Math.max(maxDepth,d3.max(data, depth)) : d3.max(data, depth);
      y.domain([0, theMaxDepth]);

      var brush = d3.svg.brush()
        .x(x)
        .on("brushend", function() {
            dispatch.d3brush(brush);
         });

      var xAxis = d3.svg.axis()
          .scale(x)
          .tickFormat(function (d) {
             if ((d / 1000000) >= 1)
               d = d / 1000000 + "M";
             else if ((d / 1000) >= 1)
               d = d / 1000 + "K";
             return d;            
          })
          .orient("bottom");
      if (formatXTick) {
        xAxis.tickFormat(formatXTick);
      }


      var yAxis = d3.svg.axis()
          .scale(y)
          .ticks(5)
          .orient("right");


      var line = d3.svg.line()
          .interpolate("linear")
          .x(function(d,i) { return x(pos(d)); })
          .y(function(d) { return y(depth(d)); });

      var area;


      if (kind == KIND_AREA) {
        area = d3.svg.area()
          .interpolate("linear")
          .x(function(d) { return x(pos(d)); })
          .y0(innerHeight)
          .y1(function(d) { return y(depth(d)); });
      } 

      
      svgGroup = svg.selectAll("g.group")
      svgGroup.selectAll("g.x").remove();
      if (showXAxis) {
        svgGroup.selectAll("g.x").data([data]).enter()
          .append("g")
          .attr("class", "x axis")
          .attr("transform", "translate(0," + innerHeight + ")")
          .call(xAxis);        
      }

      svgGroup.selectAll("g.y").remove();
      if (showYAxis) {
        svgGroup.selectAll("g.y").data([data]).enter()
            .append("g")
            .attr("class", "y axis")
            .call(yAxis);
      }
        

      // not sure why, but second time through, the svgGroup is a
      // "placeholder", so we will just select the group again
      // to remove the path and then add the new one.
      svgGroup = svg.selectAll("g.group")
      svgGroup.select("#line-chart-path").remove();
      
      var linePath = svgGroup.append("path")
        .datum(data)
        .attr("id", "line-chart-path")
        .attr("class", "line")
        .attr("d", line(data));

      if (showTransition) {
        linePath.transition()
          .duration(1000)
          .attrTween('d', function() {
              {
                var interpolate = d3.scale.quantile()
                    .domain([0,1])
                    .range(d3.range(1, data.length + 1));

                return function(t) {
                    var interpolatedArea = data.slice(0, interpolate(t));
                    return line(interpolatedArea);
                }
              }
            })
          .each("end", function(d) {
              dispatch.d3rendered();
          });
          
      }


      if (kind == KIND_AREA) {
        svgGroup.select("#area-chart-path").remove();
        var areaPath = svgGroup.append("path")
          .datum(data)
          .attr("id", "area-chart-path")
          .attr("d", area(data));

        if (showGradient) {
          areaPath.style("fill", "url(#area-chart-gradient)");
        }

        if (showTransition) {
          areaPath.transition()
             .duration(1000)
             .attrTween('d', function() {
                { 
                  var interpolate = d3.scale.quantile()
                      .domain([0,1])
                      .range(d3.range(1, data.length + 1));

                  return function(t) {
                      var interpolatedArea = data.slice(0, interpolate(t));
                      return area(interpolatedArea);
                  }
                }
              });
             
        }
       }

      if (showBrush) {
        if (brushHeight == null ) {
          brushHeight = innerHeight;
          brushY = -6;
        } else {
          brushY = 0 - (brushHeight / 2);
        }
        svgGroup.append("g")
            .attr("class", "x brush")
            .call(brush)
            .selectAll("rect")
            .attr("y", brushY)
            .attr("height", brushHeight);
      }

      if (!showTransition) {
        dispatch.d3rendered();
      }


  });
}
  exports.showCircle = function(_) {
    if (!arguments.length) return showCircle;
    showCircle = _;
    return exports;
  }
  exports.hideCircle = function(_) {
    if (!arguments.length) return hideCircle;
    hideCircle = _;
    return exports;
  }
 
  exports.showTooltip = function(_) {
    if (!arguments.length) return showTooltip;
    showTooltip = _;
    return exports;
  };

 

  exports.margin = function(_) {
    if (!arguments.length) return margin;
    margin = _;
    return exports;
  };

  exports.width = function(_) {
    if (!arguments.length) return width;
    width = _;
    return exports;
  };

  exports.height = function(_) {
    if (!arguments.length) return height;
    height = _;
    return exports;
  };

  exports.widthPercent = function(_) {
    if (!arguments.length) return widthPercent;
    widthPercent = _;
    return exports;
  };

  exports.heightPercent = function(_) {
    if (!arguments.length) return heightPercent;
    heightPercent = _;
    return exports;
  };
 
  exports.brush = function(_) {
    if (!arguments.length) return brush;
    brush = _;
    return exports; 
  };

  exports.pos = function(_) {
    if (!arguments.length) return pos;
    pos = _;
    return exports; 
  }

  exports.depth = function(_) {
    if (!arguments.length) return depth;
    depth = _;
    return exports; 
  }

  exports.kind = function(_) {
    if (!arguments.length) return kind;
    kind = _;
    return exports; 
  }

  exports.maxDepth = function(_) {
    if (!arguments.length) return maxDepth;
    maxDepth = _;
    return exports;     
  }

  exports.showXAxis = function(_) {
    if (!arguments.length) return showXAxis;
    showXAxis = _;
    return exports; 
  }

  exports.showYAxis = function(_) {
    if (!arguments.length) return showYAxis;
    showYAxis = _;
    return exports; 
  }
  
  exports.formatXTick = function(_) {
    if (!arguments.length) return formatXTick;
    formatXTick = _;
    return exports;
  }

  exports.showTransition = function(_) {
    if (!arguments.length) return showTransition;
    showTransition = _;
    return exports;
  }

  exports.showGradient = function(_) {
    if (!arguments.length) return showGradient;
    showGradient = _;
    return exports;
  }

  exports.showBrush = function(_) {
    if (!arguments.length) return showBrush;
    showBrush = _;
    return exports;
  }

  exports.brushHeight = function(_) {
    if (!arguments.length) return brushHeight;
    brushHeight = _;
    return exports;
  }

  exports.xStart = function(_) {
    if (!arguments.length) return xStart;
    xStart = _;
    return exports;
  }

  exports.xEnd = function(_) {
    if (!arguments.length) return xEnd;
    xEnd = _;
    return exports;
  }

  exports.formatCircleText = function(_) {
    if (!arguments.length) return formatCircleText;
    formatCircleText = _;
    return exports;
  }

  // This adds the "on" methods to our custom exports
  d3.rebind(exports, dispatch, "on");
  return exports;
}