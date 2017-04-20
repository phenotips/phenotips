// Create a variant model class
// Constructor
function VariantModel() {
	this.vcf = null;
	this.bam = null;

	this.vcfData = null;
	this.fbData = null;	
	this.bamData = null;

	this.vcfUrlEntered = false;
	this.vcfFileOpened = false;
	this.getVcfRefName = null;

	this.bamUrlEntered = false;
	this.bamFileOpened = false;
	this.getBamRefName = null;

	this.name = "";
	this.vcfRefNamesMap = {};
	this.sampleName = "";
	this.defaultSampleName = null;
	this.relationship = null;
	this.affectedStatus = null;

	this.GET_RSID = false;
	this.GET_HGVS = false;

	this.lastVcfAlertify = null;
	this.lastBamAlertify = null;

	this.debugMe = false;
}


VariantModel.prototype.setLoadState = function(theVcfData, taskName) {
	if (theVcfData != null) {
		if (theVcfData.loadState == null) {
			theVcfData.loadState = {};
		}
		theVcfData.loadState[taskName] = true;
	}
}

VariantModel.prototype.isLoaded = function() {
	return this.vcf != null && this.vcfData != null;
}


VariantModel.prototype.isReadyToLoad = function() {
	return this.isVcfReadyToLoad() || this.isBamReadyToLoad();
}

VariantModel.prototype.isBamReadyToLoad = function() {
	return this.bam != null && (this.bamUrlEntered || this.bamFileOpened);
}

VariantModel.prototype.isVcfReadyToLoad = function() {
	return this.vcf != null && (this.vcfUrlEntered || this.vcfFileOpened);
}


VariantModel.prototype.isBamLoaded = function() {
	return this.bam && (this.bamUrlEntered || (this.bamFileOpened && this.getBamRefName));
}

VariantModel.prototype.isVcfLoaded = function() {
	return this.vcf && (this.vcfUrlEntered || this.vcfFileOpened);
}

VariantModel.prototype.variantsHaveBeenCalled = function() {
	return this.fbData != null;
}

VariantModel.prototype.hasCalledVariants = function() {
	this.getCalledVariants();
	return this.fbData != null && this.fbData.features != null && this.fbData.features.length > 0;
}

VariantModel.prototype.isInheritanceLoaded = function() {
	return (this.vcfData != null && this.vcfData.loadState != null && this.vcfData.loadState['inheritance']);	
}

VariantModel.prototype.getVcfDataForGene = function(geneObject, selectedTranscript) {
	var me = this;
	return me._getDataForGene("vcfData", geneObject, selectedTranscript);
}

VariantModel.prototype.getFbDataForGene = function(geneObject, selectedTranscript) {
	var me = this;
	return me._getDataForGene("fbData", geneObject, selectedTranscript);
}

VariantModel.prototype._getDataForGene = function(dataKind, geneObject, selectedTranscript) {
	var me = this;
	var data = null;
	// If only alignments have specified, but not variant files, we will need to use the
	// getBamRefName function instead of the getVcfRefName function.
	var theGetRefNameFunction = me.getVcfRefName != null ? me.getVcfRefName : me.getBamRefName;

	if (theGetRefNameFunction == null) {
		theGetRefNameFunction = me._stripRefName;
	}

	if (theGetRefNameFunction) {
		if (me[dataKind] != null && me[dataKind].features && me[dataKind].features.length > 0) {
			if (theGetRefNameFunction(geneObject.chr) == me[dataKind].ref &&
				geneObject.start == me[dataKind].start &&
				geneObject.end == me[dataKind].end &&
				geneObject.strand == me[dataKind].strand) {
				data = me[dataKind];
			}		
		} 

		if (data == null) {
			// Find in cache
			data = this._getCachedData(dataKind, geneObject.gene_name, selectedTranscript);
			if (data != null && data != '') {
				me[dataKind] = data;
			}
		} 		
	} else {
		console.log("No function defined to parse ref name from file");
	}
	return data;
}

VariantModel.prototype.getBamDataForGene = function(geneObject) {
	var me = this;
	var data = null;
	
	if (me.bamData != null) {
		if (me.getBamRefName(geneObject.chr) == me.bamData.ref &&
			geneObject.start == me.bamData.start &&
			geneObject.end == me.bamData.end) {
			data = me.bamData;
		}		
	} 

	if (data == null) {
		// Find in cache
		data = this._getCachedData("bamData", geneObject.gene_name, null);
		if (data != null && data != '') {
			me.bamData = data;
		}
	} 
	return data ? data.coverage : null;
}

VariantModel.prototype.getDangerSummaryForGene = function(geneName) {
	var me = this;
	var	dangerSummary = this._getCachedData("dangerSummary", geneName, null);
	return dangerSummary;
}

VariantModel.prototype.getVariantCount = function(data) {
	var theVcfData = data != null ? data : this.getVcfDataForGene(window.gene, window.selectedTranscript);
	var loadedVariantCount = 0;
	if (theVcfData && theVcfData.features) {
		theVcfData.features.forEach(function(variant) {
			if (variant.fbCalled == 'Y') {

			} else if (variant.zygosity && variant.zygosity.toLowerCase() == "homref") {

			} else {
				loadedVariantCount++;
			}
		});
	}
	return loadedVariantCount;
}

VariantModel.summarizeDanger = function(theVcfData) {
	var dangerCounts = {};
	if (theVcfData == null ) {
		console.log("unable to summarize danger due to null data");
		dangerCounts.error = "unable to summarize danger due to null data";
		return dangerCounts;
	} else if (theVcfData.features.length == 0) {		
		return dangerCounts;		
	}
	var siftClasses = {};
	var polyphenClasses = {};
	var clinvarClasses = {};
	var impactClasses = {};
	var consequenceClasses = {};
	var inheritanceClasses = {};
	var afClazz = null;
	var afField = null;
	var lowestAf = 999;


	theVcfData.features.forEach( function(variant) {

	    for (key in variant.highestImpactSnpeff) {
	    	if (matrixCard.impactMap.hasOwnProperty(key) && matrixCard.impactMap[key].badge) {
	    		impactClasses[key] = impactClasses[key] || {};
	    		impactClasses[key][variant.type] = variant.highestImpactSnpeff[key]; // key = effect, value = transcript id
	    	}
	    }

	    for (key in variant.highestImpactVep) {
	    	if (matrixCard.impactMap.hasOwnProperty(key) && matrixCard.impactMap[key].badge) {
	    		consequenceClasses[key] = consequenceClasses[key] || {};
	    		consequenceClasses[key][variant.type] = variant.highestImpactVep[key]; // key = consequence, value = transcript id
	    	}
	    }

	    for (key in variant.highestSIFT) {
				if (matrixCard.siftMap.hasOwnProperty(key) && matrixCard.siftMap[key].badge) {
					var clazz = matrixCard.siftMap[key].clazz;
					dangerCounts.SIFT = {};
					dangerCounts.SIFT[clazz] = {};
					dangerCounts.SIFT[clazz][key] = variant.highestSIFT[key];
				}
	    }

	    for (key in variant.highestPolyphen) {
	    	if (matrixCard.polyphenMap.hasOwnProperty(key) && matrixCard.polyphenMap[key].badge) {
					var clazz = matrixCard.polyphenMap[key].clazz;
					dangerCounts.POLYPHEN = {};
					dangerCounts.POLYPHEN[clazz] = {};
					dangerCounts.POLYPHEN[clazz][key] = variant.highestPolyphen[key];
	    	}
	    }

	    if (variant.hasOwnProperty('clinVarClinicalSignificance')) {
	    	for (key in variant.clinVarClinicalSignificance) {
		    	if (matrixCard.clinvarMap.hasOwnProperty(key)  && matrixCard.clinvarMap[key].badge) {
						clinvarClasses[key] = matrixCard.clinvarMap[key];
		    	}
	    	}
	    }

	    if (variant.inheritance && variant.inheritance != 'none') {
	    	var clazz = matrixCard.inheritanceMap[variant.inheritance].clazz;
	    	inheritanceClasses[clazz] = variant.inheritance;
	    }



	    var evaluateAf = function(af, afMap) {
			afMap.forEach( function(rangeEntry) {
				if (+variant[af] > rangeEntry.min && +variant[af] <= rangeEntry.max) {
					if (rangeEntry.value < lowestAf) {
						lowestAf = rangeEntry.value;
						afClazz = rangeEntry.clazz;
						afField = af;
					}
				}
			});
		}

		// Find the highest value (the least rare AF) betweem exac and 1000g to evaluate
		// as 'lowest' af for all variants in gene
		var af = null;
		var afMap = null;
		if ($.isNumeric(variant.afExAC) && $.isNumeric(variant.af1000G)) {
			// Ignore exac n/a.  If exac is higher than 1000g, evaluate exac
			if (variant.afExAC > -100 && variant.afExAC >= variant.af1000G) {
				af = 'afExAC';
				afMap = matrixCard.afExacMap;
			} else {
				af = 'af1000G';
				afMap = matrixCard.af1000gMap;
			}
		} else if ($.isNumeric(variant.afExAC)) {
			af = 'afExAC';
			afMap = matrixCard.afExacMap;

		} else if ($.isNumeric(variant.af1000G)) {
			af = 'af1000G';
			afMap = matrixCard.af1000gMap;
		}
		if (af && afMap) {
			evaluateAf(af, afMap);
		}
	});

	var getLowestClinvarClazz = function(clazzes) {
		var lowestOrder = 9999;
		var lowestClazz = null;
		var dangerObject = null;
		for (clazz in clazzes) {
			var object = clazzes[clazz];
			if (object.value < lowestOrder) {
				lowestOrder = object.value;
				lowestClazz = clazz;
			}
		}
		if (lowestClazz) {
			dangerObject = {};
			dangerObject[lowestClazz] =  clazzes[lowestClazz];
		}
		return dangerObject;
	}

	var getLowestImpact = function(impactClasses) {
		var classes = ['HIGH', 'MODERATE', 'MODIFIER', 'LOW'];
		for(var i = 0; i < classes.length; i++) {
			var impactClass = classes[i];
			if (impactClasses[impactClass]) {
				var lowestImpact = {};
				lowestImpact[impactClass] = impactClasses[impactClass];
				return lowestImpact;
			}
		}
		return {};
	}

	dangerCounts.CONSEQUENCE = getLowestImpact(consequenceClasses);
	dangerCounts.IMPACT = filterCard.getAnnotationScheme().toLowerCase() == 'vep' ? dangerCounts.CONSEQUENCE : getLowestImpact(impactClasses);
	dangerCounts.CLINVAR = getLowestClinvarClazz(clinvarClasses);
	dangerCounts.INHERITANCE = inheritanceClasses;

	var afSummaryObject = {};
	if (afClazz != null) {
		afSummaryObject[afClazz] = {field: afField, value: lowestAf};
	}
	dangerCounts.AF = afSummaryObject;

	dangerCounts.featureCount = theVcfData.features.length;

	return dangerCounts;
}

