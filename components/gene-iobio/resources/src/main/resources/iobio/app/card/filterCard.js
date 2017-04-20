function FilterCard() {
	this.clickedAnnotIds = new Object();
	this.annotsToInclude = new Object();
	this.snpEffEffects = new Object();
	this.vepConsequences = new Object();
	this.recFilters = new Object();
	this.annotationScheme = "vep";
	this.pathogenicityScheme = "clinvar";
	this.annotClasses     = ".type, .impact, ." + IMPACT_FIELD_TO_FILTER + ", .effect, .vepConsequence, .sift, .polyphen, .regulatory, .zygosity, .afexaclevels, .af1000glevels, .inheritance, .clinvar, .uasibs, .recfilter";
	this.annotClassLabels = "Type, Impact, VEP Impact, Effect, VEP Consequence, SIFT, PolyPhen, Regulatory, Zygosity, Allele Freq ExAC, Allele Freq 1000G, Inheritance mode, ClinVar, Unaffected Sibs, VCF Filter Status";
	
	// standard filters
	this.KNOWN_CAUSATIVE           = "known_causative";
	this.DENOVO                    = "denovo";
	this.RECESSIVE                 = "recessive";
	this.FUNCTIONAL_IMPACT         = "functional_impact";
}

FilterCard.prototype.shouldWarnForNonPassVariants = function() {
	var recFilterKeys = Object.keys(this.recFilters);
	var passStatus = recFilterKeys.some(function(key) {
		return key === 'PASS';
	});
	return (passStatus && recFilterKeys.length > 1);
}

FilterCard.prototype.autoSetFilters = function() {
	
	this.displayRecFilters();
	this.initFilterListeners();
	/*
	// If filter status has unique values of PASS + another status (e.g '.' or 'FAIL'),
	// automatically filter variants to only include those with status PASS.
	var statusCount = 0;
	var passStatus = false;
	for ( key in this.recFilters) {
		if (key == 'PASS') {
			passStatus = true;
		}
		statusCount++;	
	}
	if (passStatus && statusCount > 1) {
		this.annotsToInclude.PASS = {key: "recfilter", state: true, value: "PASS"};		
		d3.select("svg#PASS").classed("current", true);
	} 
	*/

}

FilterCard.prototype.applyStandardFilter = function(button, filterName) {
	var me = this;
	filterCard.setStandardFilter(button, filterName);
	var genesCount = filterVariants();
	me.setStandardFilterCount(button, genesCount)
}

FilterCard.prototype.setStandardFilterCount = function(button, genesCount) {
	var label = genesCount.pass + " of " + genesCount.total + " genes";
	$(button.parentNode).find("span.standard-filter-count").text(label);
}

FilterCard.prototype.resetStandardFilterCounts = function() {
	$('#standard-filter-panel span.standard-filter-count').text("");
}

FilterCard.prototype.setStandardFilter = function(button, filterName) {
	var me = this;
	me.clearFilters();
	$(button).addClass('current');
	var annots = null;
	if (filterName == me.KNOWN_CAUSATIVE) {
		annots = 	{
			af1000g_rare:     {key: 'af1000glevels', label: "Allele Freq 1000G", state: true, value: 'af1000g_rare',     valueDisplay: '< 1%'},
			af1000g_uncommon: {key: 'af1000glevels', label: "Allele Freq 1000G", state: true, value: 'af1000g_uncommon', valueDisplay: '1 - 5%'},
			afexac_rare:      {key: 'afexaclevels',  label: "Allele Freq ExAC",  state: true, value: 'afexac_rare',      valueDisplay: '< 1%'},
			afexac_uncommon:  {key: 'afexaclevels',  label: "Allele Freq ExAC",  state: true, value: 'afexac_uncommon',  valueDisplay: '1 - 5 %'},
			afexac_unique_nc: {key: 'afexaclevels',  label: "Allele Freq ExAC",  state: true, value: 'afexac_unique_nc', valueDisplay: 'n/a'},
			clinvar_path:     {key: 'clinvar',       label: "ClinVar",           state: true, value: 'clinvar_path',     valueDisplay: 'pathogenic'},
			clinvar_lpath:    {key: 'clinvar',       label: "ClinVar",           state: true, value: 'clinvar_lpath',    valueDisplay: 'likely pathogenic'}
		}
	} else if (filterName == me.DENOVO) {
		annots = 	{
			af1000g_rare:     {key: 'af1000glevels', label: "Allele Freq 1000G", state: true, value: 'af1000g_rare',     valueDisplay: '< 1%'},
			afexac_rare:      {key: 'afexaclevels',  label: "Allele Freq ExAC",  state: true, value: 'afexac_rare',      valueDisplay: '< 1%'},
			afexac_unique_nc: {key: 'afexaclevels',  label: "Allele Freq ExAC",  state: true, value: 'afexac_unique_nc', valueDisplay: 'n/a'},
			denovo:           {key: 'inheritance',   label: "Inheritance mode",  state: true, value: 'denovo',           valueDisplay: 'de novo'},
			HIGH:             {key: 'highestImpactVep',label: "VEP impact",      state: true, value: 'HIGH',             valueDisplay: 'high'},
			MODERATE:         {key: 'highestImpactVep',label: "VEP impact",      state: true, value: 'MODERATE',         valueDisplay: 'moderate'},
			clinvar_path:     {key: 'clinvar',         label: "ClinVar",           state: true, not: true, value: 'clinvar_path',     valueDisplay: 'pathogenic'},
			clinvar_lpath:    {key: 'clinvar',         label: "ClinVar",           state: true, not: true, value: 'clinvar_lpath',    valueDisplay: 'likely pathogenic'}
		}
	} else if (filterName == me.RECESSIVE) {
		annots = 	{
			af1000g_rare:     {key: 'af1000glevels', label: "Allele Freq 1000G", state: true, value: 'af1000g_rare',     valueDisplay: '< 1%'},
			afexac_rare:      {key: 'afexaclevels',  label: "Allele Freq ExAC",  state: true, value: 'afexac_rare',      valueDisplay: '< 1%'},
			afexac_unique_nc: {key: 'afexaclevels',  label: "Allele Freq ExAC",  state: true, value: 'afexac_unique_nc', valueDisplay: 'n/a'},
			recessive:        {key: 'inheritance',   label: "Inheritance mode",  state: true, value: 'recessive',        valueDisplay: 'recessive'},
			HIGH:             {key: 'highestImpactVep',label: "VEP impact",      state: true, value: 'HIGH',             valueDisplay: 'high'},
			MODERATE:         {key: 'highestImpactVep',label: "VEP impact",      state: true, value: 'MODERATE',         valueDisplay: 'moderate'},
			clinvar_path:     {key: 'clinvar',         label: "ClinVar",           state: true, not: true, value: 'clinvar_path',     valueDisplay: 'pathogenic'},
			clinvar_lpath:    {key: 'clinvar',         label: "ClinVar",           state: true, not: true, value: 'clinvar_lpath',    valueDisplay: 'likely pathogenic'}
		}
	} else if (filterName == me.FUNCTIONAL_IMPACT) {
		annots = 	{
			af1000g_rare:     {key: 'af1000glevels',   label: "Allele Freq 1000G", state: true, value: 'af1000g_rare',     valueDisplay: '< 1%'},
			af1000g_uncommon: {key: 'af1000glevels',   label: "Allele Freq 1000G", state: true, value: 'af1000g_uncommon', valueDisplay: '1 - 5%'},
			afexac_rare:      {key: 'afexaclevels',    label: "Allele Freq ExAC",  state: true, value: 'afexac_rare',      valueDisplay: '< 1%'},
			afexac_uncommon:  {key: 'afexaclevels',    label: "Allele Freq ExAC",  state: true, value: 'afexac_uncommon',  valueDisplay: '1 - 5 %'},
			afexac_unique_nc: {key: 'afexaclevels',    label: "Allele Freq ExAC",  state: true, value: 'afexac_unique_nc', valueDisplay: 'n/a'},
			HIGH:             {key: 'highestImpactVep',label: "VEP impact",        state: true, value: 'HIGH',             valueDisplay: 'high'},
			MODERATE:         {key: 'highestImpactVep',label: "VEP impact",        state: true, value: 'MODERATE',         valueDisplay: 'moderate'},
			clinvar_path:     {key: 'clinvar',         label: "ClinVar",           state: true, not: true, value: 'clinvar_path',     valueDisplay: 'pathogenic'},
			clinvar_lpath:    {key: 'clinvar',         label: "ClinVar",           state: true, not: true, value: 'clinvar_lpath',    valueDisplay: 'likely pathogenic'}
		}
	}


	me.annotsToInclude = annots;
	for (var key in me.annotsToInclude) {
		var annot = me.annotsToInclude[key];
		d3.select('#filter-track #' + key + "." + annot.key).classed("current", true);
		if (annot.not) {
			d3.select('#filter-track #' + key + "." + annot.key).classed("not-equal", true);
		}
	}
}

