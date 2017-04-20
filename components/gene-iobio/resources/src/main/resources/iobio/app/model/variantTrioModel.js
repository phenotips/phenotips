function VariantTrioModel(probandVcfData, motherVcfData, fatherVcfData, sibsVcfData) {
	this.probandVcfData = probandVcfData;
	this.motherVcfData = motherVcfData;
	this.fatherVcfData = fatherVcfData;
	this.sibsVcfData = sibsVcfData;
	this.sibsVcfDataTransient = [];
}


VariantTrioModel.prototype.compareVariantsToMotherFather = function(callback) {
	var me = this;

	// Clear out the inheritance, mother/father zygosity, mother/father genotype fields 
	// stored in proband variants
	me.probandVcfData.features.forEach(function(variant) {
		variant.compareMother = null;
		variant.compareFather = null;
		variant.compareMotherFather = null;
		variant.inheritance = 'none';
		variant.fatherZygosity = null;
		variant.motherZygosity = null;
		variant.genotypeAltCountFather = null;
		variant.genotypeRefCountFather = null;
		variant.genotypeDepthFather    = null;
		variant.bamDepthFather         = null;
		variant.genotypeAltCountMother = null;
		variant.genotypeRefCountMother = null;
		variant.genotypeDepthMother    = null;
		variant.bamDepthMother         = null;

	});

	// Only continue with comparison if mother and father
	// variant cards are present
	if (me.motherVcfData == null || me.fatherVcfData == null) {
		callback(me.probandVcfData);
		return;
	} 
	// Clear out the inheritance, mother/father zygosity, mother/father genotype fields 
	// stored in proband variants
	me.motherVcfData.features.forEach(function(variant) {
		variant.compareMotherFather     = null;
		variant.probandZygosity         = null;
		variant.fatherZygosity          = null;
		variant.genotypeAltCountProband = null;
		variant.genotypeRefCountProband = null;
		variant.genotypeDepthProband    = null;
		variant.bamDepthProband         = null;
		variant.genotypeAltCountFather  = null;
		variant.genotypeRefCountFather  = null;
		variant.genotypeDepthFather     = null;
		variant.bamDepthFather          = null;
	});
	me.fatherVcfData.features.forEach(function(variant) {
		variant.compareMotherFather     = null;
		variant.probandZygosity         = null;
		variant.motherZygosity          = null;
		variant.genotypeAltCountProband = null;
		variant.genotypeRefCountProband = null;
		variant.genotypeDepthProband    = null;
		variant.bamDepthProband         = null;
		variant.genotypeAltCountMother  = null;
		variant.genotypeRefCountMother  = null;
		variant.genotypeDepthMother     = null;
		variant.bamDepthMother          = null;
	});


	// Sort the variants
	me.probandVcfData.features = me.probandVcfData.features.sort(orderVariantsByPosition);

	// Compare the proband's variants to the mother's variants
	me.promiseCompareVariants(
		me.probandVcfData,
		me.motherVcfData,
	    // This is the attribute on variant a (proband) and variant b (mother)
		// that will store whether the variant is unique or matches.
    	'compareMother',
    	// This is the callback function called every time we find the same variant
    	// in both sets. Here we take the mother variant's af and store it in the
    	// proband's variant for further sorting/display in the feature matrix.
    	function(variantA, variantB) {
    		variantA.motherZygosity = variantB.zygosity != null ? variantB.zygosity : '';
    		variantA.genotypeAltCountMother = variantB.genotypeAltCount;
		    variantA.genotypeRefCountMother = variantB.genotypeRefCount;
		    variantA.genotypeDepthMother    = variantB.genotypeDepth;
		    variantA.bamDepthMother         = variantB.bamDepth;

	        variantB.probandZygosity         = variantA.zygosity != null ? variantA.zygosity : '';
    		variantB.genotypeAltCountProband = variantA.genotypeAltCount;
		    variantB.genotypeRefCountProband = variantA.genotypeRefCount;
		    variantB.genotypeDepthProband    = variantA.genotypeDepth;
		    variantB.bamDepthProband         = variantA.bamDepth;
		}
	).then( function() {

		 // Compare the proband variants to the father's variants
		 return me.promiseCompareVariants(
		 	me.probandVcfData, 
		 	me.fatherVcfData,
	       	 // This is the attribute on variant a (proband) and variant b (father)
	        // that will store whether the variant is unique or matches.
	        'compareFather',
	    	// This is the callback function called every time we find the same variant
	    	// in both sets. Here we take the father variant's zygosity and store it in the
	    	// proband's variant for further sorting/display in the feature matrix.
	        function(variantA, variantB) {
	        	variantA.fatherZygosity = variantB.zygosity != null ? variantB.zygosity : '';
	        	variantA.genotypeAltCountFather = variantB.genotypeAltCount;
	        	variantA.genotypeRefCountFather = variantB.genotypeRefCount;
			    variantA.genotypeDepthFather    = variantB.genotypeDepth;
			    variantA.bamDepthFather         = variantB.bamDepth;

	        	variantB.probandZygosity         = variantA.zygosity != null ? variantA.zygosity : '';
	        	variantB.genotypeAltCountProband = variantA.genotypeAltCount;
	        	variantB.genotypeRefCountProband = variantA.genotypeRefCount;
			    variantB.genotypeDepthProband    = variantA.genotypeDepth;
			    variantB.bamDepthProband         = variantA.bamDepth;

	        });  	

	}, function(error) {
		console.log("error occured when comparing proband variants to mother?");
	}).then( function() {
		// This is the function that is called after the proband variants have been compared
	    // to the father variant set. 
	    
		// Fill in the inheritance mode. 
		// 1. recessive mode if mom and dad are het and proband is hom
		// 2  denovo proband has variant, but not present in mom and dad 
		//    (homref parents are also the same as variant not being present)
		me.probandVcfData.features.forEach(function(variant) {
			if (variant.zygosity != null && variant.zygosity.toLowerCase() == 'hom' 
				&& variant.motherZygosity != null && variant.motherZygosity.toLowerCase() == 'het' 
				&& variant.fatherZygosity != null && variant.fatherZygosity.toLowerCase() == 'het') {
				variant.inheritance = 'recessive';
			} else if ( (variant.compareMother == 'unique1' || (variant.compareMother == 'common' && variant.motherZygosity != null && variant.motherZygosity.toLowerCase() == 'homref'))
				     && (variant.compareFather == 'unique1' || (variant.compareFather == 'common' && variant.fatherZygosity != null && variant.fatherZygosity.toLowerCase() == 'homref'))) {
				variant.inheritance = 'denovo';
			} 
		});

		if (me.probandVcfData) {
			if (!me.probandVcfData.loadState) {
				me.probandVcfData.loadState = {};
			}
			me.probandVcfData.loadState['inheritance'] = true;
		}
		filterCard.enableInheritanceFilters(me.probandVcfData);

		callback(me.probandVcfData);
	},
	function(error) {
		console.log("error occured after comparison of proband to mother and father");
		
	});


	//
	// Now compare mother's variant to father's variants
	//
	me.promiseCompareVariants(
		me.motherVcfData,
		me.fatherVcfData,
	    // This is the attribute on variant a (proband) and variant b (mother)
		// that will store whether the variant is unique or matches.
    	'compareMotherFather',
    	// This is the callback function called every time we find the same variant
    	// in both sets. Here we take the mother variant's af and store it in the
    	// proband's variant for further sorting/display in the feature matrix.
    	function(variantMother, variantFather) {
    		variantMother.fatherZygosity = variantFather.zygosity != null ? variantFather.zygosity : '';
    		variantMother.genotypeAltCountFather = variantFather.genotypeAltCount;
		    variantMother.genotypeRefCountFather = variantFather.genotypeRefCount;
		    variantMother.genotypeDepthFather    = variantFather.genotypeDepth;
		    variantMother.bamDepthFahter         = variantFather.bamDepth;

	        variantFather.motherZygosity         = variantMother.zygosity != null ? variantMother.zygosity : '';
    		variantFather.genotypeAltCountMother = variantMother.genotypeAltCount;
		    variantFather.genotypeRefCountMother = variantMother.genotypeRefCount;
		    variantFather.genotypeDepthMother    = variantMother.genotypeDepth;
		    variantFather.bamDepthMother         = variantMother.bamDepth;
		}
	).then( function() {

	}, function(error) {
		console.log("error occured when comparing proband variants to mother?");
	})	

}

