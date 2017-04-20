function VariantTooltip() {
	this.examinedVariant = null;
	this.WIDTH_LOCK         = 650;
	this.WIDTH_HOVER        = 360;
	this.WIDTH_SIMPLE       = 280;
	this.WIDTH_SIMPLE_WIDER = 500;
}

VariantTooltip.prototype.fillAndPositionTooltip = function(tooltip, variant, lock, screenX, screenY, variantCard, html) {
	var me = this;
	tooltip.style("z-index", 1032);
	tooltip.transition()        
	 .duration(1000)      
	 .style("opacity", .9)	
	 .style("pointer-events", "all");

	if (isLevelEdu || isLevelBasic) {
		tooltip.classed("level-edu", "true");
	} 

	tooltip.classed("tooltip-wide", lock && !isLevelEdu);
	
	if (html == null) {
		if (lock) {
			html = variantTooltip.formatContent(variant, null, 'tooltip-wide');
		} else {	
			html = variantTooltip.formatContent(variant, variantCard ? "click on variant to pin tooltip" : "click on column to pin tooltip");
		}		
	}
	tooltip.html(html);
	me.injectVariantGlyphs(tooltip, variant, lock ? '.tooltip-wide' : '.tooltip');

	
	if (variantCard == null) {
		variantCard = window.getProbandVariantCard();
	}
	var selection = tooltip.select("#coverage-svg");
	me.createAlleleCountSVGTrio(variantCard, selection, variant, lock ? 150 : null);



	var hasLongText = $(tooltip[0]).find('.col-sm-8').length > 0  || $(tooltip[0]).find('.col-sm-9').length > 0;
 	var w = isLevelEdu || isLevelBasic ? (hasLongText ? me.WIDTH_SIMPLE_WIDER : me.WIDTH_SIMPLE) : (lock ? me.WIDTH_LOCK : me.WIDTH_HOVER);
	var h = d3.round(tooltip[0][0].offsetHeight);

	var x = screenX;
	var y = screenY -  +$('body #container').css('top').split("px")[0] + 10;
	if (y - h < 0) {
		y = h + 5;
	}

	x = sidebarAdjustX(x);



	if (x-33 > 0 && (x-33+w) < $('#matrix-panel').outerWidth()) {
		tooltip.classed("arrow-down-left", true);
		tooltip.classed("arrow-down-right", false);
		tooltip.classed("arrow-down-center", false);
		tooltip.style("width", w + "px")
		       .style("left", (x-33) + "px") 
		       .style("text-align", 'left')    
		       .style("top", (y - h) + "px");   

	} else if (x - w > 0 ) {
		tooltip.classed("arrow-down-right", true);
		tooltip.classed("arrow-down-left", false);
		tooltip.classed("arrow-down-center", false);
		tooltip.style("width", w + "px")
		       .style("left", (x - w + 2) + "px") 
		       .style("text-align", 'left')    
		       .style("top", (y - h) + "px");   
	}	else {
		tooltip.classed("arrow-down-right", false);
		tooltip.classed("arrow-down-left", false);
		tooltip.classed("arrow-down-center", true);
		tooltip.style("width", w + "px")
		       .style("left", (x - w/2) + "px") 
		       .style("text-align", 'left')    
		       .style("top", (y - h) + "px");   
	}		

}