FilterCard.prototype.getFilterObject = function() {
	// For mygene2 beginner mode, return a fixed filter of AF < 1% and PASS filter.
	if (isLevelBasic) {
		var annots = 	{
			af1000g_rare:     {key: 'af1000glevels', state: true, value: 'af1000g_rare'},
			exac_rare:        {key: 'afexaclevels',  state: true, value: 'afexac_rare'},
			afexac_unique_nc: {key: 'afexaclevels',  state: true, value: 'afexac_unique_nc'},
			clinvar_path:     {key: 'clinvar',       state: true, value: 'clinvar_path'},
			clinvar_lpath:    {key: 'clinvar',       state: true, value: 'clinvar_lpath'},
			clinvar_uc:       {key: 'clinvar',       state: true, value: 'clinvar_uc'},
			clinvar_cd:       {key: 'clinvar',       state: true, value: 'clinvar_cd'},
			clinvar_other:    {key: 'clinvar',       state: true, value: 'clinvar_other'},
			clinvar_lbenign:  {key: 'clinvar',       state: true, value: 'clinvar_lbenign'},
			clinvar_benign:   {key: 'clinvar',       state: true, value: 'clinvar_benign'}
		}
		if (this.shouldWarnForNonPassVariants()) {
			annots.PASS = {key: 'recfilter', state: true, value: 'PASS'};
		}

		return { annotsToInclude: annots };
	}

	var afMinExac = $('#afexac-range-filter #af-amount-start').val() != '' ? +$('#afexac-range-filter #af-amount-start').val() / 100 : null;
	var afMaxExac = $('#afexac-range-filter #af-amount-end').val()   != '' ? +$('#afexac-range-filter #af-amount-end').val()   / 100 : null;

	var afMin1000g = $('#af1000g-range-filter #af-amount-start').val() != '' ? +$('#af1000g-range-filter #af-amount-start').val() / 100 : null;
	var afMax1000g = $('#af1000g-range-filter #af-amount-end').val()   != '' ? +$('#af1000g-range-filter #af-amount-end').val()   / 100 : null;

	return {
		'coverageMin': +$('#coverage-min').val(),
		'afMinExac': afMinExac,
		'afMaxExac': afMaxExac,
		'afMin1000g': afMin1000g,
		'afMax1000g': afMax1000g,
		'annotsToInclude': this.annotsToInclude,
		'exonicOnly': $('#exonic-only-cb').is(":checked")
  };
}


FilterCard.prototype.onSelectAnnotationScheme = function() {
	this.annotationScheme = $("#select-annotation-scheme")[0].selectize.getValue();

	$('#effect-scheme .name').text(this.annotationScheme.toLowerCase() ==  'snpeff' ? 'Effect' : 'Consequence');
	this.displayEffectFilters();
	window.matrixCard.setRowLabelById("impact", isLevelEdu ? "Impact" : "Impact (" + this.annotationScheme + ")" );
	window.matrixCard.setRowAttributeById("impact", this.annotationScheme.toLowerCase() == 'snpeff' ? 'impact' : IMPACT_FIELD_TO_COLOR );
	window.loadTracksForGene();

}