VariantModel.summarizeError =  function(theError) {
	var summaryObject = {};

	summaryObject.CONSEQUENCE = {};
	summaryObject.IMPACT = {};
	summaryObject.CLINVAR = {}
	summaryObject.INHERITANCE = {};
	summaryObject.ERROR = theError;	
	summaryObject.featureCount = 0;

	return summaryObject;
}

VariantModel.prototype.getCalledVariantCount = function() {
	if (this.fbData.features ) {
		return this.fbData.features.filter(function(d) {
			// Filter homozygous reference for proband only
			if (d.zygosity && d.zygosity.toLowerCase() == 'homref') {
				return false;
			}
			return true;
	  }).length;
	}
	return 0;
}

VariantModel.prototype.filterBamDataByRegion = function(coverage, regionStart, regionEnd) {
	return coverage.filter(function(d) {
		return (d[0] >= regionStart && d[0] <= regionEnd);
	});
}

VariantModel.prototype.reduceBamData = function(coverageData, numberOfPoints) {
	var factor = d3.round(coverageData.length / numberOfPoints);
	var xValue = function(d) { return d[0]; };
	var yValue = function(d) { return d[1]; };
	return this.bam.reducePoints(coverageData, factor, xValue, yValue);
}

VariantModel.prototype.getCalledVariants = function(theRegionStart, theRegionEnd) {
	var fbData = this.getFbDataForGene(window.gene, window.selectedTranscript);
	if (fbData != null) {
		this.fbData = fbData;
	}
	if (theRegionStart && theRegionEnd) {
		// Check the local cache first to see
		// if we already have the freebayes variants
		var variants = this.fbData.features.filter(function(d) {
							return (d.start >= theRegionStart && d.start <= theRegionEnd);
					   });	
		return {'features': variants};
	} else {
		return this.fbData;
	}
}



VariantModel.prototype.getName = function() {
	return this.name;
}

VariantModel.prototype.setName = function(theName) {
	if (theName) {
		this.name = theName;
	}
}

VariantModel.prototype.setRelationship = function(theRelationship) {
	this.relationship = theRelationship;	
}

VariantModel.prototype.setAffectedStatus = function(theAffectedStatus) {
	this.affectedStatus = theAffectedStatus;
}


VariantModel.prototype.getRelationship = function() {
	return this.relationship;
}


VariantModel.prototype.setSampleName = function(sampleName) {
	this.sampleName = sampleName;
}


VariantModel.prototype.getSampleName = function() {
	return this.sampleName;
}


VariantModel.prototype.setDefaultSampleName = function(sampleName) {
	this.defaultSampleName = sampleName;
}


VariantModel.prototype.getDefaultSampleName = function() {
	return this.defaultSampleName;
}



VariantModel.prototype.init = function() {
	var me = this;	

	// init vcf.iobio
	this.vcf = vcfiobio();

};

VariantModel.prototype.promiseBamFilesSelected = function(event) {
	var me = this;
	return new Promise(function(resolve, reject) {
		me.bamData = null;
		me.fbData = null;

		me.bam = new Bam();	
		me.bam.openBamFile(event, function(success, message) {
			if (me.lastBamAlertify) {
				me.lastBamAlertify.dismiss();
			}
			if (success) {
				me.bamFileOpened = true;
				me.getBamRefName = me._stripRefName;
				resolve(me.bam.bamFile.name);							

			} else {
				if (me.lastBamAlertify) {
					me.lastBamAlertify.dismiss();
				}
				var msg = "<span style='font-size:18px'>" + message + "</span>";
		        alertify.set('notifier','position', 'top-right');
				me.lastBamAlertify = alertify.error(msg, 15); 		

				reject(message);

			}
		});

	});


}

VariantModel.prototype.onBamUrlEntered = function(bamUrl, callback) {
	var me = this;
	this.bamData = null;
	this.fbData = null;

	if (bamUrl == null || bamUrl.trim() == "") {
		this.bamUrlEntered = false;
		this.bam = null;

	} else {
	    
		this.bamUrlEntered = true;
		this.bam = new Bam(bamUrl);

		this.bam.checkBamUrl(bamUrl, function(success, errorMsg) {
			if (me.lastBamAlertify) {
				me.lastBamAlertify.dismiss();
			}
			if (!success) {
				this.bamUrlEntered = false;
				this.bam = null;
				var msg = "<span style='font-size:18px'>" + errorMsg + "</span><br><span style='font-size:12px'>" + bamUrl + "</span>";
 		        alertify.set('notifier','position', 'top-right');
				me.lastBamAlertify = alertify.error(msg, 15); 
			} 
			if(callback) {

				callback(success);
			}
		});

	}
    
    this.getBamRefName = this._stripRefName;

}

VariantModel.prototype.promiseVcfFilesSelected = function(event) {
	var me = this;

	return new Promise( function(resolve, reject) {
		me.sampleName = null;
		me.vcfData = null;
		
		me.vcf.openVcfFile( event,
			function(success, message) {
				if (me.lastVcfAlertify) {
					me.lastVcfAlertify.dismiss();
				} 
				if (success) {
					

					me.vcfFileOpened = true;
					me.vcfUrlEntered = false;
					me.getVcfRefName = null;

					// Get the sample names from the vcf header
				    me.vcf.getSampleNames( function(sampleNames) {
				    	resolve({'fileName': me.vcf.getVcfFile().name, 'sampleNames': sampleNames});
				    });
				} else {
				
					var msg = "<span style='font-size:18px'>" + message + "</span>";
			    	alertify.set('notifier','position', 'top-right');
			    	me.lastVcfAlertify = alertify.error(msg, 15);

					reject(message);					
				}
			}
		);

	});

}

VariantModel.prototype.clearVcf = function(cardIndex) {

	this.vcfData = null;
	this.vcfUrlEntered = false;
	this.vcfFileOpened = false;
	this.sampleName = null;
	window.removeUrl('sample'+ cardIndex);
	window.removeUrl('vcf' + cardIndex);
	window.removeUrl('name'+ cardIndex);
	this.vcf.clear();
}

VariantModel.prototype.clearBam = function(cardIndex) {

	this.bamData = null;
	this.bamUrlEntered = false;
	this.bamFileOpened = false;
	window.removeUrl('bam' + cardIndex);
	if (this.bam) {
		this.bam.clear();
	}
}

VariantModel.prototype.onVcfUrlEntered = function(vcfUrl, callback) {
	var me = this;
	this.vcfData = null;
	var success = true;
	this.sampleName = null;

	if (vcfUrl == null || vcfUrl == '') {
		this.vcfUrlEntered = false;
		success = false;

	} else {
		me.vcfUrlEntered = true;
	    me.vcfFileOpened = false;
	    me.getVcfRefName = null;	

	    success = this.vcf.openVcfUrl(vcfUrl, function(success, errorMsg) {
	    	if (me.lastVcfAlertify) {
		    	me.lastVcfAlertify.dismiss();
		    }
		    if (success) {
		    	
				me.vcfUrlEntered = true;
			    me.vcfFileOpened = false;
			    me.getVcfRefName = null;	
			    // Get the sample names from the vcf header
			    me.vcf.getSampleNames( function(sampleNames) {
			    	callback(success, sampleNames);
			    });	    	
		    } else {
		    	me.vcfUrlEntered = false;
		    	var msg = "<span style='font-size:18px'>" + errorMsg + "</span><br><span style='font-size:12px'>" + vcfUrl + "</span>";
		    	alertify.set('notifier','position', 'top-right');
		    	me.lastVcfAlertify = alertify.error(msg, 15);
		    	callback(success);
		    }	    	
	    });

	}

}


VariantModel.prototype._promiseVcfRefName = function(ref) {
	var me = this;
	var theRef = ref != null ? ref : window.gene.chr;
	return new Promise( function(resolve, reject) {

		if (me.getVcfRefName != null) {
			// If we can't find the ref name in the lookup map, show a warning.
			if (me.vcfRefNamesMap[me.getVcfRefName(theRef)] == null) {
				reject();
			} else {
				resolve();
			}
		} else {
			me.vcfRefNamesMap = {};
			me.vcf.getReferenceLengths(function(refData) {
				var foundRef = false;
				refData.forEach( function(refObject) {		
					var refName = refObject.name;			
		    		
			 		if (refName == theRef) {
			 			me.getVcfRefName = me._getRefName;
			 			foundRef = true;
			 		} else if (refName == me._stripRefName(theRef)) {
			 			me.getVcfRefName = me._stripRefName;
			 			foundRef = true;
			 		}

		    	});
		    	// Load up a lookup table.  We will use me for validation when
		    	// a new gene is loaded to make sure the ref exists.
		    	if (foundRef) {
		    		refData.forEach( function(refObject) {
		    			var refName = refObject.name;
		    			var theRefName = me.getVcfRefName(refName);
		    			me.vcfRefNamesMap[theRefName] = refName; 
		    		});
		    		resolve();
		    	} else  {

		    	// If we didn't find the matching ref name, show a warning.
					reject();
				}

			});
		}
	});

}



VariantModel.prototype._getRefName = function(refName) {
	return refName;
}

VariantModel.prototype._stripRefName = function(refName) {
	var tokens = refName.split("chr");
	var strippedName = refName;
	if (tokens.length > 1) {
		strippedName = tokens[1];
	} else {
		tokens = refName.split("ch");
		if (tokens.length > 1) {
			strippedName = tokens[1];
		}
	}
	return strippedName;
}


