var recordedCacheErrors = {};

function CacheHelper(loaderDisplay) {

	this.genesToCache = [];
	this.cacheQueue = [];
	this.batchSize = null;
	this.geneBadgeLoaderDisplay = loaderDisplay;
}

CacheHelper.prototype.isolateSession = function() {
	this.launchTimestamp = Date.now().valueOf();	
}

CacheHelper.prototype.showAnalyzeAllProgress = function(clearStandardFilterCounts) {
	var me = this;
	me.getAnalyzeAllCounts(function(counts) {
		$('#analyze-all-progress').removeClass("hide");
		if (counts.analyzed == 0 && counts.total > 0) {
			$('#analyze-all-progress').removeClass("done");
			$('#analyze-all-progress .bar').css("width", "0%");
			$('#analyze-all-progress .text').text(counts.analyzed + ' of ' + counts.total + ' analyzed');
		}
		else if (counts.analyzed == counts.total) {
			$('#analyze-all-progress .bar').css("width", "0%");
			$('#analyze-all-progress').addClass("done");
			$('#analyze-all-progress .bar').css("width", "100%");
			$('#analyze-all-progress .text').text(counts.analyzed + ' analyzed');
		} else {
			$('#analyze-all-progress').removeClass("done");
			$('#analyze-all-progress .bar').css("width", percentage(counts.analyzed / counts.total));
			$('#analyze-all-progress .text').text(counts.analyzed + ' of ' + counts.total + ' analyzed');
		}
		if (filterCard.hasFilters()) {
			$('#filter-progress .text').text(counts.pass + " passed filter");
			$('#filter-progress .bar').css("width", percentage(counts.pass / counts.analyzed));
			$('#filter-progress').removeClass("hide");	

			// If a standard filter has been applied, update its counts
			if (clearStandardFilterCounts) {
				$('#standard-filter-panel .standard-filter-btn').parent().find('span.standard-filter-count').text("");
			}
			if ($('#standard-filter-panel .standard-filter-btn.current').length > 0) {
				$('#standard-filter-panel .standard-filter-btn.current').parent().find('span.standard-filter-count').text(counts.pass + ' of ' + counts.analyzed + ' genes');
			}			
		} else {
			$('#filter-progress').addClass("hide");					
		}
	});	
}

CacheHelper.prototype.hideAnalyzeAllProgress = function() {
	$("#analyze-all-progress").addClass("hide");
	$("#analyze-all-progress .text").text("");
	$("#analyze-all-progress .bar").css("width", "0%");	
}


CacheHelper.prototype.analyzeAll = function() {
	var me = this;

	me.showAnalyzeAllProgress();

	// Start over with a new queue of genes to be analyzed
	// is all of the genes that need to be analyzed (and cached.)
	me.genesToCache = [];
	me.cacheQueue = [];
	genesCard.getGeneNames().forEach(function(geneName) {		
		me.genesToCache.push(geneName);
	});
	me.cacheGenes(function() {
		// After all genes have been cached, refresh the gene badges in case
		// filters were applied while genes were still in the process of being
		// analyzed.
		me.refreshGeneBadges();
	});


}



CacheHelper.prototype.cacheGenes = function(callback) {
	var me = this;

	// If there are no more genes to cache, 
	if (me.genesToCache.length == 0 && me.cacheQueue.length == 0) {
		if (callback) {
			callback();
		}
	}



	// If we still have genes in the cache queue, exit. (Wait to kick off next batch 
	// of genes to analyze until all genes in last batch are analyzed.)
	if (me.cacheQueue.length > 0) {
		return;
	}


	// Determine the batch size.  (It will be smaller that the
	// default batch size if the genes remaining to be cached is
	// smaller than the batch size.)
	me.batchSize = Math.min(me.genesToCache.length, DEFAULT_BATCH_SIZE);

	// Place next batch of genes in caching queue 
	for (var i = 0; i < me.batchSize; i++) {
		me.cacheQueue.push(me.genesToCache[i]);
	}
	// Remove this batch of genes from the list of all genes to be cached
	for (var i = 0; i < me.batchSize; i++) {
		me.genesToCache.shift();
	}
	// Invoke method to cache each of the genes in the queue
	for (var i = 0; i < me.batchSize; i++) {
		me.cacheGene(me.cacheQueue[i], callback);
	}


}



