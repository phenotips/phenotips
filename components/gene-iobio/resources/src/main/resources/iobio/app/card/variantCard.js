// Constructor
function VariantCard() {

	this.model = null;

  	this.vcfChart = null;
	this.afChart = null;
	this.zoomRegionChart = null;
	this.bamDepthChart = null;	 

	this.cardSelector = null;
	this.d3CardSelector = null;
	this.cardIndex = null;
}



VariantCard.prototype.getName = function() {
	return this.model.getName();
}

VariantCard.prototype.getRelationship = function() {
	return this.model.getRelationship();
}

VariantCard.prototype.setName = function(theName) {
	this.model.setName(theName);
}

VariantCard.prototype.setRelationship = function(theRelationship) {
	this.model.setRelationship(theRelationship);
}

VariantCard.prototype.setAffectedStatus = function(theAffectedStatus) {
	this.model.setAffectedStatus(theAffectedStatus);
}

VariantCard.prototype.setSampleName = function(sampleName) {
	this.model.setSampleName(sampleName);
	if (this.isViewable()) {
		this.setVariantCardLabel();
	}
}

VariantCard.prototype.getSampleName = function() {
	return this.model.getSampleName();
}

VariantCard.prototype.setDefaultSampleName = function(sampleName) {
	this.model.setDefaultSampleName(sampleName);
}

VariantCard.prototype.getDefaultSampleName = function() {
	return this.model.getDefaultSampleName();
}

VariantCard.prototype.getCardIndex = function() {
	return this.cardIndex;
}

VariantCard.prototype.isViewable = function() {
	return this.model.relationship != 'sibling';
}

VariantCard.prototype.isInheritanceLoaded = function() {
	return this.model.isInheritanceLoaded();
}

VariantCard.prototype.isReadyToLoad = function() {
	return this.model.isReadyToLoad();
}

VariantCard.prototype.isLoaded = function() {
	return this.model.isLoaded();
}

VariantCard.prototype.hasDataSources = function() {
	return this.model.isReadyToLoad();
}


VariantCard.prototype.isBamLoaded = function() {
	return this.model.isBamLoaded();
}

VariantCard.prototype.variantsHaveBeenCalled = function() {
	return this.model.variantsHaveBeenCalled();
}

VariantCard.prototype.getRelationship = function() {
	return this.model.getRelationship();
}

VariantCard.prototype.summarizeDanger = function(geneName, data) {
	var dangerSummary = VariantModel.summarizeDanger(data);
	this.model.cacheDangerSummary(dangerSummary, geneName);
	return dangerSummary;
}

VariantCard.prototype.summarizeError = function(geneName, error) {
	var dangerSummary = VariantModel.summarizeError(error);
	this.model.cacheDangerSummary(dangerSummary, geneName);
	return dangerSummary;
}

VariantCard.prototype.getDangerSummaryForGene = function(geneName) {
	return this.model.getDangerSummaryForGene(geneName);
}

VariantCard.prototype.promiseCacheVariants = function(ref, geneObject, transcript) {
	return this.model.promiseCacheVariants(ref, geneObject, transcript);
}

VariantCard.prototype.isCached = function(geneName, transcript) {
	return this.model.isCached(geneName, transcript);
}

VariantCard.prototype.hide = function() {
	this.cardSelector.addClass("hide");
}

VariantCard.prototype.isolateVariants = function(variants) {
	if (variants != null && variants.length > 0) {
		this.d3CardSelector.selectAll("#vcf-variants .variant")
		    .style("opacity", .1);
		this.d3CardSelector.selectAll("#vcf-variants .variant")
		    .filter( function(d,i) {
		     	var found = false;
		     	variants.forEach(function(variant) {
			        if (d.start == variant.start 
			        	&& d.end == variant.end 
			        	&& d.ref == variant.ref 
			        	&& d.alt == variant.alt 
			        	&& d.type.toLowerCase() == variant.type.toLowerCase()) {
			          found = true;
			        } 
		     	});
		     	return found;
		     })
		     .style("opacity", 1);  

		this.d3CardSelector.selectAll("#fb-variants .variant")
		    .style("opacity", .1);
		this.d3CardSelector.selectAll("#fb-variants .variant")
		    .filter( function(d,i) {
		     	var found = false;
		     	variants.forEach(function(variant) {
			        if (d.start == variant.start 
			        	&& d.end == variant.end 
			        	&& d.ref == variant.ref 
			        	&& d.alt == variant.alt 
			        	&& d.type.toLowerCase() == variant.type.toLowerCase()) {
			          found = true;
			        } 
		     	});
		     	return found;
		     })
		     .style("opacity", 1);  				
	} else {
		this.d3CardSelector.selectAll("#vcf-variants .variant")
     			.style("opacity", 1);
     	this.d3CardSelector.selectAll("#fb-variants .variant")
     			.style("opacity", 1);
	} 

}


VariantCard.prototype.init = function(cardSelector, d3CardSelector, cardIndex) {
	var me = this;	

	// init model
	this.model = new VariantModel();
	this.model.init();

	this.cardIndex = cardIndex;


	if (this.isViewable()) {
		this.cardSelector = cardSelector;
		this.d3CardSelector = d3CardSelector;


		// If the we are in guided tour mode, then clicking anywhere in the variant card unlocks
		// any locked variant.
		if (isLevelEdu) {
			me.cardSelector.on('click', function() {
				//me.unpin();
				
			});
		}

		this.cardSelector.find('#variant-panel').attr("id", "variant-panel-" + cardIndex);


		// This is an x-axis for the selected region		    
		this.zoomRegionChart = geneD3()
				    .widthPercent("100%")
				    .heightPercent("100%")
				    .width(1000)
				    .margin({top: 0, right: isLevelBasic || isLevelEdu ? 7 : 2, bottom: 0, left: isLevelBasic || isLevelEdu ? 9 : 4})
				    .showXAxis(false)
				    .showBrush(false)
				    .trackHeight(isLevelEdu || isLevelBasic ? 32 : 16)
				    .cdsHeight(isLevelEdu || isLevelBasic ? 24 : 12)
		    		.showLabel(false)
		    		.on("d3featuretooltip", function(featureObject, feature, tooltip) {
		    				    			
		    			
		    			var coord = getTooltipCoordinates(featureObject.node(), tooltip, true);

		    			tooltip.transition()        
			                   .duration(200)      
			                   .style("opacity", .9);   
			            tooltip.html(feature.feature_type + ': ' + addCommas(feature.start) + ' - ' + addCommas(feature.end))       
							   .style("left", coord.x + "px") 
				               .style("text-align", 'left')    
				               .style("top", (coord.y - 4) + "px");    
		    		});


		// Create the coverage chart
		this.bamDepthChart = lineD3()
		                    .width(1000)
		                    .height( 35 )
		                    .widthPercent("100%")
		                    .heightPercent("100%")
		                    .kind("area")
							.margin( {top: 10, right: isLevelBasic || isLevelEdu ? 7 : 2, bottom: 20, left: isLevelBasic || isLevelEdu ? 9 : 4} )
							.showXAxis(true)
							.showYAxis(false)
							.showTooltip(true)
							.pos( function(d) { return d[0] })
					   		.depth( function(d) { return d[1] })
					   		.formatCircleText( function(pos, depth) {
					   			return depth + 'x' ;
					   		});


		// Create the vcf track
		this.vcfChart = variantD3()
				    .width(1000)
				    .margin({top: 0, right: isLevelBasic || isLevelEdu ? 7 : 2, bottom: isLevelEdu  || isLevelBasic ? 12 : 17, left: isLevelBasic || isLevelEdu ? 9 : 4})
				    .showXAxis(isLevelEdu  || isLevelBasic ? false : true)
				    .variantHeight(isLevelEdu  || isLevelBasic ? EDU_TOUR_VARIANT_SIZE : 6)
				    .verticalPadding(2)
				    .showBrush(false)
				    .tooltipHTML(variantTooltip.formatContent)
				    .on("d3rendered", function() {
				    	
				    })
				    .on('d3click', function(d) {
				    	if (d != clickedVariant) {
					    	clickedVariant = isLevelBasic ? null : d;
					    	clickedVariantCard = me;
					    	me.showCoverageCircle(d, me);
					    	window.showCircleRelatedVariants(d, me);
				    	} else {
				    		me.unpin();
				    	}
					})			    
				    .on('d3mouseover', function(d) {
				    	if (clickedVariant == null) {
					    	me.showCoverageCircle(d, me);
					    	window.showCircleRelatedVariants(d, me);
				    	}
					})
					.on('d3mouseout', function() {
						if (clickedVariant == null) {
							hideCoordinateFrame();
							me.hideCoverageCircle();
							window.hideCircleRelatedVariants();
							matrixCard.clearSelections();
						}

					});

		// The 'missing variants' chart, variants that freebayes found that were not in orginal
		// variant set from vcf
		this.fbChart = variantD3()
				    .width(1000)
				    .margin({top: 0, right: isLevelBasic || isLevelEdu ? 7 : 2, bottom: 10, left: isLevelBasic || isLevelEdu ? 9 : 4}) // bottom margin for missing variant x when no vcf variants loaded
				    .showXAxis(false)
				    .variantHeight(6)
				    .verticalPadding(2)
				    .showBrush(false)
				    .tooltipHTML(variantTooltip.formatContent)
				    .on("d3rendered", function() {
				    	
				    })	
				    .on('d3click', function(d) {
				    	if (d != clickedVariant) {
					    	clickedVariant = isLevelBasic ? null : d;
					    	clickedVariantCard = me;
					    	me.showCoverageCircle(d, me);
					    	window.showCircleRelatedVariants(d, me);
				    	} else {
				    		me.unpin();
				    	}
					})				    
				    .on('d3mouseover', function(d) {
						if (clickedVariant == null) {
					    	me.showCoverageCircle(d, me);
					    	window.showCircleRelatedVariants(d, me);
				    	}

					})
					.on('d3mouseout', function() {
						if (clickedVariant == null) {					    
							me.hideCoverageCircle();
							window.hideCircleRelatedVariants();
							matrixCard.clearSelections();
						}
					});
					

	 	// Create allele frequency chart
	 	// Allele freq chart)
		// TODO:  Replace this with actual frequency after af grabbed from population (1000G/ExAC)
	    this.afChart = histogramD3()
	                       .width(400)
	                       .height(70)
						   .margin( {left: 40, right: 0, top: 0, bottom: 20})
						   .xValue( function(d, i) { return d[0] })
						   .yValue( function(d, i) { return Math.log(d[1]) })
						   .yAxisLabel( "log(frequency)" );
						   
		this.afChart.formatXTick( function(d,i) {
			return (d * 2) + '%';
		});
		this.afChart.tooltipText( function(d, i) { 
			var value = vcfAfData[i][1];
			var lowerVal =  d[0]      * 2;
			var upperVal = (d[0] + 1) * 2;
			return  d3.round(value) + ' variants with ' + lowerVal + "-" + upperVal + '%' + ' AF ';
		});

		this.cardSelector.find('#shrink-button').on('click', function() {
			me.shrinkCard(true);
		});
		this.cardSelector.find('#expand-button').on('click', function() {
			me.shrinkCard(false);
		});
		this.cardSelector.find('#minimize-button').on('click', function() {
			me.minimizeCard(true);
		});

		// Listen for side bar open and close events and adjust the position
		// of the tooltip and the variant circle if a variant is 'locked'.
		$('#slider-left').on("open", function() {
			if (clickedVariant) {
				me.adjustTooltip(clickedVariant);
			}
		});
		$('#slider-left').on("close", function() {
			if (clickedVariant) {
				me.adjustTooltip(clickedVariant);
			}
		});

	}


};


