var pageGuide = null;
var pageGuideBookmarks = null;
var pageGuidePhenolyzer = null;
var pageGuideEduTour1 = null;
var pageGuideEduTour2 = null;
var edgeObject = null;

var mainTourSteps = {
	'#enter-gene-name': {},
	'#gene-track': {},
	'#add-data-button': {},
	'#vcf-track': { 
		position: {top: '100px', bottom: 'initial'} 
	},
	'#bam-depth': { 
		position: {top: '100px', bottom: 'initial'} 
	},
	'#matrix-track': {},
	'#transcript-btn-group': {},
	'#set-gene-source': {},
	'#gene-viz': {},
	'#gene-region-buffer-input': {},
	'#call-variants-link': {},
	'#button-call-variants': {},
	'#proband-variant-card #fb-variants': { 
		position: {top: '100px', bottom: 'initial'} 
	},
	'#show-filters-link': {},
	'#filter-track svg#MODERATE': {},
	'#proband-variant-card #displayed-variant-count': { 
		position: {top: '100px', bottom: 'initial'} 
	},
	'#proband-variant-card #vcf-track #vcf-variants svg g.group g.snp': { 
		position: {top: '100px', bottom: 'initial'} 
	},
	'#vcf-variants .tooltip': { 
		position: {top: '100px', bottom: 'initial'} 
	},
	"#vcf-variants .tooltip #bookmarkLink": { 
		position: {top: '100px', bottom: 'initial'} 
	},
	'#show-bookmarks-link': {},
	'#bookmark-card #bookmark-panel': {},
	'#bookmark-card .favorite-indicator': {},
	'#show-select-genes-link': {},
	'#help-link': {}
}

var bookmarkTourSteps = {
	'#proband-variant-card #vcf-track': {
		position: {top: '100px', bottom: 'initial'} 
	}
}

var eduTour1Steps = {
	'#edu-tour-label':                                  {index: 0, first: true, noElement: true, disableTourButtons: true,
		audio: '#tour1-recording1',
		height: 'full', 		
		animation: {
			name: 'use-case01-scene01-v2', 
			clazz: 'EDGE-462912531',
		    width: "1200px",
		    height: "468px",
			container: "animation-container-1", 
			showFunction: showEduTourAnimation, 
			delay: 0}
		},
	'#phenolyzer-search-box .selectize-control.single': {index: 1, height: '20px', disableNext: true, correct: false, disableTourButtons: true},
	'#phenolyzer-results':                              {index: 2, disableTourButtons: true, 
		audio: '#tour1-recording2'
	},
	'#proband-variant-card #zoom-region-chart':         {index: 3, audio: '#tour1-recording3', height: '0px', disableTourButtons: true},
	'#gene-badge-container':                            {index: 4, disableNext: true, correct: false, disableTourButtons: true},
	'#edu-tour-1 #start-over':                          {index: 5, audio: '#tour1-recording4', noElement: true, disableTourButtons: true},
	'#children-buttons':                                {index: 6, disableNext: true, height: '100px', correct: false, disableTourButtons: false},
	'.edu-tour-1-child-buttons':                        {index: 7, close: true, noElement: true, disableTourButtons: false,
		audio: '#tour1-recording5', 
		height: 'full', 		
		animation: {
			name: 'use-case01-scene07-v1', 
			clazz: 'EDGE-462912531',
		    width: "1200px",
		    height: "468px",
			container: "animation-container-1", 
			showFunction: showEduTourAnimation, 
			delay: 0}
		}	
};

var eduTour2Steps = {
	'#edu-tour-2-label': { index: 0, first: true, noElement: true, audio: '#tour2-recording1', disableTourButtons: true,
	    height: 'full',
		animation: {
			name: 'use-case02-scene01-v1', 
			clazz: 'EDGE-462912531',
			width: "1200px",
		    height: "468px",
			container: "animation-container-1", 
			showFunction: showEduTourAnimation, 
			delay: 0
		}
	},
	'#edu-tour-2 #start-over':       {index: 1, noElement: true, audio: '#tour2-recording2', disableTourButtons: true,
	    height: 'full',
		animation: {
					name: 'use-case02-scene02-v1', 
					clazz: 'EDGE-462912531',
					width: "1200px",
				    height: "468px",
					container: "animation-container-1", 
					showFunction: showEduTourAnimation, 
					delay: 0
				}
		},
	'#proband-variant-card #vcf-track':      {index: 2, noElement: true, disableTourButtons: true},
	'#child-buttons-tour2':          {index: 3, disableNext: true, height: '120px', correct: false, disableTourButtons: false},
	'#edu-tour-2':                   {index: 4, noElement: true, audio: '#tour2-recording3', close: true, disableTourButtons: false}
};