VariantModel.prototype.getMatchingVariant = function(variant) {
	var theVcfData = this.getVcfDataForGene(window.gene, window.selectedTranscript);
	var matchingVariant = null;
	if (theVcfData && theVcfData.features) {
		theVcfData.features.forEach(function(v) {
			if (v.start == variant.start 
          && v.end == variant.end 
          && v.ref == variant.ref 
          && v.alt == variant.alt 
          && v.type.toLowerCase() == variant.type.toLowerCase()) {
	      matchingVariant = v;
	    }
		});
	}
	return matchingVariant;
}

/*
* A gene has been selected. Clear out the model's state
* in preparation for getting data.
*/
VariantModel.prototype.wipeGeneData = function () {
	var me = this;
	this.vcfData = null;
	this.fbData = null;
	this.bamData = null;
}



VariantModel.prototype.getBamDepth = function(gene, selectedTranscript, callbackDataLoaded) {	
	var me = this;


	if (!this.isBamLoaded()) {		
		if (callbackDataLoaded) {
			callbackDataLoaded();
		}
		return;
	}


	// A gene has been selected.  Read the bam file to obtain
	// the read converage.
	var refName = this.getBamRefName(gene.chr);
	var theVcfData = this.getVcfDataForGene(gene, selectedTranscript);	


	var regions = [];
	if (theVcfData != null) {
		me.flagDupStartPositions(theVcfData.features);
		if (theVcfData) {
			theVcfData.features.forEach( function(variant) {
				if (!variant.dup) {
					regions.push({name: refName, start: variant.start - 1, end: variant.start });
				}
			});
		}

	}

	// Get the coverage data for the gene region
	// First the gene vcf data has been cached, just return
	// it.  (No need to retrieve the variants from the iobio service.)
	var data = me._getCachedData("bamData", gene.gene_name);
	if (data != null && data != '') {
		me.bamData = data;

		if (regions.length > 0) {
			me._refreshVariantsWithCoverage(theVcfData, data.coverage, function() {				
				if (callbackDataLoaded) {
			   	    callbackDataLoaded(data.coverage);
		   	    }
			});				
		} else {
			if (callbackDataLoaded) {
		   	    callbackDataLoaded(data.coverage);
	   	    }
		}

	} else {
		me.bam.getCoverageForRegion(refName, gene.start, gene.end, regions, 5000, useServerCache,
	 	  function(coverageForRegion, coverageForPoints) {
	 	  	if (coverageForRegion != null) {
				me.bamData = {gene: gene.gene_name,
					          ref: refName, 
					          start: gene.start, 
					          end: gene.end, 
					          coverage: coverageForRegion};

				// Use browser cache for storage coverage data if app is not relying on
				// server-side cache
				if (!useServerCache) {
					me._cacheData(me.bamData, "bamData", gene.gene_name);	 	  		
				}
	 	  	}

			if (regions.length > 0) {
				me._refreshVariantsWithCoverage(theVcfData, coverageForPoints, function() {				
					if (callbackDataLoaded) {
				   	    callbackDataLoaded(coverageForRegion);
			   	    }
				});				
			} else {
				if (callbackDataLoaded) {
			   	    callbackDataLoaded(coverageForRegion, "bamData");
		   	    }
			}
		});
	}



}



VariantModel.prototype.promiseAnnotated = function(theVcfData) {
	var me = this;
	return new Promise( function(resolve, reject) {
		if (theVcfData != null &&
			theVcfData.features != null &&
			theVcfData.loadState != null &&
		   //(dataCard.mode == 'single' || theVcfData.loadState['inheritance'] == true) &&
			theVcfData.loadState['clinvar'] == true ) {

			resolve();

		} else {
			reject();
		}

	});

}

VariantModel.prototype.promiseAnnotatedAndCoverage = function(theVcfData) {
	var me = this;
	return new Promise( function(resolve, reject) {
		if (theVcfData != null &&
			theVcfData.features != null &&
			theVcfData.loadState != null &&
		   (dataCard.mode == 'single' || theVcfData.loadState['inheritance'] == true) &&
			theVcfData.loadState['clinvar'] == true  &&
			(!me.isBamLoaded() || theVcfData.loadState['coverage'] == true)) {

			resolve();

		} else {
			reject();
		}

	});

}

VariantModel.prototype.promiseGetVariantExtraAnnotations = function(theGene, theTranscript, variant) {
	var me = this;

	return new Promise( function(resolve, reject) {


		// Create a gene object with start and end reduced to the variants coordinates.
		var fakeGeneObject = $().extend({}, theGene);
		fakeGeneObject.start = variant.start;
		fakeGeneObject.end = variant.end;


		if (variant.fbCalled == 'Y') {
			// We already have the hgvs and rsid if this is a called variant
			resolve(variant);
		} else if ( variant.extraAnnot ) {
			// We have already retrieved the extra annot for this variant,
			resolve(variant);
		} else {	
			me._promiseVcfRefName(theGene.chr).then( function() {				
				me.vcf.promiseGetVariants(
				   me.getVcfRefName(theGene.chr), 
				   fakeGeneObject,
			       theTranscript,
			       me.sampleName,
			       filterCard.annotationScheme.toLowerCase(),
			       window.geneSource == 'refseq' ? true : false,
			       true,
			       true,
			       useServerCache
			    ).then( function(data) {
			    	var theVcfData = data[1];	

			    	if (theVcfData != null && theVcfData.features != null && theVcfData.features.length > 0) {
			    		// Now update the hgvs notation on the variant
			    		var v = theVcfData.features[0];
			    		var theVariants = me.vcfData.features.filter(function(d) {
			    			if (d.start == v.start &&
			    				d.alt == v.alt &&
			    				d.ref == v.ref) {
			    				return true;
			    			} else {
			    				return false;
			    			}
			    		});
			    		if (theVariants && theVariants.length > 0) {
				    		var theVariant = theVariants[0];
		
							// set the hgvs and rsid on the existing variant
				    		theVariant.extraAnnot = true;
				    		theVariant.vepHGVSc = v.vepHGVSc;
				    		theVariant.vepHGVSp = v.vepHGVSp;
				    		theVariant.vepVariationIds = v.vepVariationIds;

					    	// re-cache the data
					    	me._cacheData(me.vcfData, "vcfData", theGene.gene_name, theTranscript);	

					    	// return the annotated variant
							resolve(theVariant);
			    		} else {
			    			console.log("Cannot find corresponding variant to update HGVS notation");
			    			reject("Cannot find corresponding variant to update HGVS notation");
			    		}			    		
			    	} else {
			    		console.log("Cannot get variant to update HGVS notation");
			    		reject("Cannot get variant to update HGVS notation");
			    	}

				});		
			});				
		}
	});

}

VariantModel.prototype.promiseGetVariantsOnly = function(theGene, theTranscript) {
	var me = this;

	return new Promise( function(resolve, reject) {

		// First the gene vcf data has been cached, just return
		// it.  (No need to retrieve the variants from the iobio service.)
		var vcfData = me._getCachedData("vcfData", theGene.gene_name, theTranscript);
		if (vcfData != null && vcfData != '') {
			me.vcfData = vcfData;
	    	
			resolve(me.vcfData);
		} else {	
			me._promiseVcfRefName(theGene.chr).then( function() {				
				me.vcf.promiseGetVariants(
				   me.getVcfRefName(theGene.chr), 
				   theGene,
			       theTranscript,
			       me.sampleName,
			       filterCard.annotationScheme.toLowerCase(),
			       window.geneSource == 'refseq' ? true : false
			    ).then( function(data) {
			    	var annotatedRecs = data[0];
			    	var data = data[1];	

			    	if (data != null && data.features != null) {
				    	data.name = me.name;
				    	data.relationship = me.relationship;    	

				    	// Associate the correct gene with the data
				    	var theGeneObject = null;
				    	for( var key in window.geneObjects) {
				    		var go = geneObjects[key];
				    		if (me.getVcfRefName(go.chr) == data.ref &&
				    			go.start == data.start &&
				    			go.end == data.end &&
				    			go.strand == data.strand) {
				    			theGeneObject = go;
				    			data.gene = theGeneObject;
				    		}
				    	}
				    	if (theGeneObject) {

					    	// Cache the data if variants were retreived.  If no variants, don't
					    	// cache so we can retry to make sure there wasn't a problem accessing
					    	// variants.
					    	if (data.features.length > 0) {
						    	me._cacheData(data, "vcfData", data.gene.gene_name, data.transcript);	
					    	}
					    	me.vcfData = data;		    	
							resolve(me.vcfData);

				    	} else {
				    		console("ERROR - cannot locate gene object to match with vcf data " + data.ref + " " + data.start + "-" + data.end);
				    		reject();
				    	}
			    	} else {
			    		reject("No variants");
			    	}


			    	resolve(me.vcfData);
				});		
			});				
		}
	});

}

