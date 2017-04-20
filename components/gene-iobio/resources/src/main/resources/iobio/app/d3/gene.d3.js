// TODO ADD PROJECT INFO
// consumes data in following format
// var data = [ {name: 'somename',
//              features : [{start:someInt, end:someInt, feature_type:utr, strand:'+'},
//                          {start:someInt, end:someInt, feature_type:cds}, ...]
//            }, ... ]
//

function geneD3() {
  // defaults

  // dispatch events
  var dispatch = d3.dispatch("d3brush", "d3selected", "d3featuretooltip");

  var selectedTranscript = null;

  var geneD3_showBrush = false;
  var geneD3_showLabel = false;
  var geneD3_showXAxis = true;



  // dimensions
  var margin = {top: 30, right: 0, bottom: 20, left: 110};
  var geneD3_width = 800,
      geneD3_height = 400;  


  // scales
  var x = d3.scale.linear(),
      y = d3.scale.linear();
  // axis
  var xAxis = d3.svg.axis()
    .scale(x)
    .orient("bottom")
    .tickFormat(tickFormatter);
  // variables 
  var trackHeight = 20,
      borderRadius = 1,
      minFtWidth = 0.5;
      utrHeight = undefined,
      cdsHeight = undefined,
      arrowHeight = undefined,
      geneD3_regionStart = undefined,
      geneD3_regionEnd = undefined,      
      geneD3_geneD3_widthPercent = null,
      geneD3_geneD3_heightPercent = null,     
      showBrushLine = false;
      

  //  options
  var defaults = {};

  var brushAllowance = 0;
      
      
  function chart(selection, options) {
    // merge options and defaults
    options = $.extend(defaults,options);
    // set variables if not user set
    cdsHeight = cdsHeight || trackHeight;
    utrHeight = utrHeight || cdsHeight / 2;
    arrowHeight = arrowHeight || trackHeight / 2;


    selection.each(function(data) {

       brushAllowance = geneD3_showBrush ? 20 : 0;

       // calculate height
       var padding = data.length > 1 ? trackHeight/2 : 0;
       geneD3_height = data.length * (trackHeight + padding);

       // determine inner height (w/o margins)
       var innerHeight = geneD3_height - margin.top - margin.bottom;

       // set svg element
       var container = d3.select(this).classed('ibo-gene', true);    

      // Update the x-scale.
      if (geneD3_regionStart && geneD3_regionEnd) {
        x.domain([geneD3_regionStart, geneD3_regionEnd]);
      } else {
        x.domain([ d3.min(data, function(d) { 
                     return d3.min(d.features, function(f) { return parseInt(f.start); }) 
                   }),
                   d3.max(data, function(d) { 
                     return d3.max(d.features, function(f) { return parseInt(f.end); }) 
                   }) 
                ]);

      }
      x  .range([0, geneD3_width - margin.left - margin.right]);

      // Update the y-scale.
      y  .domain([0, data.length]);
      y  .range([innerHeight , 0]);


      // Select the svg element, if it exists.
      var svg = container.selectAll("svg").data([0]);

      svg.enter()
        .append("svg")
        .attr("width", geneD3_widthPercent ? geneD3_widthPercent : geneD3_width)
        .attr("height", geneD3_heightPercent ? geneD3_heightPercent : geneD3_height+margin.top+margin.bottom)
       
      // The chart dimensions could change after instantiation, so update viewbox dimensions
      // every time we draw the chart.
      if (geneD3_widthPercent && geneD3_heightPercent) {
        d3.select(this).selectAll("svg")
           .filter(function() { 
              return this.parentNode === container.node();
           })
          .attr('viewBox', "0 0 " + parseInt(geneD3_width+margin.left+margin.right) + " " + parseInt(geneD3_height+margin.top+margin.bottom))
          .attr("preserveAspectRatio", "none");
      } 

      container.selectAll("svg")
        .attr("width", geneD3_widthPercent ? geneD3_widthPercent : geneD3_width)
        .attr("height", geneD3_heightPercent ? geneD3_heightPercent : geneD3_height+margin.top+margin.bottom);


      // Otherwise, create the skeletal chart.      
      var gEnter = svg.selectAll("g").data([0]).enter().append('g');    

      var g = svg.select('g');
      // Update the inner dimensions.
      g.attr("transform", "translate(" + margin.left + "," + margin.top + ")");

      g.selectAll("line.brush-line").remove();

      // Brush
      var brush = d3.svg.brush()
        .x(x)
        .on("brushend", function() {
            var extentRect = d3.select("g.x.brush rect.extent");
            
            var xExtent = +extentRect.attr("x") + (+extentRect.attr("width") / 2);
            var heightExtent = +extentRect.attr("height") - 10;

            g.selectAll("line.brush-line").remove();

            if (showBrushLine) {
              if (!brush.empty()) {
                g.append('line')
                  .attr('class', 'brush-line')
                  .attr('x1', xExtent)
                  .attr('x2', xExtent)          
                  .attr('y1', heightExtent -20)
                  .attr('y2', heightExtent );

                 g.append('line')
                  .attr('class', 'brush-line')
                  .attr('x1', 40 )
                  .attr('x2',  geneD3_width - margin.left - margin.right - 40)          
                  .attr('y1', heightExtent)
                  .attr('y2', heightExtent);

                if (geneD3_widthPercent && geneD3_heightPercent) {
                  svg.attr('viewBox', "0 0 " + parseInt(geneD3_width+margin.left+margin.right) + " " + parseInt(geneD3_height+margin.top+margin.bottom+brushAllowance))
                     .attr("preserveAspectRatio", "xMaxYMid meet");
                } 

                svg.attr("width", geneD3_widthPercent ? geneD3_widthPercent : geneD3_width)
                   .attr("height", geneD3_heightPercent ? geneD3_heightPercent : geneD3_height+margin.top+margin.bottom+brushAllowance);


              } else {
                 if (geneD3_widthPercent && geneD3_heightPercent) {
                    svg.attr('viewBox', "0 0 " + parseInt(geneD3_width+margin.left+margin.right) + " " + parseInt(geneD3_height+margin.top+margin.bottom))
                       .attr("preserveAspectRatio", "xMaxYMid meet");
                  } 

                  svg.attr("width", geneD3_widthPercent ? geneD3_widthPercent : geneD3_width)
                     .attr("height", geneD3_heightPercent ? geneD3_heightPercent : geneD3_height+margin.top+margin.bottom);
               
              }


            }
            dispatch.d3brush(brush);


         });



      var axisEnter = svg.selectAll("g.x.axis").data([0]).enter().append('g');   
      if (geneD3_showXAxis) {
        axisEnter.attr("class", "x axis")
                 .attr("transform",   "translate(" + margin.left + "," + "0" + ")");
        svg.selectAll("g.x.axis").attr("transform",   "translate(" + margin.left + "," + "0" + ")");
      }  


      if (geneD3_showBrush) {
        var brushHeight = geneD3_height + 20;
        var brushY = -20;
        g.selectAll("g.x.brush").remove();
        var theBrush = g.selectAll("g.x.brush").data([0]);
        theBrush.enter().append("g")
            .attr("class", "x brush")
            .call(brush)
            .selectAll("rect")
            .attr("y", brushY)
            .attr("height", brushHeight);
        theBrush.exit().remove();

           
      }    

      // add tooltip div
      var tooltip = container.selectAll(".tooltip").data([0])
        .enter().append('div')
          .attr("class", "tooltip")               
          .style("opacity", 0);

               
      // Start gene model
      // add elements
      var transcript = g.selectAll('.transcript').data(data);
      transcript.enter().append('g')
          .attr('class', 'transcript')
          .attr("id", function(d,i) { return 'transcript_' +  d.transcript_id.split(".").join("_"); })
          .attr('transform', function(d,i) { return "translate(0," + y(i+1) + ")"});
      transcript.exit().remove();

      transcript.selectAll(".selection-box").remove();
      transcript.selectAll(".selection-box")
        .data(function(d) { 
          if (geneD3_regionStart && geneD3_regionEnd) {
            return [[geneD3_regionStart,geneD3_regionEnd]];
          } else {
            return [[d.start, d.end]] 

          }
        })
        .enter().append('rect')
          .attr('class', 'selection-box')
          .attr('x', margin.left * -1)
          .attr('y', 0)
          .attr('width', margin.left + geneD3_width)
          .attr('height', trackHeight)
          .on("mouseover", function(d) {
            d3.selectAll('.transcript.selected').classed("selected", false);
            d3.select(this.parentNode).classed("selected", true);
            selectedTranscript = d3.select(this.parentNode)[0][0].__data__;
          })
          .on("mouseout", function(d) {
            d3.select(this.parentNode).classed("selected", false);
          });      

      transcript.selectAll(".reference").remove();
      transcript.selectAll('.reference')
        .data(function(d) { 
          if (geneD3_regionStart && geneD3_regionEnd) {
            return [[geneD3_regionStart,geneD3_regionEnd]];
          } else {
            return [[d.start, d.end]] 

          }
        })
        .enter().append('line')
          .attr('class', 'reference')
          .attr('x1', function(d) { return d3.round(x(d[0]))})
          .attr('x2', function(d) { return d3.round(x(d[1]))})                    
          .attr('y1', trackHeight/2)
          .attr('y2', trackHeight/2)
          .on("mouseover", function(d) {
            d3.selectAll('.transcript.selected').classed("selected", false);
            d3.select(this.parentNode).classed("selected", true);
            selectedTranscript = d3.select(this.parentNode)[0][0].__data__;
          })
          .on("mouseout", function(d) {
            d3.select(this.parentNode).classed("selected", false);
          });


      transcript.selectAll(".name,.type").remove();
      if (geneD3_showLabel) {
        transcript.selectAll('.name').data(function(d) { return [[d.start, d.transcript_id]] })
                  .enter().append('text')
                    .attr('class', 'name')
                    .attr('x', function(d) { return margin.left > 5 ?  5 - margin.left : 0 })
                    .attr('y', 0 )
                    .attr('text-anchor', 'top')
                    .attr('alignment-baseline', 'left')
                    .text(function(d) { 
                      return d[1]; 
                    })
                    .style('fill-opacity', 0)
                    .on("mouseover", function(d) {
                      d3.selectAll('.transcript.selected').classed("selected", false);
                      d3.select(this.parentNode).classed("selected", true);
                      selectedTranscript = d3.select(this.parentNode)[0][0].__data__;
                    })
                    .on("mouseout", function(d) {
                      d3.select(this.parentNode).classed("selected", false);
                    });
        transcript.selectAll('.type').data(function(d) { return [[d.start, d.transcript_type, (d.isCanonical ? ' CANONICAL' : ''), (d.xref != null ? "(" + d.xref + ")": ''),  d.sort]] })
                  .enter().append('text')
                    .attr('class', 'type')
                    .attr('x', function(d) { return (geneD3_width - margin.left - margin.right) + 10 })
                    .attr('y', 12 )
                    .attr('text-anchor', 'top')
                    .attr('alignment-baseline', 'left')
                    .text(function(d) { 
                      var type =  (d[1] == 'protein_coding' || d[1] == 'mRNA' ? '' : d[1]); 
                      return type + ' ' + d[2] + ' ' + d[3];
                    })
                    .on("mouseover", function(d) {
                      d3.selectAll('.transcript.selected').classed("selected", false);
                      d3.select(this.parentNode).classed("selected", true);
                      selectedTranscript = d3.select(this.parentNode)[0][0].__data__;
                    })
                    .on("mouseout", function(d) {
                      d3.select(this.parentNode).classed("selected", false);
                    });  


      }
      
      transcript.selectAll(".arrow").remove();
      transcript.selectAll('.arrow').data(centerSpan)
        .enter().append('path')
          .attr('class', 'arrow')
          .attr('d', centerArrow);      

      var filterFeatures = null;
      if ( transcript.datum().transcript_type == 'protein_coding' 
        || transcript.datum().transcript_type == 'mRNA' 
        || transcript.datum().transcript_type == 'transcript'
        || transcript.datum().transcript_type == 'primary_transcript') {
        filterFeatures = function(d) {
           var ft = d.feature_type.toLowerCase(); 
           return  (ft == 'utr' || ft == 'cds');
        };
      } else {
        filterFeatures = function(d) {
           var ft = d.feature_type.toLowerCase(); 
           return ft == 'exon';
        };
      }


      transcript.selectAll('.transcript rect.utr, .transcript rect.cds, .transcript rect.exon').data(function(d) { 
        return d['features'].filter( function(d) {
          return filterFeatures(d); 
        }, function(d) {
          return d.feature_type + "-" + d.start + "-" + d.end;
        });
      }).enter().append('rect')
          .attr('class', function(d) { return d.feature_type.toLowerCase();})          
          .attr('rx', borderRadius)
          .attr('ry', borderRadius)
          .attr('x', function(d) { 
            return d3.round(x(d.start))
          })
          .attr('width', function(d) { 
            return Math.max(minFtWidth,d3.round(x(d.end) - x(d.start))) 
          })
          .attr('y', trackHeight /2)
          .attr('height', 0)
          .attr("pointer-events", "all")
          .style("cursor", "pointer")
          .on("mouseover", function(d) {  
              // show the tooltip
              var tooltip = container.select('.tooltip');              
              var featureObject = d3.select(this);
              dispatch.d3featuretooltip(featureObject, d, tooltip);

              // select the transcript  
              d3.selectAll('.transcript.selected').classed("selected", false);
              d3.select(this.parentNode).classed("selected", true);

              selectedTranscript = d3.select(this.parentNode)[0][0].__data__;
           })                  
           .on("mouseout", function(d) {   
              // hide the tooltip    
              container.select('.tooltip').transition()        
                 .duration(500)      
                 .style("opacity", 0);   

              // de-select the transcript   
              d3.select(this.parentNode).classed("selected", false);
           });
          

    

      // update 
      transcript.transition()
          .duration(700)
          .attr('transform', function(d,i) { return "translate(0," + y(i+1) + ")"});

      transcript.selectAll('.reference').transition()
        .duration(700)
        .attr('x1', function(d) { return x(d[0])})
        .attr('x2', function(d) { return x(d[1])});

      transcript.selectAll('.arrow').transition()
        .duration(700)
        .attr('d', centerArrow);

      transcript.selectAll('.name').transition()
        .duration(700)
        .attr('x', function(d) { return margin.left > 5 ?  5 - margin.left : 0; })
        .attr('y', function(d) { return margin.left > 5 ? trackHeight - (trackHeight/2) + 2 : -10; })   
        .text(function(d) { return d[1]; })                
        .style('fill-opacity', 1);

      transcript.selectAll('.utr,.cds,.exon').sort(function(a,b){ return parseInt(a.start) - parseInt(b.start)})
        .transition()        
          .duration(700)
          .attr('x', function(d) { return d3.round(x(d.start))})
          .attr('width', function(d) { return Math.max(minFtWidth, d3.round(x(d.end) - x(d.start)))})
          .attr('y', function(d) { 
            if(d.feature_type.toLowerCase() =='utr') return (trackHeight - utrHeight)/2; 
            else return (trackHeight - cdsHeight)/2; })
          .attr('height', function(d) { 
            if(d.feature_type.toLowerCase() =='utr') return utrHeight; 
            else return cdsHeight; });          

      // Update the x-axis.
      svg.select(".x.axis").transition()
          .duration(200)
          .call(xAxis);  
        
    });

  }
  // moves selection to front of svg
  function moveToFront(selection) {
    return selection.each(function(){
       this.parentNode.appendChild(this);
    });
  }

  // updates the hash with the center of the biggest span between features
  function centerSpan(d) {    
    var span = 0;
    var center = 0;
    var sorted = d.features
      .filter(function(f) { var ft = f.feature_type.toLowerCase(); return ft == 'utr' || ft == 'cds'})
      .sort(function(a,b) { return parseInt(a.start) - parseInt(b.start)});

    for (var i=0; i < sorted.length-1; i++) {
      var currSpan = parseInt(sorted[i+1].start) - parseInt(sorted[i].end);
      if (span < currSpan) {
        span = currSpan;
        center = parseInt(sorted[i].end) + span/2;
      }
    }      
    d.center = center;
    return [d]; 
  }

  // generates the arrow path
  function centerArrow(d) {
    var arrowHead = parseInt(d.strand + '5');
    var pathStr = "M ";            
    pathStr += x(d.center) + ' ' + (trackHeight - arrowHeight)/2;
    pathStr += ' L ' + parseInt(x(d.center)+arrowHead) + ' ' + trackHeight/2;
    pathStr += ' L ' + x(d.center) + ' ' + parseInt(trackHeight + arrowHeight)/2;
    return pathStr;
  }

  function tickFormatter (d,i) {

    if ((d / 1000000) >= 1)
      d = d / 1000000 + "M";
    else if ((d / 1000) >= 1)
      d = d / 1000 + "K";
    return d;            
  }

  chart.margin = function(_) {
    if (!arguments.length) return margin;
    margin = _;
    return chart;
  };

  chart.width = function(_) {
    if (!arguments.length) return geneD3_width;
    geneD3_width = _;
    return chart;
  };

  chart.height = function(_) {
    if (!arguments.length) return geneD3_height;
    geneD3_height = _;
    return chart;
  };

  chart.widthPercent = function(_) {
    if (!arguments.length) return geneD3_widthPercent;
    geneD3_widthPercent = _;
    return chart;
  };

  chart.heightPercent = function(_) {
    if (!arguments.length) return geneD3_heightPercent;
    geneD3_heightPercent = _;
    return chart;
  };
  
  chart.x = function(_) {
    if (!arguments.length) return x;
    x = _;
    return chart;
  };

  chart.y = function(_) {
    if (!arguments.length) return y;
    y = _;
    return chart;
  };
    
  chart.xAxis = function(_) {
    if (!arguments.length) return xAxis;
    xAxis = _;
    return chart; 
  };

  chart.yAxis = function(_) {
    if (!arguments.length) return yAxis;
    yAxis = _;
    return chart; 
  };  
  chart.trackHeight = function(_) {
    if (!arguments.length) return trackHeight;
    trackHeight = _;
    return chart;
  };

  chart.utrHeight = function(_) {
    if (!arguments.length) return utrHeight;
    utrHeight = _;
    return chart;
  };

  chart.cdsHeight = function(_) {
    if (!arguments.length) return cdsHeight;
    cdsHeight = _;
    return chart;
  };

  chart.arrowHeight = function(_) {
    if (!arguments.length) return arrowHeight;
    arrowHeight = _;
    return chart;
  };

  chart.showXAxis = function(_) {
    if (!arguments.length) return geneD3_showXAxis;
    geneD3_showXAxis = _;
    return chart;
  };

  chart.regionStart = function(_) {
    if (!arguments.length) return geneD3_regionStart;
    geneD3_regionStart = _;
    return chart;
  };
  chart.regionEnd = function(_) {
    if (!arguments.length) return geneD3_regionEnd;
    geneD3_regionEnd = _;
    return chart;
  };

  chart.showBrush = function(_) {
    if (!arguments.length) return showBrush;
    geneD3_showBrush = _;
    return chart;
  }

  chart.selectedTranscript = function(_) {
    if (!arguments.length) return selectedTranscript;
    selectedTranscript = _;
    return chart;
  }

  chart.showLabel = function(_) {
    if (!arguments.length) return geneD3_showLabel;
    geneD3_showLabel = _;
    return chart;
  }

  chart.showBrushLine = function(_) {
    if (!arguments.length) return showBrushLine;
    showBrushLine = _;
    return chart;
  }
  // This adds the "on" methods to our custom exports
  d3.rebind(chart, dispatch, "on");

  return chart;
}