VariantCard.prototype.onBamFilesSelected = function(event, callback) {
	this.model.promiseBamFilesSelected(event).then( function(fileName) {
		callback(fileName);
	});
}

VariantCard.prototype.onBamUrlEntered = function(bamUrl, callback) {
	this.model.onBamUrlEntered(bamUrl, function(success) {
		if (success) {
			if (bamUrl == null || bamUrl.trim() == "") {
				this.cardSelector.find("#bam-track").addClass("hide");
				this.cardSelector.find(".covloader").addClass("hide");
				this.cardSelector.find('#zoom-region-chart').css("visibility", "visible");

				this.cardSelector.find("#fb-chart-label").addClass("hide");
				this.cardSelector.find("#fb-separator").addClass("hide");
				this.cardSelector.find("#fb-variants").addClass("hide");
				this.cardSelector.find("#called-variant-count").text("");
			}			
		}
		if (callback) {
			callback(success);
		}

	});
}

VariantCard.prototype.onVcfFilesSelected = function(event, callback, callbackError) {
	var me = this;
	if (this.isViewable()) {
		this.cardSelector.find('#vcf-track').removeClass("hide");
		this.cardSelector.find('#vcf-variants').css("display", "none");
		this.cardSelector.find(".vcfloader").addClass("hide");
	}
	this.model.promiseVcfFilesSelected(event)
	          .then(function(resolveObject) {
				me.cardSelector.find('#vcf-name').text(resolveObject.fileName);
				callback(resolveObject.fileName, resolveObject.sampleNames);		
	          },
	          function(error) {
				if (callbackError) {
					callbackError(error);
				}
	          });
		
}

VariantCard.prototype.clearVcf = function() {
	this.model.clearVcf(this.cardIndex);

	this.cardSelector.find('#vcf-track').addClass("hide");
	this.cardSelector.find('#vcf-variants').css("display", "none");
	this.cardSelector.find(".vcfloader").addClass("hide");
	this.cardSelector.find('#vcf-variant-card-label').text("");
	this.cardSelector.find('#gene-box').text("");
	this.cardSelector.find('#gene-box').css("visibility", "hidden");


	this.cardSelector.find('#vcf-variant-count-label').addClass("hide");
	this.cardSelector.find('#vcf-variant-count').text("");
	this.cardSelector.find('#called-variant-count-label').addClass("");
	this.cardSelector.find('#called-variant-count').text("");
}

VariantCard.prototype.clearBam = function() {
	this.model.clearBam(this.cardIndex);
	this.cardSelector.find('#bam-track').addClass("hide");
}


VariantCard.prototype.onVcfUrlEntered = function(vcfUrl, callback) {
	var me = this;
	if (me.isViewable()) {
		me.cardSelector.find('#vcf-track').removeClass("hide");
		me.cardSelector.find('#vcf-variants').css("display", "none");
		me.cardSelector.find(".vcfloader").addClass("hide");
	 
	}
	this.model.onVcfUrlEntered(vcfUrl, 
		function(success, samples) {
			callback(success, samples);
		});
}


VariantCard.prototype.showDataSources = function(dataSourceName) {
	this.model.setName(dataSourceName);
	$('#add-datasource-container').css('display', 'none');

    var title = isLevelBasic && this.model.getRelationship() == "proband" ? "" : this.model.getRelationship();
    if (!isLevelBasic) {
	    if (title == null || title == '' || title == 'NONE') {
			title = 'Sample';
		}    	
    }

	this.setVariantCardLabel();
   	this.cardSelector.find('#card-relationship-label').text(title);
   	if (window.gene) {
   		this.cardSelector.find('#gene-box').text('GENE ' + window.gene.gene_name);
   	}

}

VariantCard.prototype.setVariantCardLabel = function() {
	
	if (isLevelEdu) {
		this.cardSelector.find('#variant-card-label').text(this.model.getName() + "'s Variants"  );
	} else if (isLevelBasic) {
		this.cardSelector.find('#variant-card-label').text(this.model.getName());
	} else {
		this.cardSelector.find('#variant-card-label').text(
   			this.model.getName() == this.model.getSampleName()  ? 
   		  	this.model.getName() : 
   		  	this.model.getSampleName() + " " + this.model.getName());
	}

}


VariantCard.prototype.loadBamDataSource = function(dataSourceName, callback) {
	var me = this;
	this.model.loadBamDataSource(dataSourceName, function() {
		me.showDataSources(dataSourceName);

		selection = me.d3CardSelector.select("#zoom-region-chart").datum([window.selectedTranscript]);
		me.zoomRegionChart.regionStart(+window.gene.start);
		me.zoomRegionChart.regionEnd(+window.gene.end);
		me.zoomRegionChart(selection);

		callback();
	});
}

VariantCard.prototype.shrinkCard = function(shrink) {

	this.minimizeCard(false);
	this.d3CardSelector.select('#variant-right-labels').classed("hide", shrink);
	this.d3CardSelector.select('#vcf-chart-label').classed("hide", shrink);
	this.d3CardSelector.select('#variant-right-labels').classed("hide", shrink);

	this.d3CardSelector.select('#zoom-region-chart').classed("hide", shrink);
	this.d3CardSelector.select('#bam-track').classed("hide", shrink);

	this.cardSelector.css("padding-bottom", shrink ? "4px" : "10px");

	this.d3CardSelector.select('#shrink-button').classed("disabled", shrink);
	this.d3CardSelector.select('#expand-button').classed("disabled", !shrink);
	this.d3CardSelector.select('#minimize-button').classed("disabled", false);

}

VariantCard.prototype.minimizeCard = function(minimize) {
	this.d3CardSelector.select('#variant-right-labels').classed("hide", minimize);
	this.d3CardSelector.select('#vcf-chart-label').classed("hide", minimize);
	this.d3CardSelector.select('#variant-right-labels').classed("hide", minimize);

	this.d3CardSelector.select('#variant-panel-' + this.cardIndex).classed("hide", minimize);
	this.cardSelector.css("padding-bottom", minimize ? "4px" : "10px");

	this.d3CardSelector.select('#shrink-button').classed("disabled", false);
	this.d3CardSelector.select('#expand-button').classed("disabled", false);
	this.d3CardSelector.select('#minimize-button').classed("disabled", true);
}