CacheHelper.prototype.cacheGene = function(geneName, callback) {
	var me = this;

	promiseGetGeneModel(geneName).then( function(geneModel) {
	    var geneObject = geneModel;
    	// Now that we have the gene model,
    	// load and annotate the variants for each
    	// sample (e.g. each variant card)
		me.geneBadgeLoaderDisplay.setPageCount(genesCard.getPageCount())
														 .addGene(geneName, genesCard.pageNumberForGene(geneName));


    	adjustGeneRegion(geneObject);
    	var transcript = getCanonicalTranscript(geneObject);
    	window.geneObjects[geneObject.gene_name] = geneObject;

    	isJointCallOnly( function(shouldJointCall) {
		    if (me.isCachedForProband(geneObject.gene_name, transcript)) {
		    	// take this gene off of the queue and see
		    	// if next batch of genes should be analyzed
		    	genesCard._geneBadgeLoading(geneObject.gene_name, false);
		    	me.cacheNextGene(geneObject.gene_name, callback);
		    } else if (shouldJointCall) {
		    	genesCard._geneBadgeLoading(geneObject.gene_name, true);
				cacheJointCallVariants(geneObject, transcript, function() {

					me.processCachedTrio(geneObject, transcript, callback)						
				});
			} else {

				genesCard._geneBadgeLoading(geneObject.gene_name, true);
			    // For each sample, get and annotate the genes and
			    // cache the variants
		    	getRelevantVariantCards().forEach(function(variantCard) {

		    		if (dataCard.mode == 'trio' || variantCard == getProbandVariantCard()) {
			    		variantCard.promiseCacheVariants(
			    			geneObject.chr,
			    			geneObject, 
						 	transcript)
			    		.then( function(vcfData) {
			    			// Once all analysis of the gene variants for each of
			    			// the samples is complete, determine the inheritance 
			    			// (if this is a trio)
			    			if (me.isCachedForCards(geneObject.gene_name, transcript)) {

				    			me.processCachedTrio(geneObject, transcript, callback);


			    			}

			    		}, function(error) {
			    			genesCard.setGeneBadgeError(geneObject.gene_name);			    				
		    				var message = error.hasOwnProperty("message") ? error.message : error;
			    			console.log("problem caching data for gene " + geneObject.gene_name + ". " + message);
			    			genesCard._geneBadgeLoading(geneObject.gene_name, false);

							getVariantCard("proband").summarizeError(geneObject.gene_name, error);
	    					// take this gene off of the queue and see
	    					// if next batch of genes should be analyzed
				    		me.cacheNextGene(geneObject.gene_name, callback);					
			    		});

		    		}

		    	});				    	
		    }


    	});

	},
	function(error) {
		genesCard.setGeneBadgeError(geneName);			    				
		console.log("problem caching data for gene " + geneName + ".");
		genesCard._geneBadgeLoading(geneName, false);
		getVariantCard("proband").summarizeError(geneName, error);
    	me.cacheNextGene(geneName, callback);
	});

				
}