function initializeTours() {
    if (!isLevelEdu) {
	    // Initialize app tour
		pageGuide = tl.pg.init({ 
			'auto_refresh': true, 
			'show_numbers': false,
			'close_button_text': 'X',
            'default_zindex': 1031,
			'custom_open_button': '#show-main-tour',
			'steps_element': '#tourMain',
			'track_events_cb': function(interactionName) {
				
			},
			'handle_doc_switch': function(currentTour, prevTour) {

				var step = mainTourSteps[currentTour];
				if (step) {
					customizeStep(pageGuide, step);
				} else {
					$('#tlyPageGuideMessages').css("top", "initial");
					$('#tlyPageGuideMessages').css("bottom", "0px");					
				}
				
			}
	    }); 
 	   // Initialize bookmarks tour
 		pageGuideBookmarks = tl.pg.init({ 
			'auto_refresh': true, 
			'show_numbers': false,
			'close_button_text': 'X',
			'custom_open_button': '#show-bookmarks-tour',
			'steps_element': '#tourBookmarks',
			'track_events_cb': function(interactionName) {
				
			},
			'handle_doc_switch': function(currentTour, prevTour) {
				var step = bookmarkTourSteps[currentTour];
				if (step) {
					customizeStep(pageGuideBookmarks, step);
				} else {
					$('#tlyPageGuideMessages').css("top", "initial");
					$('#tlyPageGuideMessages').css("bottom", "0px");
				}
			}
	    });     	
 
        // Initialize phenolyzer tour
    	pageGuidePhenolyzer = tl.pg.init({ 
			'auto_refresh': true, 
			'show_numbers': false,
			'close_button_text': 'X',
			'custom_open_button': '#show-phenolyzer-tour',
			'steps_element': '#tourPhenolyzer',
			'track_events_cb': function(interactionName) {
				
			},
			'handle_doc_switch': function(currentTour, prevTour) {
			}
	    }); 

    }

 
	// Initialize colon cancer tour
	if (isLevelEdu) {

		pageGuideEduTour1 = tl.pg.init({ 
			'auto_refresh': true, 
			'show_numbers': false,
			'close_button_text': 'close',
			'custom_open_button': '#show-case1-tour',
			'steps_element': '#tourEduCase1',
			'track_events_cb': function(interactionName) {	

				if (interactionName == "PG.close") {
					completeTour();
				}

				for (key in eduTour1Steps) {
					var step = eduTour1Steps[key];
					if (step.audio) {
						$(step.audio)[0].pause();
						$(step.audio)[0].currentTime = 0;
					}
				}
				
			},
			'handle_doc_switch': function(currentTour, prevTour) {


				if (currentTour == '.edu-tour-1-child-buttons') {
					$('#button-load-father-data').addClass("emphasize");
					$('.edu-tour-1-child-buttons .edu-tour-button:eq(0)').addClass("emphasize");
					$('.edu-tour-1-child-buttons .edu-tour-button:eq(1)').addClass("healthy");
					$('.edu-tour-1-child-buttons .edu-tour-button:eq(2)').addClass("emphasize");
				} else {
					$('.edu-tour-1-child-buttons .edu-tour-button').removeClass("emphasize");
					$('.edu-tour-1-child-buttons .edu-tour-button').removeClass("healthy");
					$('#button-load-father-data').removeClass("emphasize");
				}

				var step = eduTour1Steps[currentTour];
				customizeEduTourStep(pageGuideEduTour1, step);

				
			}
	    }); 


		pageGuideEduTour2 = tl.pg.init({ 
			'auto_refresh': true, 
			'custom_open_button': '#show-case2-tour',
			'show_numbers': false,
			'close_button_text': 'close',
			'steps_element': '#tourEduCase2',
			'track_events_cb': function(interactionName) {
				if (interactionName == "PG.close") {
					completeTour();
				}
				for (key in eduTour2Steps) {
					var step = eduTour2Steps[key];
					if (step.audio) {
						$(step.audio)[0].pause();
						$(step.audio)[0].currentTime = 0;
					}
				}
			},
			'handle_doc_switch': function(currentTour, prevTour) {

				var step = eduTour2Steps[currentTour];
				customizeEduTourStep(pageGuideEduTour2, step);

				if (currentTour == "#proband-variant-card #vcf-track") {
					if (getProbandVariantCard().model.vcfData && getProbandVariantCard().model.vcfData.features && getProbandVariantCard().model.vcfData.features.length > 2) {
						var variant = getProbandVariantCard().model.vcfData.features[2];
						var tooltip = getProbandVariantCard().d3CardSelector.select("#vcf-variants .tooltip");
						getProbandVariantCard().showTooltip(tooltip, variant, getProbandVariantCard(), false );
						getProbandVariantCard().showVariantCircle(variant, getProbandVariantCard());						
					}
				}

			}
	    }); 

	    if (eduTourNumber == "1") {
	    	pageGuideEduTour1.open();
	    } else if (eduTourNumber = "2") {
	    	pageGuideEduTour2.open();	    	
	    }
	}

}