VariantCard.prototype.clearBamChart = function() {
	this.cardSelector.find("#bam-depth svg").remove();
	this.cardSelector.find('#bam-depth').css("visibility", "hidden");
	this.cardSelector.find('#bam-chart-label').css("visibility", "hidden");
	this.cardSelector.find('#bam-chart-label').css("margin-bottom", "0px");
	this.cardSelector.find('#fb-chart-label').addClass("hide");
	this.cardSelector.find('#fb-separator').addClass("hide");
}

VariantCard.prototype.showBamProgress = function(message) {
	this.cardSelector.find("#bam-track").removeClass("hide");
	this.cardSelector.find(".covloader").removeClass("hide");
	this.cardSelector.find(".covloader .loader-label").text(message);
	this.cardSelector.find("#bam-depth").css("visibility", "hidden");
	this.cardSelector.find("#bam-chart-label").css("visibility", "hidden");
	this.cardSelector.find("#bam-chart-label").css("margin-bottom", "0px");
}

VariantCard.prototype.endBamProgress = function() {
	this.cardSelector.find("#bam-track").removeClass("hide");
	this.cardSelector.find(".covloader").addClass("hide");
	this.cardSelector.find(".covloader .loader-label").text("");
	this.cardSelector.find("#bam-depth").css("visibility", "visible");
	this.cardSelector.find("#bam-chart-label").css("visibility", "visible");
	this.cardSelector.find("#bam-chart-label").css("margin-bottom", "-17px");

}

VariantCard.prototype.endVariantProgress = function() {
	this.cardSelector.find(".vcfloader").addClass("hide");
}


/*
 * Load variant data only (for unaffected sibs). 
 * no variant card display
 */
VariantCard.prototype.loadVariantsOnly = function(callback) {
	this.model.promiseGetVariantsOnly(window.gene, window.selectedTranscript).then( function(data) {
		callback(data);
	});
}

VariantCard.prototype.clearWarnings = function() {
	this.cardSelector.find("#multiple-sample-warning").addClass("hide");
	this.cardSelector.find('#no-variants-warning').addClass("hide");
	this.cardSelector.find('#clinvar-warning').addClass("hide");
	this.cardSelector.find('#no-ref-found-warning').addClass("hide");
	this.cardSelector.find('#error-warning').addClass("hide");	
}

/* 
* A gene has been selected.  Load all of the tracks for the gene's region.
*/
VariantCard.prototype.promiseLoadAndShowVariants = function (classifyClazz, fullRefresh) {
	var me = this;

	return new Promise( function(resolve, reject) {
		if (fullRefresh) {
			me.prepareToShowVariants(classifyClazz);
		}
		
		// Clear out previous variant data and set up variant card
		// to show that loading messages.
		if (me.isViewable()) {

			// Load the variant chart.			
			me._showVariants( regionStart, 
				regionEnd, 
				function() {						
					readjustCards();
					resolve();
				},
				true);
		} else {
			resolve();
		}


	});
	
	
}

VariantCard.prototype.prepareToShowVariants = function(classifyClazz) {
	var me = this;

	me.cardSelector.removeClass("hide");

	// Reset any previous locked variant
	clickedVariant = null;
	clickedVariantCard = null;
	window.hideCircleRelatedVariants();
	me.unpin();


	// Clear out the previous gene's data
	me.model.wipeGeneData();

	me.cardSelector.find(".filter-flag").addClass("hide");
	me.clearWarnings();

	if (me.isViewable()) {
		//filterCard.clearFilters();

		me.vcfChart.clazz(classifyClazz);
		me.fbChart.clazz(classifyClazz);

		if (me.model.isBamLoaded() || me.model.isVcfLoaded()) {	      
			me.cardSelector.find('#zoom-region-chart').css("visibility", "hidden");

			// Workaround.  For some reason, d3 doesn't clean up previous transcript
			// as expected.  So we will just force the svg to be removed so that we
			// start with a clean slate to avoid the bug where switching between transcripts
			// resulted in last transcripts features not clearing out.
			me.d3CardSelector.select('#zoom-region-chart svg').remove();

			selection = me.d3CardSelector.select("#zoom-region-chart").datum([window.selectedTranscript]);
			me.zoomRegionChart.regionStart(+window.gene.start);
			me.zoomRegionChart.regionEnd(+window.gene.end);
			me.zoomRegionChart(selection);

		}


    	me.cardSelector.find('#displayed-variant-count-label').addClass("hide");
    	me.cardSelector.find('#displayed-variant-count-label-simple').css("visibility", "hidden");
    	me.cardSelector.find('#displayed-variant-count-label-basic').addClass("hide");
    	me.cardSelector.find('#displayed-variant-count').text("");
    	me.cardSelector.find('#vcf-variant-count-label').addClass("hide");
    	me.cardSelector.find('#vcf-variant-count').text("");
    	me.cardSelector.find('#called-variant-count-label').addClass("hide");
    	me.cardSelector.find('#called-variant-count').text("");
    	me.cardSelector.find('#gene-box').text("");
    	me.cardSelector.find('#gene-box').css("visibility", "hidden");
    	if (isLevelEdu && eduTourNumber == "1") {
	    	me.cardSelector.find("#gene-box").addClass("deemphasize");
    	}



		me.cardSelector.find('#vcf-track').removeClass("hide");
		me.cardSelector.find('#vcf-variants').css("display", "none");
		me.cardSelector.find('#vcf-chart-label').addClass("hide");
		me.cardSelector.find('#vcf-name').addClass("hide");	

		me.cardSelector.find('#fb-variants').addClass("hide");

		if (me.getRelationship() == 'proband') {
			$("#feature-matrix").addClass("hide");
			$("#feature-matrix-note").addClass("hide");
			$('#move-rows').addClass("hide");			
		}

		if (me.model.isVcfLoaded()) {
			me.cardSelector.find(".vcfloader").removeClass("hide");
			me.cardSelector.find(".vcfloader .loader-label").text("Loading variants for gene")			
		} else {
			$("#filter-and-rank-card").addClass("hide");
		}
	}	
}

VariantCard.prototype.setLoadState = function(theState) {
	var theVcfData = this.model.getVcfDataForGene(window.gene, window.selectedTranscript);
	if (theVcfData) {
		this.model.setLoadState(theVcfData, theState);
	}
}

VariantCard.prototype.onBrush = function(brush, callback) {
	var me = this;
	if (brush.empty()) {
		this.cardSelector.find("#region-flag").addClass("hide");
		// Only remove if no other filter flags are on
		if (this.cardSelector.find(".filter-flag.hide").length == this.cardSelector.find(".filter-flag").length) {
			this.cardSelector.find('#displayed-variant-count-label').addClass("hide");
			this.cardSelector.find("#displayed-variant-count").addClass("hide");
	    	this.cardSelector.find('#displayed-variant-count-label-simple').css("visibility", "hidden");
	    	this.cardSelector.find('#displayed-variant-count-label-basic').addClass("hide");
		}
	} else {
		this.cardSelector.find("#region-flag").removeClass("hide");
		this.cardSelector.find('#displayed-variant-count-label').removeClass("hide");
		this.cardSelector.find("#displayed-variant-count").removeClass("hide");
		this.cardSelector.find('#displayed-variant-count-label-simple').css("visibility", "visible");
		if (isLevelBasic) {
			this.cardSelector.find('#displayed-variant-count-label-basic').removeClass("hide");
		}


	}

	// Filter the gene model to only show 'features' in selected region
	var filteredTranscript =  $.extend({}, window.selectedTranscript);
	filteredTranscript.features = window.selectedTranscript.features.filter(function(d) {
		  var inRegion = (d.start >= regionStart && d.start <= regionEnd)
                         || (d.end >= regionStart && d.end <= regionEnd) ;
          return inRegion;

	});

    var selection = this.d3CardSelector.select("#zoom-region-chart").datum([filteredTranscript]);
	this.zoomRegionChart.regionStart(!brush.empty() ? regionStart : window.gene.start);
	this.zoomRegionChart.regionEnd(!brush.empty() ? regionEnd : window.gene.end);
	this.zoomRegionChart(selection);
	this.d3CardSelector.select("#zoom-region-chart .x.axis .tick text").style("text-anchor", "start");

	this.cardSelector.find('#zoom-region-chart').css("visibility", "visible");

	this.cardSelector.find('#vcf-track').removeClass("hide");
	this.cardSelector.find('#vcf-variants').css("display", "block");
	this.cardSelector.find(".vcfloader").addClass("hide");
	this.cardSelector.find('#vcf-name').removeClass("hide");		

	this._showBamDepth(regionStart, regionEnd);
	this._showVariants(regionStart, regionEnd, 
		function() {
			me._showFreebayesVariants(regionStart, regionEnd);
			if (callback) {
				callback();
			}
		}, 
		null, true);
}


