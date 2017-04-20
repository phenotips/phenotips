function BookmarkCard() {
	this.bookmarkedVariants = {};
	this.bookmarkedGenes = {};
	this.selectedBookmarkKey = null;
	this.favorites = {};

}

BookmarkCard.prototype.init = function() {
	var me = this;
	// Stop event propogation to get genes dropdown
	// so that clicks in text area for copy/paste
	// don't cause dropdown to close
	$('#import-bookmarks-dropdown ul li#copy-paste-li').on('click', function(event){
	    //The event won't be propagated to the document NODE and 
	    // therefore events delegated to document won't be fired
	    event.stopPropagation();
	});
	// Enter in copy/paste textarea should function as newline
	$('#import-bookmarks-dropdown ul li#copy-paste-li').keyup(function(event){

		if((event.which== 13) && ($(event.target)[0]== $("textarea#bookmarks-to-import")[0])) {
			event.stopPropagation();
		}
	});	
	// Detect when get genes dropdown opens so that
	// we can prime the textarea with the genes already
	// selected
	$('#import-bookmarks-dropdown').click(function () {
	    if($(this).hasClass('open')) {
	        // dropdown just closed
	    } else {
	    	// dropdown will open
	    	me.initImportBookmarks();
	    	setTimeout(function() {
			  $('#bookmarks-to-import').focus();
			}, 0);
	    	
	    }
	});
}
BookmarkCard.prototype.initImportBookmarks = function() {

}

BookmarkCard.prototype.importBookmarks = function() {
	var me = this;

	//chrom	start	end	ref	alt	gene
	var bookmarksString = $('#bookmarks-to-import').val();
	// trim newline at very end
	bookmarksString = bookmarksString.replace(/\s*$/, "");

	
	me.bookmarkedVariants = {};
	var recs = bookmarksString.split("\n");
	recs.forEach( function(rec) {
		var fields = rec.split(/\s+/);

		if (fields.length >= 5) {
			var chrom    = fields[0];
			var start    = +fields[1];
			var end      = +fields[2];
			var ref      = fields[3];
			var alt      = fields[4];
			var geneName = fields[5];

			
			var key = me.getBookmarkKey(geneName, chrom, start, ref, alt);
			if (me.bookmarkedVariants[key] == null) {
				me.bookmarkedVariants[key] = {isProxy: true, gene: gene, chrom: chrom, start: +start, end: +end, ref: ref, alt: alt};
			}

		}
	});

	showSidebar("Bookmarks");
	

	// Get the phenotypes for each of the bookmarked genes
	var promises = []
	for (var geneName in me.bookmarkedGenes) {
		var promise = genesCard.promiseGetGenePhenotypes(geneName).then(function() {
			Promise.all(promises).then(function() {
				me.refreshBookmarkList();
			});
		});
		promises.push(promise);
	}	

	// Add the bookmarked genes to the gene buttons
	genesCard.refreshBookmarkedGenes(me.bookmarkedGenes);

	$('#import-bookmarks-dropdown .btn-group').removeClass('open');	

}

BookmarkCard.prototype.reviseCoord = function(bookmarkEntry, gene) {
	var me = this;
	var revisedBookmarkEntry = $().extend({}, bookmarkEntry);

	// TODO:  Figure out coordinate space for GEMINI
	revisedBookmarkEntry.start++;

	// TODO: If gene is reverse strand, change ref alt to compliment
	if (gene.strand == "-") {
		//revisedBookmarkEntry.alt = me.reverseBases(bookmarkEntry.alt);
		//revisedBookmarkEntry.ref = me.reverseBases(bookmarkEntry.ref);
	}

	// TODO:  Normalize since variants are normalized

	return revisedBookmarkEntry;
}