VariantModel.prototype.promiseGetVariants = function(theGene, theTranscript, regionStart, regionEnd, onVcfData) {
	var me = this;

	return new Promise( function(resolve, reject) {

		// First the gene vcf data has been cached, just return
		// it.  (No need to retrieve the variants from the iobio service.)
		var vcfData = me._getCachedData("vcfData", theGene.gene_name, theTranscript);
		if (vcfData != null && vcfData != '') {
			me.vcfData = vcfData;
			me._populateEffectFilters(me.vcfData.features);
			me._populateRecFilters(me.vcfData.features);

			// Flag any bookmarked variants
			if (me.getRelationship() == 'proband') {
			    bookmarkCard.determineVariantBookmarks(vcfData, theGene);
			}


		    // Invoke callback now that we have annotated variants
	    	if (onVcfData) {
	    		onVcfData();
	    	}
	    	
			resolve(me.vcfData);
		} else {
			// We don't have the variants for the gene in cache, 
			// so call the iobio services to retreive the variants for the gene region 
			// and annotate them.
			me._promiseVcfRefName(theGene.chr).then( function() {
				me._promiseGetAndAnnotateVariants(
					me.getVcfRefName(theGene.chr),
					theGene,
			        theTranscript,
			        onVcfData)
				.then( function(data) {
			    	
			    	// Associate the correct gene with the data
			    	var theGeneObject = null;
			    	for( var key in window.geneObjects) {
			    		var geneObject = geneObjects[key];
			    		if (me.getVcfRefName(geneObject.chr) == data.ref &&
			    			geneObject.start == data.start &&
			    			geneObject.end == data.end &&
			    			geneObject.strand == data.strand) {
			    			theGeneObject = geneObject;
			    			data.gene = theGeneObject;
			    		}
			    	}
			    	if (theGeneObject) {

			    		// Flag any bookmarked variants
			    		if (me.getRelationship() == 'proband') {
					    	bookmarkCard.determineVariantBookmarks(data, theGeneObject);
					    }

				    	// Cache the data (if there are variants)
				    	if (data && data.features) {
					    	if (!me._cacheData(data, "vcfData", data.gene.gene_name, data.transcript)) {
					    		reject("Unable to cache annotated variants for gene " + data.gene.gene_name);
					    	};	
				    	}
				    	me.vcfData = data;		    	
						resolve(me.vcfData);

			    	} else {
			    		var error = "ERROR - cannot locate gene object to match with vcf data " + data.ref + " " + data.start + "-" + data.end;
			    		console.log(error);
			    		reject(error);
			    	}

			    }, function(error) {
			    	reject(error);
			    });
			}, function(error) {
				reject("missing reference")
			});

		}



	});

}

VariantModel.prototype.isCached = function(geneName, transcript) {
	var key = this._getCacheKey("vcfData", geneName.toUpperCase(), transcript);
	var data = localStorage.getItem(key);
	return data != null;
}

VariantModel.prototype.isCachedAndInheritanceDetermined = function(geneName, transcript) {
	var theVcfData = this._getCachedData("vcfData", geneName, transcript);
	return theVcfData && theVcfData.loadState != null && theVcfData.loadState['inheritance'];
}


VariantModel.prototype.promiseCacheVariants = function(ref, geneObject, transcript) {
	var me = this;

	return new Promise( function(resolve, reject) {

		// Is the data already cached?  If so, we are done
		var vcfData = me._getCachedData("vcfData", geneObject.gene_name, transcript);
		if (vcfData != null && vcfData != '') {	
			// Do we already have the variants cached?  If so, just return that data.		
			resolve(vcfData);
		}  else if (autoCall && !me.isVcfReadyToLoad()) {	
			// We should never get to this condition because if no vcf was supplied, then
			// cacheHelper would have performed joint calling on bams
			reject();
		} else {
			// We don't have the variants for the gene in cache, 
			// so call the iobio services to retreive the variants for the gene region 
			// and annotate them.
			me._promiseVcfRefName(ref).then( function() {
				me._promiseGetAndAnnotateVariants(me.getVcfRefName(ref), geneObject, transcript)
				.then( function(data) {
					// Associate the correct gene with the data
			    	var theGeneObject = null;
			    	for( var key in window.geneObjects) {
			    		var go = geneObjects[key];
			    		if (me.getVcfRefName(go.chr) == data.ref &&
			    			go.start == data.start &&
			    			go.end == data.end &&
			    			go.strand == data.strand) {
			    			theGeneObject = go;
			    			data.gene = theGeneObject;
			    		}
			    	}
			    	if (theGeneObject) {
			    		// Flag any bookmarked variants
					    bookmarkCard.determineVariantBookmarks(data, theGeneObject);

				    	// Cache the data
					   	me._cacheData(data, "vcfData", data.gene.gene_name, data.transcript);	
						resolve(data);				    	
				    } else {
				    	reject({isValid: false, message: "Cannot find gene object to match data for " + data.ref + " " + data.start + "-" + data.end});
				    }
			    	

			    }, function(error) {
			    });
			}, function(error) {
				var isValid = false;
				// for caching, treat missing chrX as a normal case.
				if (ref != null && ref.toUpperCase().indexOf("X")) {
					isValid = true;
				}

				reject({isValid: isValid, message: "missing reference"});
			});
		}

	});

}

VariantModel.prototype._promiseCacheCalledVariants = function(ref, geneObject, transcript) {
	var me = this;
	return new Promise( function(resolve, reject) {
		var fbData = me._getCachedData("fbData", geneObject.gene_name, transcript);
		if (fbData) {
			resolve(fbData);
		} else {

			// Call Freebayes variants
			me.bam.getFreebayesVariants(ref, 
				geneObject.start, 
				geneObject.end, 
				geneObject.strand, 
				window.geneSource == 'refseq' ? true : false,
				function(data) {

				if (data == null || data.length == 0) {
					reject("A problem occurred while calling variants.");
				}

				// Parse string into records
				var fbRecs = [];
				var recs = data.split("\n");
				recs.forEach( function(rec) {
					fbRecs.push(rec);
				});
				

				// Annotate the fb variants
				me.vcf.promiseAnnotateVcfRecords(fbRecs, me.getBamRefName(ref), geneObject, 
					                             transcript, null, 
					                             filterCard.annotationScheme.toLowerCase(),
					                             window.geneSource == 'refseq' ? true : false)
			    .then( function(data) {

			    	var theData = data[1];

			    	// Flag the called variants
				   	theData.features.forEach( function(feature) {
				   		feature.fbCalled = 'Y';
				   	});
		    		return me.vcf.promiseGetClinvarRecords(
					    		theData, 
					    		me._stripRefName(ref), geneObject, 
					    		isClinvarOffline ? me._refreshVariantsWithClinvarVariants.bind(me, theData) : me._refreshVariantsWithClinvar.bind(me, theData));




			    }, function(error) {
			    	var message = "Problem when annotating called variants in Analyze All. " + error;
					console.log(message);
					reject({isValid: false, message: message});
			    })
			    .then (function(data) {


					// We are done getting the clinvar data for called variants.
			    	// Now merge called data with variant set and display.
					// Prepare vcf and fb data for comparisons
					//me._prepareVcfAndFbData(data);

		        	// Filter the freebayes variants to only keep the ones
		        	// not present in the vcf variant set.
					me._determineVariantAfLevels(data, transcript);

		        	// Pileup the variants
		        	var pileupObject = me._pileupVariants(data.features, geneObject.start, geneObject.end);
					data.maxLevel = pileupObject.maxLevel + 1;
					data.featureWidth = pileupObject.featureWidth;		

					// Cache the freebayes variants.
					me._cacheData(data, "fbData", geneObject.gene_name, transcript);
					me._cacheData(data, "vcfData", geneObject.gene_name, transcript);
					resolve(data);
			
			    	
			    }, function(error) {
			    	var message = "Problem when calling variants in Analyze All. " + error;
					console.log(message);
					reject({isValid: false, message: message});
			    });
			
			});	

		}		
	});
	
}

VariantModel.prototype._getCacheKey = function(dataKind, geneName, transcript) {
	return cacheHelper.getCacheKey(
		{relationship: this.getRelationship(),
		 sample: (this.sampleName != null ? this.sampleName : "null"),
		 gene: (geneName != null ? geneName : gene.gene_name),
		 transcript: (transcript != null ? transcript.transcript_id : "null"),
		 annotationScheme: (filterCard.getAnnotationScheme().toLowerCase()),
		 dataKind: dataKind
		}
	);	

}
VariantModel.prototype.cacheDangerSummary = function(dangerSummary, geneName) {
	this._cacheData(dangerSummary, "dangerSummary", geneName);
}

VariantModel.prototype.clearCacheItem = function(dataKind, geneName, transcript) {
	var me = this;
	cacheHelper.clearCacheItem(me._getCacheKey(dataKind, geneName, transcript));
}

VariantModel.prototype._cacheData = function(data, dataKind, geneName, transcript) {
	var me = this;
	geneName = geneName.toUpperCase();
	if (localStorage) {
		var success = true;
		var dataString = JSON.stringify(data);

    	stringCompress = new StringCompress();

    	var dataStringCompressed = null;
    	try {
			dataStringCompressed = LZString.compressToUTF16(dataString);
    	} catch (e) {    		
	   		success = false;
	   		console.log("an error occurred when compressing vcf data for key " + e + " " + me._getCacheKey(dataKind, geneName, transcript));
    		alertify.set('notifier','position', 'top-right');
	   		alertify.error("Error occurred when compressing analyzed data before caching.", 15);
    	}

    	if (success) {
	    	try {
	    		if (me.debugMe) {
		    		console.log("caching "  + dataKind + ' ' + me.relationship + ' ' + geneName + " = " + dataString.length + '->' + dataStringCompressed.length);
	    		}
		      	localStorage.setItem(me._getCacheKey(dataKind, geneName, transcript), dataStringCompressed);
	    		
	    	} catch(error) {
	    		success = false;
		      	CacheHelper.showError(me._getCacheKey(dataKind, geneName, transcript), error);
		      	genesCard.hideGeneBadgeLoading(geneName);
	    	}    		
    	}

    	if (!success) {
	   		genesCard.hideGeneBadgeLoading(geneName);
	   		genesCard.clearGeneGlyphs(geneName);
	   		genesCard.setGeneBadgeError(geneName);    		
    	}

    	
      	return success;
    } else {
   		genesCard.hideGeneBadgeLoading(geneName);
   		genesCard.clearGeneGlyphs(geneName);
   		genesCard.setGeneBadgeError(geneName);    		

    	return false;
    }
}

VariantModel.prototype._getCachedData = function(dataKind, geneName, transcript) {
	var me = this;

	geneName = geneName.toUpperCase();

	var data = null;
	if (localStorage) {
      	var dataCompressed = localStorage.getItem(this._getCacheKey(dataKind, geneName, transcript));
      	if (dataCompressed != null) {
			var dataString = null;
			var start = Date.now();
			try {
				//dataString = stringCdompress.inflate(dataCompressed);
				 dataString = LZString.decompressFromUTF16(dataCompressed);
	 			 data =  JSON.parse(dataString); 
	 			 if (me.debugMe) {     		
				 	console.log("time to decompress cache " + dataKind + " = " + (Date.now() - start));
				 }
			} catch(e) {
				console.log("an error occurred when uncompressing vcf data for key " + me._getCacheKey(dataKind, geneName, transcript));
			}
      	} 
	} 
	return data;
}