FilterCard.prototype.getAnnotationScheme = function() {
	return this.annotationScheme;
}

FilterCard.prototype.setAnnotationScheme = function(scheme) {
	this.annotationScheme = scheme;
    $('#select-annotation-scheme')[0].selectize.setValue(scheme, true);	
    
	$('#effect-scheme .name').text(this.annotationScheme.toLowerCase() ==  'snpeff' ? 'Effect' : 'Consequence');
	d3.select('#filter-card .impact').classed(IMPACT_FIELD_TO_FILTER,this.annotationScheme.toLowerCase() == 'vep');
	d3.select('#filter-card .' + IMPACT_FIELD_TO_FILTER).classed('impact',!this.annotationScheme.toLowerCase() == 'vep');
	this.displayEffectFilters();
	window.matrixCard.setRowLabelById("impact", isLevelEdu ? "Impact" : "Impact (" + this.annotationScheme + ")");
	window.matrixCard.setRowAttributeById("impact", this.annotationScheme.toLowerCase() == 'snpeff' ? 'impact' : IMPACT_FIELD_TO_COLOR );
}





FilterCard.prototype.init = function() {
	var me = this;

	me.annotClassMap = {};
	var annotLabels = me.annotClassLabels.split(", ");
	var idx = 0;
	me.annotClasses.split(", ").forEach(function(classToken) {
		var clazz = classToken.slice(1);
		var clazzLabel = annotLabels[idx];
		me.annotClassMap[clazz] = clazzLabel;
		idx++;
	});


	var filterCardSelector = $('#filter-track');
	filterCardSelector.find('#expand-button').on('click', function() {
		filterCardSelector.find('.fullview').removeClass("hide");
		//filterCardSelector.css('min-width', "665px");
	});
	filterCardSelector.find('#minimize-button').on('click', function() {
		filterCardSelector.find('.fullview').addClass("hide");
		//filterCardSelector.css('min-width', "185px");
	});


	$('#select-annotation-scheme').selectize(
		{ create: true }
	);
	$('#select-annotation-scheme')[0].selectize.on('change', function() {
		me.onSelectAnnotationScheme();
	});

/*
	$('#select-annotation-scheme').selectivity();
    $('#select-annotation-scheme').on('change', function(event) {
    	me.onSelectAnnotationScheme(event.value);
    });
	$('#select-af-scheme').selectivity();
    $('#select-af-scheme').on('change', function(event) {
    	me.onSelectAFScheme(event.value);
    });
 */

	// Default annotation scheme to VEP
	this.setAnnotationScheme("VEP");

	// listen for enter key on af amount input range
	$('#af-amount-start').on('keydown', function() {
		if(event.keyCode == 13) {
			// We are filtering on range, so clear out the af level filters
			me.resetAfFilters("af1000glevel");
			me.resetAfFilters("afexaclevel");

			window.filterVariants();
	    }
	});
	$('#af-amount-end').on('keydown', function() {
		if(event.keyCode == 13) {
			// We are filtering on range, so clear out the af level filters
			me.resetAfFilters("af1000glevel");
			me.resetAfFilters("afexaclevel");


			window.filterVariants();
	    }
	});
	// listen for go button on af range
	$('#afexac-range-filter #af-go-button').on('click', function() {
		// We are filtering on range, so clear out the af level filters
		me.resetAfFilters("af1000glevel");
		me.resetAfFilters("afexaclevel");

		window.filterVariants();
	});
	$('#af1000g-range-filter #af-go-button').on('click', function() {
		// We are filtering on range, so clear out the af level filters
		me.resetAfFilters("af1000glevel");
		me.resetAfFilters("afexaclevel");

		window.filterVariants();
	});
	// listen for enter key on min coverage
	$('#coverage-min').on('keydown', function() {
		if(event.keyCode == 13) {
			window.filterVariants();
	    }
	});
	// listen for go button on coverage
	$('#coverage-go-button').on('click', function() {
		window.filterVariants();
	});
	// listen to checkbox for filtering exonic only variants
	$('#exonic-only-cb').click(function() {	   
		window.filterVariants();	    
	});




	  d3.selectAll('#impact-scheme')
	    .on("click", function(d) {
	    	d3.select('#impact-scheme').classed("current", true);
	    	d3.select('#effect-scheme' ).classed("current", false);
	    	d3.select('#zygosity-scheme').classed("current", false);

	    	d3.selectAll(".impact").classed("nocolor", false);
	    	d3.selectAll(".effect").classed("nocolor", true);
	    	d3.selectAll(".vepConsequence").classed("nocolor", true);
	    	d3.selectAll(".zygosity").classed("nocolor", true);

			window.variantCards.forEach(function(variantCard) {
				variantCard.variantClass(me.classifyByImpact);
			});
		    window.filterVariants();


	    });
	    d3.selectAll('#effect-scheme')
	    .on("click", function(d) {
	    	d3.select('#impact-scheme').classed("current", false);
	    	d3.select('#effect-scheme').classed("current", true);
	    	d3.select('#zygosity-scheme').classed("current", false);


	    	d3.selectAll(".impact").classed("nocolor", true);
	    	d3.selectAll(".effect").classed("nocolor", false);
	    	d3.selectAll(".vepConsequence").classed("nocolor", false);
	    	d3.selectAll(".zygosity").classed("nocolor", true);

			window.variantCards.forEach(function(variantCard) {
		    	variantCard.variantClass(me.classifyByEffect);		    	
		  	});
			window.filterVariants();
		

	    });
		d3.selectAll('#zygosity-scheme')
	      .on("click", function(d) {
	    	d3.select('#impact-scheme').classed("current", false);
	    	d3.select('#effect-scheme').classed("current", false);
	    	d3.select('#zygosity-scheme').classed("current", true);


	    	d3.selectAll(".impact").classed("nocolor", true);
	    	d3.selectAll(".effect").classed("nocolor", true);
	    	d3.selectAll(".vepConsequence").classed("nocolor", true);
	    	d3.selectAll(".zygosity").classed("nocolor", false);

			window.variantCards.forEach(function(variantCard) {
		    	variantCard.variantClass(me.classifyByZygosity);
			});
		    window.filterVariants();


	    });	    
	   d3.selectAll('#afexac-scheme')
	    .on("click", function(d) {
	    	d3.select('#afexac-scheme' ).classed("current", true);
	    	d3.select('#af1000g-scheme' ).classed("current", false);

	    	d3.selectAll(".afexaclevels").classed("nocolor", false);
	    	d3.selectAll(".af1000glevels").classed("nocolor", true);

	    	// De-select an af1000g filters
	    	me.resetAfFilters("af1000glevel");
	    	me.resetAfRange();
	   
	    	window.filterVariants();

	    });
	   d3.selectAll('#af1000g-scheme')
	    .on("click", function(d) {
	    	d3.select('#afexac-scheme' ).classed("current", false);
	    	d3.select('#af1000g-scheme' ).classed("current", true);

	    	d3.selectAll(".afexaclevels").classed("nocolor", true);
	    	d3.selectAll(".af1000glevels").classed("nocolor", false);

	    	me.resetAfFilters("afexaclevel");
	    	me.resetAfRange();

	    	window.filterVariants();
	    });

	    this.initFilterListeners();
	  
}