VariantTrioModel.prototype.promiseCompareVariants = function(vcfData, otherVcfData, compareAttribute, onMatchFunction, onNoMatchFunction ) {
	var me = this;

	return new Promise( function(resolve, reject) {

		

	    var set1Label = 'unique1';
	    var set2Label = 'unique2';
	    var commonLabel = 'common';
	    var comparisonAttribute = compareAttribute;
	    if (comparisonAttribute == null) {
	      comparisonAttribute = 'consensus';
	    }

	    otherVcfData.features = otherVcfData.features.sort(orderVariantsByPosition);
		if (comparisonAttribute) {
			otherVcfData.features.forEach( function(feature) {			
				feature[comparisonAttribute] = '';
			});			
		}

		variants1 = vcfData;
		variants2 = otherVcfData;

	    variants1.count = variants1.features.length;
	    variants2.count = variants2.features.length;

	    var features1 = variants1.features;
	    var features2 = variants2.features;

	    // Flag duplicates as this will throw off comparisons.  Also bypass homrefs
	    var flagDups = function(features) {
	      for (var i =0; i < features.length - 1; i++) {
	        var variant = features[i];

	        var nextVariant = features[i+1];
	        if (i == 0) {
	          variant.dup = false;
	        }
	        nextVariant.dup = false;

	        if (variant.start == nextVariant.start) {
	             var refAlt = variant.type.toLowerCase() + ' ' + variant.ref + "->" + variant.alt;
	             var nextRefAlt = nextVariant.type.toLowerCase() + ' ' + nextVariant.ref + "->" + nextVariant.alt;

	             if (refAlt == nextRefAlt) {
	                nextVariant.dup = true;
	             }
	        }
	      }
	    }

	    var flagHomRefs = function(features) {
	    	features.forEach(function(variant) {
	    		if (variant.zygosity != null && variant.zygosity.toLowerCase() == 'homref') {
	        		variant.ignore = true;
	        	} else {
	        		variant.ignore == false;
	        	}
	    	});
	    }
	    flagDups(features1);
	    flagDups(features2);
	    //flagHomRefs(features1);
	    //flagHomRefs(features2);


	    // Iterate through the variants from the first set,
	    // marking the consensus field based on whether a 
	    // matching variant from the second list is encountered.
	    var idx1 = 0;
	    var idx2 = 0;
	    while (idx1 < features1.length && idx2 < features2.length) {
	      // Bypass duplicates
	      if (features1[idx1].dup || features1[idx1].ignore) {
	        idx1++;
	      }
	      if (features2[idx2].dup || features2[idx2].ignore) {
	        idx2++;
	      }

	      if (idx1 >= features1.length || idx2 >= features2.length) {
	      	continue;
	      }

	      variant1 = features1[idx1];
	      variant2 = features2[idx2];
	      
	      var refAlt1 = variant1.type.toLowerCase() + ' ' + variant1.ref + "->" + variant1.alt;
	      var refAlt2 = variant2.type.toLowerCase() + ' ' + variant2.ref + "->" + variant2.alt;

	      if (variant1.start == variant2.start) {

	        if (refAlt1 == refAlt2) {
	          variant1[comparisonAttribute] =  commonLabel;
	          variant2[comparisonAttribute] =  commonLabel;

	          if (onMatchFunction) {
	            onMatchFunction(variant1, variant2);
	          }
	          idx1++;
	          idx2++;
	        } else if (refAlt1 < refAlt2) {
	          variant1[comparisonAttribute] = set1Label;
	          if (onNoMatchFunction) {
	            onNoMatchFunction(variant1, null);
	          }
	          idx1++;
	        } else {
	          variant2[comparisonAttribute] = set2Label;
	          if (onNoMatchFunction) {
	            onNoMatchFunction(null, variant2);
	          }
	          idx2++;
	        }
	      } else if (variant1.start < variant2.start) {
	        variant1[comparisonAttribute] = set1Label;
	        if (onNoMatchFunction) {
	            onNoMatchFunction(variant1, null);
	        }
	        idx1++;
	      } else if (variant2.start < variant1.start) {
	        variant2[comparisonAttribute] = set2Label;
	        if (onNoMatchFunction) {
	            onNoMatchFunction(null, variant2);
	        }
	        idx2++;
	      }

	    }


	    // If we get to the end of one set before the other,
	    // mark the remaining as unique
	    //
	    if (idx1 < features1.length) {
	      for(x = idx1; x < features1.length; x++) {
	        var variant1 = features1[x];
	        variant1[comparisonAttribute] = set1Label;
	        if (onNoMatchFunction) {
	            onNoMatchFunction(variant1, null);
	        }
	      }
	    } 
	    if (idx2 < features2.length) {
	      for(x = idx2; x < features2.length; x++) {
	        var variant2 = features2[x];
	        variant2[comparisonAttribute] = set2Label;
	        if (onNoMatchFunction) {
	            onNoMatchFunction(null, variant2);
	        }        
	      }
	    } 


		resolve();	
	
	});


}