BookmarkCard.prototype.reverseBases = function(bases) {
	var reversedBases = "";
	for (var i = 0; i < bases.length; i++) {
    	var base = bases.charAt(i);
    	var rb = null;
    	if (base == 'A') {
    		rb = 'T';
    	} else if (base == 'T') {
    		rb = 'A';
    	} else if (base == 'C') {
    		rb = 'G';
    	} else if (base == 'G') {
    		rb = 'C';
    	}
    	reversedBases += rb;
    }
    return reversedBases;
}

BookmarkCard.prototype.bookmarkVariant = function(variant) {
	var me = this;
	if (variant) {		
		var key = this.getBookmarkKey(gene.gene_name, gene.chr, variant.start, variant.ref, variant.alt);
		if (this.bookmarkedVariants[key] == null) {
			this.bookmarkedVariants[key] = variant;
			//getProbandVariantCard().unpin();
			getProbandVariantCard().addBookmarkFlag(variant, me.compressKey(key), false);
			matrixCard.addBookmarkFlag(variant);
			variant.isBookmark = 'Y';

			var keys = me.bookmarkedGenes[gene.gene_name];
	    	if (keys == null) {
	    		keys = [];
	    	}
	    	keys.push(key);
	    	me.bookmarkedGenes[gene.gene_name] = keys;
		}
		genesCard.setBookmarkBadge(gene.gene_name);
	}
}

BookmarkCard.prototype.removeBookmark = function(key, variant) {
	var me = this;
	variant.isBookmark = 'N';
	getProbandVariantCard().removeBookmarkFlag(variant, this.compressKey(key));
	matrixCard.removeBookmarkFlag(variant);


	var geneName = key.split(": ")[0];
	delete this.bookmarkedVariants[key];

	var bookmarkKeys = me.bookmarkedGenes[geneName];
	var index = bookmarkKeys.indexOf(key);
	if (index >= 0) {
		bookmarkKeys.splice(index, 1);
	}
	if (bookmarkKeys.length == 0) {
		delete this.bookmarkedGenes[geneName];
		genesCard.setBookmarkBadge(geneName);
	}

	this.refreshBookmarkList();


}

BookmarkCard.prototype.getBookmarkKey = function(geneName, chrom, start, ref, alt) {
	return geneName + ": " 
         + chrom + " " + start  
         + " " + ref + "->" + alt;         
}

BookmarkCard.prototype.compressKey = function(bookmarkKey) {
	bookmarkKey = bookmarkKey.split(": ").join("-");
	bookmarkKey = bookmarkKey.split("->").join("-");
	bookmarkKey = bookmarkKey.split(" ").join("-");
	bookmarkKey = bookmarkKey.split(".").join("-");
	return bookmarkKey;
}

BookmarkCard.prototype.determineVariantBookmarks = function(vcfData, geneObject) {
	var me = this;
	if (vcfData && vcfData.features) {
		var bookmarkKeys = me.bookmarkedGenes[geneObject.gene_name];
		if (bookmarkKeys && bookmarkKeys.length > 0) {
			bookmarkKeys.forEach( function(bookmarkKey) {
				var bookmarkEntry = me.bookmarkedVariants[bookmarkKey];
				var theBookmarkEntry = bookmarkEntry.hasOwnProperty("isProxy") ? me.reviseCoord(bookmarkEntry, geneObject) : bookmarkEntry;
				var variant = getProbandVariantCard().getBookmarkedVariant(theBookmarkEntry, vcfData);
				if (variant) {
					variant.isBookmark = 'Y';
					me.bookmarkedVariants[bookmarkKey] = variant;
				}
			});
		}
		me.refreshBookmarkList();
	}
}



/*
 This method is called when a gene is selected and the variants are shown.
*/
BookmarkCard.prototype.flagBookmarks = function(variantCard, geneObject) {
	var me = this;
	var bookmarkKeys = me.bookmarkedGenes[geneObject.gene_name];
	if (bookmarkKeys) {
		me._flagBookmarksForGene(variantCard, geneObject, bookmarkKeys, true);
	}

}