FilterCard.prototype.initFilterListeners = function() {
	var me = this;

	d3.selectAll(me.annotClasses)
	  .on("mouseover", function(d) {  	  	
		var id = d3.select(this).attr("id");

		d3.selectAll(".variant")
		   .style("opacity", .1);

	    d3.selectAll(".variant")
	      .filter( function(d,i) {
	      	var theClasses = d3.select(this).attr("class");
	    	if (theClasses.indexOf(id) >= 0) {
	    		return true;
	    	} else {
	    		return false;
	    	}
	      })
	      .style("opacity", 1);
	  })
	  .on("mouseout", function(d) {
	  	d3.selectAll(".variant")
		   .style("opacity", 1);
	  })
	  .on("click", function(d) {
	  	var on = null;
	  	if (d3.select(this).attr("class").indexOf("current") >= 0) {
	  		on = false;
	  	} else {
	  		on = true;
	  	}
	  	var clazzes = d3.select(this).attr("class");
	  	var schemeClass = null;
	  	var schemeLabel = "";
	  	clazzes.split(" ").forEach(function(classToken) {
	  		if (me.annotClassMap[classToken]) {
	  			schemeClass = classToken;
	  			schemeLabel = me.annotClassMap[classToken];
	  		}
	  	});
	  	/*
	  	// strip out extraneous 'no color' and 'current' class
	  	// so that we are left with the attribute name of the
	  	// annotation we will be filtering on.
	  	if (schemeClass.indexOf('nocolor') >= 0) {
	  		var tokens = schemeClass.split(' ');
	  		tokens.forEach(function(clazz) {
	  			if (clazz != 'nocolor') {
	  				schemeClass = clazz;
	  			}
	  		})
	  	}
	  	if (schemeClass.indexOf('current') >= 0) {
	  		var tokens = schemeClass.split(' ');
	  		tokens.forEach(function(clazz) {
	  			if (clazz != 'current') {
	  				schemeClass = clazz;
	  			}
	  		})
	  	}
	  	if (schemeClass.indexOf('inactive') >= 0) {
	  		var tokens = schemeClass.split(' ');
	  		tokens.forEach(function(clazz) {
	  			if (clazz != 'inactive') {
	  				schemeClass = clazz;
	  			}
	  		})
	  	}
	  	*/

	  	// If af level clicked on, reset af range filter
	  	if (d3.select(this).attr("class").indexOf("af1000glevel") || 
	  		d3.select(this).attr("class").indexOf("afexaclevel")) {
	  		if (on) {
				me.resetAfRange();
	  		}
	  	}


	  	// Remove from or add to list of clicked ids
	  	me.clickedAnnotIds[d3.select(this).attr("id")] = on;
	  	var valueDisplay =  "";
	  	if (!d3.select(this).select("text").empty()) {
	  		valueDisplay = d3.select(this).select("text").text();
	  	} else if (!d3.select(this).empty() > 0 ) {
	  		valueDisplay = d3.select(this).text();
	  	} else {
	  		valueDisplay = d3.select(this).attr("id");
	  	}
	  	me.annotsToInclude[d3.select(this).attr("id")] = {'key':   schemeClass , 
	  													  'label': schemeLabel,
	  													  'value': d3.select(this).attr("id"),
	  													  'valueDisplay': valueDisplay,   
	  													  'state': on};

	  	d3.select(this).classed("current", on);
	  	if (!on) {
		  	d3.select(this).classed("not-equal", false);
	  	}
	  	window.filterVariants();
	  });

}

FilterCard.prototype.setExonicOnlyFilter = function(on) {
	if (on == null) {
		on  = true;
	}
	$('#exonic-only-cb').prop('checked', on);
	this.displayFilterSummary();
}