VariantModel.prototype.pruneIntronVariants = function(data) {
	if (data.features.length > 500) {
		filterCard.setExonicOnlyFilter();

	}	
}


VariantModel.prototype._promiseGetAndAnnotateVariants = function(ref, geneObject, transcript, onVcfData) {
	var me = this;

	return new Promise( function(resolve, reject) {


		// If this is the refseq gene model, set the annotation
		// scheme on the filter card to 'VEP' since snpEff will
		// be bypassed at this time.
		if (window.geneSource == 'refseq') {
			filterCard.setAnnotationScheme("VEP");
		}


		var sampleNames = me.sampleName;
		if (sampleNames != null && sampleNames != "") {
			if (me.relationship != 'proband') {
				sampleNames += "," + getProbandVariantCard().getSampleName();
			}			
		}



		me.vcf.promiseGetVariants(
		   me.getVcfRefName(ref), 
		   geneObject,
		   transcript,
	       sampleNames,
	       filterCard.annotationScheme.toLowerCase(),
	       window.geneSource == 'refseq' ? true : false,
	       window.isLevelBasic ? true : me.GET_HGVS,
	       me.GET_RSID
	    ).then( function(data) {

	    	var annotatedRecs = data[0];
	    	var theVcfData = data[1];

		    if (theVcfData != null && theVcfData.features != null && theVcfData.features.length > 0) {


		    	// We have the AFs from 1000G and ExAC.  Now set the level so that variants
			    // can be filtered by range.
			    me._determineVariantAfLevels(theVcfData, transcript );


			    // Show the snpEff effects / vep consequences in the filter card
			    me._populateEffectFilters(theVcfData.features);

			    // Determine the unique values in the VCF filter field 
			    me._populateRecFilters(theVcfData.features);

			    // Invoke callback now that we have annotated variants
			    me.vcfData = theVcfData;
		    	if (onVcfData) {
		    		onVcfData(theVcfData);
		    	}
		
		    	// Get the clinvar records (for proband, mom, data)
		    	// 
		    	if (me.getRelationship() != 'sibling') {
			    	return me.vcf.promiseGetClinvarRecords(

			    		theVcfData, 
			    		me._stripRefName(ref), geneObject, 
			    		isClinvarOffline ? me._refreshVariantsWithClinvarVariants.bind(me, theVcfData) : me._refreshVariantsWithClinvar.bind(me, theVcfData));

		    	} else {
		    		
		    		resolve(theVcfData);
		    	}	


	    	} else if (theVcfData.features.length == 0) {

			    // Invoke callback now that we have annotated variants
			    me.vcfData = theVcfData;
		    	if (onVcfData) {
		    		onVcfData(theVcfData);
		    	}
		    	resolve(theVcfData);


	    	} else {
	    		reject("_promiseGetAndAnnotateVariants() No variants");
	    	}

		
	    }, 
	    function(error) {
	    	// If error when annotating clinvar records	    	
	    	console.log("an error occurred when annotating vcf records " + error);
	    	reject(error);

	    }).then( function(data) {
	    	// We are done getting clinvar records.
	    	me.setLoadState(data, 'clinvar');
	    	resolve(data);
	    }, 
	    function(error) {
	    	console.log("an error occurred after vcf data returned (post processing) " + error);
	    	reject(error);
	    });

	
		



	});


}

VariantModel.prototype.determineMaxAlleleCount = function(vcfData) {
	var theVcfData = null;
	if (vcfData) {
		theVcfData = vcfData;
	} else {
		theVcfData = this.getVcfDataForGene(window.gene, window.selectedTranscript);
	}
	if (theVcfData == null || theVcfData.features == null) {
		return;
	}

	var maxAlleleCount = 0;
	var setMaxAlleleCount = function(depth) {
		if (depth) {
			if ((+depth) > maxAlleleCount) {
				maxAlleleCount = +depth;
			}
		}
	};

	if (theVcfData.features.length > 0) {
		theVcfData.features.forEach(function(variant) {
			setMaxAlleleCount(variant.genotypeDepth);
			setMaxAlleleCount(variant.genotypeDepthMother);
			setMaxAlleleCount(variant.genotypeDepthFather);
		});
		theVcfData.maxAlleleCount = maxAlleleCount;
	} else if (dataCard.mode == 'trio') {
		// If the gene doesn't have any variants for the proband, determine the
		// max allele count by iterating through the mom and data variant
		// cards to examine these features.
		window.variantCards.forEach(function(variantCard) {
			if (variantCard.getRelationship() == 'mother' || variantCard.getRelationship() == 'father') {
				var data = variantCard.model.getVcfDataForGene(window.gene, window.selectedTranscript);
				data.features.forEach(function(theVariant) {
					setMaxAlleleCount(theVariant.genotypeDepth);
				});
			}
		});
		theVcfData.maxAlleleCount = maxAlleleCount;
	}
	return theVcfData;

}

VariantModel.prototype.populateEffectFilters = function() {
	var theVcfData = this.getVcfDataForGene(window.gene, window.selectedTranscript);
	if (theVcfData && theVcfData.features) {
		this._populateEffectFilters(theVcfData.features);
	}
}

VariantModel.prototype._populateEffectFilters  = function(variants) {
	variants.forEach(function(variant) {
		for (effect in variant.effect) {
			filterCard.snpEffEffects[effect] = effect;
		}
		for (vepConsequence in variant.vepConsequence) {
			filterCard.vepConsequences[vepConsequence] = vepConsequence;
		}
	});
}

VariantModel.prototype._populateRecFilters  = function(variants) {
	variants.forEach( function(variant) {
		filterCard.recFilters[variant.recfilter] = variant.recfilter;
	});	
}



VariantModel.prototype._determineVariantAfLevels = function(theVcfData, transcript) {
	var me = this;
    // Post processing:
    // We have the af1000g and afexac, so now determine the level for filtering
    // by range.
   theVcfData.features.forEach(function(variant) {
  	// For ExAC levels, differentiate between af not found and in
  	// coding region (level = private) and af not found and intronic (non-coding)
  	// region (level = unknown)
  	if (variant.afExAC == 0) {
		variant.afExAC = -100;
    	getCodingRegions(transcript).forEach(function(codingRegion) {
    		if (variant.start >= codingRegion.start && variant.end <= codingRegion.end) {
    			variant.afExAC = 0;
    		}
    	});
  	}

    variant.afexaclevels = {};
		matrixCard.afExacMap.forEach( function(rangeEntry) {
			if (+variant.afExAC > rangeEntry.min && +variant.afExAC <= rangeEntry.max) {
				variant.afexaclevel = rangeEntry.clazz;
				variant.afexaclevels[rangeEntry.clazz] = rangeEntry.clazz;
			}
		});

		variant.af1000glevels = {};
		matrixCard.af1000gMap.forEach( function(rangeEntry) {
			if (+variant.af1000G > rangeEntry.min && +variant.af1000G <= rangeEntry.max) {
				variant.af1000glevel = rangeEntry.clazz;
				variant.af1000glevels[rangeEntry.clazz] = rangeEntry.clazz;
			}
		});
	});
}



VariantModel.prototype._pileupVariants = function(features, start, end) {
	var me = this;
	var width = 1000;
	var theFeatures = features;
	theFeatures.forEach(function(v) {
		v.level = 0;
	});

	var featureWidth = isLevelEdu || isLevelBasic ? EDU_TOUR_VARIANT_SIZE : 4;
	var posToPixelFactor = Math.round((end - start) / width);
	var widthFactor = featureWidth + (isLevelEdu || isLevelBasic ? EDU_TOUR_VARIANT_SIZE * 2 : 4);
	var maxLevel = this.vcf.pileupVcfRecords(theFeatures, start, posToPixelFactor, widthFactor);
	if ( maxLevel > 30) {
		for(var i = 1; i < posToPixelFactor; i++) {
			// TODO:  Devise a more sensible approach to setting the min width.  We want the 
			// widest width possible without increasing the levels beyond 30.
			if (i > 4) {
				featureWidth = 1;
			} else if (i > 3) {
				featureWidth = 2;
			} else if (i > 2) {
				featureWidth = 3;
			} else {
				featureWidth = 4;
			}

			features.forEach(function(v) {
		  		v.level = 0;
			});
			var factor = posToPixelFactor / (i * 2);
			maxLevel = me.vcf.pileupVcfRecords(theFeatures, start, factor, featureWidth + 1);
			if (maxLevel <= 50) {
				i = posToPixelFactor;
				break;
			}
		}
	}
	return { 'maxLevel': maxLevel, 'featureWidth': featureWidth };
}


VariantModel.prototype.flagDupStartPositions = function(variants) {
	// Flag variants with same start position as this will throw off comparisons
 	for (var i =0; i < variants.length - 1; i++) {
        var variant = variants[i];
        var nextVariant = variants[i+1];
        if (i == 0) {
          variant.dup = false;
        }
        nextVariant.dup = false;

        if (variant.start == nextVariant.start) {
        	nextVariant.dup = true;
	    }
	}
	
}

VariantModel.prototype._refreshVariantsWithCoverage = function(theVcfData, coverage, callback) {
	var me = this;
	var vcfIter = 0;
	var covIter = 0;
	if (theVcfData == null) {
		callback();
	}
	var recs = theVcfData.features;
	
    me.flagDupStartPositions(recs);
	
	for( var vcfIter = 0, covIter = 0; vcfIter < recs.length; null) {
		// Bypass duplicates
		if (recs[vcfIter].dup) {
			recs[vcfIter].bamDepth = recs[vcfIter-1].bamDepth;
			vcfIter++;
		}
		if (vcfIter >= recs.length) {

		} else {
	      	if (covIter >= coverage.length) {
	      		recs[vcfIter].bamDepth = "";
	      		vcfIter++;      			
		  	} else {
				var coverageRow = coverage[covIter];
				var coverageStart = coverageRow[0];
				var coverageDepth = coverageRow[1];

				// compare curr variant and curr coverage record
				if (recs[vcfIter].start == coverageStart) {			
					recs[vcfIter].bamDepth = +coverageDepth;
					vcfIter++;
					covIter++;
				} else if (recs[vcfIter].start < coverageStart) {	
					recs[vcfIter].bamDepth = "";
					vcfIter++;
				} else {
					//console.log("no variant corresponds to coverage at " + coverageStart);
					covIter++;
				}

	      	}			
		}

	}
	theVcfData.loadState['coverage'] = true;
	callback();


}