CacheHelper.prototype.processCachedTrio = function(geneObject, transcript, callback) {
	var me = this;

	// we need to compare the proband variants to mother and father variants to determine
	// the inheritance mode. 
	var probandVcfData = getVariantCard("proband").model.getVcfDataForGene(geneObject, transcript);
	var motherVcfData  = getVariantCard("mother" ).model.getVcfDataForGene(geneObject, transcript);
	var fatherVcfData  = getVariantCard("father" ).model.getVcfDataForGene(geneObject, transcript);

	if (dataCard.mode == 'trio' && (probandVcfData == null || motherVcfData == null || fatherVcfData == null)) {
		console.log("Unable to determine inheritance during Analyze All for gene " + geneObject.gene_name + " because full trio data not available");
		genesCard.clearGeneGlyphs(geneObject.gene_name);
		genesCard.setGeneBadgeError(geneObject.gene_name);		
		me.cacheNextGene(geneObject.gene_name, callback);
		return;
	} 

	var trioModel = new VariantTrioModel(probandVcfData, motherVcfData, fatherVcfData);
	trioModel.compareVariantsToMotherFather(function() {

		if (!getVariantCard("proband").model._cacheData(probandVcfData, "vcfData", geneObject.gene_name, transcript)) {
			return;
		}		
		if (!getVariantCard("mother" ).model._cacheData(motherVcfData,  "vcfData", geneObject.gene_name, transcript)) {
			return;
		}		
		if (!getVariantCard("father" ).model._cacheData(fatherVcfData,  "vcfData", geneObject.gene_name, transcript)) {					
			return;
		}		

		// Now that inheritance has been determined,
		// summarize the variants for the proband to
		// create the gene badges, representing the
		// most pathogenic variants for this gene
		var filteredVcfData = getVariantCard('proband').model.filterVariants(probandVcfData, filterCard.getFilterObject(), geneObject.start, geneObject.end, true);
		var dangerObject = getVariantCard("proband").summarizeDanger(geneObject.gene_name, filteredVcfData);
		
		genesCard._geneBadgeLoading(geneObject.gene_name, false);
		if (probandVcfData.features.length == 0) {
			//genesCard.setGeneBadgeWarning(geneObject.gene_name);
		} else {
			genesCard.setGeneBadgeGlyphs(geneObject.gene_name, dangerObject, false);
		}

		// Re-cache the results now that inheritance has been determined
		getRelevantVariantCards().forEach(function(vc) {
			if (autoCall && !vc.model.isVcfReadyToLoad()) {
				var data = vc.model.getVcfDataForGene(geneObject, transcript);
				vc.model._cacheData(data, "fbData", geneObject.gene_name, transcript);
				vc.model._cacheData(data, "vcfData", geneObject.gene_name, transcript);											
			}
		})

		
		// Now clear out mother and father from cache.  Don't clear cache for currently selected
		// gene though as this will result in no inheritance mode being detected.
		if (window.gene == null || window.gene.gene_name != geneObject.gene_name) {
			getVariantCard("mother" ).model.clearCacheItem("vcfData", geneObject.gene_name, transcript);					
			getVariantCard("father" ).model.clearCacheItem("vcfData", geneObject.gene_name, transcript);					

			getVariantCard("mother" ).model.clearCacheItem("fbData", geneObject.gene_name, transcript);					
			getVariantCard("father" ).model.clearCacheItem("fbData", geneObject.gene_name, transcript);								
		}


		// take this gene off of the queue and see
		// if next batch of genes should be analyzed
		me.cacheNextGene(geneObject.gene_name, callback);
	}, 
	function(error) {
		console.log("problem determining inheritance for " + geneObject.gene_name + ". " + error);
		// take this gene off of the queue and see
		// if next batch of genes should be analyzed
		me.cacheNextGene(geneObject.gene_name, callback);
	});
}

CacheHelper.prototype.isGeneInProgress = function(geneName) {
	return this.cacheQueue.indexOf(geneName) >= 0;
}

CacheHelper.prototype.cacheNextGene = function(geneName, callback) {
	this.showAnalyzeAllProgress();

	this.geneBadgeLoaderDisplay.setPageCount(genesCard.getPageCount())
														 .removeGene(geneName);
	// Take the analyzed (and cached) gene off of the cache queue
	var idx = this.cacheQueue.indexOf(geneName);
	if (idx >= 0) {
		this.cacheQueue.splice(idx,1);
	} else {
		idx = this.cacheQueue.indexOf(geneName.toUpperCase());
		if (idx >= 0) {
			this.cacheQueue.splice(idx,1);
		} else {
			console.log("Unexpected error occurred during caching of genes.  Could not remove " + geneName + " from cache queue");
			return;
		}
	}
	// Invoke cacheGenes, which will kick off the next batch
	// of genes to analyze once all of the genes in
	// the current batch have been analyzed.
	this.cacheGenes(callback);
}