VariantCard.prototype._showFreebayesVariants = function(regionStart, regionEnd) {
	if (!this.model.hasCalledVariants()) {
		return;
	}

	if (this.isViewable()) {
		var fbDataFiltered = this.model.getCalledVariants(regionStart, regionEnd);
		var filteredVcfData = this.filterVariants(fbDataFiltered, this.fbChart);
		if (regionStart && regionEnd)
  			this._fillFreebayesChart(filteredVcfData, regionStart, regionEnd);
  		else
  			this._fillFreebayesChart(filteredVcfData, window.gene.start, window.gene.end);
	}
}

VariantCard.prototype.promiseLoadBamDepth = function() {	
	var me = this;

	return new Promise( function(resolve, reject) {
		if (!me.model.isBamLoaded()) {		
			resolve(null);
		}


		var callVariantsAndLoadCoverage = function() {
			if (!me.model.isVcfReadyToLoad()) {
				genesCard.showGeneBadgeLoading(window.gene.gene_name);
			}
			
			// If no vcf supplied, automatically call variants (then get coverage)
			if (autoCall && !me.model.isVcfReadyToLoad() && !me.model.hasCalledVariants()) {	
				
				me.callVariants(regionStart, regionEnd, function() {
					loadCoverage();

					// For the proband, we need to determine the inheritance and then
					// fill in the mother/father genotype and allele counts on the
					// proband's variants.  So we do this first before caching
					// the called variants and resolving this promise.
					
					// Once all variant cards have freebayes variants,
					// the app will determine in the inheritance mode
					// for the freebayes variants
					promiseDetermineInheritance(promiseFullTrioCalledVariants).then( function() {
	
						variantCards.forEach(function(variantCard) {
							// Reflect me new info in the freebayes variants.
							variantCard.model.loadCalledTrioGenotypes();

							//  Refresh the feature matrix (proband only) and variant cards
							if (variantCard.getRelationship() == 'proband') {
								variantCard.fillFeatureMatrix(regionStart, regionEnd);
							} else {
								variantCard._showVariants(regionStart, regionEnd, null, false);
							}

						})

					}, function(error) {
						console.log("error when determining inheritance for called variants for " + this.getRelationship() + ". " + error);
					});

				});

			} else {
				// Otherwise, if a vcf was loaded, just get the coverage
				//me.cardSelector.find('#zoom-region-chart').css("margin-top", "0px");	
				loadCoverage();
			}			
		}

		var loadCoverage = function() {
			var coverage = me.model.getBamDataForGene(window.gene);
			if (coverage != null) {
				genesCard.hideGeneBadgeLoading(window.gene.gene_name);
				resolve(coverage);
			} else {
				// If we have varaitns, get coverage for every variant
				me.showBamProgress("Calculating coverage");
				me.model.getBamDepth(window.gene, window.selectedTranscript, function(coverageData) {
					me.endBamProgress();
					genesCard.hideGeneBadgeLoading(window.gene.gene_name);
					resolve(coverageData);
				});
			}					
		};


		if (!me.model.isVcfReadyToLoad() && me.model.isBamLoaded()) {
			if (autoCall == null) {
				alertify.confirm("Automatically call variants from alignments?",
			        function () {	
			        	// ok		     
			        	autoCall = true;  
			        	callVariantsAndLoadCoverage();
			    	},
					function () {
						// cancel
						autoCall = false;
						callVariantsAndLoadCoverage();
					}).set('labels', {ok:'Yes', cancel:'No'}); 
			} else {
				callVariantsAndLoadCoverage();
			}
		} else {
			callVariantsAndLoadCoverage();
		}

		


	});



}

VariantCard.prototype.showBamDepth = function(maxDepth, callbackDataLoaded) {
	this._showBamDepth(regionStart, regionEnd, maxDepth, callbackDataLoaded);
}

VariantCard.prototype._showBamDepth = function(regionStart, regionEnd, maxDepth, callbackDataLoaded) {	
	var me = this;

	filterCard.enableCoverageFilters();

	if (!this.model.isBamLoaded()) {
		// We can still apply the filter coverage if the vcf has the read depth in the
		// genotype field, so go ahead and show the coverage range filter.
		this.cardSelector.find("#bam-track").addClass("hide");
		filterCard.enableCoverageFilters();
		if (callbackDataLoaded) {
			callbackDataLoaded();
		}
		return;
	}


	if (this.isViewable()) {
		this.cardSelector.removeClass("hide");
	}

	var coverage = this.model.getBamDataForGene(window.gene);
	var theVcfData = this.model.getVcfDataForGene(window.gene, selectedTranscript);
	if (coverage != null) {
		me.endBamProgress();
		if (regionStart && regionEnd) {
			var filteredData = me.model.filterBamDataByRegion(coverage, regionStart, regionEnd);
			me._fillBamChart(filteredData, regionStart, regionEnd, maxDepth);
		} else {
			me._fillBamChart(coverage, window.gene.start, window.gene.end, maxDepth);
		}
		if (callbackDataLoaded) {
	   	    callbackDataLoaded();
   	    }
	} else {

		// If we have variants, get coverage for every variant
		me.showBamProgress("Calculating coverage");

		
		this.model.getBamDepth(window.gene, window.selectedTranscript, function(coverageData) {
			me.endBamProgress();
			me._fillBamChart(coverageData, window.gene.start, window.gene.end, maxDepth);

			if (callbackDataLoaded) {
		   	    callbackDataLoaded();
	   	    }

		});

	}


}


VariantCard.prototype._fillBamChart = function(data, regionStart, regionEnd, maxDepth) {
	if (this.isViewable()) {
		// Reduce down to 1000 points
        var reducedData = this.model.reduceBamData(data, 1000);

		this.bamDepthChart.xStart(regionStart);
		this.bamDepthChart.xEnd(regionEnd);

		// Decide if we should show the x-axis.
		this.bamDepthChart.showXAxis(!(this.model.isVcfLoaded()));
		this.bamDepthChart.height(!(this.model.isVcfLoaded()) ? 65 : 45 );
		this.bamDepthChart.margin(!(this.model.isVcfLoaded()) ? {top: 10, right: 2, bottom: 20, left: 4} : {top: 10, right: 2, bottom: 0, left: 4} );
	
		// Detemine the y-scale be setting the maxDepth accross all samples
		if (maxDepth) {
			this.bamDepthChart.maxDepth(maxDepth);
		}

		this.bamDepthChart(this.d3CardSelector.select("#bam-depth").datum(reducedData));		
		this.d3CardSelector.select("#bam-depth .x.axis .tick text").style("text-anchor", "start");

		this.cardSelector.find('#zoom-region-chart').css("visibility", "visible");
	}
}

/*
*  This method is invoked with the variants have been fully annotated, including
*  vep, clinvar, coverage (from alignments), and inheritance (for trio).
*  
*/
VariantCard.prototype.showFinalizedVariants = function() {
	var me = this;
	me.endVariantProgress();
	theVcfData = this.model.getVcfDataForGene(window.gene, window.selectedTranscript);
	// For the proband, the fillFeatureMatrix method will both display the variants
	// in the Ranked Variants card (the feature matrix) as well as the Variant card for
	// the proband sample.  
	if (me.model.getRelationship() == 'proband') {
		me.fillFeatureMatrix(regionStart, regionEnd);
	} else {
		// For mother and father, show the variants in their respective
		// Variant cards.
		me._showVariants(regionStart, regionEnd, null, false);
	}

}



VariantCard.prototype.getBookmarkedVariant = function(variantProxy, data) {
	theVcfData = data != null ? data : this.model.getVcfDataForGene(window.gene, window.selectedTranscript);
	if (theVcfData == null) {
		return null;
	}
	var theVariant = null;
	theVcfData.features.forEach( function (d) {
       if (d.start == variantProxy.start 
          && d.ref == variantProxy.ref 
          && d.alt == variantProxy.alt) {
          theVariant = d;
       }
    });	
    return theVariant;
}