BookmarkCard.prototype._flagBookmark = function(variantCard, geneObject, variant, bookmarkKey) {
	var me = this;
	
	// Flag the bookmarked variant
	if (variant) {
		variant.isBookmark = 'Y';
		variantCard.addBookmarkFlag(variant, me.compressKey(bookmarkKey), true);

		matrixCard.highlightVariant(variant, true);

		// Now that we have resolved the bookmark entries for a gene, refresh the
		// bookmark list so that the glyphs show for each resolved bookmark.
		me.refreshBookmarkList();

	}
}

BookmarkCard.prototype._flagBookmarksForGene = function(variantCard, geneObject, bookmarkKeys, displayVariantFlag) {
	var me = this;
	
	// Now flag all other bookmarked variants for a gene
	bookmarkKeys.forEach( function(key) {		
		var theBookmarkEntry = me.bookmarkedVariants[key];
		var theVariant = me.resolveBookmarkedVariant(key, theBookmarkEntry, geneObject);
		if (theVariant) {
			theVariant.isBookmark = 'Y';
			if (displayVariantFlag) {
				variantCard.addBookmarkFlag(theVariant, me.compressKey(key), false);		
			}

		}
	});

	// Now that we have resolved the bookmark entries for a gene, refresh the
	// bookmark list so that the glyphs show for each resolved bookmark.
	//me.refreshBookmarkList();
	me.refreshBookmarkList();

	
}

BookmarkCard.prototype.resolveBookmarkedVariant = function(key, bookmarkEntry, geneObject) {
	var me = this;

	var variant = null;
	if (bookmarkEntry.hasOwnProperty("isProxy")) {
		variant = getProbandVariantCard().getBookmarkedVariant(me.reviseCoord(bookmarkEntry, geneObject));
		if (variant) {
			variant.isBookmark = "Y";
			variant.chrom = bookmarkEntry.chrom;
			me.bookmarkedVariants[key] = variant;
			bookmarkEntry = variant;									
		} 
	} else {
		variant = bookmarkEntry;
		variant.isBookmark = "Y";
	}
	return variant;
}

BookmarkCard.prototype.sortBookmarksByGene = function() {
	var me = this;
    var tuples = [];

    for (var key in me.bookmarkedVariants) {
    	tuples.push([key, me.bookmarkedVariants[key]]);
	}

    tuples.sort(function(a, b) { 
    	var keyA    = a[0];
    	var keyB    = b[0];
    	var startA  = a[1].start;
    	var startB  = b[1].start;
    	var refAltA = a[1].ref + "->" + a[1].alt;
    	var refAltB = b[1].ref + "->" + b[1].alt;

    	var geneA = keyA.split(": ")[0];
    	var geneB = keyB.split(": ")[0];

    	if (geneA == geneB) {
    		if (startA == startB) {
				return refAltA < refAltB ? 1 : refAltA > refAltB ? -1 : 0;
    		} else {
	    		return startA < startB ? 1 : startA > startB ? -1 : 0;
    		}
    	} else {
	    	return geneA < geneB ? 1 : geneA > geneB ? -1 : 0;
    	}
    });

    var length = tuples.length;
    var sortedBookmarks = {};
    me.bookmarkedGenes = {};
    while (length--) {
    	var key   = tuples[length][0];
    	var value = tuples[length][1];
    	var geneName = key.split(": ")[0];
    	
    	sortedBookmarks[key] = value;

    	var keys = me.bookmarkedGenes[geneName];
    	if (keys == null) {
    		keys = [];
    	}
    	keys.push(key);
    	me.bookmarkedGenes[geneName] = keys;
    }

    me.bookmarkedVariants = sortedBookmarks;
}

BookmarkCard.prototype.isBookmarkedGene = function(geneName) {
	return this.bookmarkedGenes[geneName] != null;
}


