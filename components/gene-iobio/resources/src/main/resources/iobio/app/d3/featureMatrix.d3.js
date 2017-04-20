function featureMatrixD3() {
   var dispatch = d3.dispatch("d3click", "d3mouseover", "d3mouseout", "d3rowup", "d3rowdown");

  // dimensions
  var margin = {top: 10, right: 10, bottom: 10, left: 10};  
  // scales
  var x = d3.scale.ordinal(),
      y = d3.scale.ordinal();

  // color scheme
  var colorScale = null;

  var container = null;

  var tooltipHTML = function(colObject, rowIndex) {
    return "tootip at row " + rowIndex;
  }

  var columnLabel = function( d, i) {
    return d.type;
  }
 
 
  // variables 
  var heightPercent = "100%",
      widthPercent = "100%",
      showTransition = true,
      matrixRows = null,
      matrixRowNames = null;
      cellSize = 10,
      cellWidth = null,
      cellHeight = null,
      cellHeights = null,
      rowLabelWidth = 100,
      columnLabelHeight = 100;
      
  //  options
  var defaults = {};

      
  function chart(selection, options) {
    var me = this;
    // merge options and defaults
    options = $.extend(defaults,options);

    selection.each(function(data) {
      // Calculate height of matrix

      var firstCellHeight = null;
      var matrixHeight = null;
      if (cellHeights && cellHeights.length > 0) {
        matrixHeight = cellHeights.reduce(function(pv, cv) { return pv + cv; }, 0);
        firstCellHeight = cellHeights[0];
      } else {
        matrixHeight  = matrixRowNames.length * (cellHeight != null ? cellHeight : cellSize);   
        firstCellHeight = (cellHeight != null ? cellHeight : cellSize);
      }
      height = matrixHeight;
      height += margin.top + margin.bottom;
      if (options.showColumnLabels) {
        height += columnLabelHeight;
      }
      var innerHeight = height - margin.top - margin.bottom;
      if (options.showColumnLabels) {
        innerHeight -= columnLabelHeight;
      } 

      width = data.length *  (cellWidth != null ? cellWidth : cellSize);   
      width += margin.left + margin.right + rowLabelWidth +  (cellWidth != null ? cellWidth : cellSize);
      var innerWidth = width - margin.left - margin.right - rowLabelWidth;

      container = d3.select(this);



    
      x.domain(data.map(function(d) {  return d.type + " " + d.start + " " + d.ref + "->" + d.alt }));
      x.rangeRoundBands([0, innerWidth], 0, 0);
 
      y.domain(matrixRowNames);
      y.rangeRoundBands([0, innerHeight], 0, 0);

    
      // axis
      //var xAxis = d3.svg.axis()
      //                  .scale(x)
      //                  .orient("start")
      //                  .ticks(data.length);
                       

      var yAxis = d3.svg.axis()
                        .scale(y)
                        .orient("left")
                        .outerTickSize(0)   
                        .ticks(matrixRowNames.length);



                       
                        
      
      // Select the svg element, if it exists.
      var svg = container.selectAll("svg").data([0]);

      svg.enter()
        .append("svg")
        .attr("width", parseInt(width))
        .attr("height", heightPercent)
        .attr('viewBox', "0 0 " + parseInt(width) + " " + parseInt(height))
        .attr("preserveAspectRatio", "none");

      // The chart dimensions could change after instantiation, so update viewbox dimensions
      // every time we draw the chart.
      d3.select(this).selectAll("svg")
          .filter(function() { 
            return this.parentNode === container.node();
          })
         .attr("width", parseInt(width))
         .attr('viewBox', "0 0 " + parseInt(width) + " " + parseInt(height));




      // Generate the column headers
      svg.selectAll("g.colhdr").remove();
      if (options.showColumnLabels) {
        var translateColHdrGroup = "";
        if (options.simpleColumnLabels) {
          if (cellWidth) {
            translateColHdrGroup = "translate(" + (+rowLabelWidth + (cellWidth/2) - 4) + "," + (columnLabelHeight-4) + ")";
          } else {
            translateColHdrGroup = "translate(" + (+rowLabelWidth) + "," + (columnLabelHeight-4) + ")";
          }
        } else {
          translateColHdrGroup = "translate(" + (+rowLabelWidth+( (cellWidth != null ? cellWidth : cellSize) / 2 )) + "," + (columnLabelHeight) + ")";
        }
        var colhdrGroup =  svg.selectAll("g.colhdr").data([data])
          .enter()
          .append("g")
          .attr("class", "colhdr")
          .attr("transform",  translateColHdrGroup);

        var colhdrs = colhdrGroup.selectAll('.colhdr').data(data);
        colhdrs.enter().append('g')
            .attr('class', 'colhdr')
            .attr('transform', function(d,i) { 
              return "translate(" + ( (cellWidth != null ? cellWidth : cellSize) * (i+1)) + ",0)";
            })
            .append("text")
            .style("text-anchor", options.simpleColumnLabels ? "middle" : "start")
            .attr("dx", ".8em")
            .attr("dy", ".15em")
            .attr("transform", function(d) {
              if (options.simpleColumnLabels) {
                return "" ;
              } else {
                return "rotate(-65)" ;
              }
            })
            .text( columnLabel );

      }


      var translate = "translate(" + rowLabelWidth + ",";
      if (options.showColumnLabels) {
        translate += (+columnLabelHeight - firstCellHeight) + ")";
      } else {
        translate += "-30)"
      }
      svg.selectAll("g.group").remove();
      var g =  svg.selectAll("g.group").data([data])
        .enter()
        .append("g")
        .attr("class", "group")
        .attr("transform",  translate);


      // Create the y-axis at the top.  This will show the labels for the rows 
      svg.selectAll(".y.axis").remove();    
      svg.selectAll("g.y").data([matrixRowNames]).enter()
          .append("g")
          .attr("class", "y axis")
          .attr("transform", "translate(20," + (options.showColumnLabels ? columnLabelHeight + (cellHeights != null && cellHeights.length > 0 ? 4 : 0) : "0") + ")")
          .call(yAxis)
          .selectAll("text")
          .style("text-anchor", "start")
          .attr("x", "-28")
          .attr("dx", ".8em")
          .attr("dy", ".15em");

      // Add the up and down arrows to the x-axis
      svg.selectAll("g.y.axis .tick .up").remove();
      svg.selectAll("g.y.axis .tick")
         .append("g")
         .attr("class", "up faded")
         //.attr("transform", "translate(" + rowLabelWidth + ", -13)")
         .attr("transform", "translate(" + (+rowLabelWidth - 60) + ", -24)")
         .append("use")
         .attr("id", "arrow-up")
         .attr("xlink:href", "#arrow-up-symbol")
         .attr("width", 44)
         .attr("height", 44)
         .on("click", function(d,i) {
            // We want to mark the row label that is going to be shifted up
            // or down so that after the matrix is resorted and rewdrawn,
            // the row that the user was moving is highlighted to show the
            // user what row we just shifted up or down..
            matrixRows.forEach( function(matrixRow) {
              matrixRow.current = 'N';
            });
            matrixRows[i].current = 'Y';
            container.select(".y.axis").selectAll("text").each( function(d1,i1) {
              if (i1 == i) {
                d3.select(this).classed('current', true);
              } else {
                d3.select(this).classed('current', false);
              }
            });
            // When the user clicks on the 'up' button for a row label,
            // disppatch the event so that the app can shift
            // the rows and re-sort the matrix data.
            dispatch.d3rowup(i);
         })
        .on("mouseover", function(d,i) {
            container.selectAll('.y.axis .up').classed("faded", true);
            container.selectAll('.y.axis .down').classed("faded", true);
            d3.select(this.parentNode).classed("faded", false);
            container.selectAll('.y.axis text').classed("active", false);
            d3.select(this.parentNode.parentNode).select("text").classed("active", true);
         });



      svg.selectAll("g.y.axis .tick .down").remove();
      svg.selectAll("g.y.axis .tick")
         .append("g")
         .attr("class", "down faded")
         //.attr("transform", "translate(" + (+rowLabelWidth + 17) + ", 9 )")
         .attr("transform",  "translate(" + (+rowLabelWidth - 40) + ", -24)")
         .append("use")
         .attr("id", "arrow-down")
         .attr("xlink:href", "#arrow-down-symbol")
         .attr("width", 44)
         .attr("height", 44)
         .on("click", function(d,i) {
            // We want to mark the row label that is going to be shifted up
            // or down so that after the matrix is resorted and rewdrawn,
            // the row that the user was moving is highlighted to show the
            // user what row we just shifted up or down..
            matrixRows.forEach( function(matrixRow) {
              matrixRow.current = 'N';
            });
            matrixRows[i].current = 'Y';
            container.select(".y.axis").selectAll("text").each( function(d1,i1) {
              if (i1 == i) {
                d3.select(this).classed('current', true);
              } else {
                d3.select(this).classed('current', false);
              }
            });
            // When the user clicks on the 'up' button for a row label,
            // disppatch the event so that the app can shift
            // the rows and re-sort the matrix data.
            dispatch.d3rowdown(i);
         })
         .on("mouseover", function(d,i) {
            container.selectAll('.y.axis .up').classed("faded", true);
            container.selectAll('.y.axis .down').classed("faded", true);
            d3.select(this.parentNode).classed("faded", false);
            container.selectAll('.y.axis text').classed("active", false);
            d3.select(this.parentNode.parentNode).select("text").classed("active", true);

         });
  
      // Highlight of the last row label that we moved up or down.  Highlight this
      // row label so that user can keep track of the row he just moved.
      svg.selectAll(".y.axis .tick text").each( function(d,i) {
        d3.select(this).classed('current', matrixRows[i].current == 'Y');
      });
      // On the highlight matrix row, don't fade the arrow buttons.
      svg.selectAll(".y.axis .tick .up").each( function(d,i) {
        d3.select(this).classed('faded', matrixRows[i].current != 'Y');
      });
      svg.selectAll(".y.axis .tick .down").each( function(d,i) {
        d3.select(this).classed('faded', matrixRows[i].current != 'Y');
      });

      // Hide the ticks and the path of the x-axis, we are just interested
      // in the text
      //svg.selectAll("g.x.axis .tick line").classed("hide", true);
      //svg.selectAll("g.x.axis path").classed("hide", true);
      svg.selectAll("g.y.axis .tick line").classed("hide", true);
      svg.selectAll("g.y.axis path").classed("hide", true);

      // Adjust the y-axis transform attribute so that translate(x,y) changes y
      // to cell heights
      if (cellHeights != null && cellHeights.length > 0) {
        svg.selectAll("g.y.axis g.tick").each( function(d,i) {
            var y = 0;
            for (var idx = 0; idx < i; idx++) {
              y += cellHeights[idx] ;
            }  
            y += 8;
            d3.select(this).attr('transform', 'translate(0,' + y + ')');
        })
      }

      // add tooltip div
      var tooltip = container.selectAll(".tooltip").data([0])
        .enter().append('div')
          .attr("class", "tooltip")               
          .style("opacity", 0);


      // Generate the cols
      var cols = g.selectAll('.col').data(data);
      cols.enter().append('g')
          .attr('class', function(d,i) {
            return "col  " + d.featureClass;
          })
          .attr('transform', function(d,i) { 
            return "translate(" + ( (cellWidth != null ? cellWidth : cellSize) * (i+1)) + ",0)";
          });
      

      // Generate cells
      var cells = cols.selectAll('.cell').data(function(d) { 
        return d['features'];
      }).enter().append('g')
          .attr('class', "cell") 
          .attr('transform', function(d,i) {
            var yPos = 0;
            if (cellHeights && cellHeights.length > 0) {
              var pos = (i+1) % matrixRows.length;
              if (pos == 0) {
                pos = matrixRows.length;
              } 
              for (var idx = 0; idx < pos-1; idx++) {
                yPos += cellHeights[idx] ;
              }  
              yPos = yPos + firstCellHeight;            
            } else {
              yPos = y(matrixRowNames[i]) + y.rangeBand();
            }
            return 'translate(0,' +  yPos + ')';
        });



      cells.append('rect')
          .attr('class', function(d,i) { 
            return "cellbox";
          })          
          .attr('x', function(d,i) { 
            return 0;
          })
          .attr('height', function(d, i) { 
            if (cellHeights && cellHeights.length > 0) {
              var pos = (i+1) % matrixRows.length;
              if (pos == 0) {
                pos = matrixRows.length -1;
              } else {
                pos = pos - 1;
              }
              return cellHeights[pos] - 1;
            } else {
              return  (cellHeight != null ? cellHeight : cellSize) - 1; 
            }
          })
          .attr('y', 0)
          .attr('width',  (cellWidth != null ? cellWidth : cellSize) - 1);
         


      cells.append("text")
          .text( function(d,i) {
            return d.value;
          })
          .attr('class', 'hide')
          .attr("x", 0)
          .attr("y", function(d,i) { 
            return (y.rangeBand()/2);
          });

      var symbolCells = cells.filter( function(d,i) {
              return matrixRows[i].symbol != null;
           });

      cells.each( function(d,i) {
         var symbolFunction = d.symbolFunction;
         if (symbolFunction) {
           d3.select(this).call(symbolFunction, {'cellSize': (cellWidth != null ? cellWidth : cellSize)});
         }
      });

      cols.append('rect')
          .attr('class', 'colbox')
          .attr('x', function(d,i) { 
            return 0;
          })
          .attr('height', function(d, i) { 
            return matrixHeight - 1;
          })
          .attr('y', function(d,i) {
            if (cellHeights && cellHeights.length > 0) {
              return firstCellHeight;
            } else {
              return y(matrixRowNames[0]) + y.rangeBand();
            }

          }) 
          .attr('width',  (cellWidth != null ? cellWidth : cellSize) - 1);


      g.selectAll('rect.cellbox')
           .on("mouseover", function(d) {  
              var colObject = d3.select(this.parentNode.parentNode).datum();

              var column = d3.select(this.parentNode.parentNode);
              column.classed("active", true);

              // Get screen coordinates of column.  We will use this to position the
              // tooltip above the column.
              var matrix = column.node()
                         .getScreenCTM()
                         .translate(+column.node().getAttribute("cx"),+column.node().getAttribute("cy"));

              // Firefox doesn't consider the transform (slideout's shift left) with the getScreenCTM() method,
              // so instead the app will use getBoundingClientRect() method instead which does take into consideration
              // the transform. 
              var boundRect = column.node().getBoundingClientRect();   
              colObject.screenXMatrix = d3.round(boundRect.left + (boundRect.width/2)) + margin.left;                     
              //colObject.screenXMatrix = window.pageXOffset + matrix.e + margin.left;
              colObject.screenYMatrix = window.pageYOffset + matrix.f + margin.top;

              dispatch.d3mouseover(colObject); 


            })                  
           .on("mouseout", function(d) {      
              var column = d3.select(this.parentNode.parentNode);
              column.classed("active", false);

              dispatch.d3mouseout(); 
            })
            .on("click", function(d, i) {                
              var colObject = d3.select(this.parentNode.parentNode).datum();

              if (d.clickFunction) {
                d.clickFunction(colObject, d);
              }

              var colIndex = Math.floor(i / matrixRowNames.length);  
              var on = !(d3.select(this.parentNode.parentNode).select(".colbox").attr("class").indexOf("current") > -1);
              d3.select(this.parentNode.parentNode.parentNode).select(".colbox.current").classed("current", false);             
              if (on) {
                d3.select(this.parentNode.parentNode).select(".colbox").classed("current", on);
                dispatch.d3click(colObject);
              } else {
                dispatch.d3click();
              }
            });


      // update 
      /*
      if (showTransition) {
        cols.transition()
            .duration(1000)
            .attr('transform', function(d,i) { 
                return "translate(" + (x.rangeBand() * (i+1)) + ",0)";
            });


        cols.selectAll('rect.cell')
              .transition()        
              .duration(1000)
              .attr('x', function(d,i) { 
                return 0;
              })
              .attr('width', function(d) { 
                return  cellSize - 1;
              })
              .attr('y', function(d, i) {             
                return y(matrixRowNames[i]) + y.rangeBand();
              })
              .attr('height', function(d) { 
                return cellSize - 1; 
              });
 
      }
      */
    });

  }

  chart.columnLabel = function(_) {
    if (!arguments.length) {
      return columnLabel;
    } else {
      columnLabel = _;
      return chart;
    }
  }

  chart.margin = function(_) {
    if (!arguments.length) return margin;
    margin = _;
    return chart;
  };

  chart.width = function(_) {
    if (!arguments.length) return width;
    width = _;
    return chart;
  };

  chart.height = function(_) {
    if (!arguments.length) return height;
    height = _;
    return chart;
  };

  chart.widthPercent = function(_) {
    if (!arguments.length) return widthPercent;
    widthPercent = _;
    return chart;
  };

  chart.heightPercent = function(_) {
    if (!arguments.length) return heightPercent;
    heightPercent = _;
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


  chart.showTransition = function(_) {
    if (!arguments.length) return showTransition;
    showTransition = _;
    return chart;
  }

  chart.matrixRows = function(_) {
    if (!arguments.length) return matrixRows;
    matrixRows = _;
    matrixRowNames = matrixRows.map( function(row) {
      return row.name;
    });
    colorScale = colorbrewer.YlGnBu[matrixRows.length+1];
    return chart;
  }

  chart.cellSize = function(_) {
    if (!arguments.length) return cellSize;
    cellSize = _;
    return chart;
  }


  chart.cellWidth = function(_) {
    if (!arguments.length) {
      return cellWidth;
    } else {
      cellWidth = _;
      return chart;
    }
  }

  chart.cellHeight = function(_) {
    if (!arguments.length) {
      return cellHeight;
    } else {
      cellHeight = _;
      return chart;
    }
  }

  chart.cellHeights = function(_) {
    if (!arguments.length) {
      return cellHeights;
    } else {
      cellHeights = _;
      return chart;
    }
  }

  chart.rowLabelWidth = function(_) {
    if (!arguments.length) return rowLabelWidth;
    rowLabelWidth = _;
    return chart;
  }
  chart.columnLabelHeight = function(_) {
    if (!arguments.length) return columnLabelHeight;
    columnLabelHeight = _;
    return chart;
  }
  chart.tooltipHTML = function(_) {
    if (!arguments.length) return tooltipHTML;
    tooltipHTML = _;
    return chart;
  }


  
  // This adds the "on" methods to our custom exports
  d3.rebind(chart, dispatch, "on");

  return chart;
}