CacheHelper.prototype.isCachedForProband = function(geneName, transcript) {
	return getProbandVariantCard().model.isCachedAndInheritanceDetermined(geneName, transcript)
		|| getProbandVariantCard().model.getDangerSummaryForGene(geneName);
}

CacheHelper.prototype.isCachedForCards = function(geneName, transcript) {
	var me = this;
	var count = 0;
	getRelevantVariantCards().forEach( function(variantCard) {
		if (variantCard.isCached(geneName, transcript)) {
			count++;
		}
	});
	if (dataCard.mode == 'single') {
		return count == 1;
	} else {
		return count == getRelevantVariantCards().length;
	}
}


CacheHelper.prototype.getAnalyzeAllCounts = function(callback) {
	var countObject = {total: 0, analyzed: 0, unanalyzed: 0, error: 0, pass: 0};
	genesCard.getGeneNames().forEach(function(geneName) {
		//genesToCount.push(geneName);
		
		var key = getProbandVariantCard().model._getCacheKey("dangerSummary", geneName);
    	var dangerSummary = CacheHelper.getCachedData(key);
		if (dangerSummary != null) {
			countObject.analyzed++;
			if (dangerSummary.featureCount && dangerSummary.featureCount > 0) {
				countObject.pass++;
			}
		} else {
			countObject.unanalyzed++;						
		}
		countObject.total++;
	});
	callback(countObject);



/*
	var genesToCount = [];
	var countObject = {total: 0, analyzed: 0, unanalyzed: 0, error: 0};
	genesCard.getGeneNames().forEach(function(geneName) {
		genesToCount.push(geneName);
	});
	var countNextGeneTranscript = function(callback) {
		if (genesToCount.length == 0) {
			callback();
		}
		var geneName = genesToCount.splice(0,1);
		var url = geneInfoServer + 'api/gene/'  + geneName;
		$.ajax({
		    url: url,
		    jsonp: "callback",
		    type: "GET",
		    dataType: "jsonp",
		    success: function( response ) {
		    	// Now that we have the gene model,
		    	// load and annotate the variants for each
		    	// sample (e.g. each variant card)
		    	if (response[0].hasOwnProperty('gene_name')) {
			    	var geneObject = response[0];
			    	adjustGeneRegion(geneObject);
			    	var transcript = getCanonicalTranscript(geneObject);
			    	var key = getProbandVariantCard().model._getCacheKey("dangerSummary", geneObject.gene_name);
			    	var dangerSummary = CacheHelper.getCachedData(key);
					if (dangerSummary != null) {
						countObject.analyzed++;
					} else {
						countObject.unanalyzed++;						
					}
					countObject.total++;
					countNextGeneTranscript(callback);
			    }
			},
			error: function( xhr, status, errorThrown ) {
				countObject.total++;
				countObject.error++;
				countNextGeneTranscript(callback);
			}
		});

	}

	var countObject = {total: 0, analyzed: 0, unanalyzed: 0, error: 0};
	countNextGene(function() {
		callback(countObject);
	});
*/
}