BookmarkCard.prototype.refreshBookmarkList = function() {
	var me = this;
	var container = d3.select('#bookmark-card #bookmark-panel');
	container.selectAll('.bookmark-gene').remove();

	if (Object.keys(this.bookmarkedVariants).length > 0) {
		$('#edit-bookmarks-box').removeClass("hide");
	} else {
		$('#edit-bookmarks-box').addClass("hide");
		$('#bookmark-panel').removeClass("editmode");
		$('#bookmark-card #done-edit-bookmarks').addClass("hide");
		$('#bookmark-card #edit-bookmarks').removeClass("hide");
	}

	// Sort bookmarks by gene, then start position
	me.sortBookmarksByGene();


	container.selectAll(".bookmark-gene")
	         .data(d3.entries(this.bookmarkedGenes))
	         .enter()
	         .append("div")
	         .attr("class", "bookmark-gene")
	         .append("a")
	         .attr("class", "bookmark-gene")
	         .text(function(entry,i) {
	         	var geneName = entry.key;
	         	var entryKeys = entry.value;
	         	var parts = entryKeys[0].split(": ");
	         	var chr = parts[1].split(" ")[0];
	         	if (chr.indexOf("chr") < 0) {
	         		chr = "chr " + chr;
	         	}
	         	return  geneName + " " + chr;
	         })
	         .on('click', function(entry,i) {
	         	d3.select('#bookmark-card #bookmark-panel a.current').classed("current", false);
	         	d3.select(this).classed("current", true);

	         	me.selectedBookmarkKey = entry.key;

				var geneName = entry.key;
				var bookmarkKeys = entry.value;

				// Remove any locked tooltip, hide coordinate frame
				unpinAll();

				if (window.gene.gene_name != geneName || !getProbandVariantCard().isLoaded()) {
					genesCard.selectGene(geneName, function(variantCard) {
						if (variantCard.getRelationship() == 'proband') {
							me._flagBookmarksForGene(variantCard, window.gene, bookmarkKeys, true);
						}
					});
				} else {
					me._flagBookmarksForGene(getProbandVariantCard(), window.gene, bookmarkKeys, true);
				}				
			});

	me.addPhenotypeGlyphs(container);


	container.selectAll("div.bookmark-gene")
	         .each( function(entry, i) {
	         	var geneContainer = d3.select(this);

	         	var keys = entry.value;
	         	var bookmarkedVariantsForGene = {};
	         	keys.forEach( function(key) {
	         		bookmarkedVariantsForGene[key] = me.bookmarkedVariants[key];
	         	});

	         	var bookmark = geneContainer.selectAll("div.bookmark-box")
	         	     .data(d3.entries(bookmarkedVariantsForGene))
	         	     .enter()
	         	     .append("div")
	         	     .attr("class", "bookmark-box");

			    bookmark.append("a")
			            .attr("class", "bookmark")
			            .on('click', function(entry,i) {
				         	d3.select('#bookmark-card #bookmark-panel a.current').classed("current", false);
		         			d3.select(this).classed("current", true);

		         			me.selectedBookmarkKey = entry.key;

				         	var geneName = entry.key.split(": ")[0];
							var bookmarkEntry = entry.value;
							var key = entry.key;

							// Remove any locked tooltip, hide coordinate frame
							unpinAll();


							if (window.gene.gene_name != geneName  || !getProbandVariantCard().isLoaded()) {
								genesCard.selectGene(geneName, function(variantCard) {
									if (variantCard.getRelationship() == 'proband') {
										var variant = me.resolveBookmarkedVariant(key, bookmarkEntry, window.gene);
										me._flagBookmark(variantCard, window.gene, variant, key);
									}
								});
							} else {
								var variant = me.resolveBookmarkedVariant(key, bookmarkEntry, window.gene);					
								me._flagBookmark(getProbandVariantCard(), window.gene, variant, key);
							}

							
			            });
	        });
			
	


	container.selectAll(".bookmark")
	 		 .append("span")
	         .attr("class", "variant-symbols");

	 container.selectAll(".bookmark")
	 		 .append("span")
	         .attr("class", "variant-label");

	 container.selectAll(".bookmark")
	 		 .append("span")
	         .attr("class", "favorite-indicator")
	         .style("float", "right");
	        
	container.selectAll(".bookmark span.variant-label")
	         .text(function(entry,i) {	
	         	var key = entry.key;
	         	var bookmarkEntry = entry.value;

	         	var rsId = getRsId(bookmarkEntry);

				// Strip off gene name and chr
				var tokens = key.split(": ")[1].split(" ");
	         	return tokens[1] + " " + tokens[2] + (rsId ? " " + rsId : "");
	         });

	container.selectAll(".bookmark .variant-symbols")
         .each( function(entry, i) {
		    var selection = d3.select(this);
         	var variant = entry.value;	   
         	var impactField = filterCard.getAnnotationScheme().toLowerCase() == 'snpeff' ? 'impact' : IMPACT_FIELD_TO_COLOR;      
         	if (variant[impactField]) {
	         	for (var impact in variant[impactField]) {		         		
         			var svg = selection.append("svg")
								       .attr("class", "impact-badge")
								       .attr("height", 12)
								       .attr("width", 14);
		         	var impactClazz =  'impact_' + impact.toUpperCase();
		         	matrixCard.showImpactBadge(svg, variant, impactClazz);	         		
	         	}	         		
         	}
         	if (variant.clinVarClinicalSignificance) {
         		var lowestValue = 9999;
         		var lowestClazz = null; 
         		for (var clinvar in variant.clinVarClinicalSignificance) {
         			if (matrixCard.clinvarMap[clinvar]) {
         				if (matrixCard.clinvarMap[clinvar].value < lowestValue) {
         					lowestValue = matrixCard.clinvarMap[clinvar].value;
         					lowestClazz = matrixCard.clinvarMap[clinvar].clazz;
         				}
         				
         			}
         		}
         		if (lowestClazz != null && lowestClazz != '') {
					var options = {width:10, height:10, transform: 'translate(0,1)', clazz: lowestClazz};
					var svg = selection.append("svg")
								       .attr("class", "clinvar-badge")
								       .attr("height", 12)
								       .attr("width", 14);
			        matrixCard.showClinVarSymbol(svg, options);	         		
     			}

         	}
         	if (variant.vepSIFT) {
				for (var sift in variant.vepSIFT) {
					if (matrixCard.siftMap[sift]) {
		         		var clazz = matrixCard.siftMap[sift].clazz;
		         		var badge = matrixCard.siftMap[sift].badge;
		         		if (clazz != '') {
							var options = {width:11, height:11, transform: 'translate(0,1)', clazz: clazz};
							var svg = selection.append("svg")
								        .attr("class", "sift-badge")
								        .attr("height", 12)
								        .attr("width", 14);
					        matrixCard.showSiftSymbol(svg, options);	         		
		         		}							
					}

         		}
         	}
         	if (variant.vepPolyPhen) {
				for (var polyphen in variant.vepPolyPhen) {
					if (matrixCard.polyphenMap[polyphen]) {
		         		var clazz = matrixCard.polyphenMap[polyphen].clazz;
		         		var badge = matrixCard.polyphenMap[polyphen].badge;
		         		if (clazz != '') {
							var options = {width:10, height:10, transform: 'translate(0,2)', clazz: clazz};
							var svg = selection.append("svg")
								        .attr("class", "polyphen-badge")
								        .attr("height", 12)
								        .attr("width", 14);
					        matrixCard.showPolyPhenSymbol(svg, options);	         		
		         		}
					}
         		}
         	}
         	if (variant.inheritance) {
         		if (variant.inheritance == 'recessive') {
					var svg = selection.append("svg")
								        .attr("class", "inheritance-badge")
								        .attr("height", 14)
								        .attr("width", 16);
					var options = {width: 18, height: 16, transform: "translate(-1,1)"};
					matrixCard.showRecessiveSymbol(svg, options);										        
         		} else if (variant.inheritance == 'denovo') {
					var svg = selection.append("svg")
								        .attr("class", "inheritance-badge")
								        .attr("height", 14)
								        .attr("width", 16);
					var options = {width: 18, height: 16, transform: "translate(-1,1)"};
					matrixCard.showDeNovoSymbol(svg, options);				
         		}
         	}
         });


	container.selectAll(".bookmark-box .favorite-indicator")
         .each( function(entry, i) {
		    var selection = d3.select(this);
         	var variant = entry.value;	         


 			// favorite badge
 			var svg = selection.append("svg")
						       .attr("class", "favorite-badge")
						       .attr("height", 15)
						       .attr("width", 14);

			svg.on('click', function(d,i) {

					var selected = me.favorites.hasOwnProperty(d.key) && me.favorites[d.key] == true;
					var fill = selected ? "none" : "gold";
			   		d3.select(this).select("#favorite-badge").style("fill", fill);
			   		me.favorites[entry.key] = !selected;
			   });
			var group = svg.append("g")
			   .attr("transform", "translate(0,1)");
			group.append("use")
			   .attr("xlink:href", "#star-symbol")
			   .attr("id", "favorite-badge")
			   .attr("width", 14)
			   .attr("height", 14)
			   .style("fill", function(d,i) {
			   		var isFavorite = me.favorites.hasOwnProperty(d.key) && me.favorites[d.key] == true;
					var fill = isFavorite ? "gold" : "none";
					return fill;
			   })
			   .style("pointer-events", "none");
		
			// remove button
			var removeIcon = selection.append("i")
         	         .attr("class", "material-icons")
         	         .attr("id", "remove-bookmark")
         	         .text("close");  

 			removeIcon.on('click', function(d,i) {
					d3.event.stopPropagation(); 				
 					me.removeBookmark(d.key, d.value);
			   });

         });

		d3.selectAll('#bookmark-card a.bookmark-gene, #bookmark-card a.bookmark')
		  .filter(function(d,i) {  
		  	return d.key == me.selectedBookmarkKey; 
		  })
		  .classed("current", true);


}

