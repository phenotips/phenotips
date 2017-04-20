function Legend() {
	this.legendMap = {
		exon:     {width: 190, tooltip: 'The segment of DNA in the gene containing information coding for a protein'},
		HIGH:     {width: 170, tooltip: 'A variant having a high (disruptive) impact in the protein.'},
		MODERATE: {width: 170, tooltip: 'A non-disruptive variant that might change the protein effectiveness.'},
		MODIFIER: {width: 240, tooltip: 'Usually non-coding variants or variants affecting non-coding genes, where predictions are difficult or there is no evidence of impact.'},
		LOW:      {width: 170, tooltip: 'Assumed to be mostly harmless or unlikely to change protein behavior.'},
		snp:      {width: 240, tooltip: 'A single nucleotide change.  Multiple nucleotide changes (e.g. AT->GG) are also also identified by the square symbol'},
		ins:      {width: 210, tooltip: 'An insertion of a nucleotide(s)'},
		del:      {width: 190, tooltip: 'A deletion of nucleotide(s)'},
		complex:  {width: 240, tooltip: 'A complex variant, usually involving a combination of insertions and deletions of nucleotides'}
	}
}

Legend.prototype.init = function() {
	var me = this;

	d3.selectAll(".legend-element")
	  .on("mouseover", function(d) {
	  	var legendId = d3.select(this).attr("id");
	  	me.showLegendTooltip(legendId, d3.event.pageX, d3.event.pageY);

      })
      .on("mouseout", function(d) {
      	me.hideLegendTooltip();
      });
}

Legend.prototype.showLegendTooltip = function(legendId, screenX, screenY) {
	var me = this;
	var tooltip = d3.select('#legend-tooltip');

	var legendObject =  this.legendMap[legendId];
	if (legendObject == null) {
		return;
	}

	var tooltipText = legendObject.tooltip;

	tooltip.style("z-index", 9999);
	tooltip.transition()
	 .duration(1000)
	 .style("opacity", .9)
	 .style("pointer-events", "all");

	tooltip.html(tooltipText);
	var h = tooltip[0][0].clientHeight;
	var w = legendObject.width;


	var x = screenX - 10;
	var y = screenY - 10;

	tooltip.style("width", w + "px")
		       .style("left", x + "px")
		       .style("text-align", 'left')
		       .style("top", (y - h) + "px")
		       .style("overflow-y", "hidden");

}

Legend.prototype.hideLegendTooltip = function() {
	var tooltip = d3.select('#legend-tooltip');
	tooltip.transition()
           .duration(500)
           .style("opacity", 0)
           .style("z-index", 0)
           .style("pointer-events", "none");
}
