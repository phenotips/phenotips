$(document).ready(function() {
	$.material.init();
	$("#exhibit-video").on("ended", function() {
		$('.exhibit-welcome').animateVideoDone('fadeOutRight');	
	});

	// Encapsulate logic for animate.css into a jquery function
    $.fn.extend({
    animateIt: function (animationName, customClassName) {
    		$(this).removeClass("hide");
    		var additionalClass = customClassName ? ' ' + customClassName : '';
	        var animationEnd = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
	        $(this).addClass('animated ' + animationName + additionalClass).one(animationEnd, function() {
	            $(this).removeClass('animated ' + animationName);
	        });
	    }
	});

	// Encapsulate logic for animate.css into a jquery function
    $.fn.extend({
	animateVideoDone: function (animationName, customClassName) {
			$(this).removeClass("hide");
			var additionalClass = customClassName ? ' ' + customClassName : '';
	        var animationEnd = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
	        $(this).addClass('animated ' + animationName + additionalClass).one(animationEnd, function() {
	            $(document).addClass("hide");	           
	            //$(this).removeClass('animated ' + animationName);	
				//hideVideo();   	
				showCaseStudies();	
        	});
	    }
	});

    if (hasTimeout) {
		checkForInactivity();
    }

});

function showVideo() {
	$('#video-container').removeClass('hide');
	$('#start-here').addClass('hide');
	$('h1').addClass('hide');
	$('h3').addClass('hide');
	$('#thankyou').addClass('hide');
	
	playVideo();
}
function hideVideo() {
	$('#video-container').addClass('hide');
}
function playVideo() {
	$("#exhibit-video")[0].play();
	//$("#play").addClass("disabled")
	//$("#pause").removeClass("disabled")
	//$("#stop").removeClass("disabled")
}
function pauseVideo() {
	$("#exhibit-video")[0].pause();
	//$("#play").removeClass("disabled")
	//$("#pause").addClass("disabled")
	//$("#stop").removeClass("disabled")
}
function stopVideo() {
	$("#exhibit-video")[0].pause();
	$("#exhibit-video")[0].currentTime = 0;
	hideVideo();
	//$("#play").removeClass("disabled")
	//$("#pause").addClass("disabled")
	//$("#stop").removeClass("disabled")
}
function showCaseStudy(tourNumber) {
	var url = (isOffline ? "/?mode=edutour&tour=" : "./?mode=edu&tour=") + tourNumber;
	window.location.href = url;
}
function showNewCaseStudy() {
	var url = (isOffline ? "/?mode=edutour&tour=" : "./?mode=edu&tour=") + getUrlParameter("tour") + '&completedTour=' + getUrlParameter("completedTour");
	window.location.href = url;
}
function showCaseStudies() {
	var url = "exhibit-cases.html"
	window.location.href = url;
	
}

function getUrlParameter(sParam) {
    var sPageURL = window.location.search.substring(1);
    var sURLVariables = sPageURL.split('&');
    var hits = {};
    for (var i = 0; i < sURLVariables.length; i++) 
    {    	
        var sParameterName = sURLVariables[i].split('=');        
        if (typeof sParam == 'string' || sParam instanceof String) {
	        if (sParameterName[0] == sParam) 
	        {
	            return sParameterName[1];
	        }
	    } else {
	    	var matches = sParameterName[0].match(sParam);
	    	if ( matches != undefined && matches.length > 0 ) {
	    		hits[sParameterName[0]] = sParameterName[1];
	    	}
	    }
    }
    if (Object.keys(hits).length == 0)
    	return undefined;
    else
    	return hits;
}