BookmarkCard.prototype.showTooltip = function(html, screenX, screenY, width) {
	var me = this;

	var tooltip = d3.select('#bookmark-gene-tooltip');
	tooltip.style("z-index", 1032);
	tooltip.transition()        
	 .duration(1000)      
	 .style("opacity", .9)	
	 .style("pointer-events", "all");

	tooltip.html(html);
	var h = tooltip[0][0].offsetHeight;
	var w = width;

	var x = screenX + 30;
	var y = screenY;

	if (window.outerHeight < y + h + 30) {
		tooltip.style("width", w + "px")
			       .style("left", x + "px") 
			       .style("text-align", 'left')    
			       .style("top", (y - h) + "px")
			       .style("z-index", 1032)
			       .style("overflow-y", "hidden");
			       

	} else {
		tooltip.style("width", w + "px")
			       .style("left", x + "px") 
			       .style("text-align", 'left')    
			       .style("top", (y) + "px")
			       .style("z-index", 1032)
			       .style("overflow-y", "hidden");

	}


}

BookmarkCard.prototype.hideTooltip = function() {
	var tooltip = d3.select('#bookmark-gene-tooltip');
	tooltip.transition()        
           .duration(500)      
           .style("opacity", 0)
           .style("z-index", 0)
           .style("pointer-events", "none");
}