VariantCard.prototype._showVariants = function(regionStart, regionEnd, onVariantsDisplayed, showTransition, isZoom) {
	var me = this;

	// If we have alignments but no vcf, we want to load the called variants and return.
	if (!this.model.isVcfReadyToLoad()) {
		if (me.model.isBamLoaded()) {
			this.cardSelector.removeClass("hide");
		}
		if (!me.model.hasCalledVariants()) {
			genesCard.hideGeneBadgeLoading(window.gene.gene_name);
		} else {
			// Show the proband's (cached) freebayes variants (loaded with inheritance) 
			if (me.model.isBamLoaded()) {	
				filterCard.enableVariantFilters(true);
				me.populateEffectFilters();
				filterCard.enableClinvarFilters();
				var filteredFBData = me.filterCalledVariants();			
				me._fillFreebayesChart(filteredFBData, 
									   regionStart ? regionStart : window.gene.start, 
									   regionEnd ? regionEnd : window.gene.end);
				me.cardSelector.find('#called-variant-count').removeClass("hide");
				me.cardSelector.find('#called-variant-count').text(me.model.getCalledVariantCount());	        	

				if (me.getRelationship() == 'proband') {
					
					me.fillFeatureMatrix(regionStart, regionEnd);
					genesCard.refreshCurrentGeneBadge(null, me.model.getCalledVariants());
				} 
			}
			me.cardSelector.find('#called-variant-count-label').removeClass("hide");
			genesCard.hideGeneBadgeLoading(window.gene.gene_name);
		}
 		if (onVariantsDisplayed) {
			onVariantsDisplayed();
		}
		return;
	}

	//me.cardSelector.find("#zoom-region-chart").css("margin-top", "-30px");
	if (this.isViewable()) {
		this.cardSelector.removeClass("hide");
		this.cardSelector.find('#vcf-track').removeClass("hide");
	}

	var theVcfData = this.model.getVcfDataForGene(window.gene, window.selectedTranscript);
	if (theVcfData) {
		// The user has selected a region to zoom into or the data has come back for a selected gene that
		// has now been cached.  Filter the  variants based on the selected region
		if (this.isViewable()) {
			me.cardSelector.find('.vcfloader').addClass("hide");
			me.cardSelector.find('#vcf-variant-count-label').removeClass("hide");
	        me.cardSelector.find('#vcf-variant-count').text(me.model.getVariantCount(theVcfData));	

			me.clearWarnings();		

	        if (me.model.hasCalledVariants()) {
		        me.cardSelector.find('#called-variant-count-label').removeClass("hide");
				me.cardSelector.find('#called-variant-count').removeClass("hide");
				me.cardSelector.find('#called-variant-count').text(me.model.getCalledVariantCount());	        	
	        } else if (me.model.variantsHaveBeenCalled()) {
	        	// If call variants has occurred but 0 variants returned.
		        me.cardSelector.find('#called-variant-count-label').removeClass("hide");
				me.cardSelector.find('#called-variant-count').removeClass("hide");
				me.cardSelector.find('#called-variant-count').text("0");	        		        	
	        }	


			// Show the proband's (cached) freebayes variants (loaded with inheritance) 
			if (me.model.isBamLoaded()) {
				var filteredFBData = me.filterCalledVariants();			
				me._fillFreebayesChart(filteredFBData, 
									   regionStart ? regionStart : window.gene.start, 
									   regionEnd ? regionEnd : window.gene.end);
			}	
			me.populateRecFilters(theVcfData);
			if (!isZoom) {
				filterCard.autoSetFilters();
			}
			if (me.getRelationship() == 'proband') {
				me.model.pruneIntronVariants(theVcfData);
		    }
		    // Filter variants runs filter and then fills the variant chart.
			var filteredVcfData = this.filterVariants(theVcfData, showTransition);
			me.cardSelector.find('#gene-box').css("visibility", "visible");
			me.cardSelector.find('#gene-box').text('GENE ' + window.gene.gene_name);	

			// Now enable the filter controls that apply for the variants of this sample
			filterCard.enableVariantFilters(true);

			
	
		}
		if (onVariantsDisplayed) {
	   	    onVariantsDisplayed();
   	    }
   	    if (me.getRelationship() == 'proband') {
	   	    genesCard.hideGeneBadgeLoading(window.gene.gene_name);
   	    }

	} else {

		if (me.isViewable()) {
			me.cardSelector.find('.vcfloader').removeClass("hide");
			var annotationEngines = filterCard.getAnnotationScheme().toLowerCase() == "vep" ? "VEP" : "SnpEff and VEP";
			me.cardSelector.find('.vcfloader .loader-label').text("Annotating variants with " + annotationEngines);
			me.cardSelector.find("#region-flag").addClass("hide");			
		}


		//  The user has entered a gene.  Get the annotated variants.
		var theGene =  $.extend({}, window.gene);
		var theTranscript = $.extend({}, window.selectedTranscript);
		this.model.promiseGetVariants(theGene, theTranscript, regionStart, regionEnd,
			function(data) {
				// When variants annotated with snpEff and VEP...

				if (me.isViewable()) {
					// show the 'Loading Clinvar' progress 
				 	me.cardSelector.find('.vcfloader').removeClass("hide");
					me.cardSelector.find('.vcfloader .loader-label').text("Accessing ClinVar");
					me.cardSelector.find('#clinvar-warning').addClass("hide");		

					// We have variants, so show them now even though we still
					// don't have clinvar annotations nor coverage
					// Here we call this method again and since we
					// have vcf data, the variant chart will be filled
					//me._showVariants(regionStart ? regionStart : window.gene.start, 
					//				 regionEnd ? regionEnd : window.gene.end,
					//				 onVcfData,
					//				 onVariantsDisplayed);	
					filterCard.enableVariantFilters(true);

					filterCard.autoSetFilters();
						
				}
				//if (onVcfData) {
				 //   onVcfData();
			    //}
				
			}).then ( function(data) {
				// After clinvar data retrieved...

			    if (me.isViewable()) {
			    	// Show the variant count
					me.cardSelector.find('#vcf-variant-count-label').removeClass("hide");
			        me.cardSelector.find('#vcf-variant-count').text(me.model.getVariantCount());

					me.cardSelector.find('#gene-box').css("visibility", "hidden");
					me.cardSelector.find('.vcfloader').addClass("hide");
				    

					// At this point, the variants should be cached.  If they aren't,
 					// an error occurred
					var theVcfData = me.model.getVcfDataForGene(window.gene, window.selectedTranscript);
 					if (theVcfData) {

			  			// Here we call this method again and since we
						// have vcf data, the variant chart will be filled
			  			me._showVariants(regionStart ? regionStart : window.gene.start, 
										 regionEnd ? regionEnd : window.gene.end,
										 onVariantsDisplayed,
										 true);

			  			// Enable the variant filters 
			  			if (me.getRelationship() == 'proband') {
					    	filterCard.enableClinvarFilters(data);
					    }

						// Show the 'Call from alignments' button if we a bam file/url was specified
						if (me.isBamLoaded() && me.isViewable()) {
							me.cardSelector.find('#button-find-missing-variants').removeClass("hide");
						} else {
							me.cardSelector.find('#button-find-missing-variants').addClass("hide");						
						}	 				
				   	    
						if (me.getRelationship() == 'proband') {
							genesCard.refreshCurrentGeneBadge();
						}
					} else {
						if (me.getRelationship() == 'proband') {
							me.model.cacheDangerSummary({}, window.gene.gene_name);
							genesCard._geneBadgeLoading(window.gene.gene_name, false);
							genesCard.setGeneBadgeWarning(window.gene.gene_name);
						}
					}

			    }

			}, function(error) {
				me.cardSelector.find('.vcfloader').addClass("hide");

				if (me.getRelationship() == 'proband') {
	   	 		   genesCard.hideGeneBadgeLoading(window.gene.gene_name);
				   genesCard.refreshCurrentGeneBadge(error);
   	    		}
				
				if (error && error == "missing reference") {
					me._displayRefNotFoundWarning();
				} else if (error && ($.type(error) === "string") && error.toLowerCase() == "no variants") {

					if (me.isViewable()) {
					   $('#matrix-track').addClass("hide");
					    me.cardSelector.find("#vcf-track").addClass("hide");
					    me.cardSelector.find('#vcf-variant-count-label').addClass("hide");
					    me.cardSelector.find("#vcf-variant-count").text("");
					    me.cardSelector.find('.vcfloader').addClass("hide");
					    me.cardSelector.find('#error-warning #message').text(error);
					    me.cardSelector.find('#error-warning').removeClass("hide");	

					    if (getProbandVariantCard().isLoaded()) {
						    $("#matrix-panel .loader").addClass("hide");
							getProbandVariantCard().fillFeatureMatrix(regionStart, regionEnd);
					    }
					}

				} else {
					console.log(error);
					if (me.isViewable()) {
					   $('#matrix-track').addClass("hide");
					    me.cardSelector.find("#vcf-track").addClass("hide");
					    me.cardSelector.find('#vcf-variant-count-label').addClass("hide");
					    me.cardSelector.find("#vcf-variant-count").text("");
					    me.cardSelector.find('.vcfloader').addClass("hide");
					    me.cardSelector.find('#error-warning #message').text(error);
					    me.cardSelector.find('#error-warning').removeClass("hide");	
					}
				}
				

			});
	}
}