VariantTooltip.prototype.injectVariantGlyphs = function(tooltip, variant, selector) {
	if (selector == ".tooltip") {
		var impactList =  (filterCard.annotationScheme == null || filterCard.annotationScheme.toLowerCase() == 'snpeff' ? variant.impact : variant[IMPACT_FIELD_TO_COLOR]);
		for (impact in impactList) {
			var theClazz = 'impact_' + impact;	
			if ($(tooltip[0]).find(".tooltip-title.main-header").length > 0) {
				$(tooltip[0]).find(".tooltip-title.main-header").prepend("<svg class=\"impact-badge\" height=\"11\" width=\"14\">");
				var selection = tooltip.select('.impact-badge').data([{width:10, height:10,clazz: theClazz,  type: variant.type}]);
				matrixCard.showImpactBadge(selection);					
			}

		}

		if ($(tooltip[0]).find(".tooltip-title.highest-impact-badge").length > 0) {
			var highestImpactList =  (filterCard.annotationScheme == null || filterCard.annotationScheme.toLowerCase() == 'snpeff' ? variant.highestImpact : variant.highestImpactVep);
			for (impact in highestImpactList) {
				var theClazz = 'impact_' + impact;	
				$(tooltip[0]).find(".tooltip-title.highest-impact-badge").prepend("<svg class=\"impact-badge\" height=\"11\" width=\"14\">");
				var selection = tooltip.select('.highest-impact-badge .impact-badge').data([{width:10, height:10,clazz: theClazz,  type: variant.type}]);
				matrixCard.showImpactBadge(selection);	
			}		
		}
		for (key in variant.vepSIFT) {
			if (matrixCard.siftMap[key]) {
				var clazz = matrixCard.siftMap[key].clazz;
				if (clazz) {
					if (!tooltip.select(".sift").empty()) {
						$(tooltip[0]).find(".sift").prepend("<svg class=\"sift-badge\" height=\"12\" width=\"13\">");
						var selection = tooltip.select('.sift-badge').data([{width:11, height:11, transform: 'translate(0,1)', clazz: clazz }]);					
						matrixCard.showSiftSymbol(selection);				
					}
				}			
			}

		}

		for (key in variant.vepPolyPhen) {
			if (matrixCard.polyphenMap[key]) {
				var clazz = matrixCard.polyphenMap[key].clazz;
				if (clazz) {
					if (!tooltip.select(".polyphen").empty()) {
						$(tooltip[0]).find(".polyphen").prepend("<svg class=\"polyphen-badge\" height=\"12\" width=\"12\">");
						var selection = tooltip.select('.polyphen-badge').data([{width:10, height:10, transform: 'translate(0,2)', clazz: clazz }]);					
						matrixCard.showPolyPhenSymbol(selection);				
					}
				}
			}
		}		
	} else {
		var translate = variant.type.toLowerCase() == "snp" || variant.type.toLowerCase() == "mnp" ? 'translate(0,2)' : 'translate(5,6)';
		
		var impactList =  (filterCard.annotationScheme == null || filterCard.annotationScheme.toLowerCase() == 'snpeff' ? variant.impact : variant[IMPACT_FIELD_TO_COLOR]);
		for (impact in impactList) {
			var theClazz = 'impact_' + impact;	
			if ($(selector + " .tooltip-value.impact-badge").length > 0) {			
				$(selector + " .tooltip-value.impact-badge").prepend("<svg class=\"impact-badge\" style=\"float:left\" height=\"12\" width=\"14\">");
				var selection = d3.select(selector + ' .impact-badge svg.impact-badge ').data([{width:10, height:10,clazz: theClazz,  transform: translate, type: variant.type}]);
				matrixCard.showImpactBadge(selection);	
			}
		}		

		if ($(selector + " .tooltip-value.highest-impact-badge").length > 0) {
			var highestImpactList =  (filterCard.annotationScheme == null || filterCard.annotationScheme.toLowerCase() == 'snpeff' ? variant.highestImpact : variant.highestImpactVep);
			for (impact in highestImpactList) {
				var theClazz = 'impact_' + impact;	
				if ($(selector + " .tooltip-value.highest-impact-badge").length > 0) {				
					$(selector + " .tooltip-value.highest-impact-badge").prepend("<svg class=\"impact-badge\" style=\"float:left\" height=\"12\" width=\"14\">");
					var selection = d3.select(selector + ' .highest-impact-badge svg.impact-badge').data([{width:10, height:10,clazz: theClazz, transform: translate, type: variant.type}]);
					matrixCard.showImpactBadge(selection);	
				}
			}
		}

	}


    for (key in variant.clinVarClinicalSignificance) {
    	if (matrixCard.clinvarMap.hasOwnProperty(key)) {
		    var clazz = matrixCard.clinvarMap[key].clazz;
		    var badge = matrixCard.clinvarMap[key].examineBadge;
		    if (badge && $(selector + " .tooltip-header:contains('ClinVar')").length > 0) {
				$(selector + " .tooltip-header:contains('ClinVar')").next().prepend("<svg class=\"clinvar-badge\" style=\"float:left\"  height=\"12\" width=\"14\">");
				var selection = d3.select(selector + ' .clinvar-badge').data([{width:10, height:10, transform: 'translate(0,1)', clazz: clazz}]);
				matrixCard.showClinVarSymbol(selection);						    			    	
		    }
		}
	}

	for (key in variant.vepSIFT) {
		if (matrixCard.siftMap[key]) {
			var clazz = matrixCard.siftMap[key].clazz;
			if (clazz && $(selector + " .tooltip-header:contains('SIFT')").length > 0) {
				$(selector + " .tooltip-header:contains('SIFT')").next().prepend("<svg class=\"sift-badge\" style=\"float:left\"  height=\"12\" width=\"13\">");
				var selection = d3.select(selector + ' .sift-badge').data([{width:11, height:11, transform: 'translate(0,1)', clazz: clazz }]);					
				matrixCard.showSiftSymbol(selection);				
			}			
		}

	}

	for (key in variant.vepPolyPhen) {
		if (matrixCard.polyphenMap[key]) {
			var clazz = matrixCard.polyphenMap[key].clazz;
			if (clazz && $(selector + " .tooltip-header:contains('PolyPhen')").length > 0) {
				$(selector + " .tooltip-header:contains('PolyPhen')").next().prepend("<svg class=\"polyphen-badge\" style=\"float:left\"   height=\"12\" width=\"12\">");
				var selection = d3.select(selector + ' .polyphen-badge').data([{width:10, height:10, transform: 'translate(0,2)', clazz: clazz }]);					
				matrixCard.showPolyPhenSymbol(selection);				
			}
		}
	}



	if (variant.inheritance && variant.inheritance != '') {
		var clazz = matrixCard.inheritanceMap[variant.inheritance].clazz;
		var symbolFunction = matrixCard.inheritanceMap[variant.inheritance].symbolFunction;
		if ($(selector + " .tooltip-title:contains('inheritance')").length > 0) {
			$(selector + " .tooltip-title:contains('inheritance')").prepend("<svg class=\"inheritance-badge\"  height=\"20\" width=\"20\">");
			var options = {width:22, height:22, transform: 'translate(0,4)'};
			var selection = d3.select(selector + ' .inheritance-badge').data([{clazz: clazz}]);
			symbolFunction(selection, options);					
		}
	}	

	if (matrixCard.isNumeric(variant.afExAC)) {
		var rawValue = variant.afExAC;
		var afClazz = null;
		var afSymbolFunction = null;
		var lowestValue = 999;
		matrixCard.afExacMap.forEach( function(rangeEntry) {
			if (+rawValue > rangeEntry.min && +rawValue <= rangeEntry.max) {
				if (rangeEntry.value < lowestValue) {
					lowestValue = rangeEntry.value;
					afClazz = rangeEntry.clazz;
					afSymbolFunction = rangeEntry.symbolFunction;					
				}
			}
		});
		if (afClazz && $(selector + " .tooltip-header:contains('Allele Freq ExAC')").length > 0) {
			$(selector + " .tooltip-header:contains('Allele Freq ExAC')").next().prepend("<svg class=\"afexac-badge\" style=\"float:left\" height=\"14\" width=\"15\">");
			var selection = d3.select(selector + ' .afexac-badge').data([{clazz: afClazz}]);
			afSymbolFunction(selection, {transform: 'translate(2,0)'});		
		}		
	}
	
	if (matrixCard.isNumeric(variant.af1000G)) {
		var rawValue = variant.af1000G;
		var afClazz = null;
		var afSymbolFunction = null;
		var lowestValue = 999;
		matrixCard.afExacMap.forEach( function(rangeEntry) {
			if (+rawValue > rangeEntry.min && +rawValue <= rangeEntry.max) {
				if (rangeEntry.value < lowestValue) {
					lowestValue = rangeEntry.value;
					afClazz = rangeEntry.clazz;
					afSymbolFunction = rangeEntry.symbolFunction;
				}
			}
		});
		if (afClazz && $(selector + " .tooltip-header:contains('Allele Freq 1000G')").length > 0) {
			$(selector + " .tooltip-header:contains('Allele Freq 1000G')").next().prepend("<svg class=\"af1000g-badge\" style=\"float:left\" height=\"14\" width=\"15\">");
			var selection = d3.select(selector + ' .af1000g-badge').data([{clazz: afClazz}]);
			afSymbolFunction(selection, {transform: 'translate(2,0)'});		
		}		
	}	
}