FilterCard.prototype.clearFilters = function() {
	
	this.clickedAnnotIds = [];
	this.annotsToInclude = {};

	$('#filter-progress').addClass("hide");
	$('#filter-progress .text').text("");
	$('#filter-progress .bar').css("width", "0%");


	d3.selectAll('.standard-filter-btn').classed('current', false);
	
	d3.selectAll('#filter-track .recfilter').classed('current', false);
	d3.select('#recfilter-flag').classed("hide", true);

	d3.selectAll('#filter-track .highestImpactVep').classed('current', false);
	d3.selectAll('#filter-track .vepImpact').classed('current', false);
	d3.selectAll('#filter-track .vepConsequence').classed('current', false);
	d3.selectAll('#filter-track .impact').classed('current', false);
	d3.selectAll('#filter-track .effect').classed('current', false);
	d3.selectAll('#filter-track .af1000glevels').classed('current', false);
	d3.selectAll('#filter-track .afexaclevels').classed('current', false);
	d3.selectAll('#filter-track .type').classed('current', false);
	d3.selectAll('#filter-track .zygosity').classed('current', false);
	d3.selectAll('#filter-track .sift').classed('current', false);
	d3.selectAll('#filter-track .polyphen').classed('current', false);
	d3.selectAll('#filter-track .clinvar').classed('current', false);
	d3.selectAll('#filter-track .clinvar').classed('not-equal', false);
	d3.selectAll('#filter-track .inheritance').classed('current', false);
	d3.selectAll('#filter-track .regulatory').classed('current', false);
	d3.selectAll('#filter-track .uasibs').classed('current', false);
	$('#afexac-range-filter #af-amount-start').val("");
	$('#afexac-range-filter #af-amount-end').val("");
	$('#af1000g-range-filter #af-amount-start').val("");
	$('#af1000g-range-filter #af-amount-end').val("");
	$('#coverage-min').val('');
	this.setExonicOnlyFilter(false);

	this.displayFilterSummary();
	
}

FilterCard.prototype.resetAfRange = function() {
	$('#af-amount-start').val("");
	$('#af-amount-end').val("");	

	$("#af1000grange-flag").addClass("hide");
	$("#afexacrange-flag").addClass("hide");


}

FilterCard.prototype.resetAfFilters = function(scheme) {
	var me = this;

	// De-select af level filters
	d3.selectAll("." + scheme).classed("current", false);

	d3.selectAll("." + scheme).each(function(d,i) {
		var id = d3.select(this).attr("id");
		me.clickedAnnotIds[id] = false;
  		me.annotsToInclude[id] = {'key':   scheme, 
									'value': id,  
									'state': false};

	});
}

FilterCard.prototype.enableFilters = function() {
	d3.selectAll(".impact").each( function(d,i) {		
		d3.select(this).classed("inactive", false);
	});
	d3.selectAll(".highestImpactVep").each( function(d,i) {		
		d3.select(this).classed("inactive", false);
	});
	d3.selectAll(".vepImpact").each( function(d,i) {		
		d3.select(this).classed("inactive", false);
	});
	d3.selectAll(".type").each( function(d,i) {		
		d3.select(this).classed("inactive", false);
	});
	d3.selectAll(".zygosity").each( function(d,i) {		
		d3.select(this).classed("inactive", false);
	});
	d3.selectAll(".effect").each( function(d,i) {		
		d3.select(this).classed("inactive", false);
	});
	d3.selectAll(".vepConsequence").each( function(d,i) {		
		d3.select(this).classed("inactive", false);
	});
	d3.selectAll(".sift").each( function(d,i) {		
		d3.select(this).classed("inactive", false);
	});
	d3.selectAll(".polyphen").each( function(d,i) {		
		d3.select(this).classed("inactive", false);
	});
	d3.selectAll(".regulatory").each( function(d,i) {		
		d3.select(this).classed("inactive", false);
	});
	d3.selectAll(".afexaclevels").each( function(d,i) {		
		d3.select(this).classed("inactive", false);
	});
	d3.selectAll(".af1000glevels").each( function(d,i) {		
		d3.select(this).classed("inactive", false);
	});
	d3.selectAll(".inheritance").each( function(d,i) {		
		d3.select(this).classed("inactive", false);
	});
	d3.selectAll(".clinvar").each( function(d,i) {		
		d3.select(this).classed("inactive", false);
	});

	$("#af-range-filter").removeClass("hide");
	$("#coverage-filter").removeClass("hide");
}

FilterCard.prototype.disableFilters = function() {
	/*
	d3.selectAll(".impact").each( function(d,i) {		
		d3.select(this).classed("inactive", true);
	});
	d3.selectAll(".highestImpactVep").each( function(d,i) {		
		d3.select(this).classed("inactive", true);
	});
	d3.selectAll(".vepImpact").each( function(d,i) {		
		d3.select(this).classed("inactive", true);
	});
	d3.selectAll(".type").each( function(d,i) {		
		d3.select(this).classed("inactive", true);
	});
	d3.selectAll(".zygosity").each( function(d,i) {		
		d3.select(this).classed("inactive", true);
	});
	d3.selectAll(".effect").each( function(d,i) {		
		d3.select(this).classed("inactive", true);
	});
	d3.selectAll(".vepConsequence").each( function(d,i) {		
		d3.select(this).classed("inactive", true);
	});
	d3.selectAll(".sift").each( function(d,i) {		
		d3.select(this).classed("inactive", true);
	});
	d3.selectAll(".polyphen").each( function(d,i) {		
		d3.select(this).classed("inactive", true);
	});
	d3.selectAll(".regulatory").each( function(d,i) {		
		d3.select(this).classed("inactive", true);
	});
	d3.selectAll(".afexaclevels").each( function(d,i) {		
		d3.select(this).classed("inactive", true);
	});
	d3.selectAll(".af1000glevels").each( function(d,i) {		
		d3.select(this).classed("inactive", true);
	});
	d3.selectAll(".inheritance").each( function(d,i) {		
		d3.select(this).classed("inactive", true);
	});
	d3.selectAll(".clinvar").each( function(d,i) {		
		d3.select(this).classed("inactive", true);
	});

	$("#af-range-filter").addClass("hide");
	$("#coverage-filter").addClass("hide");
*/
}

FilterCard.prototype.enableClinvarFilters = function(theVcfData) {	
	/*
	if (theVcfData == null || theVcfData.features == null) {
		return;
	}
	
	var clinvarVariantMap = {};
	theVcfData.features.forEach( function(variant) {
		if (variant.clinvar != null && variant.clinvar != '' && variant.clinvar != 'none') {
			clinvarVariantMap[variant.clinvar] = 'Y';
		}
	});
	d3.selectAll(".clinvar").each( function(d,i) {
		var clinvar = d3.select(this).attr("id");
		var clinvarPresent = clinvarVariantMap[clinvar];
		d3.select(this).classed("inactive", clinvarPresent == null);
	});
*/

}

