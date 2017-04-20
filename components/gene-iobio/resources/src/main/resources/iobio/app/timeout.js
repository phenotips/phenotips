
function checkForInactivity() {
 	//Increment the idle time counter every second.
    var idleInterval = setInterval(timerIncrement, IDLE_INTERVAL); 

    //Zero the idle timer on mouse movement.
    $(this).mousemove(function (e) {
        idleTime = 0;
    });
    $(this).keypress(function (e) {
        idleTime = 0;
    });	
    
}

function timerIncrement() {
	if (idlePrompting) {
		return;
	}
    // If we are on the exhibit welcome page, no need for timeout
    if (location.pathname.indexOf("exhibit.html") >= 0) {
        idleTime = 0;
        return;
    }
    // If the video is playing, don't check for inactivity
    if ($('#video-container').length > 0 && !$('#video-container').hasClass("hide")) {
        idleTime = 0;
        return;        
    }
    // If an animation is playing, don't check for inactivity
    if ($('.tour-animation-container').length > 0) {
        var hideCount = 0;
        $('.tour-animation-container').each(function() {
            if ($(this).hasClass("hide")) {
                hideCount++;
            } 
        });
        // If an animation container is shown (not all are hidden)
        // we reset the idle. 
        if (hideCount < $('.tour-animation-container').length ) {
            idleTime = 0;
            return;

        }
    }


    idleTime = idleTime + 1;
    if (idleTime > MAX_IDLE ) {
    	idlePrompting = true; 
    	// If the user hasn't pressed continue in the next x seconds, restart the app.
		setTimeout(restartApp, IDLE_RESTART);  //

    	
    	//alertify.set({ buttonReverse: true });
    	alertify.defaults.glossary.ok = "Yes, I want to continue.";
		alertify.alert("Warning", 
			"This app will restart in 10 seconds unless there is activity. Do you want to continue?", 
			function () {
				// okay
				idleTime = 0;
			    idlePrompting = false;
			}			
		 );

        
    }
}

function restartApp() {
	if (idleTime > MAX_IDLE) {
		//window.location.reload();
		startOver();
	}
}

function startOver() {

	window.location.href = EXHIBIT_URL;
}