CacheHelper.prototype.refreshGeneBadges = function() {  
	var me = this;
	var geneCount = {total: 0, pass: 0};

	$('#gene-badges-loader').removeClass("hide");

	var theGeneNames = {};
	genesCard.getGeneNames().forEach(function(geneName) {
		theGeneNames[geneName] = true;
	});

	for (var i=0; i<=localStorage.length-1; i++)  
	{  
		key = localStorage.key(i);  
		keyObject = CacheHelper._parseCacheKey(key);
		if (keyObject && keyObject.launchTimestamp == me.launchTimestamp) {

		  	if (keyObject.dataKind == 'vcfData' && keyObject.relationship == "proband" && theGeneNames[keyObject.gene]) {
		  		var theVcfData = CacheHelper.getCachedData(key);
		  		var geneObject = window.geneObjects[keyObject.gene];
		  		var filteredVcfData = getVariantCard('proband').model.filterVariants(theVcfData, filterCard.getFilterObject(), geneObject.start, geneObject.end, true);

		  		geneCount.total++;
		  		if (filteredVcfData.features.length > 0) {
		  			geneCount.pass++;
		  		}

		  		var dangerObject = getVariantCard("proband").summarizeDanger(keyObject.gene, filteredVcfData);
				getVariantCard('proband').model.cacheDangerSummary(dangerObject, keyObject.gene);
		
				genesCard.setGeneBadgeGlyphs(keyObject.gene, dangerObject, false);
		  	}
		}
	}  
	genesCard.sortGenes();
	$('#gene-badges-loader').addClass("hide");
	return geneCount;
}

CacheHelper.prototype.clearCache = function(launchTimestampToClear) {
	var me = this;
	if (keepLocalStorage) {
		
	} else {
		me._clearCache(launchTimestampToClear, false, false);
		me.genesToCache = [];
	}
}

CacheHelper.prototype.getCacheKey = function(cacheObject) {
	return      "gene.iobio"  
		+ "---" + this.launchTimestamp
	    + "---" + cacheObject.relationship 
		+ "---" + cacheObject.sample
		+ "---" + cacheObject.gene
		+ "---" + cacheObject.transcript
	    + "---" + cacheObject.annotationScheme
		+ "---" + cacheObject.dataKind;
}


CacheHelper.prototype.getCacheSize = function() {  // provide the size in bytes of the data currently stored
	var me = this;
	var size = 0;
	var otherSize = 0;
	var coverageSize = 0;
	var otherAppSize = 0;
	for (var i=0; i<=localStorage.length-1; i++)  
	{  
		key = localStorage.key(i);  
		var keyObject = CacheHelper._parseCacheKey(key);
		if (keyObject) {			
			if (keyObject.launchTimestamp == me.launchTimestamp) {
			  	var dataSize = localStorage.getItem(key).length;
			  	size     += dataSize;

			  	if (keyObject.dataKind == 'bamData') {
			  		coverageSize +=  dataSize;
			  	}
			  	
			} else {
				var dataSize = localStorage.getItem(key).length;
				otherSize += dataSize;
			}
		} else {
			otherAppSize += localStorage.getItem(key).length;
		}
	}  
	return {total:     (CacheHelper._sizeMB(size) + " MB"), 
	        coverage:  (CacheHelper._sizeMB(coverageSize) + " MB"),
	        other:     (CacheHelper._sizeMB(otherSize) + " MB"),
	        otherApp:  (CacheHelper._sizeMB(otherAppSize) + " MB")
	    };
}

CacheHelper._logCacheSize = function() {
	var cacheInfo = {};
	for (var i=0; i<=localStorage.length-1; i++)  
	{  
		var key = localStorage.key(i); 
		var keyPart = "";
		if (key.indexOf("---")) {
			keyPart = key.split("---")[0];
		} else {
			keyPart = key;
		}
			
		var size = cacheInfo[keyPart];
		if (size == null) {
			size = 0;
		}
		size += localStorage.getItem(key).length;
		cacheInfo[keyPart] = size;			
	}
	console.log(cacheInfo);
	var totalSize = 0;
	Object.keys(cacheInfo).forEach(function(key) {
		totalSize += cacheInfo[key];
	})
	console.log(totalSize);	
}

CacheHelper._logCacheContents = function() {
	var x, xLen, log=[],total=0;
	for (x in localStorage){
		xLen =  ((localStorage[x].length * 1 + x.length * 1)/1024); 
		log.push(x + " = " +  xLen.toFixed(2) + " KB"); 
		total+= xLen}; 
		if (total > 1024){
			log.unshift("Total = " + (total/1024).toFixed(2)+ " MB");
		} else{
			log.unshift("Total = " + total.toFixed(2)+ " KB");}; 
			console.log(log.join("\n")
	);	
}

