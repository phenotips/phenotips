var PreferencesManager = Class.create( {
    initialize: function( defaultPreferences ) {
        this.preferences = defaultPreferences;
    },

    load: function(callWhenReady) {
        var numConfigs = $xwiki.getDocument('XWiki.XWikiPreferences').getObjectNumbers('PhenoTips.PedigreeGlobalSettings');
        if (numConfigs > 0) {
            this.preferences.global.dateDisplayFormat = "$xwiki.getDocument('XWiki.XWikiPreferences').getObject('PhenoTips.PedigreeGlobalSettings').getProperty('dateDisplayFormat').value";
            this.preferences.global.dateEditFormat    = "$xwiki.getDocument('XWiki.XWikiPreferences').getObject('PhenoTips.PedigreeGlobalSettings').getProperty('dateInputFormat').value";        
            this.preferences.global.nonStandardAdoptedOutGraphic = ($xwiki.getDocument('XWiki.XWikiPreferences').getObject('PhenoTips.PedigreeGlobalSettings').getProperty('nonStandardAdoptedOutGraphic').value == 1);
            this.preferences.global.propagateFatherLastName      = ($xwiki.getDocument('XWiki.XWikiPreferences').getObject('PhenoTips.PedigreeGlobalSettings').getProperty('propagateFatherLastName').value == 1);
        }

        /* The current way of loading properties is sub-optimal, in the future may have to introduce
         * a velocity service and AJAX it.
         *
        var preferencesJsonURL = new XWiki.Document('...', 'PhenoTips').getURL('get', '');
        // IE caches AJAX requests, use a random URL to break that cache (TODO: investigate)
        preferencesJsonURL += "&rand=" + Math.random();
        new Ajax.Request(preferencesJsonURL, {
            method: "GET",
            onSuccess: this.onPreferencesAvailable.bind(this),
            onComplete: callWhenReady ? callWhenReady : {}
        });
        */
        this.onPreferencesAvailable();
        callWhenReady();
    },

    onPreferencesAvailable : function(response) {
        if (!this.preferences) {
            this.preferences = {};
        }
        if (!this.preferences.hasOwnProperty("global")) {
            this.preferences.global = {};
        }
        //TODO: use response.responseJSON when using AJAX
        console.log("Loaded global preferences: " + stringifyObject(this.preferences.global));
    },

    /**
     * Returns value of the given config option. Pedigree-specific settings overwrite user settings,
     * user settings overwrite global installation settings
     * @method getConfigurationOption
     * @param {String} Config option name
     * @return null if not defined, option value otherwise
     */
    getConfigurationOption: function(optionName) {
        if (this.preferences.pedigree.hasOwnProperty(optionName)) {
            return this.preferences.pedigree[optionName];
        }
        if (this.preferences.user.hasOwnProperty(optionName)) {
            return this.preferences.user[optionName];
        }
        if (this.preferences.global.hasOwnProperty(optionName)) {
            return this.preferences.global[optionName];
        }
        return null;
    },

    setConfigurationOption: function(domain, optionName, value) {
        if (domain == "user") {
            this.preferences.user[optionName] = value;
            // TODO: save to user profile
        } else if (domain == "pedigree") {
            this.preferences.pedigree[optionName] = value;
            // no save: will be saved when the rest of pedigree is saved
        } else {
            throw "Unsupported options domain: " + domain;
        }
    }
});