function customizeStep(pageGuide, step) {
	if (step.position) {
		$('#tlyPageGuideMessages').css("top", step.position.top);
		$('#tlyPageGuideMessages').css("bottom", step.position.bottom);
	} else {
		$('#tlyPageGuideMessages').css("top", "initial");
		$('#tlyPageGuideMessages').css("bottom", "0px");
	}
}


function customizeEduTourStep(pageGuide, step) {
	if (step.hasOwnProperty('noElement') && step.noElement == true) {
		$('.tlypageguide_shadow')[step.index].style.visibility = 'hidden';
	} else {
		$('.tlypageguide_shadow')[step.index].style.visibility = 'visible';
	}

	if (step.disableNext == true && step.correct == false) {
		$('.pageguide-next').addClass("disabled");
	} else {
		$('.pageguide-next').removeClass("disabled");		
	}
	if (step.disableTourButtons) {
		$('.edu-tour-data-button').addClass("disabled");
	} else {
		$('.edu-tour-data-button').removeClass("disabled");
	}
	if (step.position) {
		$('#tlyPageGuideMessages').css("top", step.position.top);
		$('#tlyPageGuideMessages').css("bottom", step.position.bottom);
	} else {
		$('#tlyPageGuideMessages').css("top", "initial");
		$('#tlyPageGuideMessages').css("bottom", "0px");
	}
	if (step.height) {
		if (step.height == 'full') {
			var stepHeight = window.innerHeight - 10;
			$('#tlyPageGuideMessages .tlypageguide_text').css("min-height", stepHeight);
		} else {
			$('#tlyPageGuideMessages .tlypageguide_text').css("min-height", step.height);
		}
	} else {
		$('#tlyPageGuideMessages .tlypageguide_text').css("min-height", "10px");					
	}
	if (step.animation) {
		setTimeout( function() {
			step.animation.showFunction(true, step)}, 
			step.animation.delay);
	} else {
		showEduTourAnimation(false);		
	}
	if (step.dialog) {
		$('#edu-tour-modal').modal('show');
	} else {
		$('#edu-tour-modal').modal("hide");
	}
	if (step.audio) {
		var audioSelector = step.audio;
		$(audioSelector)[0].play();		
		// When audio finished, automatically move to next step
		$(audioSelector).on("ended", function() {
			if (!step.close && step.index == pageGuide.cur_idx) {
				pageGuide.navigateForward();
			} else if (step.close) {
				setTimeout(function() {
					completeTour();
				},2000);
			}
		});			
	} else {
		$('#page-guide-listen-button').addClass('hide');										
	}
	if (step.first) {
		$('#pageguide-prev-button').addClass("hide");
	} else {
		$('#pageguide-prev-button').removeClass("hide");		
	}
	if (step.close) {
		$('#pageguide-close-button').removeClass("hide");
		$('#pageguide-next-button').addClass("hide");
	} else {
		$('#pageguide-close-button').addClass("hide");
		$('#pageguide-next-button').removeClass("hide");
	} 

	if (step.animation  && hideNextButtonAnim) {
		$('#pageguide-next-button').addClass("hide");
	} 
}

function eduTourCheckPhenolyzer() {
	$('#select-phenotype-edutour').selectize();
	$('#select-phenotype-edutour')[0].selectize.clear();
	$('#select-phenotype-edutour')[0].selectize.on('change', function() {
		var phenotype = $('#select-phenotype-edutour')[0].selectize.getValue().toLowerCase();
		var correct = true;
		if (isLevelEdu && eduTourNumber == 1) {
			if (phenotype != 'colon_cancer') {
				alertify.alert("Please select 'Colon cancer' to continue with this tour.")
				correct = false;
			}
		}
		if (correct) {
			eduTour1Steps['#phenolyzer-search-box .selectize-control.single'].correct = true;
			genesCard.getPhenolyzerGenes(phenotype);
			if (eduTourNumber == 1  && pageGuideEduTour1.cur_idx == 1) {
				pageGuideEduTour1.navigateForward();
			}
			
		} else {
			eduTour1Steps['#phenolyzer-search-box .selectize-control.single'].correct = false;
		}
	});			
}

function eduTourCheckVariant(variant) {
	if (isLevelEdu && eduTourNumber == "1" 
		&& pageGuideEduTour1.cur_idx == 4
		&& variant.vepImpact[HIGH] != "HIGH" 
		&& variant.start == 112116592 
		&& window.gene.gene_name == 'APC') {
		eduTour1Steps['#gene-badge-container'].correct = true;		
		$('.pageguide-next').addClass("disabled");	
		pageGuideEduTour1.navigateForward();
	} else {
		eduTour1Steps['#gene-badge-container'].correct = false;			

	}	
}