VariantTooltip.prototype.formatContent = function(variant, pinMessage, type) {
	var me = this;

	if (type == null) {
		type = 'tooltip';
	}

	var effectDisplay = "";
	for (var key in variant.effect) {
		if (effectDisplay.length > 0) {
		  	effectDisplay += ", ";
		}formatContent
		// Strip out "_" from effect
		var tokens = key.split("_");
		if (isLevelEdu) {
			effectDisplay = tokens.join(" ");
		} else {
			effectDisplay += tokens.join(" ");
		}
	}    
	var impactDisplay = "";
	for (var key in variant.impact) {
		if (impactDisplay.length > 0) {
		  	impactDisplay += ", ";
		}
		if (isLevelEdu) {
			impactDisplay = levelEduImpact[key];
		} else {
			impactDisplay += key;
		}
	} 
	var clinSigDisplay = "";
	for (var key in variant.clinVarClinicalSignificance) {
		if (key != 'none' && key != 'undefined' ) {
			if (!isLevelEdu || (key.indexOf("uncertain_significance") >= 0 || key.indexOf("pathogenic") >= 0)) {
				if (clinSigDisplay.length > 0 ) {
				  	clinSigDisplay += ", ";
				}
				clinSigDisplay += key.split("_").join(" ");	
			}		
		}
	}

	var phenotypeDisplay = "";
	for (var key in variant.clinVarPhenotype) {
		if (key != 'not_specified'  && key != 'undefined') {
			if (phenotypeDisplay.length > 0) {
			  	phenotypeDisplay += ", ";
			}
			phenotypeDisplay += key.split("_").join(" ");
		}
	}      
	//var coord = variant.start + (variant.end > variant.start+1 ?  '-' + variant.end : "");
	var coord = gene.chr + ":" + variant.start;
	var refalt = variant.ref + "->" + variant.alt;
	if (variant.ref == '' && variant.alt == '') {
		refalt = '(' + variant.len + ' bp)';
	}

	var clinvarUrl = "";
	if (clinSigDisplay != null && clinSigDisplay != "") {
		if (variant.clinVarUid != null && variant.clinVarUid != '') {
			var url = 'http://www.ncbi.nlm.nih.gov/clinvar/variation/' + variant.clinVarUid;
			clinvarUrl = '<a href="' + url + '" target="_new"' + '>' + clinSigDisplay + '</a>';
		} else {
			clinvarUrl = clinSigDisplay;
		}		
	}

	var zygosity = "";
	if (variant.zygosity && variant.zygosity.toLowerCase() == 'het') {
		zygosity = "Heterozygous";
	} else if (variant.zygosity && variant.zygosity.toLowerCase() == 'hom') {
		zygosity = "Homozygous";
	}

	var vepImpactDisplay = "";
	for (var key in variant.vepImpact) {
		if (vepImpactDisplay.length > 0) {
		  	vepImpactDisplay += ", ";
		}
		if (isLevelEdu) {
			vepImpactDisplay = levelEduImpact[key];
		} else {
			vepImpactDisplay += key;
		}
	} 

	// If the highest impact occurs in a non-canonical transcript, show the impact followed by
	// the consequence and corresponding transcripts
	var vepHighestImpacts = VariantModel.getNonCanonicalHighestImpactsVep(variant);
	var vepHighestImpactDisplay = "";	
	var vepHighestImpactDisplaySimple = "";
	for (impactKey in vepHighestImpacts) {


		var nonCanonicalEffects = vepHighestImpacts[impactKey];
		if (vepHighestImpactDisplay.length > 0) {
		  	vepHighestImpactDisplay += ", ";
		  	vepHighestImpactDisplaySimple += ", ";
		}

		vepHighestImpactDisplay += impactKey.toLowerCase();
		vepHighestImpactDisplaySimple += impactKey.toLowerCase();
		
		nonCanonicalEffects.forEach(function(nonCanonicalEffect) {
			vepHighestImpactDisplay += " ("; 
			for (effectKey in nonCanonicalEffect) {
				var transcriptString = nonCanonicalEffect[effectKey].url;
				vepHighestImpactDisplay += " " + effectKey.split("\&").join(" & ") + ' in ' + transcriptString;
				//vepHighestImpactDisplaySimple += effectKey.split("\&").join(" & ") + "  ";
			}
			vepHighestImpactDisplay += ")"; 
		})
		vepHighestImpactDisplaySimple += " in non-canonical transcripts";
	}

	var vepHighestImpactRow = "";
	var vepHighestImpactExamineRow = "";
	if (vepHighestImpactDisplay.length > 0) {
		vepHighestImpactRow = me._tooltipHeaderRow(vepHighestImpactDisplaySimple, '', '', '', 'highest-impact-badge');
		vepHighestImpactExamineRow = me._tooltipRow('Most severe impact', vepHighestImpactDisplay, null, true, 'highest-impact-badge');
	}

	var vepConsequenceDisplay = "";
	for (var key in variant.vepConsequence) {
		if (vepConsequenceDisplay.length > 0) {
		  	vepConsequenceDisplay += ", ";
		}
		if (isLevelEdu) {
			vepConsequenceDisplay = key.split("_").join(" ");
		} else {
			vepConsequenceDisplay += key.split("_").join(" ");
		}
	}     	
	var vepHGVScDisplay = "";
	for (var key in variant.vepHGVSc) {
		if (vepHGVScDisplay.length > 0) {
		  	vepHGVScDisplay += ", ";
		}
		vepHGVScDisplay += key;
	}   
	var vepHGVSpDisplay = "";
	for (var key in variant.vepHGVSp) {
		if (vepHGVSpDisplay.length > 0) {
		  	vepHGVSpDisplay += ", ";
		}
		vepHGVSpDisplay += key;
	}   
	var vepSIFTDisplay = "";
	for (var key in variant.vepSIFT) {
		if (vepSIFTDisplay.length > 0) {
		  	vepSIFTDisplay += ", ";
		}
		vepSIFTDisplay += key.split("_").join(" ");
	} 
	var vepPolyPhenDisplay = "";
	for (var key in variant.vepPolyPhen) {
		if (vepPolyPhenDisplay.length > 0) {
		  	vepPolyPhenDisplay += ", ";
		}
		if (isLevelEdu) {
			vepPolyPhenDisplay = key.split("_").join(" ");
		} else {
			vepPolyPhenDisplay += key.split("_").join(" ");
		}
	} 
	
	var vepRegDisplay = "";
	for (var key in variant.regulatory) {
		// Bypass motif-based features
		if (key.indexOf("mot_") == 0) {
			continue;
 		}
 		if (vepRegDisplay.length > 0) {
		  	vepRegDisplay += ", ";
		}
		var value = variant.regulatory[key];
		vepRegDisplay += value;
	} 
	var vepRegMotifDisplay = "";
	if (variant.vepRegs) {
		for (var i = 0; i < variant.vepRegs.length; i++) {
			vr = variant.vepRegs[i];
			if (vr.motifName != null && vr.motifName != '') {
				
				if (vepRegMotifDisplay.length > 0) {
				  	vepRegMotifDisplay += ", ";
				}

				var tokens = vr.motifName.split(":");
				var baseMotifName;
				if (tokens.length == 2) {
					baseMotifName = tokens[1];
				}

				var regUrl = "http://jaspar.genereg.net/cgi-bin/jaspar_db.pl?ID=" + baseMotifName + "&rm=present&collection=CORE"
				vepRegMotifDisplay += '<a href="' + regUrl + '" target="_motif">' + vr.motifName + '</a>';
			}
		} 		
	}

	var dbSnpUrl = "";
	for (var key in variant.vepVariationIds) {
		if (key != 0 && key != '') {
			var tokens = key.split("&");
			tokens.forEach( function(id) {
				if (id.indexOf("rs") == 0) {
					if (dbSnpUrl.length > 0) {
						dbSnpUrl += ",";
					}
					var url1 = "http://www.ncbi.nlm.nih.gov/projects/SNP/snp_ref.cgi?rs=" + id;
					dbSnpUrl +=  '<a href="' + url1 + '" target="_dbsnp"' + '>' + id + '</a>';					
				}
			});
		}
	};

	var inheritanceModeRow =  variant.inheritance == null || variant.inheritance == '' || variant.inheritance == 'none' 
	                          ? ''
						      : me._tooltipHeaderRow('<strong><em>' + variant.inheritance + ' inheritance</em></strong>', '', '', '', null, 'padding-top:6px;');

	var effectLabel = filterCard.getAnnotationScheme() == null || filterCard.getAnnotationScheme() == 'snpEff' 
	                  ? effectDisplay 
					  : vepConsequenceDisplay;

	var impactLabel = filterCard.getAnnotationScheme() == null || filterCard.getAnnotationScheme() == 'snpEff' 
	                  ? impactDisplay 
					  : vepImpactDisplay;					  

	var siftLabel = vepSIFTDisplay != ''  && vepSIFTDisplay != 'unknown' 
	                ? 'SIFT ' + vepSIFTDisplay
	                : "";
	var polyphenLabel = vepPolyPhenDisplay != '' && vepPolyPhenDisplay != 'unknown' 
	                    ? 'PolyPhen ' + vepPolyPhenDisplay
	                    : "";
	var sep = siftLabel != '' && polyphenLabel != '' ? '&nbsp;&nbsp;&nbsp;&nbsp;' : ''
	var siftPolyphenRow = '';
	if (siftLabel || polyphenLabel) {
		siftPolyphenRow = me._tooltipClassedRow(polyphenLabel + sep, 'polyphen', siftLabel, 'sift', 'padding-top:5px;');
	}

	var clinvarRow = '';
	if (clinvarUrl != '') {
		clinvarRow = me._tooltipLongTextRow('ClinVar', clinvarUrl);
	}

	var clinvarRow1 = '';
	var clinvarRow2 = '';
	if (clinSigDisplay) {
		if (isLevelEdu) {
			clinvarRow1 = me._tooltipWideHeadingRow('Known from research', clinSigDisplay, '10px');		
		} else {
			clinvarRow1 = me._tooltipWideHeadingSecondRow('ClinVar', clinSigDisplay);		
		}
		if (phenotypeDisplay) {
			if (isLevelEdu) {
				clinvarRow2 = me._tooltipWideHeadingSecondRow('', phenotypeDisplay);		
			} else {
				clinvarRow2 = me._tooltipLongTextRow('', phenotypeDisplay);		
			}
		}
	}

	var polyphenRowSimple = vepPolyPhenDisplay != "" ? me._tooltipWideHeadingRow('Predicted effect', vepPolyPhenDisplay + ' to protein', '3px') : "";

	
	var dbSnpId = getRsId(variant);	

	var genotypeRow = isLevelEdu && eduTourNumber == 2 ? me._tooltipHeaderRow('Genotype', switchGenotype(variant.eduGenotype), '','')  : "";

	var qualityWarningRow = "";
	if (filterCard.shouldWarnForNonPassVariants()) {
		if (variant.recfilter != 'PASS') {
			if (!variant.hasOwnProperty('fbCalled') || variant.fbCalled != 'Y') {
				qualityWarningRow = me._tooltipLowQualityHeaderRow();
			}
		}
	}

	if (isLevelEdu) {
		return (
			genotypeRow
			+ me._tooltipMainHeaderRow('Severity - ' + impactLabel , '', '', '')
			//+ me._tooltipHeaderRow((variant.type ? variant.type.toUpperCase() : ''), effectLabel, '', '')
			+ inheritanceModeRow
			+ polyphenRowSimple
			+ clinvarRow1
			+ clinvarRow2 );
	} else if (type == 'tooltip') {
		return (
			  qualityWarningRow
			+  me._tooltipMainHeaderRow(variant.type ? variant.type.toUpperCase() : "", refalt, coord, dbSnpId ? '    (' + dbSnpId  + ')' : '')
			+ me._tooltipHeaderRow(effectLabel, '', '', '')
			+ vepHighestImpactRow
			+ inheritanceModeRow
			+ siftPolyphenRow
			+ me._tooltipLabeledRow('Allele Freq ExAC', (variant.afExAC == -100 ? "n/a" : percentage(variant.afExAC)), '10px')
			+ me._tooltipLabeledRow('Allele Freq 1000G', percentage(variant.af1000G), null, '10px')
			+ clinvarRow1
			+ clinvarRow2
			+ me._tooltipRowAlleleCounts() 
			+ me._linksRow(variant, pinMessage)
		);                  

	} else if (type == 'tooltip-wide') {

		var leftDiv =  
		    '<div class="tooltip-left-column">' 
		    + me._tooltipRow((filterCard.getAnnotationScheme() == null || filterCard.getAnnotationScheme() == 'snpEff' ? 'SnpEff Effect' : 'VEP Consequence'),  
					        (filterCard.getAnnotationScheme() == null || filterCard.getAnnotationScheme() == 'snpEff' ? effectDisplay : vepConsequenceDisplay))
			+ me._tooltipRow((filterCard.getAnnotationScheme() == null || filterCard.getAnnotationScheme() == 'snpEff' ? 'Impact' : 'Impact'),  
					        ' ' + (filterCard.getAnnotationScheme() == null || filterCard.getAnnotationScheme() == 'snpEff' ? impactDisplay.toLowerCase() : vepImpactDisplay.toLowerCase()), null, true, 'impact-badge')
			+ vepHighestImpactExamineRow			
			+ me._tooltipRow('PolyPhen', vepPolyPhenDisplay, null, true)
			+ me._tooltipRow('SIFT', vepSIFTDisplay, null, true)
			+ me._tooltipRowURL('Regulatory', vepRegDisplay, null, true)
			+ me._tooltipRow('ClinVar', '<span style="float:left">' + clinvarUrl + '</span>', null, true)
			+ me._tooltipRow('&nbsp;', phenotypeDisplay)
			+ me._tooltipRow('HGVSc', vepHGVScDisplay, null, true)
			+ me._tooltipRow('HGVSp', vepHGVSpDisplay, null, true)
			+ "</div>";

		var rightDiv = 
			'<div class="tooltip-right-column">' 
			+ me._tooltipRow('Allele Freq ExAC', '<span style="float:left">' + (variant.afExAC == -100 ? "n/a" : percentage(variant.afExAC) + '</span>'))
			+ me._tooltipRow('Allele Freq 1000G', '<span style="float:left">' + percentage(variant.af1000G) + '</span>')
			+ me._tooltipRow('Qual', variant.qual, null, true) 
			+ me._tooltipRow('Filter', variant.recfilter, null, true) 
			+ me._tooltipRowAlleleCounts() 
			+ "</div>";

		var div =
		    '<div class="tooltip-wide">'
	        + qualityWarningRow
			+ me._tooltipMainHeaderRow(variant.type ? variant.type.toUpperCase() : "", refalt, '   ', dbSnpUrl)
			+ me._tooltipHeaderRow(window.gene.gene_name, coord, '', '')
			+ inheritanceModeRow
			+ leftDiv
			+ rightDiv
			+ me._linksRow(variant)	
			+ "</div>";

		return div;

	} else {
		return (
			qualityWarningRow
			+ me._tooltipMainHeaderRow(variant.type ? variant.type.toUpperCase() : "", refalt, '   ', dbSnpUrl)
			+ me._tooltipHeaderRow(window.gene.gene_name, coord, '', '')
			+ inheritanceModeRow

			+ me._tooltipRow((filterCard.getAnnotationScheme() == null || filterCard.getAnnotationScheme() == 'snpEff' ? 'SnpEff Effect' : 'VEP Consequence'),  
					        (filterCard.getAnnotationScheme() == null || filterCard.getAnnotationScheme() == 'snpEff' ? effectDisplay : vepConsequenceDisplay),
					        "10px")
			+ me._tooltipRow((filterCard.getAnnotationScheme() == null || filterCard.getAnnotationScheme() == 'snpEff' ? 'Impact' : 'Impact'),  
					        (filterCard.getAnnotationScheme() == null || filterCard.getAnnotationScheme() == 'snpEff' ? impactDisplay.toLowerCase() : vepImpactDisplay.toLowerCase()), null, true, 'impact-badge')
			+ vepHighestImpactExamineRow			
			+ me._tooltipRow('SIFT', vepSIFTDisplay)
			+ me._tooltipRow('PolyPhen', vepPolyPhenDisplay)
			+ me._tooltipRow('ClinVar', clinvarUrl)
			+ me._tooltipRow('&nbsp;', phenotypeDisplay)
			+ me._tooltipRow('Allele Freq ExAC', (variant.afExAC == -100 ? "n/a" : percentage(variant.afExAC)))
			+ me._tooltipRow('Allele Freq 1000G', percentage(variant.af1000G))
			+ me._tooltipRowURL('Regulatory', vepRegDisplay)
			+ me._tooltipRow('HGVSc', vepHGVScDisplay)
			+ me._tooltipRow('HGVSp', vepHGVSpDisplay)
			+ me._tooltipRow('Qual', variant.qual) 
			+ me._tooltipRow('Filter', variant.recfilter) 
			+ me._tooltipRowAlleleCounts() 
			+ me._linksRow(variant)
		);                  

	}
	

	        

}


