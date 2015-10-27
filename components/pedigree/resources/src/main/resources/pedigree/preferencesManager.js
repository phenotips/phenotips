var PreferencesManager = Class.create( {
    initialize: function( templatePreferences ) {
        this.preferences = templatePreferences;
    },

    load: function(callWhenReady) {
        // TODO: get URL from endpoints.js after merger with family-studies
        var preferencesJsonURL = new XWiki.Document('PedigreeInterface', 'PhenoTips').getURL('get', 'action=getPreferences');
        preferencesJsonURL += "&rand=" + Math.random();

        new Ajax.Request(preferencesJsonURL, {
            method: "GET",
            onSuccess: this.onPreferencesAvailable.bind(this),
            onComplete: callWhenReady ? callWhenReady : {}
        });
    },

    onPreferencesAvailable : function(response) {
        if (response.responseJSON) {
            // only set preferences which are given in the template
            setByTemplate(this.preferences, response.responseJSON);
            console.log("Loaded preferences: " + stringifyObject(response.responseJSON));
        } else {
            console.log("Failed to loaded properties, no JSON");
        }
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