function onEduTour1Check(checkbox) {
	var answer   = { "jimmy": true, "bobby": false, "sarah": true};
	var name     = checkbox[0].id;
	var checked  = checkbox[0].checked
	var answerLabel = $('#' + name + "-answer");
	// If the correct answer is "true"
	if (answer[name] == true) {
		if (answer[name] == checked) {
			answerLabel.css("visibility", "visible");	
		} else {
			answerLabel.css("visibility", "hidden");	
		}
	} else {
		if (answer[name] == checked) {
			answerLabel.css("visibility", "hidden");	
		} else {
			answerLabel.css("visibility", "visible");	
		}

	}
	if ($('#jimmy')[0].checked == answer['jimmy']
		&& $('#bobby')[0].checked == answer['bobby']
		&& $('#sarah')[0].checked == answer['sarah']) {
		eduTour1Steps['#children-buttons'].correct = true;	
		$('#pageguide-next-button').removeClass("disabled");		
	} else {
		eduTour1Steps['#children-buttons'].correct = false;			
		$('#pageguide-next-button').addClass("disabled");
	}
}

function onEduTour2Check(checkbox) {
	var answer   = { "john": 'lower', "diego": 'lowest', "anna": 'normal'};
	var checkboxId       = checkbox[0].id;
	var tokens  = checkboxId.split("-");
	var name    = tokens[0];
	var dosage  = tokens[1];
	var checked          = checkbox[0].checked
	var answerLabel      = $('#' + checkboxId + "-answer");
	var allAnswerLabels  = $('.' + name + "-answer");
	var allCheckboxes    = $('.' + name + "-checkbox");

	allCheckboxes.each(function(i,val) {
		if ($(this)[0].id == checkboxId) {

		} else {
			$(this)[0].checked = false;
		}
	})
	
	// Show if the answer is correct or incorrect
	allAnswerLabels.addClass("hide");
	if (checked) {
		answerLabel.removeClass("hide");	
	} else {
		answerLabel.addClass("hide");	
	}

	var correctCount = 0;
	for (var key in answer) {
		var dosage = answer[key];
		var selector = "#" + key + "-" + dosage;
		if ($(selector)[0].checked) {
			correctCount++;
		}
	}

	var stepSelector = '#child-buttons-tour2';
	if (correctCount == 3) {
		eduTour2Steps[stepSelector].correct = true;	
		//pageGuideEduTour2.navigateForward();
		$('#pageguide-next-button').removeClass("disabled");
	} else {
		eduTour2Steps[stepSelector].correct = false;	
		$('#pageguide-next-button').addClass("disabled");
	}
}


function showEduTourAnimation(show, step) {
	if (show) {


		$('#' + step.animation.container).removeClass("hide");

		if (step.animation.edgeObject && step.animation.edgeObject.getStage()) {
			step.animation.edgeObject.getStage().play(0);
		} else {

			
			AdobeEdge.loadComposition(
				step.animation.name, 
				step.animation.clazz, {
				    scaleToFit: "both",
				    centerStage: "both",
				    minW: "0px",
				    maxW: "800px",
				    width:  step.animation.width,
				    height: step.animation.height,
				    htmlRoot: "assets/animations/"
				}, 
				{"dom":{}}, 
				{"dom":{}}
			);
			step.animation.edgeObject = AdobeEdge.getComposition(step.animation.clazz);
		/*
			AdobeEdge.Symbol.bindElementAction(step.animation.clazz, step.animation.name, "document", "compositionReady", 
				function(sym, e) {
					alert('trigger');

				});
			var stage = $(step.animation.edgeObject.getStage());
			var rescale = '.5';
			stage.scale(.5);
		*/
			/*
			stage.css('transform', 'scale(' + rescale + ')');
			stage.css( '-o-transform', 'scale(' + rescale + ')');
			stage.css('-ms-transform', 'scale(' + rescale + ')');
			stage.css('-webkit-transform', 'scale(' + rescale + ')');
			stage.css('-moz-transform', 'scale(' + rescale + ')');
			stage.css('-o-transform', 'scale(' + rescale + ')');
			*/

		}

		return edgeObject;


	} else {
		$('.tour-animation-container').addClass("hide");
	}
 
}

function completeTour() {
	if (isLevelEdu) {
		var completedTour = getUrlParameter("completedTour");
		var url = null;
		if (completedTour != null && completedTour != "") {
			url = EXHIBIT_URL2;
		} else {
			url = EXHIBIT_URL1 + '?tour=' + (eduTourNumber == 1 ? 2 : 1) + '&completedTour=' + eduTourNumber;
		}
		window.location.href = url;
	} 
}