VariantModel.prototype._refreshVariantsWithClinvar = function(theVcfData, clinVars) {	
	var me = this;
	var clinVarIds = clinVars.uids;
	if (theVcfData == null) {
		return;
	}

	var loadClinvarProperties = function(recs) {
		for( var vcfIter = 0, clinvarIter = 0; vcfIter < recs.length && clinvarIter < clinVarIds.length; null) {
			var uid = clinVarIds[clinvarIter];
			var clinVarStart = clinVars[uid].variation_set[0].variation_loc.filter(function(v){return v["assembly_name"] == genomeBuildHelper.getCurrentBuildName()})[0].start;
			var clinVarAlt   = clinVars[uid].variation_set[0].variation_loc.filter(function(v){return v["assembly_name"] == genomeBuildHelper.getCurrentBuildName()})[0].alt;
			var clinVarRef   = clinVars[uid].variation_set[0].variation_loc.filter(function(v){return v["assembly_name"] == genomeBuildHelper.getCurrentBuildName()})[0].ref;

			
			// compare curr variant and curr clinVar record
			if (recs[vcfIter].clinvarStart == clinVarStart) {			
				// add clinVar info to variant if it matches
				if (recs[vcfIter].clinvarAlt == clinVarAlt &&
					recs[vcfIter].clinvarRef == clinVarRef) {
					me._addClinVarInfoToVariant(recs[vcfIter], clinVars[uid]);
					vcfIter++;
					clinvarIter++;
				} else {
					// Only advance the clinvar iterator if clinvar entry didn't match the variant
					// because there can be multiple clinvar entries for the same position.
					clinvarIter++;
				}
			} else if (recs[vcfIter].start < clinVarStart) {						
				vcfIter++;
			} else {
				clinvarIter++;
			}
		}
	}

	// Load the clinvar info for the variants loaded from the vcf	
	var sortedFeatures = theVcfData.features.sort(orderVariantsByPosition);
	loadClinvarProperties(sortedFeatures);

}


VariantModel.prototype._refreshVariantsWithClinvarVariants= function(theVcfData, clinvarVariants) {	
	var me = this;
	if (theVcfData == null) {
		return;
	}

	var parseClinvarInfo = function(variant, clinvarVariant) {		
		clinvarCodes = {
			'0':   'not_provided',
			'1':   'not_provided',
			'2':   'benign',
			'3':   'likely_benign',
			'4':   'likely_pathogenic',
			'5':   'pathogenic',
			'6':   'drug_response',
			'7':   'other',
			'255': 'other'
		};
		clinvarVariant.info.split(";").forEach( function (annotToken) {
			if (annotToken.indexOf("CLNSIG=") == 0) {
            	var clinvarCode = annotToken.substring(7, annotToken.length);  
            	variant.clinVarClinicalSignificance = {};
            	clinvarCode.split("|").forEach(function(code) {
	            	clinvarToken = clinvarCodes[code];
	            	var mapEntry = matrixCard.clinvarMap[clinvarToken];
					if (mapEntry != null) {
						if (variant.clinvarRank == null || 
							mapEntry.value < variant.clinvarRank) {
							variant.clinvarRank = mapEntry.value;
							variant.clinvar = mapEntry.clazz;
						}
						variant.clinVarClinicalSignificance[clinvarToken] = "Y";
					}	

            	})
            } else if (annotToken.indexOf("CLNDBN=") == 0) {
            	phenotypes = annotToken.substring(7, annotToken.length);  
            	variant.clinVarPhenotype = {};
            	phenotypes.split("|").forEach(function(phenotype) {
            		
            		variant.clinVarPhenotype[phenotype] = "Y";
            	})
            }       
		})
	}


	var loadClinvarProperties = function(recs) {
		for( var vcfIter = 0, clinvarIter = 0; vcfIter < recs.length && clinvarIter < clinvarVariants.length; null) {

			var clinvarVariant = clinvarVariants[clinvarIter];
			
			// compare curr variant and curr clinVar record
			if (recs[vcfIter].start == +clinvarVariant.pos) {			
				// add clinVar info to variant if it matches
				if (recs[vcfIter].alt == clinvarVariant.alt &&
					recs[vcfIter].ref == clinvarVariant.ref) {
					parseClinvarInfo(recs[vcfIter], clinvarVariant);
					vcfIter++;
					clinvarIter++;
				} else {
					// Only advance the clinvar iterator if clinvar entry didn't match the variant
					// because there can be multiple clinvar entries for the same position.
					clinvarIter++;
				}
				
			} else if (recs[vcfIter].start < +clinvarVariant.pos) {						
				vcfIter++;
			} else {
				clinvarIter++;
			}
		}
	}

	// Load the clinvar info for the variants loaded from the vcf	
	var sortedFeatures = theVcfData.features.sort(orderVariantsByPosition);
	loadClinvarProperties(sortedFeatures);

}


VariantModel.prototype._addClinVarInfoToVariant = function(variant, clinvar) {		
	variant.clinVarUid = clinvar.uid;

	if (!variant.clinVarAccession) {
		variant.clinVarAccession = clinvar.accession;
	}

	var clinSigObject = variant.clinVarClinicalSignificance;
	if (clinSigObject == null) {
		variant.clinVarClinicalSignificance = {"none": "Y"};
	}

	var clinSigString = clinvar.clinical_significance.description;
	var clinSigTokens = clinSigString.split(", ");
	clinSigTokens.forEach( function(clinSigToken) {
		if (clinSigToken != "") {		
			// Replace space with underlink
			clinSigToken = clinSigToken.split(" ").join("_").toLowerCase();
			variant.clinVarClinicalSignificance[clinSigToken] = 'Y';

			// Get the clinvar "classification" for the highest ranked clinvar 
			// designation. (e.g. "pathologic" trumps "benign");
			var mapEntry = matrixCard.clinvarMap[clinSigToken];
			if (mapEntry != null) {
				if (variant.clinvarRank == null || 
					mapEntry.value < variant.clinvarRank) {
					variant.clinvarRank = mapEntry.value;
					variant.clinvar = mapEntry.clazz;
				}
			}		
		}

	});



	var phenotype = variant.clinVarPhenotype;
	if (phenotype == null) {
		variant.clinVarPhenotype = {};
	}

	var phTokens = clinvar.trait_set.map(function(d) { return d.trait_name; }).join ('; ')
	if (phTokens != "") {
		var tokens = phTokens.split("; ");
		tokens.forEach(function(phToken) {
			// Replace space with underlink
			phToken = phToken.split(" ").join("_");
			variant.clinVarPhenotype[phToken.toLowerCase()] = 'Y';
		});
	}
}

VariantModel.prototype.clearCalledVariants = function() {
	this._cacheData(null, "fbData", window.gene.gene_name, window.selectedTranscript);
}


VariantModel.prototype.promiseCallVariants = function(regionStart, regionEnd, onVariantsCalled, onVariantsAnnotated) {
	var me = this;


	return new Promise( function(resolve, reject) {


		// If we don't have alignments, return.
		if (me.bam == null || me.getBamRefName == null) {
			resolve();
		} else if (me.getRelationship() == 'sibling') {
			resolve();
		} else {

			// Check the local cache first to see
			// if we already have the freebayes variants
			var fbData = me._getCachedData("fbData", window.gene.gene_name, window.selectedTranscript);
			if (fbData != null) {
				me.fbData = fbData;

				// Show populate the effect filters for the freebayes variants
				me._populateEffectFilters(me.fbData.features);

			    // Determine the unique values in the VCF filter field 
				me._populateRecFilters(me.fbData.features);

				if (onVariantsCalled) {
					onVariantsCalled();
				}

				if (onVariantsAnnotated) {
					onVariantsAnnotated(me.fbData);
				}

				resolve(me.fbData);


			} else {
				// We haven't cached the freebayes variants yet,
				// so call variants now.
				var refName = window.gene.chr;
				me.fbData = null;
				if (!me.isVcfLoaded()) {
					me.vcfData = null;
				}

				// Call Freebayes variants
				me.bam.getFreebayesVariants(refName, 
					window.gene.start, 
					window.gene.end, 
					window.gene.strand, 
					window.geneSource == 'refseq' ? true : false,
					function(data) {

					if (data == null || data.length == 0) {
						reject("A problem occurred while calling variants.");
					}

					// Parse string into records
					var fbRecs = [];
					var recs = data.split("\n");
					recs.forEach( function(rec) {
						fbRecs.push(rec);
					});
					

					// Reset the featurematrix load state so that after freebayes variants are called and
					// integrated into vcfData, we reload the feature matrix.
					if (me.isVcfLoaded()) {
						if (me.vcfData.loadState != null && me.vcfData.loadState['featurematrix']) {
							me.vcfData.loadState['featurematrix'] = null;
						}					
					} 

					if (onVariantsCalled) {
						onVariantsCalled();
					}

					// Annotate the fb variants
					me.vcf.promiseAnnotateVcfRecords(fbRecs, me.getBamRefName(refName), window.gene, 
						                             window.selectedTranscript, null, 
						                             filterCard.annotationScheme.toLowerCase(),
						                             window.geneSource == 'refseq' ? true : false)
				    .then( function(data) {

				    	var annotatedRecs = data[0];
				    	me.processFreebayesVariants(data[1], onVariantsAnnotated);


				    }, function(error) {
				    	reject('Error occurred when getting clinvar records:' + error);
				    })
				    .then (function() {

						// The variant records in vcfData have updated clinvar and inheritance info.
						// Reflect me new info in the freebayes variants.
						me.fbData.features.forEach(function (fbVariant) {
							if (fbVariant.source) {
								fbVariant.clinVarUid                  = fbVariant.source.clinVarUid;
								fbVariant.clinVarClinicalSignificance = fbVariant.source.clinVarClinicalSignificance;
								fbVariant.clinVarAccession            = fbVariant.source.clinVarAccession;
								fbVariant.clinvarRank                 = fbVariant.source.clinvarRank;
								fbVariant.clinvar                     = fbVariant.source.clinvar;
								fbVariant.clinVarPhenotype            = fbVariant.source.clinVarPhenotype;
							}
							
						});	 
						// Cache the freebayes variants.
						me._cacheData(me.fbData, "fbData", window.gene.gene_name, window.selectedTranscript);
						resolve(me.fbData);
				
				    	
				    }, function(error) {
				    	reject('An error occurred when getting clinvar recs for called variants: ' + error);
				    });
				
				});							

			}


		}

	});


} 