// TODO:  Need to load unaffected sibs vcf data (variantModel.loadVariantDataOnly) beforehand
VariantTrioModel.prototype.determineSibsStatus = function(sibsVcfData, affectedStatus, sibsCount, onUpdate) {
	var me = this;
	me.sibsVcfData = sibsVcfData;

	me.probandVcfData.features = me.probandVcfData.features.sort(orderVariantsByPosition);

	me.sibsTransientVcfData = [];
	me.sibsVcfData.forEach( function(vcfData) {
		me.sibsTransientVcfData.push(vcfData);
	})

	var sibZygosityAttr = affectedStatus + "_zygosity";
	var affectedAttr    = affectedStatus + "Sibs";
	me.nextCompareSib(affectedStatus, sibZygosityAttr, function() {

		me.probandVcfData.features.forEach( function(variant) {
			 variant[affectedAttr] = "none";
			 if (variant[sibZygosityAttr]) {
			 	 var matchesCount = 0;
			 	 var matchesHomCount = 0;
				 Object.keys(variant[sibZygosityAttr]).forEach( function(key) {
				 	var sibZygosity = variant[sibZygosityAttr][key];
					if (sibZygosity != null) {
						if (sibZygosity.toLowerCase() != 'none') {
						 	matchesCount++;
						}
					 	if (sibZygosity.toLowerCase() == 'hom' && variant.inheritance.toLowerCase() == 'recessive') {
						 	matchesHomCount++;
					 	}
				 	}
				 });

				 if (variant.inheritance.toLowerCase() == 'recessive'
				 	&& matchesHomCount > 0 
				 	&& matchesHomCount == sibsCount) { 
				 	variant[affectedAttr] = "recessive_all";
				 } else if (variant.inheritance.toLowerCase() == 'recessive'
				 	      && matchesHomCount > 0 )  {
				 	variant[affectedAttr] = "recessive_some";
				 } else if (variant.inheritance.toLowerCase() == 'recessive' && affectedStatus == 'unaffected')  {
				 	variant[affectedAttr] = "recessive_none";
				 } else if (matchesCount > 0 && matchesCount == sibsCount) {
				 	variant[affectedAttr] = "present_all";
				 }  else if (matchesCount > 0) {
				 	variant[affectedAttr] = "present_some"
				 }  else {
				 	variant[affectedAttr] = "present_none";
				 }  	 	 
			 } 
		});

		if (onUpdate) {
			onUpdate();
		}

	});

}