BookmarkCard.prototype._setPhenotypeGlyph = function(container, geneName) {
	var me = this;
	genesCard.promiseGetGenePhenotypes(geneName).then(function(data) {

			var phenotypes = data[0];
			var theGeneName = data[1];
			if (theGeneName != null && phenotypes != null && phenotypes.length > 0) {
				$(container.node()).append("<svg class=\"phenotype-badge\" height=\"16\" width=\"16\">");
				var selection = container.select(".phenotype-badge").data([{width:12, height:12,clazz: 'phenotype', translate: 'translate(0,5)', phenotypes: phenotypes}]);
				matrixCard.showPhenotypeSymbol(selection);	
				selection.on("mouseover", function(d,i) {
					
					var symbol = d3.select(this);
					var matrix = symbol.node()
                         .getScreenCTM()
                         .translate(+symbol.node().getAttribute("cx"),+symbol.node().getAttribute("cy"));
		            var screenX = window.pageXOffset + matrix.e - 20;
		            var screenY = window.pageYOffset + matrix.f + 5;
		            
					var htmlObject = genesCard.formatPhenotypesHTML(d.phenotypes);
					me.showTooltip(htmlObject.html, screenX, screenY, htmlObject.width);	
						
				});
				selection.on("mouseout", function(d,i) {
					me.hideTooltip();
				});	
			}
		});	
}