VariantModel.prototype.processFreebayesVariants = function(theFbData, callback) {
	var me = this;

	me.fbData = theFbData;
	// Flag the called variants
   	me.fbData.features.forEach( function(feature) {
   		feature.fbCalled = 'Y';
   	});

	// We may have called variants that are slightly outside of the region of interest.
	// Filter these out.
	if (window.regionStart != null && window.regionEnd != null ) {	
		me.fbData.features = me.fbData.features.filter( function(d) {
			meetsRegion = (d.start >= window.regionStart && d.start <= window.regionEnd);
			return meetsRegion;
		});
	}	


	// We are done getting the clinvar data for called variants.
	// Now merge called data with variant set and display.
	// Prepare vcf and fb data for comparisons
	me._prepareVcfAndFbData();

	// Determine allele freq levels
	me._determineVariantAfLevels(me.fbData, window.selectedTranscript);

	// Filter the freebayes variants to only keep the ones
	// not present in the vcf variant set.
	me._determineUniqueFreebayesVariants();


	// Show the snpEff effects / vep consequences in the filter card
	me._populateEffectFilters(me.fbData.features);

	// Determine the unique values in the VCF filter field 
	me._populateRecFilters(me.fbData.features);


	var theVcfData = me.getVcfDataForGene(window.gene, window.selectedTranscript);
	
	// Now get the clinvar data		    	
	me.vcf.promiseGetClinvarRecords(
	    		me.fbData, 
	    		me._stripRefName(window.gene.chr), window.gene, 
	    		isClinvarOffline ? me._refreshVariantsWithClinvarVariants.bind(me, me.fbData) : me._refreshVariantsWithClinvar.bind(me, me.fbData)
	    ).then( function() {
	    	if (callback) {

	    		// We need to refresh the fb variants in vcfData with the latest clinvar annotations
				me.fbData.features.forEach(function (fbVariant) {
					if (fbVariant.source) {
						fbVariant.source.clinVarUid                  = fbVariant.clinVarUid;
						fbVariant.source.clinVarClinicalSignificance = fbVariant.clinVarClinicalSignificance;
						fbVariant.source.clinVarAccession            = fbVariant.clinVarAccession;
						fbVariant.source.clinvarRank                 = fbVariant.clinvarRank;
						fbVariant.source.clinvar                     = fbVariant.clinvar;
						fbVariant.source.clinVarPhenotype            = fbVariant.clinVarPhenotype;
					}					
				});	 


	    		callback(me.fbData);
	    	}
	    });


}

VariantModel.prototype.loadCalledTrioGenotypes = function() {
	var me = this;
	if (this.vcfData == null || this.vcfData.features == null) {
		return;
	}
	var sourceVariants = this.vcfData.features
							 .filter(function (variant) {
								return variant.fbCalled == 'Y';
							 })
							 .reduce(function(object, variant) {
							 	var key = variant.type + " " + variant.start + " " + variant.ref + " " + variant.alt;
					  			object[key] = variant; 
					  			return object;
					 		 }, {});
	if (this.fbData) {
		this.fbData.features.forEach(function (fbVariant) {
			var key = fbVariant.type + " " + fbVariant.start + " " + fbVariant.ref + " " + fbVariant.alt;
			var source = sourceVariants[key];
			if (source) {
				fbVariant.inheritance                 = source.inheritance;
				fbVariant.genotypeRefCountMother      = source.genotypeRefCountMother;
				fbVariant.genotypeAltCountMother      = source.genotypeAltCountMother;
				fbVariant.genotypeDepthMother         = source.genotypeDepthMother;
				fbVariant.bamDepthMother              = source.bamDepthMother;
				fbVariant.genotypeRefCountFather      = source.genotypeRefCountFather;
				fbVariant.genotypeAltCountFather      = source.genotypeAltCountFather;
				fbVariant.genotypeDepthFather         = source.genotypeDepthFather;
				fbVariant.bamDepthFather              = source.bamDepthFather;
				fbVariant.fatherZygosity              = source.fatherZygosity;
				fbVariant.motherZygosity              = source.motherZygosity;
				fbVariant.uasibsZygosity              = source.uasibsZygosity;
				if (me.relationship != 'proband') {
					fbVariant.genotypeRefCountProband      = source.genotypeRefCountProband;
					fbVariant.genotypeAltCountProband      = source.genotypeAltCountProband;
					fbVariant.genotypeDepthProband         = source.genotypeDepthProband;
					fbVariant.probandZygosity              = source.probandZygosity;
				}
			}
				
			
		});	
		// Re-Cache the freebayes variants for proband now that we have mother/father genotype
		// and allele counts.							
		me._cacheData(me.fbData, "fbData", window.gene.gene_name, window.selectedTranscript);



	}
}



VariantModel.prototype._prepareVcfAndFbData = function(data) {
	var me = this;
	var theFbData = data ? data : me.fbData;
	// Deal with case where no variants were loaded
	if (!me.isVcfLoaded()) {
		// If no variants are loaded, create a dummy vcfData with 0 features
		me.vcfData = $.extend({}, theFbData);
		me.vcfData.features = [];
		me.setLoadState(me.vcfData, 'clinvar');
		me.setLoadState(me.vcfData, 'coverage');
		me.setLoadState(me.vcfData, 'inheritance');
	}


	// Flag the variants as called by Freebayes and add unique to vcf
	// set
	me.vcfData.features = me.vcfData.features.filter( function(feature) {
   		return feature.fbCalled == null;
   	});

	// This may not be the first time we call freebayes, so to
	// avoid duplicate variants, get rid of the ones
	// we added last round.					
	me.vcfData.features = me.vcfData.features.filter( function(d) {
		return d.consensus != 'unique2';
	});	


}


VariantModel.prototype._determineUniqueFreebayesVariants = function() {
	var me = this;

	// We have to order the variants in both sets before comparing
	me.vcfData.features = me.vcfData.features.sort(orderVariantsByPosition);					
	me.fbData.features  = me.fbData.features.sort(orderVariantsByPosition);

	// Compare the variant sets, marking the variants as unique1 (only in vcf), 
	// unique2 (only in freebayes set), or common (in both sets).	
	if (me.isVcfLoaded()) {
		// Compare fb data to vcf data
		me.vcf.compareVcfRecords(me.vcfData, me.fbData);

		// Add unique freebayes variants to vcfData
    	me.fbData.features = me.fbData.features.filter(function(d) {
    		return d.consensus == 'unique2';
    	});
	} 


	// Add the unique freebayes variants to vcf data to include 
	// in feature matrix
	me.fbData.features.forEach( function(v) {
		var variantObject = $.extend({}, v);
   		me.vcfData.features.push(variantObject);
   		v.source = variantObject;
   	});

   	// Figure out max level (lost for some reason)
   	var maxLevel = 1;
   	me.vcfData.features.forEach(function(feature) {
   		if (feature.level > maxLevel) {
   			maxLevel = feature.level;
   		}
   	});
   	me.vcfData.maxLevel = maxLevel;

    pileupObject = me._pileupVariants(me.fbData.features, gene.start, gene.end);
	me.fbData.maxLevel = pileupObject.maxLevel + 1;
	me.fbData.featureWidth = pileupObject.featureWidth;

	// Re-cache the vcf data now that the called variants have been merged
	me._cacheData(me.vcfData, "vcfData", window.gene.gene_name, window.selectedTranscript);
}