VariantTooltip.prototype.variantTooltipMinimalHTML = function(variant) {
	var me = this;

	var zygosity = "";
	if (variant.zygosity.toLowerCase() == 'het') {
		zygosity = "Heterozygous";
	} else if (variant.zygosity.toLowerCase() == 'hom') {
		zygosity = "Homozygous";
	}
	
	
	return (
		me._tooltipRow('Zygosity',  zygosity)
		+ me._tooltipRow('Qual &amp; Filter', variant.qual + ', ' + variant.filter) 
		);
              

}


VariantTooltip.prototype._linksRow = function(variant, pinMessage) {
	if (pinMessage == null) {
		pinMessage = 'Click on variant to pin tooltip';
	}


	var bookmarkLink =  '<a id="bookmarkLink" href="javascript:void(0)" onclick="bookmarkVariant();showAsBookmarked(this)">bookmark this variant</a>';
	
	var bookmarkBadge = '<svg class="bookmark-badge" height="14" ><g class="bookmark" transform="translate(0,0)"><use xmlns:xlink="http://www.w3.org/1999/xlink" xlink:href="#bookmark-symbol" width="12" height="12"></use><text x="12" y="11" style="fill: black;">bookmarked</text></g></svg>';
	var removeBookmarkLink  =  '<a id="remove-bookmark-link" href="javascript:void(0)" onclick="removeBookmarkOnVariant();showAsNotBookmarked(this)">remove bookmark</a>'
	showAsBookmarked = function(container) {
		$(container).parent().html(bookmarkBadge + removeBookmarkLink);
	};
	showAsNotBookmarked = function(container) {
		$(container).parent().html(bookmarkLink);
	};

	if (window.clickedVariant) {
		if (window.clickedVariant.hasOwnProperty('isBookmark') && window.clickedVariant.isBookmark == 'Y') {
			return '<div class="row tooltip-footer">'
			  + '<div class="col-sm-8" id="bookmarkLink" style="text-align:left;">' +  bookmarkBadge + removeBookmarkLink  + '</div>'
			  + '<div class="col-sm-4" style="text-align:right;">' + '<a id="unpin" href="javascript:void(0)">unpin</a>' + '</div>'
			  + '</div>';

		} else {
			return '<div class="row tooltip-footer" style="">'
			  + '<div class="col-sm-8" style="text-align:left;">' + bookmarkLink + '</div>'
			  + '<div class="col-sm-4" style="text-align:right;">' + '<a id="unpin" href="javascript:void(0)">unpin</a>' + '</div>'
			  + '</div>';

		}
		
	} else {
		if (variant.hasOwnProperty('isBookmark') && variant.isBookmark == 'Y') {
			return '<div class="row tooltip-footer">'
			  + '<div class="col-sm-6 " id="bookmarkLink" style="text-align:left;">' +  bookmarkBadge + '</div>'
			  + '<div class="col-md-6 " style="text-align:right;">' +  '<em>' + pinMessage + '</em>' + '</div>'
			  + '</div>';

		} else {
			return '<div class="row tooltip-footer">'
			  + '<div class="col-md-12 " style="text-align:right;">' +  '<em>' + pinMessage + '</em>' + '</div>'
			  + '</div>';
		}
	}
}