FilterCard.prototype.enableInheritanceFilters = function(theVcfData) {

	/*
	if (theVcfData == null || theVcfData.features == null) {
		return;
	}
	var inheritanceVariantMap = {};
	if (theVcfData == null || theVcfData.features == null) {
		return;
	}
	theVcfData.features.forEach( function(variant) {
		if (variant.inheritance != null && variant.inheritance != '' && variant.inheritance != 'none') {
			inheritanceVariantMap[variant.inheritance] = 'Y';
		}
	});
	d3.selectAll(".inheritance").each( function(d,i) {
		var inheritance = d3.select(this).attr("id");
		var inheritancePresent = inheritanceVariantMap[inheritance];
		d3.select(this).classed("inactive", inheritancePresent == null);
	});
*/
}

FilterCard.prototype.enableCoverageFilters = function() {
	/*
	$("#coverage-filter").removeClass("hide");
	*/
}



FilterCard.prototype.enableVariantFilters = function(fullRefresh) {
	if (dataCard.mode == "trio") {
		d3.selectAll("#filter-track .inheritance").classed("inactive", false);
	} else {
		d3.selectAll("#filter-track .inheritance").classed("inactive", true);
		d3.selectAll("#filter-track .inheritance").classed("current", false);
	}
	this.displayEffectFilters();
	this.initFilterListeners();

	/*
	var me = this;

	d3.selectAll(".impact").each( function(d,i) {
		var impact = d3.select(this).attr("id");
		var count = d3.selectAll('#vcf-track .variant.' + impact)[0].length;
		d3.select(this).classed("inactive", count == 0);
	});
	d3.selectAll(".highestImpactVep").each( function(d,i) {
		var impact = d3.select(this).attr("id");
		var count = d3.selectAll('#vcf-track .variant.' + impact)[0].length;
		d3.select(this).classed("inactive", count == 0);
	});
	d3.selectAll(".vepImpact").each( function(d,i) {
		var impact = d3.select(this).attr("id");
		var count = d3.selectAll('#vcf-track .variant.' + impact)[0].length;
		d3.select(this).classed("inactive", count == 0);
	});
	d3.selectAll(".type").each( function(d,i) {
		var type = d3.select(this).attr("id");
		var count = d3.selectAll('#vcf-track .variant.' + type)[0].length;
		d3.select(this).classed("inactive", count == 0);
	});
	d3.selectAll(".zygosity").each( function(d,i) {
		var zygosity = d3.select(this).attr("id");
		var count = d3.selectAll('#vcf-track .variant.' + zygosity)[0].length;
		d3.select(this).classed("inactive", count == 0);
	});
	d3.selectAll(".sift").each( function(d,i) {
		var sift = d3.select(this).attr("id");
		var count = d3.selectAll('#vcf-track .variant.' + sift)[0].length;
		d3.select(this).classed("inactive", count == 0);
	});
	d3.selectAll(".polyphen").each( function(d,i) {
		var polyphen = d3.select(this).attr("id");
		var count = d3.selectAll('#vcf-track .variant.' + polyphen)[0].length;
		d3.select(this).classed("inactive", count == 0);
	});
	d3.selectAll(".regulatory").each( function(d,i) {
		var reg = d3.select(this).attr("id");
		var count = d3.selectAll('#vcf-track .variant.' + reg)[0].length;
		d3.select(this).classed("inactive", count == 0);
	});

	this.displayEffectFilters();
	this.initFilterListeners();
	d3.selectAll(".afexaclevels").each( function(d,i) {
		var afexaclevel = d3.select(this).attr("id");
		var count = d3.selectAll('#vcf-track .variant.' + afexaclevel)[0].length;
		d3.select(this).classed("inactive", count == 0);
	});
	d3.selectAll(".af1000glevels").each( function(d,i) {
		var af1000glevel = d3.select(this).attr("id");
		var count = d3.selectAll('#vcf-track .variant.' + af1000glevel)[0].length;
		d3.select(this).classed("inactive", count == 0);
	});
	$("#af-range-filter").removeClass("hide");
	*/

}

FilterCard.prototype.displayEffectFilters = function() {
	var me = this;
	$('#effect-filter-box .effect').remove();
	$('#effect-filter-box .vepConsequence').remove();
	var nocolor = $('#effect-filter-box #effect-scheme').hasClass("current") ? "" : "nocolor";
	var values = this.annotationScheme.toLowerCase() == 'snpeff' ? this.snpEffEffects : this.vepConsequences;
	var field  = this.annotationScheme.toLowerCase() == 'snpeff' ? 'effect' : 'vepConsequence';

	var effectKeys = Object.keys(values).sort();

	effectKeys.forEach( function(key) {
		var count = d3.selectAll('#vcf-track .variant')
		              .filter( function(d,i) {
		              	var match = false; 
		              	for (ef in d[field]) {
		              		if (ef == key) {
		              			match = true;
		              		}
		              	}
		              	return match;
		              })[0].length;

		if (count > 0) {
			var effectLabel = me.capitalizeFirstLetter(key.split("_gene_variant").join("").split("_variant").join("").split("_").join(" "));
			var svgElem = null;
			if (effectLabel.length < 20) {
				svgElem = '<svg id="' + key + '" class="' + field + ' ' + nocolor + '" width="100" height="15" transform="translate(0,0)">' +
                          '<text class="name" x="9" y="9" style="fill-opacity: 1;font-size: 9px;">' + effectLabel + '</text>' +
        				  '<rect class="filter-symbol  effect_' + key + '" rx="1" ry="1" x="1" width="5" y="2" height="5" style="opacity: 1;"></rect>' +
      					  '</svg>';

			} else {
				// find first space after 20th character
				var pos = 0;
				for (var i = 20; i < effectLabel.length; i++) {
					if (pos == 0 && effectLabel[i] == " ") {
						pos = i;
					}
				}
				var label1 = effectLabel.substring(0, pos);
				var label2 = effectLabel.substring(pos+1, effectLabel.length);
				svgElem = '<svg id="' + key + '" class="' + field + ' ' + nocolor + '" width="80" height="26" transform="translate(0,0)">' +
                          '<text class="name" x="9" y="7" style="fill-opacity: 1;font-size: 9px;">' + label1 + '</text>' +
                          '<text class="name" x="9" y="17" style="fill-opacity: 1;font-size: 9px;">' + label2 + '</text>' +
        				  '<rect class="filter-symbol  effect_' + key + '" rx="1" ry="1" x="1" width="5" y="2" height="5" style="opacity: 1;"></rect>' +
      					  '</svg>';

			}

      		$('#effect-filters').append(svgElem);
		}
	});	
}