VariantModel.prototype.filterVariants = function(data, filterObject, start, end, bypassRangeFilter) {
	var me = this;

	var afFieldExac  = "afExAC";
	var afField1000g = "af1000G";
	var impactField = filterCard.annotationScheme.toLowerCase() === 'snpeff' ? 'impact' : IMPACT_FIELD_TO_FILTER;
	var effectField = filterCard.annotationScheme.toLowerCase() === 'snpeff' ? 'effect' : 'vepConsequence';

	// coverageMin is always an integer or NaN
	var	coverageMin = filterObject.coverageMin;
	var intronsExcludedCount = 0;


	var filteredFeatures = data.features.filter(function(d) {

		if (filterCard.shouldWarnForNonPassVariants()) {
			if (d.recfilter != 'PASS') {
				if (!d.hasOwnProperty('fbCalled') || d.fbCalled != 'Y') {
					d.featureClass = 'low-quality';
				} else {
					d.featureClass = '';
				}
			} else {
				d.featureClass = '';
			}
		}

		// We don't want to display homozygous reference variants in the variant chart
		// or feature matrix (but we want to keep it to show trio allele counts).
		var isHomRef = (d.zygosity != null && d.zygosity.toLowerCase() == 'homref') ? true : false;

		var meetsRegion = true;
		if (!bypassRangeFilter) {
			if (start != null && end != null ) {
				meetsRegion = (d.start >= start && d.start <= end);
			}			
		}

		// Allele frequency Exac - Treat null and blank af as 0
		var variantAf = d[afFieldExac] || 0;
		var meetsAfExac = true;
		if ($.isNumeric(filterObject.afMinExac) && $.isNumeric(filterObject.afMaxExac)) {
			// Exclude n/a ExAC allele freq (for intronic variants, af=-100) from range criteria
			meetsAfExac = (variantAf >= filterObject.afMinExac && variantAf <= filterObject.afMaxExac);
		}
		// Allele frequency 1000g - Treat null and blank af as 0
		variantAf = d[afField1000g] || 0;
		var meetsAf1000g = true;
		if ($.isNumeric(filterObject.afMin1000g) && $.isNumeric(filterObject.afMax1000g)) {
			// Exclude n/a 1000g allele freq (for intronic variants, af=-100) from range criteria
			meetsAf1000g = (variantAf >= filterObject.afMin1000g && variantAf <= filterObject.afMax1000g);
		}

		var meetsExonic = false;
		if (filterObject.exonicOnly) {
			for (key in d[impactField]) {
				if (key.toLowerCase() == 'high' || key.toLowerCase() == 'moderate') {
					meetsExonic = true;
				}
			}
			if (!meetsExonic) {
				for (key in d[effectField]) {
					if (key.toLowerCase() != 'intron_variant' && key.toLowerCase() != 'intron variant' && key.toLowerCase() != "intron") {
						meetsExonic = true;
					}
				}
			}
			if (!meetsExonic) {
				intronsExcludedCount++;
			}
		} else {
			meetsExonic = true;
		}


		// Evaluate the coverage for the variant to see if it meets min.
		var meetsCoverage = true;
		if (coverageMin && coverageMin > 0) {
			if ($.isNumeric(d.bamDepth)) {
				meetsCoverage = d.bamDepth >= coverageMin;
			} else if ($.isNumeric(d.genotypeDepth)) {
				meetsCoverage = d.genotypeDepth >= coverageMin;
			}
		}

		var incrementEqualityCount = function(condition, counterObject) {
			var countAttribute = condition ? 'matchCount' : 'notMatchCount';
			counterObject[countAttribute]++;
		}
		// Iterate through the clicked annotations for each variant. The variant
		// needs to match
		// at least one of the selected values (e.g. HIGH or MODERATE for IMPACT)
		// for each annotation (e.g. IMPACT and ZYGOSITY) to be included.
		var evaluations = {};
		for (key in filterObject.annotsToInclude) {
			var annot = filterObject.annotsToInclude[key];
			if (annot.state) {
				var evalObject = evaluations[annot.key];
				if (!evalObject) {
					evalObject = {};
					evaluations[annot.key] = evalObject;
				}

				var annotValue = d[annot.key] || '';

				// Keep track of counts where critera should be true vs counts
				// for critera that should be false.  
				//
				// In the simplest case,
				// the filter is evalated for equals, for example,
				// clinvar == pathogenic or clinvar == likely pathogenic.
				// In this case, if a variant's clinvar = pathogenic, the
				// evaluations will look like this:
				//  evalEquals: {matchCount: 1, notMatchCount: 0}
				// When variant's clinvar = benign
				//  evalEquals: {matchCount: 0, notMatchCount: 1}
	  		    //
				// In a case where the filter is set to clinvar NOT EQUAL 'pathogenic'
				// AND NOT EQUAL 'likely pathogenic'
				// the evaluation will be true on if the variant's clinvar is NOT 'pathogenic'
				// AND NOT 'likely pathogenic'
				// When variant's clinvar is blank:
				//  evalNotEquals: {matchCount: 0, notMatchCount: 2}
				//
				// If variant's clinvar is equal to pathogenic
				//  evalNotEquals: {matchCount: 1, notMatchCount 1}
				//
				var evalKey = 'equals';
				if (annot.hasOwnProperty("not") && annot.not) {
					evalKey = 'notEquals';
				} 
				if (!evalObject.hasOwnProperty(evalKey)) {
					evalObject[evalKey] = {matchCount: 0, notMatchCount: 0};
				}
				if ($.isPlainObject(annotValue)) {
					for (avKey in annotValue) {
						var doesMatch = avKey.toLowerCase() == annot.value.toLowerCase();
						incrementEqualityCount(doesMatch, evalObject[evalKey])
					}
				} else {
					var doesMatch = annotValue.toLowerCase() == annot.value.toLowerCase();
					incrementEqualityCount(doesMatch, evalObject[evalKey])
				}
			}
		}

		// If zero annots to evaluate, the variant meets the criteria.
		// If annots are to be evaluated, the variant must match
		// at least one value for each annot to meet criteria
		var meetsAnnot = true;
		for (key in evaluations) {
			var evalObject = evaluations[key];

			// Bypass evaluation for non-proband on inheritance mode.  This only
			// applied to proband.
			if (key == 'inheritance' && me.getRelationship() != 'proband') {
				continue;
			}
			if (evalObject.hasOwnProperty("equals") && evalObject["equals"].matchCount == 0) {
				meetsAnnot = false;
				break;
			}
		}

		// For annotations set to 'not equal', any case where the annotation matches (matchCount > 0),
		// we set that the annotation critera was not met.  Example:  When filter is 
		// clinvar 'not equal' pathogenic, and variant.clinvar == 'pathogenic' matchCount > 0,
		// so the variants does not meet the annotation criteria
		var meetsNotEqualAnnot = true
		for (key in evaluations) {
			var evalObject = evaluations[key];

			// Bypass evaluation for non-proband on inheritance mode.  This only
			// applied to proband.
			if (key == 'inheritance' && me.getRelationship() != 'proband') {
				continue;
			}
			// Any case where the variant attribute matches value on a 'not equal' filter, 
			// we have encountered a condition where the criteria is not met.
			if (evalObject.hasOwnProperty("notEquals") && evalObject["notEquals"].matchCount > 0) {
				meetsNotEqualAnnot = false;
				break;
			}
		}


		return !isHomRef && meetsRegion && meetsAfExac && meetsAf1000g && meetsCoverage && meetsAnnot && meetsNotEqualAnnot && meetsExonic;
	});

	var pileupObject = this._pileupVariants(filteredFeatures, start, end);

	var vcfDataFiltered = {
		count: data.count,
		countMatch: data.countMatch,
		countUnique: data.countUnique,
		sampleCount : data.sampleCount,
		intronsExcludedCount: intronsExcludedCount,
		end: end,
		features: filteredFeatures,
		maxLevel: pileupObject.maxLevel + 1,
		featureWidth: pileupObject.featureWidth,
		name: data.name,
		start: start,
		strand: data.strand,
		variantRegionStart: start
	};
	return vcfDataFiltered;
}


VariantModel.prototype.promiseCompareVariants = function(theVcfData, compareAttribute, matchAttribute, matchFunction, noMatchFunction ) {
	var me = this;

	return new Promise( function(resolve, reject) {
		if (me.vcfData == null) {
			me._promiseVcfRefName().then( function() {

				me.vcf.promiseGetVariants(
					 me.getVcfRefName(window.gene.chr), 
					 window.gene.start, 
					 window.gene.end, 
					 window.gene.strand, 
					 window.selectedTranscript,
					 me.sampleName,
					 filterCard.annotationScheme.toLowerCase(),
					 window.geneSource == 'refseq' ? true : false)
				.then( function(data) {

					if (data != null && data.features != null) {
						var annotatedRecs = data[0];
				    	me.vcfData = data[1];

					 	me.vcfData.features = me.vcfData.features.sort(orderVariantsByPosition);
						me.vcfData.features.forEach( function(feature) {
							feature[compareAttribute] = '';
						});
						me.vcf.compareVcfRecords(theVcfData, me.vcfData, compareAttribute, matchFunction, noMatchFunction); 	
						resolve();							 							
					} else {
						var error = 'promiseCompareVariants() has null data returned from promiseGetVariants';
						console.log(error);
						reject(error);
					}
				}, function(error) {
					var message = 'error occurred when getting variants in promiseCompareVariants: ' + error;
					console.log(message);
					reject(message);
				});
			}, function(error) {
				console.log("missing reference");
				reject("missing reference");
			});
		
		} else {
			me.vcfData.features = me.vcfData.features.sort(orderVariantsByPosition);
			if (compareAttribute) {
				me.vcfData.features.forEach( function(feature) {			
					feature[compareAttribute] = '';
				});			
			}
			me.vcf.compareVcfRecords(theVcfData, me.vcfData, compareAttribute, matchFunction, noMatchFunction); 
			resolve();	
		}

	});


}

/*
*  Evaluate the highest impacts for a variant across all transcripts.
*  Cull the impact if it already annotated for the canonical transcript
*  or the impact is less severe than the one for the canonical
*  transcripts.  Returns an object that looks like this:
*  {HIGH: {frameshift: 
*            {
*			  transcripts: [ENST000245.1,ENSTxxxx],
*			  display: 'ENST000241.1,ENSTxxxx'
*			} 
*		  stop_gain: 
*			  {
*			  transcripts: [ENST000245.1,ENSTxxxx],
*			  display: 'ENST000241.1,ENSTxxxx'
*			} 
*		  }
*	}
*/
VariantModel.getNonCanonicalHighestImpactsVep = function(variant) {
	var vepHighestImpacts = {};
	for (var impactKey in variant.highestImpactVep) {
		var nonCanonicalEffects = [];
		var allEffects = variant.highestImpactVep[impactKey];
		
		var lowestImpactValue = 99;
		for (key in variant.vepImpact) {
			var value = matrixCard.impactMap[key].value;
			if (value < lowestImpactValue) {
				lowestImpactValue = value;
			}
		}	

		var theValue = matrixCard.impactMap[impactKey].value;
		if (theValue < lowestImpactValue) {
			for (effectKey in allEffects) {
				var allTranscripts = allEffects[effectKey];
				if (Object.keys(allTranscripts).length > 0) {
					var ncObject = {};
					var transcriptUrls = "";
					for(transcriptId in allTranscripts) {
						if (transcriptUrls.length > 0) {
							transcriptUrls += ", ";
						}
						var url = '<a href="javascript:void(0)" onclick="selectTranscript(\'' + transcriptId + '\')">' + transcriptId + '</a>'; 
						transcriptUrls += url;
					}
					ncObject[effectKey] = {transcripts: Object.keys(allTranscripts), display: Object.keys(allTranscripts).join(","), url: transcriptUrls};
					nonCanonicalEffects.push(ncObject);
				}

			}

			if (nonCanonicalEffects.length > 0) {
				vepHighestImpacts[impactKey] = nonCanonicalEffects;
			}
		}
	}	
	return vepHighestImpacts;
}