VariantTooltip.prototype._tooltipBlankRow = function() {
	return '<div class="row">'
	  + '<div class="col-md-12">' + '  ' + '</div>'
	  + '</div>';
}

VariantTooltip.prototype._tooltipHeaderRow = function(value1, value2, value3, value4, clazz, style) {
	var theStyle = style ? style : '';
	var clazzList = "col-md-12 tooltip-title";
	if (clazz) {
		clazzList += " " + clazz;
	}
	return '<div class="row" style="' + theStyle + '">'
	      + '<div class="' + clazzList + '" style="text-align:center">' + value1 + ' ' + value2 + ' ' + value3 +  ' ' + value4 + '</div>'
	      + '</div>';	
}
VariantTooltip.prototype._tooltipMainHeaderRow = function(value1, value2, value3, value4) {
	return '<div class="row">'
	      + '<div class="col-md-12 tooltip-title main-header" style="text-align:center">' + value1 + ' ' + value2 + ' ' + value3 +  ' ' + value4 + '</div>'
	      + '</div>';	
}
VariantTooltip.prototype._tooltipLowQualityHeaderRow = function() {
	return '<div class="row">'
	      + '<div class="col-md-12 tooltip-title danger" style="text-align:center">' + 'FLAGGED FOR NOT MEETING FILTERING CRITERIA' + '</div>'
	      + '</div>';	
}

VariantTooltip.prototype._tooltipHeaderLeftJustifyRow = function(value1, value2, value3, value4) {
	return '<div class="row">'
	      + '<div class="col-md-12 tooltip-title" style="text-align:left">' + value1 + ' ' + value2 + ' ' + value3 +  ' ' + value4 + '</div>'
	      + '</div>';	
}

VariantTooltip.prototype._tooltipHeaderLeftJustifySimpleRow = function(value1) {
	return '<div class="row">'
	      + '<div class="col-md-12 tooltip-title" style="text-align:left">' + value1 + '</div>'
	      + '</div>';	
}

VariantTooltip.prototype._tooltipClassedRow = function(value1, class1, value2, class2, style) {
	var theStyle = style ? style : '';
	return '<div class="row" style="' + theStyle + '">'
	      +  '<div class="col-md-12 tooltip-title" style="text-align:center">' 
	      +    "<span class='" + class1 + "'>" + value1 + '</span>' 
	      +    "<span class='" + class2 + "'>" + value2 + '</span>' 
	      +  '</div>'
	      + '</div>';	
}