VariantCard.prototype._fillVariantChart = function(data, regionStart, regionEnd, bypassFeatureMatrix, showTransition) {
	
	if (bypassFeatureMatrix == null) {
		bypassFeatureMatrix = false;
	}

	if (data == null || data.features == null) {
		return;
	}


	$('#vcf-legend').css("display", "block");		
	this.cardSelector.find('#vcf-chart-label').removeClass("hide");		
	this.cardSelector.find('#vcf-name').removeClass("hide");		
	this.cardSelector.find('#vcf-variants').css("display", "inline");	

	this.vcfChart.regionStart(regionStart);
	this.vcfChart.regionEnd(regionEnd);
	
	// Set the vertical layer count so that the height of the chart can be recalculated	     
	if (data.maxLevel == null) {
		data.maxLevel = d3.max(data.features, function(d) { return d.level; });
	}                           	
	this.vcfChart.verticalLayers(data.maxLevel);
	this.vcfChart.lowestWidth(data.featureWidth);

	// Filter out freebayes data for showing in variant chart since these variants
	// show in there own chart above the loaded variants.
	var dataWithoutFB = $.extend({}, data);
	//if (bypassFeatureMatrix) {
		dataWithoutFB.features = data.features.filter( function(feature) {
			return feature.fbCalled == null;
		 });
	//}

	// Load the chart with the new data
	var selection = this.d3CardSelector.select("#vcf-variants").datum([dataWithoutFB]);    
	this.vcfChart.showTransition(showTransition);
    this.vcfChart(selection);


    if (isLevelEdu && eduTourNumber == "2") {
		this.cardSelector.find('#zoom-region-chart').addClass("hide");
		this.cardSelector.find('#zoom-region-chart').css("visibility", "hidden");
    } else {
		this.cardSelector.find('#zoom-region-chart').removeClass("hide");
		this.cardSelector.find('#zoom-region-chart').css("visibility", "visible");
    }

	resizeCardWidths();

	if (this.getRelationship() == 'proband' && data.features.length > 0) {
	    $('#filter-and-rank-card').removeClass("hide");
	    $('#matrix-track').removeClass("hide");		
	}

   	this.d3CardSelector.select("#vcf-variants .x.axis .tick text").style("text-anchor", "start");


   	// Fill in the feature matrix for the proband variant card.
   	if (!bypassFeatureMatrix) {
	   	if ( this.getRelationship() == 'proband') {
	   		window.matrixCard.setFeatureMatrixSource(data);
	   	}
	}

	bookmarkCard.flagBookmarks(getProbandVariantCard(), window.gene);



}

VariantCard.prototype._displayRefNotFoundWarning = function() {
	this.cardSelector.find('#vcf-track').addClass("hide");
	this.cardSelector.find(".vcfloader").addClass("hide");
	$('#matrix-track').addClass("hide");
	this.cardSelector.find('#no-ref-found-warning #message').text("Unable to find reference " + window.gene.chr + " in vcf header.");
	this.cardSelector.find('#no-ref-found-warning').removeClass("hide");

	//filterCard.clearFilters();	
}


VariantCard.prototype.fillFeatureMatrix = function(regionStart, regionEnd) {
	// Don't show the feature matrix (rank card) if there are no variants for the proband
	var theVcfData = this.model.getVcfDataForGene(window.gene, window.selectedTranscript);
	if (this.getRelationship() == 'proband' && theVcfData != null && theVcfData.features != null && theVcfData.features.length == 0) {
		$('#filter-and-rank-card').addClass("hide");
    	//$('#matrix-track').addClass("hide");
    	return;
	}


	$('#filter-and-rank-card').removeClass("hide");
    $('#matrix-track').removeClass("hide");
	if (firstTimeShowVariants) {
		firstTimeShowVariants = false;
	}

	var filteredVcfData = this.model.isVcfLoaded() ? 
	       this.filterVariants(null, false) 
	     : this.filterCalledVariants();
	
	window.matrixCard.fillFeatureMatrix(filteredVcfData);
}

VariantCard.prototype.sortFeatureMatrix = function() {

	var filteredVcfData = this.model.isVcfLoaded() ? 
	       this.filterVariants() 
	     : this.filterCalledVariants();
	
	window.matrixCard.fillFeatureMatrix(filteredVcfData);
}



VariantCard.prototype._fillFreebayesChart = function(data, regionStart, regionEnd) {
	var me = this;
	
	if (data) {
		this.cardSelector.find('#fb-chart-label').removeClass("hide");
		me.cardSelector.find('#zoom-region-chart').css("visibility", "visible");	
		if (me.model.isVcfReadyToLoad()) {
			this.cardSelector.find('#fb-separator').removeClass("hide");
			//me.cardSelector.find('#zoom-region-chart').css("margin-top", "0px");	

		} else {
			this.cardSelector.find('#fb-separator').addClass("hide");
			//me.cardSelector.find('#zoom-region-chart').css("margin-top", "-25px");	
		}

		this.cardSelector.find('#fb-variants').removeClass("hide");

		this.fbChart.regionStart(regionStart);
		this.fbChart.regionEnd(regionEnd);
	
		// Set the vertical layer count so that the height of the chart can be recalculated	    
		this.fbChart.verticalLayers(data.maxLevel);
		this.fbChart.lowestWidth(data.featureWidth);

		this.d3CardSelector.selectAll("#fb-variants").selectAll("svg").remove();

		// Load the chart with the new data
		var selection = this.d3CardSelector.select("#fb-variants").datum([data]);    
	    this.fbChart(selection);

		this.cardSelector.find('.vcfloader').addClass("hide");

	   	this.d3CardSelector.select("#fb-variants .x.axis .tick text").style("text-anchor", "start");


	}  else {
		this.cardSelector.find('#fb-chart-label').addClass("hide");
		this.cardSelector.find('#fb-separator').addClass("hide");
		this.d3CardSelector.select('#fb-variants svg').remove();
	}                      	

	

}

VariantCard.prototype.clearCalledVariants = function() {
	var me = this;
	// Clear out the freebayes charts in the variant card
	me.cardSelector.find('#fb-chart-label').addClass("hide");
	me.cardSelector.find('#fb-separator').addClass("hide");
	me.d3CardSelector.select('#fb-variants svg').remove();
	
	// Clear out data
	this.model.clearCalledVariants();
}

VariantCard.prototype.showCallVariantsProgress = function(state, message) {
	var me = this;
	if (state == 'starting') {
		if (this.isViewable() && this.isBamLoaded()) {
			this.cardSelector.find("#vcf-track").removeClass("hide");
			this.cardSelector.find(".vcfloader").removeClass("hide");
			this.cardSelector.find('.vcfloader .loader-label').text("Calling Variants with Freebayes");

			$('#recall-card .' + this.getRelationship() + '.covloader').removeClass("hide");
			$('#recall-card .' + this.getRelationship() + '.call-variants-count').addClass("hide");

		}		
	} else if (state == 'running') {
		// After variants have been been called from alignments...
    	me.cardSelector.find('.vcfloader').removeClass("hide");
		me.cardSelector.find('.vcfloader .loader-label').text();

	} else if (state == 'counting') {
		// After variants have been called from alignments and annotated from snpEff/VEP...
		// Show the called variant count
		me.cardSelector.find('#called-variant-count-label').removeClass("hide");
		me.cardSelector.find('#called-variant-count').removeClass("hide");
		me.cardSelector.find('#called-variant-count').text(me.model.getCalledVariantCount());
		me.cardSelector.find('#displayed-called-variant-count-label').addClass("hide");
		me.cardSelector.find('#displayedcalled-variant-count').addClass("hide");
		$('#recall-card .' + me.getRelationship() + '.covloader').addClass("hide");
		$('#recall-card .' + me.getRelationship() + '.call-variants-count').removeClass("hide");
		$('#recall-card .' + me.getRelationship() + '.call-variants-count').text(me.model.getCalledVariantCount() + " variants called for " + me.getRelationship());
	} else if (state == 'done') {
		me.cardSelector.find('.vcfloader').addClass("hide");			
	} else if (state == 'error') {
		me.cardSelector.find('.vcfloader').addClass("hide");
		$('#recall-card .' + me.getRelationship() + '.covloader').addClass("hide");
		me.cardSelector.find('#freebayes-error').removeClass("hide");
	}
}


VariantCard.prototype.callVariants = function(regionStart, regionEnd, callback) {
	var me = this;

	me.showCallVariantsProgress('starting');

	this.model.promiseCallVariants(
		regionStart,
		regionEnd,
		function() {
			// After variants have been been called from alignments...
	    	var msg = "Annotating variants with " + (filterCard.getAnnotationScheme().toLowerCase() == "vep" ? "VEP" : "SnpEff and VEP");
	    	me.showCallVariantsProgress('running', message);
		},
		function(data) {
			// After variants have been annotated
			// Enable the variant filters based on the new union of 
			// vcf variants + called variants
			filterCard.enableVariantFilters(true);

			me.showCallVariantsProgress('counting');

			// Show the called variants
			me._fillFreebayesChart(data, regionStart, regionEnd);

		}).then( function(data) {
			// After variants have been annotated with clinvar and inheritance has been determined...

			// Hide the clinvar loader
			me.showCallVariantsProgress('done');

			// Show the called variants
			me._fillFreebayesChart(data, regionStart, regionEnd);

			// If this is the proband card, refresh the feature matrix to
			// show union of vcf variants and called variants
			if (me.getRelationship() == 'proband') {
				me.fillFeatureMatrix(regionStart, regionEnd);
			}

			// Show gene badges
			if (me.getRelationship() == 'proband') {
				genesCard.refreshCurrentGeneBadge();
			}

			// Enable inheritance filters
			filterCard.enableInheritanceFilters(me.model.getVcfDataForGene(window.gene, window.selectedTranscript));

			// Enable the clinvar filter
			filterCard.enableClinvarFilters(me.model.getVcfDataForGene(window.gene, window.selectedTranscript));

			if (callback) {
				callback();
			}


		}, function(error) {

			console.log(error);
			me.showCallVariantsProgress('error');
			
		});


} 