VariantTrioModel.prototype.nextCompareSib = function(affectedStatus, zygosityAttr, callback) {
	var me = this;
	if (me.sibsTransientVcfData.length > 0) {
		var vcfData = me.sibsTransientVcfData.shift();
		me.promiseCompareToSib(vcfData, zygosityAttr).then( function() {
			me.nextCompareSib(affectedStatus, zygosityAttr, callback);
		});		
	} else {
		callback();
	} 
}


VariantTrioModel.prototype.promiseCompareToSib = function(sibVcfData, zygosityAttr) {
	var me = this;
	
	return new Promise( function(resolve, reject) {

		me.probandVcfData.features.forEach(function(variant) {
			if (variant.uasibsZygosity) {
				variant[zygosityAttr][unaffectedSibVcfData.name] = "none";		
			} else {
				variant[zygosityAttr] = {};
			}
		});
				
		var idx = 0;
		me.promiseCompareVariants(
			me.probandVcfData,	
			sibVcfData,		
			// This is the attribute on variant a (proband) and variant b (unaffected sib)
	        // that will store whether the variant is unique or matches.
	        null,	        
	    	// This is the callback function called every time we find the same variant
	    	// in both sets. Here we take the father variant's zygosity and store it in the
	    	// proband's variant for further sorting/display in the feature matrix.
	        function(variantA, variantB) {
	        	variantA[zygosityAttr][sibVcfData.name] = variantB.zygosity;
	        },
	        function(variantA, variantB) {
	        	if (variantA) {
	        		variantA[zygosityAttr][sibVcfData.name] = "none";
	        	}
	        }
	     ).then( function() {
	     	resolve();
	     });

	});




}




