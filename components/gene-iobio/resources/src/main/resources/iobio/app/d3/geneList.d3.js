 function geneListD3() {
	var dispatch = d3.dispatch("d3click");

	var svg = null;

	var margin = {top: 10, right: 10, bottom: 10, left: 50},
		width = 200,
		barHeight = 20,
		labelWidth = 100,
		gap = 2,
		defaults = {},

	    xValue = function(d) { return d[0]; },
	    yValue = function(d) { return d[1]; },
	  
	    tooltipText = function(d, i) {
	      return d[0] + ", " + d[1]; 
	    },
  	    x = d3.scale.linear(),
	    y = d3.scale.ordinal();

	var widthPercent = null;
	var heightPercent = null;
	  
	function chart(selection, options) {
		var me = this;
		// merge options and defaults
		options = $.extend(defaults,options);
   
	    selection.each(function(data) {
	        // select the container for the svg
		    var container  = d3.select(this);
	    
		    var height = barHeight * data.length;
			var innerHeight = height - margin.top - margin.bottom;  
			var innerWidth = width - margin.left - margin.right;  
	       
			x = d3.scale.linear()
						 .domain([0, d3.max(data, function(d) { return xValue(d);})])
						 .range([0, innerWidth]);

			
			y = d3.scale.ordinal()
					    .domain(function(d){ return yValue(d) })
					    .rangeBands([0, (barHeight + 2 * gap) * data.length]);

			container.selectAll("svg").remove();		    


			// Select the svg element, if it exists.
		   	var svgData = container.selectAll("svg").data([0]);




		   	// Create the svg select if it doesn't already exist
			var svg = svgData.enter()
					    .append('svg')
					    .attr('class', 'chart')
					    .attr('width',  width)
					    .attr('height', (barHeight + gap * 2) * data.length + 30)
					    .append("g")
					    .attr("transform", "translate(" + margin.left +", " + margin.top + ")");



		    var line = svg.selectAll('.line')
		                .data(data)    
		                .enter()
		                .append("g")
		                .attr("class", "line")
		                .attr("transform", function(d, i) { 
		                	var y = i*(barHeight + gap * 2) + (barHeight / 2) + 2; 
		                	return "translate(0," + y + ")";
		                });

			line.append("text")
			 .attr("x", 0)
			 .attr("y", 0 )
			 .attr("dx", -5)
			 .attr("dy", ".36em")
			 .attr("text-anchor", "start")
			 .attr('class', 'score')
			 .text(function(d, i) {return i+1 + "."});		    	

			line.append("rect")
			    .attr("class", "cellbox")
			    .attr("x", 20)
			    .attr("y", -6)
			    .attr("height", "13")
			    .attr("width", "13")
			    .style("stroke", "rgb(230,230,230")
			    .style("stroke-width", 1.5)
			    .style("fill", "transparent")
			    .style("cursor", "pointer")
			    .on("click", function(d,i) {
					d.selected = d.selected ? false : true;

					d3.select(this.parentNode)
					  .select("g use")
					  .style("fill", function(d,i) {
				    	if (d.selected) {
				    		return "#81A966";
				    	} else {
				    		return "none";
				    	}
				      });

				   
					dispatch.d3click(d);
			    })
			    .on("mouseover", function(d,i) {
			    	d3.select(this).style("fill", "rgb(242,242,242)");

			    	d3.select(this.parentNode)
					  .select("text.name")
					  .style("font-weight", "bold");

			    })
			    .on("mouseout", function(d,i) {
			    	d3.select(this).style("fill", "transparent");

			    	d3.select(this.parentNode)
					  .select("text.name")
					  .style("font-weight", "normal");
			    })

			line.append("g")
			    .attr("transform", "translate(20,-5)")
					.append("use")
					.attr("xlink:href", '#checkmark-symbol')
					.attr("width", 12)
					.attr("height", 12)
					.style("fill", function(d,i) {
				    	if (d.selected) {
				    		return "#81A966";
				    	} else {
				    		return "none";
				    	}
				    })
					.style("pointer-events", "none");


		

			line.append("text")
				 .attr("x", 40)
				 .attr("y", 0 )
				 .attr("dy", ".36em")
				 .attr("text-anchor", "start")
				 .attr('class', 'name')				 
				 .text(function(d) {return yValue(d)});	
			if (!options.simpleGeneList) {
				line.append("text")
				     .attr("class", "score")
					 .attr("x", function(d) { return 120; })
					 .attr("y", 0 )
					 .attr("dx", -5)
					 .attr("dy", ".36em")
					 .attr("text-anchor", "start")
					 .attr('class', 'score')
					 .text(function(d) {return xValue(d)});

			}
			

		});
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

	chart.barHeight = function(_) {
	    if (!arguments.length) return barHeight;
	    barHeight = _;
	    return chart;
	};

	chart.xValue = function(_) {
	    if (!arguments.length) return xValue;
	    xValue = _;
	    return chart;
	};

	chart.yValue = function(_) {
	    if (!arguments.length) return yValue;
	    yValue = _;
	    return chart;
	};


	chart.gap = function(_) {
	    if (!arguments.length) return gap;
	    gap = _;
	    return chart;
	};

	chart.labelWidth = function(_) {
	    if (!arguments.length) return labelWidth;
	    labelWidth = _;
	    return chart;
	};
	  
	chart.brush = function(_) {
	    if (!arguments.length) return brush;
	    brush = _;
	    return chart; 
	};


	chart.tooltipText = function(_) {
	    if (!arguments.length) return tooltipText;
	    tooltipText = _;
	    return chart;
	};

	// This adds the "on" methods to our custom exports
	d3.rebind(chart, dispatch, "on");

	return chart;
}