VariantCard.prototype.updateCalledVariantsWithInheritance = function() {
	this.model.updateCalledVariantsWithInheritance();
}



VariantCard.prototype.variantClass = function(clazz) {
	this.vcfChart.clazz(clazz);
	this.fbChart.clazz(clazz);
}


VariantCard.prototype.filterCalledVariants = function() {
	if (this.model.hasCalledVariants()) {
		var filteredFBData = this._filterVariants(this.model.getCalledVariants(), this.fbChart);


		// Only show the 'displayed variant' count if a variant filter is turned on.  Test for
		// this by checking if the number filter flags exceed those that are hidden
		if (this.cardSelector.find(".filter-flag").length > this.cardSelector.find(".filter-flag.hide").length 
			|| this.cardSelector.find("#region-flag").length > this.cardSelector.find("#region-flag.hide").length
			|| this.cardSelector.find("#recfilter-flag").length > this.cardSelector.find("#recfilter-flag.hide").length) {
			this.cardSelector.find('#displayed-called-variant-count-label').removeClass("hide");
			this.cardSelector.find('#displayed-called-variant-count').removeClass("hide");
			this.cardSelector.find('#displayed-called-variant-count').text(filteredFBData.features.length);
		} else {
			this.cardSelector.find('#displayed-called-variant-count-label').addClass("hide");
			this.cardSelector.find('#displayed-called-variant-count').addClass("hide");
			this.cardSelector.find('#displayed-called-variant-count').text("");
		}

		this._fillFreebayesChart(filteredFBData, regionStart, regionEnd, true);
		return filteredFBData;
	}  else {
		return null;
	}
}


VariantCard.prototype.filterVariants = function(theVcfData, showTransition) {
	if (this.model.isVcfLoaded()) {
		var data = theVcfData ? theVcfData : this.model.getVcfDataForGene(window.gene, window.selectedTranscript);
		var filteredVcfData = this._filterVariants(data, this.vcfChart);

		// Only show the 'displayed variant' count if a variant filter is turned on.  Test for
		// this by checking if the number filter flags exceed those that are hidden
		if (filterCard.hasFilters()) {
			this.cardSelector.find('#displayed-variant-count-label').removeClass("hide");
			this.cardSelector.find('#displayed-variant-count').removeClass("hide");
			this.cardSelector.find('#displayed-variant-count').text(this.model.getVariantCount(filteredVcfData));
			this.cardSelector.find('#displayed-variant-count-label-simple').css("visibility", "visible");	
			if (isLevelBasic) {
				this.cardSelector.find('#displayed-variant-count-label-basic').removeClass("hide");
			}
		
		} else {
			this.cardSelector.find('#displayed-variant-count-label').addClass("hide");
			this.cardSelector.find('#displayed-variant-count').addClass("hide");
			this.cardSelector.find('#displayed-variant-count').text("");
			this.cardSelector.find('#displayed-variant-count-label-simple').css("visibility", "hidden");	
			this.cardSelector.find('#displayed-variant-count-label-basic').addClass("hide");
		
		}


		this._fillVariantChart(filteredVcfData, regionStart, regionEnd, null, showTransition);	
		return filteredVcfData;
	} else {
		return null;
	}
}


VariantCard.prototype._filterVariants = function(dataToFilter, theChart) {
	var me = this;

	var data = dataToFilter ? dataToFilter : this.model.getVcfDataForGene(window.gene, window.selectedTranscript);
	if (data == null || data.features == null || data.features.length == 0) {
		return;
	}
	
	// Filter variants
	var filterObject = filterCard.getFilterObject();
	var filteredData = this.model.filterVariants(data, filterObject, window.gene.start, window.gene.end);

/*
	me.cardSelector.find(".filter-flag").addClass("hide");

	// Filter variants
	var filterObject = filterCard.getFilterObject();
	var filteredData = this.model.filterVariants(data, filterObject);

	// Show a badge when the intronic variants have been removed
	if ($('#exonic-only-cb').is(":checked")) {
		me.cardSelector.find("#too-many-variants-flag").removeClass("hide");
		me.cardSelector.find("#excluded-variant-count").text(filteredData.intronsExcludedCount);
	}

	// Set the filter badges
	if (filterCard.afScheme == 'exac') {
		afField = "afExAC";
	} else {
		afField = "af1000G";
	}
	var afLowerVal = filterObject.afMin;
	var afUpperVal = filterObject.afMax;
	if (afLowerVal != null && afUpperVal != null) {
		if (afLowerVal <= 0 && afUpperVal == 1) {
			// We are not filtering on af if the range is 0-1
			me.cardSelector.find("#" + afField.toLowerCase() + "range-flag").addClass("hide");
		} else {
			// We are filtering on af range.  show the filter flag
			me.cardSelector.find("#" + afField.toLowerCase() + "range-flag").removeClass("hide");
		}
	} else {
		me.cardSelector.find("#" + afField.toLowerCase() + "range-flag").addClass("hide");
	}

	if (filterObject.coverageMin && filterObject.coverageMin > 0) {
		me.cardSelector.find("#coverage-flag").removeClass("hide");
	}

	// Iterate through the filters to see which badges to turn on in the variant card.
	// First we need to gather all filters for the same field (for exampe, there might be
	// a MODERATE and HIGH filter).  If any of the filters for the same
	// field are turned on, we want to show the filter badge.
	var annotStates = {};
	for (key in filterObject.annotsToInclude) {
		var annot = filterObject.annotsToInclude[key];
		var states = annotStates[annot.key];
		if (states == null) {
			states = {};
		}
		states[annot.state] = annot.state;
		annotStates[annot.key] = states;
	}
	for (key in annotStates) {
		var states = annotStates[key];
		var filterOn = false;
		for (state in states) {
			if (state == "true") {
				filterOn = true;
			}
		}
		if (filterOn) {
			me.cardSelector.find("#" + key + "-flag").removeClass("hide");
		}  else {
			me.cardSelector.find("#" + key + "-flag").addClass("hide");
		}

	}
	*/

	return filteredData;

}


VariantCard.prototype.determineMaxAlleleCount = function(vcfData) {
	return this.model.determineMaxAlleleCount(vcfData);
}

VariantCard.prototype.populateEffectFilters = function() {
	this.model.populateEffectFilters();
}
VariantCard.prototype.populateRecFilters = function(theVcfData) {
	this.model._populateRecFilters(theVcfData.features);
}



VariantCard.prototype.promiseCompareVariants = function(theVcfData, compareAttribute, 
	matchAttribute, matchFunction, noMatchFunction ) {

	return this.model.promiseCompareVariants(theVcfData, compareAttribute, 
		matchAttribute, matchFunction, noMatchFunction);
}

VariantCard.prototype.adjustTooltip = function(variant) {
	var me = this;
	// Check the fb called variants first.  If present, circle and don't
	// show X for missing variant on vcf variant chart.
	var matchingVariant = null;
	var tooltip = null;
	if (this.fbChart != null && this.model.hasCalledVariants()) {
		var container = this.d3CardSelector.selectAll('#fb-variants > svg');
		matchingVariant = this.fbChart.showCircle()(variant, container, false, true);
		if (matchingVariant) {
			tooltip = this.d3CardSelector.select("#fb-variants .tooltip");
		}
		
	}
	if (this.vcfChart != null) {
		var container = this.d3CardSelector.selectAll('#vcf-variants > svg');;
		matchingVariant = this.vcfChart.showCircle()(variant, container, false, true);
		if (matchingVariant ) {
			tooltip = this.d3CardSelector.select("#vcf-variants .tooltip");
		}
	}
	if (tooltip) {
		if (tooltip.style("opacity") != 0) {
			this._showTooltipImpl(tooltip, matchingVariant, this, false);		
		}
	}

}


