define([
      "pedigree/model/helpers",
      "pedigree/model/phenotipsJSON"
    ], function(
      Helpers,
      PhenotipsJSON
    ){

    var PreferencesManager = Class.create( {
        initialize: function( templatePreferences ) {
            this.preferences = templatePreferences;
            this._cookiePrefix = "pedigree_preference_";
        },

        load: function(callWhenReady) {
            var preferencesJsonURL = editor.getExternalEndpoint().getPedigreePreferencesURL();
            preferencesJsonURL += "&rand=" + Math.random();

            new Ajax.Request(preferencesJsonURL, {
                method: "GET",
                onSuccess: this.onPreferencesAvailable.bind(this),
                onComplete: callWhenReady
            });
        },

        onPreferencesAvailable : function(response) {
            if (response.responseJSON) {
                // only set preferences which are given in the template
                Helpers.setByTemplate(this.preferences, response.responseJSON);
                this.preferences.phenotipsVersion = response.responseJSON.phenotipsVersion;

                console.log("Loaded pedigree system preferences: " + Helpers.stringifyObject(response.responseJSON));

                if (!PhenotipsJSON.isVersionSupported(this.getPhenotipsVersion())) {
                    alert("Current PhenoTips Patient JSON version (" + this.getPhenotipsVersion() + ") may not be supported");
                }
            } else {
                console.log("Failed to load pedigree system preferences, no JSON");
            }

            // read user preferences from cookies
            // TODO: read from user profile instead of using cookies?
            this.preferences.user = Helpers.getAllCookies(this._cookiePrefix);
        },

        getPhenotipsVersion: function() {
            return this.preferences.hasOwnProperty("phenotipsVersion") ? this.preferences.phenotipsVersion : "";
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
                Helpers.setCookie(this._cookiePrefix + optionName, value); // no expire date
                // TODO: save to user profile instead of using cookies?
            } else if (domain == "pedigree") {
                this.preferences.pedigree[optionName] = value;
                // no save: will be saved when the rest of pedigree is saved
            } else {
                throw "Unsupported options domain: " + domain;
            }
        }
    });
    return PreferencesManager;
});