FilterCard.prototype.displayRecFilters = function() {
	var me = this;
	$('#rec-filter-box .recfilter').remove();

	var recFilterCount = 0;
	var recFilterKeys = Object.keys(this.recFilters).sort(function(a,b) {
		if (a == 'PASS') {
			return -1;
		} else if (b == 'PASS') {
			return 1
		} else {
			if (a < b) {
				return -1;
			} else if (a > b) {
				return 1
			} else {
				return 0;
			}
		}
	});
	
	recFilterKeys.forEach(function(key) {
		recFilterCount++;
		var label = key === "." ? ". (unassigned)" : key;
		var elmId = key === "." ? "unassigned" : key;
		var svgElem = '<svg id="' + elmId + '" class="recfilter" width="90" height="15" transform="translate(0,0)">' +
                      '<text class="name" x="9" y="8" style="fill-opacity: 1;font-size: 9px;">' + me.capitalizeFirstLetter(label) + '</text>' +
  					  '</svg>';
  		$('#rec-filter-box').append(svgElem);
	});
	/*
	if (recFilterCount > 0) {
		$('#rec-filter-panel').removeClass("hide");
	} else {
		$('#rec-filter-panel').addClass("hide");		
	}
	*/	
}

FilterCard.prototype.hasFilters = function() {
	if (this._getFilterString().length > 0) {
		return true;
	} else {
		return false;
	}
}


FilterCard.prototype.filterGenes = function() {
	var me = this;
	// After the filter has been applied to the current gene's variants,
	// refresh all of the gene badges based on the filter
	var geneCounts = cacheHelper.refreshGeneBadges();
	if (me.hasFilters()) {
		$('#filter-progress .text').text(geneCounts.pass + " passed filter");
		$('#filter-progress .bar').css("width", percentage(geneCounts.pass / geneCounts.total));
		$('#filter-progress').removeClass("hide");		
	}
	return geneCounts;
}


FilterCard.prototype._getFilterString = function() {
	var filterString = "";
	var filterObject = this.getFilterObject();


	var AND = function(filterString) {
		if (filterString.length > 0) {
			return   " <span>and</span> ";
		} else {
			return "";
		}
	}

	var filterBox = function(filterString) {
		return "<span class=\"filter-flag label label-primary\">" + filterString + "</span>";
	}



	if ($('#exonic-only-cb').is(":checked")) {
		filterString += AND(filterString) + filterBox("not intronic");
	}

	if (filterObject.afMinExac != null && filterObject.afMaxExac != null) {
		if (filterObject.afMinExac >= 0 && filterObject.afMaxExac < 1) {
			filterString += AND(filterString) + filterBox("ExAC allele frequency between " + filterObject.afMinExac + " and  " + filterObject.afMaxExac);
		}
	}
	if (filterObject.afMin1000g != null && filterObject.afMax1000g != null) {
		if (filterObject.afMin1000g >= 0 && filterObject.afMax1000g < 1) {
			filterString += AND(filterString) + filterBox("1000g allele frequency between " + filterObject.afMin1000g + " and  " + filterObject.afMax1000g);
		}
	}

	if (filterObject.coverageMin && filterObject.coverageMin > 0) {
		if (filterString.length > 0) {
			filterString += AND(filterString) +  filterBox("coverage at least " + filterObject.coverageMin + "X");
		}
	}


	var annots = {};
	for (key in filterObject.annotsToInclude) {
		var annot = filterObject.annotsToInclude[key];
		if (annot.state) {
			var annotObject = annots[annot.key];
			if (annotObject == null) {
				annotObject = {values: [], label: annot.label};
				annots[annot.key] = annotObject;
			}
			annotObject.values.push((annot.not ? "NOT " : "") + annot.valueDisplay);
		}
	}

	for (key in annots) {
		var annotObject = annots[key];
		var theValues = "";
		annotObject.values.forEach(function(theValue) {
			if (theValues.length > 0) {
				theValues += ", "
			} else if (annotObject.values.length > 1) {
				theValues +=  "(";
			}
			theValues += theValue;
		});
		if (annotObject.values.length > 1) {
			theValues += ")";
		}
		
		filterString += AND(filterString) + filterBox(annotObject.label + '&nbsp;&nbsp;' + theValues);
	}
	return filterString;
}

FilterCard.prototype.displayFilterSummary = function() {
	var filterString = this._getFilterString();
	if (filterString.length > 0) {
		$("#filter-summary-track").removeClass("hide")
		$("#filter-summary-track .card-label").nextAll().remove();
		$('#filter-summary-track .card-label').after(filterString);		
	} else {
		$("#filter-summary-track").addClass("hide")
	}
}



FilterCard.prototype.capitalizeFirstLetter = function(string) {
    return string.charAt(0).toUpperCase() + string.slice(1);
}