VariantTooltip.prototype._tooltipLabeledRow = function(value1, value2, paddingTop, paddingBottom) {
	var thePaddingTop    = paddingTop    ? "padding-top:"    + paddingTop    + ";" : "";
	var thePaddingBottom = paddingBottom ? "padding-bottom:" + paddingBottom + ";" : "";
	return '<div class="row" style="' + thePaddingTop + thePaddingBottom + '">'
	      + '<div class="col-sm-6 tooltip-title"  style="text-align:right;word-break:normal">' + value1  +'</div>'
	      + '<div class="col-sm-6 tooltip-title" style="text-align:left;word-break:normal">' + value2 + '</div>'
	      + '</div>';	
}

VariantTooltip.prototype._tooltipWideHeadingRow = function(value1, value2, paddingTop) {
	var thePaddingTop = paddingTop ? "padding-top:" + paddingTop + ";" : "";
	return '<div class="row" style="padding-bottom:5px;' + thePaddingTop + '">'
	      + '<div class="col-sm-4 tooltip-title"  style="text-align:right;word-break:normal">' + value1  +'</div>'
	      + '<div class="col-sm-8 tooltip-title" style="text-align:left;word-break:normal">' + value2 + '</div>'
	      + '</div>';	
}
VariantTooltip.prototype._tooltipWideHeadingSecondRow = function(value1, value2, paddingTop) {
	var thePaddingTop = paddingTop ? "padding-top:" + paddingTop + ";" : "";
	return '<div class="row" style="padding-bottom:5px;' + thePaddingTop + '">'
	      + '<div class="col-sm-4 tooltip-title" style="text-align:right;word-break:normal">' + value1  +'</div>'
	      + '<div class="col-sm-8 tooltip-title" style="text-align:left;word-break:normal">' + value2 + '</div>'
	      + '</div>';	
}

VariantTooltip.prototype._tooltipLongTextRow = function(value1, value2, paddingTop) {
	var thePaddingTop = paddingTop ? "padding-top:" + paddingTop + ";" : "";
	return '<div class="row" style="' + thePaddingTop + '">'
	      + '<div class="col-sm-3 tooltip-title" style="text-align:left;word-break:normal">' + value1  +'</div>'
	      + '<div class="col-sm-9 tooltip-title" style="text-align:left;word-break:normal">' + value2 + '</div>'
	      + '</div>';	
}
VariantTooltip.prototype._tooltipShortTextRow = function(value1, value2, value3, value4, paddingTop) {
	var thePaddingTop = paddingTop ? "padding-top:" + paddingTop + ";" : "";

	return '<div class="row" style="padding-bottom:5px;' + thePaddingTop + '">'
	      + '<div class="col-sm-4 tooltip-label" style="text-align:right;word-break:normal;padding-right:5px;">' + value1  +'</div>'
	      + '<div class="col-sm-2 " style="text-align:left;word-break:normal;padding-left:0px;">' + value2 + '</div>'
	      + '<div class="col-sm-4 tooltip-label" style="text-align:right;word-break:normal;padding-right:5px;">' + value3  +'</div>'
	      + '<div class="col-sm-2 " style="text-align:left;word-break:normal;padding-left:0px">' + value4 + '</div>'
	      + '</div>';			

}



VariantTooltip.prototype._tooltipRow = function(label, value, paddingTop, alwaysShow, valueClazz) {
	if (alwaysShow || (value && value != '')) {
		var style = paddingTop ? ' style="padding-top:' + paddingTop + '" '  : '';
		var valueClazzes = "tooltip-value";
		if (valueClazz) {
			valueClazzes += " " + valueClazz;
		}
		return '<div class="tooltip-row"' + style + '>'
		      + '<div class="tooltip-header" style="text-align:right">' + label + '</div>'
		      + '<div class="' + valueClazzes + '">' + value + '</div>'
		      + '</div>';
	} else {
		return "";
	}
}

VariantTooltip.prototype._tooltipRowURL = function(label, value, paddingTop, alwaysShow) {
	if (alwaysShow || (value && value != '')) {
		var style = paddingTop ? ' style="padding-top:' + paddingTop + '" '  : '';
		return '<div class="tooltip-row"' + style + '>'
		      + '<div class="tooltip-header" style="text-align:right">' + label + '</div>'
		      + '<div class="tooltip-value">' + value + '</div>'
		      + '</div>';
	} else {
		return "";
	}
}

VariantTooltip.prototype._tooltipRowAF = function(label, afExAC, af1000g) {
	return '<div class="tooltip-row">'
		      + '<div class="tooltip-header" style="text-align:right">' + label + '</div>'
		      + '<div class="tooltip-value">' + 'ExAC: ' + afExAC  + '    1000G: ' + af1000g + '</div>'
		 + '</div>';
}

VariantTooltip.prototype._tooltipRowAlleleCounts = function(label) {
	return '<div  id="coverage-svg" style="padding-top:0px">'
		 + '</div>';
}