CacheHelper.prototype.clearCacheItem = function(key) {
	var me = this;
	if (localStorage) {
		localStorage.removeItem(key);			
	}
}

CacheHelper.prototype._clearCache = function(launchTimestampToClear, clearOther, clearOtherApp) {
	var me = this;
	var theLaunchTimeStamp = launchTimestampToClear ? launchTimestampToClear : me.launchTimestamp;
	if (localStorage) {
		//CacheHelper._logCacheSize();
		var keysToRemove = [];
		for (var i=0; i<=localStorage.length-1; i++)  {  
			var key = localStorage.key(i); 	
			var keyObject = CacheHelper._parseCacheKey(key);
			if (keyObject) {
				if (keyObject.launchTimestamp == theLaunchTimeStamp && !clearOther && !clearOtherApp) {
					keysToRemove.push(key);
					if (keyObject.gene && keyObject.relationship == 'proband') {
						genesCard.clearGeneGlyphs(keyObject.gene);
						genesCard.clearGeneInfo(keyObject.gene);

					}
				} else if (keyObject.launchTimestamp != theLaunchTimeStamp && clearOther && !clearOtherApp) {
					keysToRemove.push(key);
				}				
			} else if (clearOtherApp) {
				keysToRemove.push(key);
			}
		}	
		keysToRemove.forEach( function(key) {
			localStorage.removeItem(key);			
		})
		window.gene = null;
		me.hideAnalyzeAllProgress();
		//CacheHelper._logCacheSize();
		//CacheHelper._logCacheContents();
	}
}

CacheHelper.prototype.clearAll = function() {
	var me = this;
	// confirm dialog
	alertify.confirm("Clear all cached data for this session?", function (e) {
	    if (e) {
			// user clicked "ok"
			me._clearCache(me.launchTimestampToClear, false, false);
			cacheHelper.showAnalyzeAllProgress();
  			me.refreshDialog();
	        
	    } else {
	        // user clicked "cancel"
	    }
	});
}
CacheHelper.prototype.clearOther = function() {
	var me = this;
	// confirm dialog
	alertify.confirm("Clear all cached data for other gene.iobio sessions?", function (e) {
	    if (e) {
			// user clicked "ok"
			me._clearCache(null, true, false);
  			me.refreshDialog();
	        
	    } else {
	        // user clicked "cancel"
	    }
	});
}
CacheHelper.prototype.clearOtherApp = function() {
	var me = this;
	// confirm dialog
	alertify.confirm("Clear all cached data for other web applications?", function (e) {
	    if (e) {
			// user clicked "ok"
			me._clearCache(null, false, true);
  			me.refreshDialog();
	        
	    } else {
	        // user clicked "cancel"
	    }
	});
}

CacheHelper.prototype.clearCoverageCache = function() {
	var me = this;
	for (var i=0; i<=localStorage.length-1; i++)  {  
  		var key = localStorage.key(i); 	
		var keyObject = CacheHelper._parseCacheKey(key);
	  		if (keyObject && keyObject.launchTimestamp == me.launchTimestamp) {
				if (keyObject.dataKind == "bamData") {
					localStorage[key] = "";
				}
	  		}
	}
	me.refreshDialog();
}
CacheHelper.prototype.clearNonBadgeCache = function() {
	var me = this;
	for (var i=0; i<=localStorage.length-1; i++)  {  
  		var key = localStorage.key(i); 	
  		var keyObject = CacheHelper._parseCacheKey(key);
  		if (keyObject && keyObject.launchTimestamp == me.launchTimestamp) {
			if (me._isProbandVariantCache(key) && !me._hasBadgeOfInterest(key)) {
				me.clearCacheForGene(keyObject.gene);
			}  			
  		}
	}
	me.refreshDialog();
}