FilterCard.prototype.classifyByImpact = function(d) {
	var impacts = "";
	var colorimpacts = "";
	var effects = "";
	var sift = "";
	var polyphen = "";
	var regulatory = "";

	// this is not FilterCard because we are calling class function within d3 
	var annotationScheme = filterCard.annotationScheme;

	var effectList = (annotationScheme == null || annotationScheme.toLowerCase() == 'snpeff' ? d.effect : d.vepConsequence);	
	for (key in effectList) {
      if (annotationScheme.toLowerCase() == 'vep' && key.indexOf("&") > 0) {
      	var tokens = key.split("&");
      	tokens.forEach( function(token) {
	      effects += " " + token;
    		
      	});
      } else {
	      effects += " " + key;	      
      }
    }
    var impactList =  (annotationScheme == null || annotationScheme.toLowerCase() == 'snpeff' ? d.impact : d[IMPACT_FIELD_TO_FILTER]);
    for (key in impactList) {
      impacts += " " + key;
    }
    var colorImpactList =  (annotationScheme == null || annotationScheme.toLowerCase() == 'snpeff' ? d.impact : d[IMPACT_FIELD_TO_COLOR]);
    for (key in colorImpactList) {
      colorimpacts += " " + 'impact_'+key;
    }
    if (colorimpacts == "") {
    	colorimpacts = "impact_none";
    }
    for (key in d.sift) {
    	sift += " " + key;		
    }
    for (key in d.polyphen) {
    	polyphen += " " + key;		
    }
    for (key in d.regulatory) {
    	regulatory += " " + key;		
    }

    var af1000g = Object.keys(d.af1000glevels).join(" ");
    var afexac = Object.keys(d.afexaclevels).join(" ");
	
	return  'variant ' + d.type.toLowerCase()  + ' ' + d.zygosity.toLowerCase() + ' ' + d.inheritance.toLowerCase() + ' ua_' + d.ua + ' '  + sift + ' ' + polyphen + ' ' + regulatory + ' recfilter_' + d.recfilter + ' ' + afexac + ' ' + af1000g + ' ' + d.clinvar + ' ' + impacts + ' ' + effects + ' ' + d.consensus + ' ' + colorimpacts; 
}

FilterCard.prototype.classifyByEffect = function(d) { 
	var effects = "";
	var coloreffects = "";
	var impacts = "";
	var sift = "";
	var polyphen = "";
	var regulatory = "";

	// this is not FilterCard because we are calling class function within d3 
	var annotationScheme = filterCard.annotationScheme;
	
	
	var effectList = (annotationScheme == null || annotationScheme.toLowerCase() == 'snpeff' ? d.effect : d.vepConsequence);
    for (key in effectList) {
      if (annotationScheme.toLowerCase() == 'vep' && key.indexOf("&") > 0) {
      	var tokens = key.split("&");
      	tokens.forEach( function(token) {
      	  effects += " " + token;
	      coloreffects += " effect_" + token;      		
      	});
      } else {
      	  effects += " " + key;
	      coloreffects += " effect_" + key;
      }
    }
    if (coloreffects == "") {
    	coloreffects = "effect_none";
    }
    var impactList =  (annotationScheme == null || annotationScheme.toLowerCase() == 'snpeff' ? d.impact : d[IMPACT_FIELD_TO_FILTER]);
    for (key in impactList) {
      impacts += " " + key;
    }
    for (key in d.sift) {
    	sift += " " + key;		
    }
    for (key in d.polyphen) {
    	polyphen += " " + key;		
    }
    for (key in d.regulatory) {
    	regulatory += " " + key;		
    }

    var af1000g = Object.keys(d.af1000glevels).join(" ");
    var afexac = Object.keys(d.afexaclevels).join(" ");

    
    return  'variant ' + d.type.toLowerCase() + ' ' + d.zygosity.toLowerCase() + ' ' + + d.inheritance.toLowerCase() + ' ua_' + d.ua + ' ' + sift + ' ' + polyphen + ' ' + regulatory + ' ' + ' recfilter_' + d.recfilter + ' ' + afexac + ' ' + af1000g + ' ' + d.clinvar + ' ' + effects + ' ' + impacts + ' ' + d.consensus + ' ' + coloreffects; 
}


FilterCard.prototype.classifyByZygosity = function(d) { 
	var effects = "";
	var impacts = "";
	var sift = "";
	var polyphen = "";
	var regulatory = "";
	var colorzygs = "";

	// this is not FilterCard because we are calling class function within d3 
	var annotationScheme = filterCard.annotationScheme;
	
	
	var effectList =  (annotationScheme == null || annotationScheme.toLowerCase() == 'snpeff' ? d.effect : d.vepEffect);
	for (key in effectList) {
      if (annotationScheme.toLowerCase() == 'vep' && key.indexOf("&") > 0) {
      	var tokens = key.split("&");
      	tokens.forEach( function(token) {
	      effects += " " + token;	     
      	});
      } else {
	      effects += " " + key;
      }
    }
    var impactList =  (annotationScheme == null || annotationScheme.toLowerCase() == 'snpeff' ? d.impact : d[IMPACT_FIELD_TO_FILTER]);
    for (key in impactList) {
      impacts += " " + key;
    }
    for (key in d.sift) {
    	sift += " " + key;		
    }
    for (key in d.polyphen) {
    	polyphen += " " + key;		
    }
    for (key in d.regulatory) {
    	regulatory += " " + key;		
    }
    var af1000g = Object.keys(d.af1000glevels).join(" ");
    var afexac = Object.keys(d.afexaclevels).join(" ");

    
    return  'variant ' + d.type.toLowerCase() + ' ' + 'zyg_'+d.zygosity.toLowerCase() + ' ' + d.inheritance.toLowerCase() + ' ua_' + d.ua + ' ' + sift + ' ' + polyphen + ' ' + regulatory + ' ' + ' recfilter_' + d.recfilter +  ' ' + afexac + ' ' + af1000g + ' ' + d.clinvar + ' ' + effects + ' ' + impacts + ' ' + d.consensus + ' '; 
}