VariantTooltip.prototype.createAlleleCountSVGTrio = function(variantCard, container, variant, barWidth) {
	var me = this;
	
	// Get the alt, ref, depth and zygosity field for proband, mother, father of trio
	var trioFields = me._getTrioAlleleCountFields(variantCard, variant);

	container.select("div.proband-alt-count").remove();	
	me._appendReadCountHeading(container);

	
	// Show the Proband's allele counts
	var selectedClazz = dataCard.mode == 'trio' && trioFields.PROBAND.selected ? 'selected' : '';
	var row = container.append("div")
	                   .attr("class", "proband-alt-count tooltip-row");
	row.append("div")
	   .attr("class", "proband-alt-count tooltip-header-small")
	   .html("<span class='tooltip-subtitle " + selectedClazz + "'>" + 'Proband' + "</span>");
	row.append("div")
		   .attr("class", "tooltip-zygosity label " + ( trioFields.PROBAND.zygosity ? trioFields.PROBAND.zygosity.toLowerCase() : ""))
		   .text(trioFields.PROBAND.zygosity ? capitalizeFirstLetter(trioFields.PROBAND.zygosity.toLowerCase()) : "");
	var column = row.append("div")
	                .attr("class", "proband-alt-count tooltip-allele-count-bar");
	if (trioFields.PROBAND.zygosity && trioFields.PROBAND.zygosity != '') {
		me._appendAlleleCountSVG(column, 
			trioFields.PROBAND.genotypeAltCount, 
			trioFields.PROBAND.genotypeRefCount, 
			trioFields.PROBAND.genotypeDepth, 
			trioFields.PROBAND.bamDepth, 
			barWidth);	
	}  else if (!trioFields.PROBAND.done) {
		column.append("span").attr("class", "processing").text("analyzing..");
	}             

	
	if (dataCard.mode == 'trio') {
		// For a trio, now show the mother and father allele counts

		// Mother
		selectedClazz = trioFields.MOTHER.selected ? 'selected' : '';
		container.select("div.mother-alt-count").remove();
		row = container.append("div")
	                   .attr("class", "mother-alt-count tooltip-row");		    
		row.append("div")
		   .attr("class", "mother-alt-count tooltip-header-small")
		   .html("<span class='tooltip-subtitle " + selectedClazz + "'>Mother</span>");
		row.append("div")
		   .attr("class", "tooltip-zygosity label " + (trioFields.MOTHER.zygosity != null ? trioFields.MOTHER.zygosity.toLowerCase() : ""))
		   .text(trioFields.MOTHER.zygosity ? capitalizeFirstLetter(trioFields.MOTHER.zygosity.toLowerCase()) : "");
		column = row.append("div")
		            .attr("class", "mother-alt-count tooltip-allele-count-bar");
		if (trioFields.MOTHER.zygosity && trioFields.MOTHER.zygosity != '') {			            
			this._appendAlleleCountSVG(column, 
				trioFields.MOTHER.genotypeAltCount,
				trioFields.MOTHER.genotypeRefCount, 
				trioFields.MOTHER.genotypeDepth, 
				trioFields.MOTHER.bamDepth, 
				barWidth);		
		} else if (!trioFields.MOTHER.done) {
			column.append("span").attr("class", "processing").text("analyzing..");
		}

		// Father
		selectedClazz = trioFields.FATHER.selected ? 'selected' : '';
		container.select("div.father-alt-count").remove();
		row = container.append("div")
	                   .attr("class", "father-alt-count tooltip-row");	
		row.append("div")
	       .attr("class", "father-alt-count tooltip-header-small")
	       .html("<span class='tooltip-subtitle " + selectedClazz + "'>Father</span>");
		row.append("div")
		   .attr("class",  "tooltip-zygosity label " + (trioFields.FATHER.zygosity != null ? trioFields.FATHER.zygosity.toLowerCase() : ""))
		   .text(trioFields.FATHER.zygosity ? capitalizeFirstLetter(trioFields.FATHER.zygosity.toLowerCase()) : "");
		column = row.append("div")
	                .attr("class", "father-alt-count tooltip-allele-count-bar")
		if (trioFields.FATHER.zygosity && trioFields.FATHER.zygosity != '') {			            
			this._appendAlleleCountSVG(column, 
				trioFields.FATHER.genotypeAltCount, 
				trioFields.FATHER.genotypeRefCount, 
				trioFields.FATHER.genotypeDepth, 
				trioFields.FATHER.bamDepth,  
				barWidth);
		} else if (!trioFields.FATHER.done) {
			column.append("span").attr("class", "processing").text("analyzing..");
		}
    
	} 
}


VariantTooltip.prototype._getTrioAlleleCountFields = function(variantCard, variant) {
	var trioFields = {};
	if (variantCard.model.getRelationship() == 'proband') {
		trioFields.PROBAND = { zygosity: variant.zygosity, 
			                   genotypeAltCount: variant.genotypeAltCount, 
			                   genotypeRefCount: variant.genotypeRefCount, 
			                   genotypeDepth: variant.genotypeDepth,
			                   bamDepth: variant.bamDepth,
			                   selected: true,
			                   done: true };
		trioFields.MOTHER  = { zygosity: variant.motherZygosity, 
			                   genotypeAltCount: variant.genotypeAltCountMother, 
			                   genotypeRefCount: variant.genotypeRefCountMother, 
			                   genotypeDepth: variant.genotypeDepthMother,
			                   bamDepth: variant.bamDepthMother,
			                   done: variant.hasOwnProperty("motherZygosity") };
		trioFields.FATHER  = { zygosity: variant.fatherZygosity, 
			                   genotypeAltCount: variant.genotypeAltCountFather, 
			                   genotypeRefCount: variant.genotypeRefCountFather, 
			                   genotypeDepth: variant.genotypeDepthFather,
			                   bamDepth: variant.bamDepthFather,
			                   done: variant.hasOwnProperty("fatherZygosity") };
	} else if (variantCard.model.getRelationship() == 'mother') {
		trioFields.PROBAND = { zygosity: variant.probandZygosity, 
			                   genotypeAltCount: variant.genotypeAltCountProband, 
			                   genotypeRefCount: variant.genotypeRefCountProband, 
			                   genotypeDepth: variant.genotypeDepthProband,
			                   bamDepth: variant.bamDepthProband,
			                   done: variant.hasOwnProperty("probandZygosity")  };
		trioFields.MOTHER  = { zygosity: variant.zygosity, 
			                   genotypeAltCount: variant.genotypeAltCount, 
			                   genotypeRefCount: variant.genotypeRefCount, 
			                   genotypeDepth: variant.genotypeDepth,
			                   bamDepth: variant.bamDepth,
			                   selected: true,
			                   done: true };
		trioFields.FATHER =  { zygosity: variant.fatherZygosity, 
			                   genotypeAltCount: variant.genotypeAltCountFather, 
			                   genotypeRefCount: variant.genotypeRefCountFather, 
			                   genotypeDepth: variant.genotypeDepthFather,
			                   bamDepth: variant.bamDepthFather,
			                   done: variant.hasOwnProperty("fatherZygosity") };
	} else if (variantCard.model.getRelationship() == 'father') {
		trioFields.PROBAND = { zygosity: variant.probandZygosity, 
			                   genotypeAltCount: variant.genotypeAltCountProband, 
			                   genotypeRefCount: variant.genotypeRefCountProband, 
			                   genotypeDepth: variant.genotypeDepthProband,
			                   bamDepth: variant.bamDepthProband,
			                   done: variant.hasOwnProperty("probandZygosity") };
		trioFields.MOTHER  = { zygosity: variant.motherZygosity, 
			                   genotypeAltCount: variant.genotypeAltCountMother, 
			                   genotypeRefCount: variant.genotypeRefCountMother, 
			                   genotypeDepth: variant.genotypeDepthMother,
			                   bamDepth: variant.bamDepthMother,
			                   done: variant.hasOwnProperty("motherZygosity")  };
		trioFields.FATHER  = { zygosity: variant.zygosity, 
			                   genotypeAltCount: variant.genotypeAltCount, 
			                   genotypeRefCount: variant.genotypeRefCount, 
			                   genotypeDepth: variant.genotypeDepth,
			                   bamDepth: variant.bamDepth,
			                   selected: true,
			                   done: true };
	} 
	return trioFields;

}