CacheHelper.prototype.refreshDialog = function() {
	var sizes = this.getCacheSize();
	$("#cache-size").text(sizes.total);
	$("#coverage-size").text(sizes.coverage);
	$("#other-cache-size").text(sizes.other);
	$("#other-app-cache-size").text(sizes.otherApp);
}

CacheHelper.prototype.openDialog = function() {
	this.refreshDialog();
	$('#manage-cache-modal').modal('show');
}



CacheHelper.prototype.clearCacheForGene = function(geneName) {
	var me = this;
	var keys = me._getKeysForGene(geneName);
	keys.forEach( function(key) {
		localStorage[key] = "";
	});

	// Clear out the loading message by the page control for this gene
	me.geneBadgeLoaderDisplay.setPageCount(genesCard.getPageCount())
							 .removeGene(geneName);

	// Clear the gene out from the cache 'analyze all' queue
	if (me.isGeneInProgress(geneName)) {
		me.cacheNextGene(geneName);
	}
}


CacheHelper.prototype._getKeysForGene = function(geneName) {
	var me = this;
	var keys = [];
	for (var i=0; i<=localStorage.length-1; i++)  {  
  		var key = localStorage.key(i); 	
		var keyObject = CacheHelper._parseCacheKey(key);
		if (keyObject && keyObject.launchTimestamp == me.launchTimestamp) {
			if (keyObject.gene == geneName) {
				keys.push(key);
			}			
		}
	}
	return keys;
}


CacheHelper.prototype._isProbandVariantCache = function(key) {
	var cacheObject = CacheHelper._parseCacheKey(key);
	return (cacheObject 
		&& cacheObject.launchTimestamp == this.launchTimestamp 
		&& ( cacheObject.dataKind == "vcfData"  || cacheObject.dataKind == "fbData")
		&& cacheObject.relationship == "proband");

}




CacheHelper._sizeMB = function(size) {
	var _sizeMB = size / (1024*1024);
	return  Math.round(_sizeMB * 100) / 100;
}


CacheHelper._parseCacheKey = function(cacheKey) {
	if (cacheKey.indexOf("---") > 0) {
		var tokens = cacheKey.split("---");
		if (tokens.length == 8 && tokens[0] == "gene.iobio") {
			return { app: tokens[0],
				     launchTimestamp: tokens[1],
				     relationship: tokens[2], 
				     sample: tokens[3], 
				     gene: tokens[4], 
				     transcript: tokens[5], 
				     annotationScheme: tokens[6], 
				     dataKind: tokens[7]
				    };

		} else {
			return null;
		}

	} else {
		return null;
	}

}


CacheHelper.getCachedData = function(key) {
	var data = null;
	if (localStorage) {
      	var dataCompressed = localStorage.getItem(key);
      	if (dataCompressed != null) {
			var dataString = null;
			try {
				 dataString = LZString.decompressFromUTF16(dataCompressed);
	 			 data =  JSON.parse(dataString);      		
			} catch(e) {
				console.log("an error occurred when uncompressing vcf data for key " + key);
			}
      	} 
	} 
	return data;	
}


CacheHelper.showError = function(key, cacheError) {
	var cacheObject = CacheHelper._parseCacheKey(key);
	if (cacheObject) {
		var errorType = cacheError.name && cacheError.name.length > 0 ? cacheError.name : "A problem";
		var errorKey = cacheObject.gene + "---" + errorType;

		var consoleMessage = errorType + " occurred when caching analyzed " + cacheObject.dataKind + " data for gene " + cacheObject.gene + ". Click on 'Clear cache...' link to clear cache."
	    console.log(consoleMessage);
	    console.log(cacheError.toString());
	    
	    // Only show the error once
	    if (!recordedCacheErrors[errorKey]) {
	    	recordedCacheErrors[errorKey] = message;
		    var message = errorType + " occurred when caching analyzed data for gene " + cacheObject.gene + ". Unable to analyze remaining genes."
			alertify.alert(message, function() {
				recordedCacheErrors[errorKey] = null;			
			});	
	    }		
	}

}