VariantCard.prototype.showVariantCircle = function(variant, sourceVariantCard) {
	var me = this;
	// Check the fb called variants first.  If present, circle and don't
	// show X for missing variant on vcf variant chart.
	var matchingVariant = null;
	var indicateMissingVariant = false;
	if (this.fbChart != null && this.model.hasCalledVariants()) {
		var container = this.d3CardSelector.selectAll('#fb-variants > svg');
		var lock = clickedVariant != null && this == sourceVariantCard;
			
		// Show the missing variant on the fbchart if we just have variants from those
		// called from alignments (no vcf variants loaded)	
		if (!me.model.isVcfLoaded()) {
			indicateMissingVariant = true;
		}

		matchingVariant = this.fbChart.showCircle()(variant, container, indicateMissingVariant, lock);


		if (matchingVariant && sourceVariantCard) {
			var tooltip = this.d3CardSelector.select("#fb-variants .tooltip");
			this.showTooltip(tooltip, matchingVariant, sourceVariantCard, lock);		

		}
		
	}
	if (this.vcfChart != null) {
		var container = this.d3CardSelector.selectAll('#vcf-variants > svg');;
		var lock = clickedVariant != null && this == sourceVariantCard;

		// Only show the X for missing variant if we didn't already find the variant in
		// the fb variants
		var indicateMissingVariant = matchingVariant == null ? true : false;
	
		matchingVariant = this.vcfChart.showCircle()(variant, container, indicateMissingVariant, lock);

		if (matchingVariant && sourceVariantCard) {
			var tooltip = this.d3CardSelector.select("#vcf-variants .tooltip");
			this.showTooltip(tooltip, matchingVariant, sourceVariantCard, lock);

		}
		
	}
	
}



VariantCard.prototype.showTooltip = function(tooltip, variant, sourceVariantCard, lock) {
	var me = this;

	// Only show the tooltip for the chart user mouses over / click
    if (this != sourceVariantCard) {
    	return;
    }

	// Don't show the tooltip for mygene2 beginner mode
	if (isLevelBasic) {
		return;
	}

	var screenX = variant.screenX;
	var screenY = variant.screenY;
		
	if (lock) {
		matrixCard.unpin(true);		
		if (isLevelEdu) {
			eduTourCheckVariant(variant);
		}
	}

	if (lock  && !isLevelEdu && !isLevelBasic)  {
		// Show tooltip before we have hgvs notations
		me._showTooltipImpl(tooltip, variant, sourceVariantCard, true);
		
		me.model.promiseGetVariantExtraAnnotations(window.gene, window.selectedTranscript, variant)
		        .then( function(refreshedVariant) {

		        	// Now show tooltip again with the hgvs notations.  Only show
		        	// if we haven't clicked on another variant
		        	if (clickedVariant &&
		        		clickedVariant.start == refreshedVariant.start &&
		        		clickedVariant.ref == refreshedVariant.ref &&
		        		clickedVariant.alt == refreshedVariant.alt) {

						refreshedVariant.screenX= screenX;
			        	refreshedVariant.screenY = screenY;

						me._showTooltipImpl(tooltip, refreshedVariant, sourceVariantCard, true)


		        	}
		        });

				
	} else {
		me._showTooltipImpl(tooltip, variant, sourceVariantCard, lock);
	}
}





VariantCard.prototype._showTooltipImpl = function(tooltip, variant, sourceVariantCard, lock) {
	var me = this;

	if (lock) {
		tooltip.style("pointer-events", "all");
	} else {
		tooltip.style("pointer-events", "none");          
	}

	matrixCard.clearSelections();
	matrixCard.highlightVariant(variant);


	var x = variant.screenX + 7;
	var y = variant.screenY - 27;
	
	
	variantTooltip.fillAndPositionTooltip(tooltip, variant, lock, x, y, me);

	tooltip.select("#unpin").on('click', function() {
		me.unpin();
	});


}


VariantCard.prototype.hideVariantCircle = function(variant) {
	if (this.vcfChart != null) {
		var container = this.d3CardSelector.selectAll('#vcf-variants > svg');
		var parentContainer = this.d3CardSelector.selectAll('#vcf-variants');
		this.vcfChart.hideCircle()(container, parentContainer);
	}
	if (this.fbChart != null && this.model.hasCalledVariants()) {
		var container = this.d3CardSelector.selectAll('#fb-variants > svg');
		var parentContainer = this.d3CardSelector.selectAll('#fb-variants');
		this.fbChart.hideCircle()(container, parentContainer);
	}
}

VariantCard.prototype.showCoverageCircle = function(variant, sourceVariantCard) {
	if (this.model.getBamDataForGene(window.gene) != null) {
		var bamDepth = null;
		if (sourceVariantCard == this && variant.bamDepth != null && variant.bamDepth != '') {
			bamDepth = variant.bamDepth;
		} else {
			var matchingVariant = this.model.getMatchingVariant(variant);
			if (matchingVariant != null) {
				bamDepth = matchingVariant.bamDepth;
				// If samtools mpileup didn't return coverage for this position, use the variant's depth
				// field.
				if (bamDepth == null || bamDepth == '') {
					bamDepth = matchingVariant.genotypeDepth;
				}
			}
		}

		this.bamDepthChart.showCircle()(variant.start, bamDepth);


    }
}

VariantCard.prototype.hideCoverageCircle = function() {
	if (this.model.getBamDataForGene(window.gene) != null){
		this.bamDepthChart.hideCircle()();
	}	
}

VariantCard.prototype.getMaxAlleleCount = function() {
	var theVcfData = this.model.isVcfLoaded() 
				      ? this.model.getVcfDataForGene(window.gene, window.selectedTranscript)
				      : this.model.getCalledVariants();
	if (theVcfData == null) {
		return null;
	}
	var count = theVcfData.maxAlleleCount;
	if (!count) {
		this.determineMaxAlleleCount(theVcfData);
		count = theVcfData.maxAlleleCount;
	}
	return count;
}


VariantCard.prototype.highlightBookmarkedVariants = function() {
	// This is too confusing because there is no easy way to reset
	// to show all variants in full opacity.
	// Until a better approach is implemented, just keep
	// the opacity at 1 on all variants.
	d3.selectAll("#proband-variant-card .variant")
		   .style("opacity", 1);

	d3.selectAll("#proband-variant-card .variant")
	      .filter( function(d,i) {
	      	return d.hasOwnProperty("isBookmark") && d.isBookmark == 'Y';
	      })
	      .style("opacity", 1);
}


VariantCard.prototype.removeBookmarkFlags = function() {
	// Remove the current indicator from the bookmark flag
	this.d3CardSelector.selectAll('#vcf-track .bookmark').remove();
}

VariantCard.prototype.removeBookmarkFlag = function(variant, key) {
	// Remove the current indicator from the bookmark flag
	if (variant.fbCalled == 'Y') {
		this.d3CardSelector.select("#fb-variants > svg .bookmark#" + key).remove();
		var container = this.d3CardSelector.selectAll('#fb-variants > svg');
		this.fbChart.removeBookmark(container, variant);
	} else {
		this.d3CardSelector.select("#vcf-variants > svg .bookmark#" + key).remove();
		var container = this.d3CardSelector.selectAll('#vcf-variants > svg');
		this.vcfChart.removeBookmark(container, variant);
	}

}

VariantCard.prototype.addBookmarkFlag = function(variant, key, singleFlag) {
	if (variant == null) {
		return;
	}

	// Remove the current indicator from the bookmark flag
	this.d3CardSelector.selectAll('#vcf-track .bookmark.current').classed("current", false);

	// If we are just flagging one bookmarked variants, get rid of all previously shown flags
	// for this gene
	if (singleFlag) {
		this.d3CardSelector.selectAll('#vcf-track .bookmark').remove();
	}

	var container = null;
	if (variant.fbCalled == 'Y') {
		// Check to see if the bookmark flag for this variant already exists
		var isEmpty = this.d3CardSelector.selectAll("#fb-variants svg .bookmark#" + key).empty();

		// If the flag isn't present, add it to the freebayes variant
		if (isEmpty) {
			container = this.d3CardSelector.selectAll('#fb-variants svg');
			variant.isBookmark = "Y";
			this.fbChart.addBookmark(container, variant, key);
		}
	} else {
		// Check to see if the bookmark flag for this variant already exists
		var isEmpty = this.d3CardSelector.selectAll("#vcf-variants > svg .bookmark#" + key).empty();

		// If the flag isn't present, add it to the vcf variant
		if (isEmpty) {
			container = this.d3CardSelector.selectAll('#vcf-variants > svg');
			variant.isBookmark = "Y";
			this.vcfChart.addBookmark(container, variant, key);
		}
	}

	//this.fillFeatureMatrix();

	if (singleFlag) {
		this.d3CardSelector.selectAll("#vcf-track .bookmark#" + key).classed("current", true);
	}
}


VariantCard.prototype.unpin = function(saveClickedVariant) {
	if (!saveClickedVariant) {
		clickedVariant = null;
		clickedVariantCard = null;
	}

	this._hideTooltip();
	this.hideCoverageCircle();
	window.hideCircleRelatedVariants();	
	window.hideCoordinateFrame();
}

VariantCard.prototype._hideTooltip = function() {
	var tooltip = this.d3CardSelector.select("#vcf-variants .tooltip");
	tooltip.transition()        
           .duration(500)      
           .style("opacity", 0)
           .style("z-index", 0)
           .style("pointer-events", "none");
}