VariantTooltip.prototype._appendReadCountHeading = function(container) {

	var svg = container.append("div")	
					   .attr("id", "allele-count-legend")	
		           	   .style("padding-top", "5px")		           	   
				       .append("svg")
				       .attr("width", 198)
	           		   .attr("height", "20");
	svg.append("text")
		   .attr("x", "0")
		   .attr("y", "14")
		   .attr("anchor", "start")		 
		   .attr("class", "tooltip-header-small")
		   .text("Read Counts");	  	           		   

	var g = svg.append("g")
	           .attr("transform", "translate(77,0)");

	g.append("text")
		   .attr("x", "13")
		   .attr("y", "9")
		   .attr("class", "alt-count-under")
		   .attr("anchor", "start")
		   .text("alt");	           		   
	g.append("text")
		   .attr("x", "37")
		   .attr("y", "9")
		   .attr("class", "other-count-under")
		   .attr("anchor", "start")
		   .text("other");	           		   
	g.append("text")
		   .attr("x", "70")
		   .attr("y", "9")
		   .attr("class", "ref-count")
		   .attr("anchor", "start")
		   .text("ref");	
	g.append("text")
		   .attr("x", "90")
		   .attr("y", "14")
		   .attr("class", "ref-count")
		   .attr("anchor", "start")
		   .text("total");	  		              		   

	g.append("rect")
	   .attr("x", "1")
	   .attr("y", "10")
	   .attr("height", 4)
	   .attr("width",28)
	   .attr("class", "alt-count");
	g.append("rect")
	   .attr("x", "29")
	   .attr("y", "10")
	   .attr("height", 4)
	   .attr("width",28)
	   .attr("class", "other-count");
	g.append("rect")
	   .attr("x", "57")
	   .attr("y", "10")
	   .attr("height", 4)
	   .attr("width",28)
	   .attr("class", "ref-count");

}

VariantTooltip.prototype._appendAlleleCountSVG = function(container, genotypeAltCount, 
	genotypeRefCount, genotypeDepth, bamDepth, barWidth) {

	var MAX_BAR_WIDTH = barWidth ? barWidth : 185;
	var PADDING = 20;
	var BAR_WIDTH = 0;
	if ((genotypeDepth == null || genotypeDepth == '') && (genotypeAltCount == null || genotypeAltCount.indexOf(",") >= 0)) {
		container.text("");
		var svg = container
	            .append("svg")
	            .attr("width", MAX_BAR_WIDTH + PADDING)
	            .attr("height", "21");
	    return;
	}

	if (genotypeAltCount == null || genotypeAltCount.indexOf(",") >= 0) {
		BAR_WIDTH = d3.round(MAX_BAR_WIDTH * (genotypeDepth / getProbandVariantCard().getMaxAlleleCount()));
		container.select("svg").remove();
		var svg = container
	            .append("svg")
	            .attr("width", MAX_BAR_WIDTH + PADDING)
	            .attr("height", "12");
		svg.append("rect")
		   .attr("x", "1")
  	  	   .attr("y", "1")
  		   .attr("height", 10)
		   .attr("width",BAR_WIDTH)
		   .attr("class", "ref-count");
		
		svg.append("text")
		   .attr("x", BAR_WIDTH + 5)
		   .attr("y", "9")
		   .text(genotypeDepth);

		var g = svg.append("g")
		           .attr("transform", "translate(0,0)");
		g.append("text")
		    .attr("x", BAR_WIDTH / 2)
		    .attr("y", "9")
		    .attr("text-anchor", "middle")
		    .attr("class", "ref-count")
	   		.text("?");
		return;
	} 

	var totalCount = genotypeDepth;
	var otherCount = totalCount - (+genotypeRefCount + +genotypeAltCount);

	// proportion the widths of alt, other (for multi-allelic), and ref
	BAR_WIDTH      = d3.round((MAX_BAR_WIDTH) * (totalCount / getProbandVariantCard().getMaxAlleleCount()));
	if (BAR_WIDTH < 10) {
		BAR_WIDTH = 10;
	}
	if (BAR_WIDTH > PADDING + 10) {
		BAR_WIDTH = BAR_WIDTH - PADDING;
	}
	var altPercent = +genotypeAltCount / totalCount;
	var altWidth   = d3.round(altPercent * BAR_WIDTH);
	var refPercent = +genotypeRefCount / totalCount;
	var refWidth   = d3.round(refPercent * BAR_WIDTH);
	var otherWidth = BAR_WIDTH - (altWidth+refWidth); 

	// Force a separate line if the bar width is too narrow for count to fit inside or
	// this is a multi-allelic.
	var separateLineForLabel = (altWidth > 0 && altWidth / 2 < 11) || (refWidth > 0 && refWidth / 2 < 11) || (otherWidth > 0);

	container.select("svg").remove();
	var svg = container
	            .append("svg")
	            .attr("width", MAX_BAR_WIDTH + PADDING)
	            .attr("height", separateLineForLabel ? "21" : "12");
	
	if (altWidth > 0) {
		svg.append("rect")
		 .attr("x", "1")
		 .attr("y", "1")
		 .attr("height", 10)
		 .attr("width",altWidth)
		 .attr("class", "alt-count");

	}

	if (otherWidth > 0) {
		svg.append("rect")
			 .attr("x", altWidth)
			 .attr("y", "1")
			 .attr("height", 10)
			 .attr("width", otherWidth)
			 .attr("class", "other-count");			
	}
	 
	if (refWidth > 0) {
		svg.append("rect")
		 .attr("x",  altWidth + otherWidth)
		 .attr("y", "1")
		 .attr("height", 10)
		 .attr("width", refWidth)
		 .attr("class", "ref-count");		
	}

	

	svg.append("text")
	   .attr("x", BAR_WIDTH + 5)
	   .attr("y", "9")
	   .text(totalCount);



	var altX = 0;
	var otherX = 0;
	var refX = 0;
	var g = svg.append("g")
	           .attr("transform", (separateLineForLabel ? "translate(-6,11)" : "translate(0,0)"));
	if (altWidth > 0) {
		var altX = d3.round(altWidth / 2);
		if (altX < 6) {
			altX = 6;
		}
		 g.append("text")
		   .attr("x", altX)
		   .attr("y", "9")
		   .attr("text-anchor", separateLineForLabel ? "start" : "middle")
		   .attr("class", separateLineForLabel ? "alt-count-under" : "alt-count")
		   .text(genotypeAltCount);

	}

 	if (otherCount > 0) {
 		otherX = altWidth  + d3.round(otherWidth / 2);
 		// Nudge the multi-allelic "other" count over to the right if it is
 		// too close to the alt count.
 		if (otherX - 11 < altX) {
 			otherX = altX + 10;
 		} 		
 		g.append("text")
		   .attr("x", otherX)
		   .attr("y", "9")
		   .attr("text-anchor", separateLineForLabel ? "start" : "middle")
		   .attr("class", separateLineForLabel ? "other-count-under" : "other-count")
		   .text(otherCount);

		var gNextLine = g.append("g")
		                 .attr("transform", "translate(-15,9)");
		svg.attr("height", 31);
		gNextLine.append("text")
		         .attr("x", otherX < 20 ? 20 : otherX)
				 .attr("y", "9")
				 .attr("text-anchor", "start")
				 .attr("class", "other-count-under" )
				 .text("(multi-allelic)");
	}	 
	if (genotypeRefCount > 0  && (altWidth > 0 || otherWidth > 0)) {
		refX = altWidth + otherWidth + d3.round(refWidth / 2);
		if (refX - 11 < otherX || refX - 11 < altX) {
			refX = refX + 10;
		}
		g.append("text")
			   .attr("x", refX)
			   .attr("y", "9")
			   .attr("text-anchor", separateLineForLabel ? "start" : "middle")
			   .attr("class", "ref-count")
			   .text(genotypeRefCount);
	}
	 
}