BookmarkCard.prototype.addPhenotypeGlyphs = function(container) {
	var me = this;

	var phenotypesContainer = container.selectAll("div.bookmark-gene")
	    .append("span")
	    .attr("class", "phenotypes");

	phenotypesContainer.each( function(entry, i) {
		var container = d3.select(this);
		var geneName = entry.key;
		me._setPhenotypeGlyph(container, geneName);

	});
}


BookmarkCard.prototype.addPhenotypeList = function(container) {
	     var phenotypesContainer = container.selectAll("div.bookmark-gene")
	        .append("div")
	        .attr("class", function(entry, i) {
	        	var geneName = entry.key;
	        	var html = "";
	        	var phenotypes = genePhenotypes[geneName];
	        	if (phenotypes == null || phenotypes.length == 0) {
	        		return "phenotypes-container hide";
	        	} else if (phenotypes.length > 6) {
	        		return "phenotypes-container large";
	        	}else {
	        		return "phenotypes-container";
	        	}
	        });

	     phenotypesContainer.append("div")
	        .attr("class", "phenotypes")
	        .html( function(entry,i) {
	        	var geneName = entry.key;
	        	var html = "";
	        	var phenotypes = genePhenotypes[geneName];
	        	if (phenotypes && phenotypes.length > 0) {
	        		phenotypes.forEach(function(phenotype) {
	        			
	        			html += "<div>" + phenotype.hpo_term_name + "</div>";

	        		});

	        	}
	        	return html;
	        });
	      var expander = phenotypesContainer.append("div")
	                                        .attr("class", "expander");
	      expander.append("a")
	              .attr("id", "more")
	              .text("more...")
	              .on("click", function(entry, i) {
	              	d3.select(this.parentNode.parentNode).classed("expand", true);
	              });
	      expander.append("a")
	              .attr("id", "less")
	              .text("less...")
	              .on("click", function(entry, i) {
	              	d3.select(this.parentNode.parentNode).classed("expand", false);
	              });	
}

BookmarkCard.prototype.exportBookmarks = function(scope) {
	var me = this;
	var output = "chrom" + "\t" + "start" + "\t" + "end" + "\t" + "ref" + "\t" + "alt" + "\t" + "gene" + "\t" + "impact" + "\t" + "inheritance mode" +"\n";

	for (key in this.bookmarkedVariants) {
		var entry = me.bookmarkedVariants[key];	
		var isFavorite = me.favorites[key];	
		var geneName = key.split(": ")[0];
		var impact = "";
		if (!entry.hasOwnProperty("isProxy")) {
			if (Object.keys(entry.effect).length > 0) {
				impact = Object.keys(entry.effect).join(",");
			} else if (Object.keys(entry.vepConsequence).length > 0) {				
				impact = Object.keys(entry.vepConsequence).join(",");
			}
		}
		var inheritance = entry.hasOwnProperty("isProxy") ? "" : entry.inheritance;
		if (scope == "all" || isFavorite) {
			var start = entry.start - 1;
			var end   = entry.end - 1;
			output += entry.chrom + "\t" + start + "\t" + end + "\t" + entry.ref + "\t" + entry.alt + "\t" + geneName + "\t" + impact + "\t" + inheritance + "\n";
		}
	}


	window.open('about:blank').document.body.innerText += output;
}

