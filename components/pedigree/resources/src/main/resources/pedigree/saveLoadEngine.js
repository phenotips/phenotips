/**
 * SaveLoadEngine is responsible for automatic and manual save and load operations.
 *
 * @class SaveLoadEngine
 * @constructor
 */

var ProbandDataLoader = Class.create( {
    initialize: function() {
        this.isFamily = false;
        this.hasFamily = false;
        this.probandData = {};
    },

    load: function(callWhenReady) {
        var _this = this;
        this._loadFamilyInfo( function() {
            _this._loadPatientData(callWhenReady);
        });
    },

    _loadFamilyInfo: function(callWhenReady) {
        var probandID = editor.getGraph().getCurrentPatientId();
        var familyJsonURL = editor.getExternalEndpoint().getFamilyInterfaceURL(probandID);
        new Ajax.Request(familyJsonURL, {
            method: "POST",
            onSuccess: this._onFamilyDataReady.bind(this),
            onComplete: callWhenReady ? callWhenReady : {},
            parameters: {"proband": probandID, "family_status": true }
        });
    },

    _onFamilyDataReady: function(response) {
        if (response.responseJSON) {
            this.isFamily  = response.responseJSON.isFamilyPage;
            this.hasFamily = response.responseJSON.hasFamily;
        } else {
            console.log("[!] Error parsing family JSON");
        }
        console.log("Family data:  [hasFamily: " + stringifyObject(this.hasFamily) +
                               "], [isFamily: " + stringifyObject(this.isFamily) + "]");
    },

    _loadPatientData: function(callWhenReady) {
        var probandID = editor.getGraph().getCurrentPatientId();
        var patientJsonURL = editor.getExternalEndpoint().getLoadPatientDataJSONURL(probandID);
        new Ajax.Request(patientJsonURL, {
            method: "GET",
            onSuccess: this._onProbandDataReady.bind(this),
            onComplete: callWhenReady ? callWhenReady : {}
        });
    },

    _onProbandDataReady : function(response) {
        if (response.responseJSON) {
            this.probandData = response.responseJSON;
        } else {
            console.log("[!] Error parsing patient JSON");
        }
        console.log("Proband data: " + stringifyObject(this.probandData));
    },
});


var SaveLoadEngine = Class.create( {

    initialize: function() {
        this._saveInProgress = false;
    },

    /**
     * Saves the state of the pedigree (including any user preferences and current color scheme)
     *
     * @return Serialization data for the entire graph
     */
    serialize: function() {
        var jsonObject = editor.getGraph().toJSONObject();

        jsonObject["settings"] = editor.getView().getSettings();

        return JSON.stringify(jsonObject);
    },

    createGraphFromSerializedData: function(JSONString, noUndo, centerAroundProband) {
        console.log("---- load: parsing data ----");
        document.fire("pedigree:load:start");

        try {
            var jsonObject = JSON.parse(JSONString);

            // load the graph model of the pedigree & node data
            var changeSet = editor.getGraph().fromJSONObject(jsonObject);

            // load/process metadata such as pedigree options and color choices
            if (jsonObject.hasOwnProperty("settings")) {
                editor.getView().loadSettings(jsonObject.settings);
            }
        }
        catch(err)
        {
            console.log("ERROR loading the graph: " + err);
            alert("Error loading the graph");
            document.fire("pedigree:graph:clear");
            document.fire("pedigree:load:finish");
            return;
        }

        if (!noUndo && !editor.isFamilyPage()) {
            var probandJSONObject = editor.getProbandDataFromPhenotips();
            var genderOk = editor.getGraph().setNodeDataFromPhenotipsJSON( editor.getGraph().getProbandId(), probandJSONObject);
            if (!genderOk)
                alert("Proband gender defined in Phenotips is incompatible with this pedigree. Setting proband gender to 'Unknown'");
            JSONString = this.serialize();
        }

        if (editor.getView().applyChanges(changeSet, false)) {
            editor.getWorkspace().adjustSizeToScreen();
        }

        if (centerAroundProband)
            editor.getWorkspace().centerAroundNode(editor.getGraph().getProbandId());

        if (!noUndo)
            editor.getActionStack().addState(null, null, JSONString);

        document.fire("pedigree:load:finish");
    },

    createGraphFromImportData: function(importString, importType, importOptions, noUndo, centerAroundProband) {
        console.log("---- import: parsing data ----");
        document.fire("pedigree:load:start");

        try {
            var changeSet = editor.getGraph().fromImport(importString, importType, importOptions);
            if (changeSet == null) throw "unable to create a pedigree from imported data";
        }
        catch(err)
        {
            alert("Error importing pedigree: " + err);
            document.fire("pedigree:load:finish");
            return;
        }

        if (!noUndo && !editor.isFamilyPage()) {
            var probandJSONObject = editor.getProbandDataFromPhenotips();
            var genderOk = editor.getGraph().setProbandData(probandJSONObject);
            if (!genderOk)
                alert("Proband gender defined in Phenotips is incompatible with the imported pedigree. Setting proband gender to 'Unknown'");
            JSONString = this.serialize();
        }

        if (editor.getView().applyChanges(changeSet, false)) {
            editor.getWorkspace().adjustSizeToScreen();
        }

        if (centerAroundProband)
            editor.getWorkspace().centerAroundNode(editor.getGraph().getProbandId());

        if (!noUndo)
            editor.getActionStack().addState(null, null, JSONString);

        document.fire("pedigree:load:finish");
    },

    save: function() {
        if (this._saveInProgress)
            return;   // Don't send parallel save requests

        editor.getView().unmarkAll();

        var me = this;
        me._notSaved = true;

        var jsonData = this.serialize();

        console.log("[SAVE] data: " + stringifyObject(jsonData));

        var image = $('canvas');
        var background = image.getElementsByClassName('panning-background')[0];
        var backgroundPosition = background.nextSibling;
        var backgroundParent =  background.parentNode;
        backgroundParent.removeChild(background);
        var bbox = image.down().getBBox();
        var savingNotification = new XWiki.widgets.Notification("Saving", "inprogress");

        var svgText = image.innerHTML.replace(/xmlns:xlink=".*?"/, '').replace(/width=".*?"/, '').replace(/height=".*?"/, '').replace(/viewBox=".*?"/, "viewBox=\"" + bbox.x + " " + bbox.y + " " + bbox.width + " " + bbox.height + "\" width=\"" + bbox.width + "\" height=\"" + bbox.height + "\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"");
        // remove invisible elements to slim down svg
        svgText = svgText.replace(/<[^<>]+display: none;[^<>]+>/, "");

        var familyServiceURL = editor.getExternalEndpoint().getSavePedigreeURL();
        new Ajax.Request(familyServiceURL, {
        //new Ajax.Request(XWiki.currentDocument.getRestURL('objects/PhenoTips.PedigreeClass/0', 'method=PUT'), {
            method: 'POST',
            onCreate: function() {
                me._saveInProgress = true;
                // Disable save and close buttons during a save
                var closeButton = $('action-close');
                var saveButton = $('action-save');
                Element.addClassName(saveButton, "disabled-menu-item");
                Element.removeClassName(saveButton, "menu-item");
                Element.addClassName(saveButton, "no-mouse-interaction");
                Element.addClassName(closeButton, "disabled-menu-item");
                Element.removeClassName(closeButton, "menu-item");
                Element.addClassName(closeButton, "no-mouse-interaction");
            },
            onComplete: function() {
                me._saveInProgress = false;
                if (!me._notSaved) {
                    var actionAfterSave = editor.getAfterSaveAction();
                    actionAfterSave && actionAfterSave();
                }
                // Enable save and close buttons after a save
                var closeButton = $('action-close');
                var saveButton = $('action-save');
                Element.addClassName(saveButton, "menu-item");
                Element.removeClassName(saveButton, "disabled-menu-item");
                Element.removeClassName(saveButton, "no-mouse-interaction");
                Element.addClassName(closeButton, "menu-item");
                Element.removeClassName(closeButton, "disabled-menu-item");
                Element.removeClassName(closeButton, "no-mouse-interaction");
            },
            onSuccess: function(response) {
                if (response.responseJSON) {
                    if (response.responseJSON.error) {
                        savingNotification.replace(new XWiki.widgets.Notification("Pedigree was not saved"));
                        SaveLoadEngine._displayFamilyPedigreeInterfaceError(response.responseJSON);
                    } else {
                        me._notSaved = false;
                        editor.getActionStack().addSaveEvent();
                        savingNotification.replace(new XWiki.widgets.Notification("Successfuly saved"));
                    }
                } else  {
                    savingNotification.replace(new XWiki.widgets.Notification("Save attempt failed: server reply is incorrect"));
                    editor.getOkCancelDialogue().showError('Server error - unable to save pedigree',
                            'Error saving pedigree', "OK", undefined );
                }
            },
            parameters: {"proband": editor.getGraph().getCurrentPatientId(), "json": jsonData, "image": svgText}
            //parameters: {"property#data": jsonData, "property#image": svgText}
        });
        backgroundParent.insertBefore(background, backgroundPosition);
    },

    load: function() {
        var probandID = editor.getGraph().getCurrentPatientId();
        var pedigreeJsonURL = editor.getExternalEndpoint().getLoadPatientPedigreeJSONURL(probandID);
        new Ajax.Request(pedigreeJsonURL, {
            method: 'GET',
            onCreate: function() {
                document.fire("pedigree:load:start");
            },
            onSuccess: function (response) {
                //console.log("Data from LOAD: >>" + response.responseText + "<<");
                if (response.responseJSON) {
                    console.log("[LOAD] recived JSON: " + stringifyObject(response.responseJSON));

                    var updatedJSONData = editor.getVersionUpdater().updateToCurrentVersion(response.responseText);

                    this.createGraphFromSerializedData(updatedJSONData);

                    // since we just loaded data from disk data in memory is equivalent to data on disk
                    editor.getActionStack().addSaveEvent();
                } else {
                    new TemplateSelector(true);
                }
            }.bind(this)
        })
    }
});


SaveLoadEngine._displayFamilyPedigreeInterfaceError = function(replyJSON)
{
    var errorMessage = replyJSON.errorMessage ? replyJSON.errorMessage : "Unknown problem";
    errorMessage = "<font color='#660000'>" + errorMessage + "</font><br><br><br>";
    if (replyJSON.errorType == "familyConflict") {
        errorMessage += "(for now it is only possible to add persons who is not in another family to a family)";
    }
    if (replyJSON.errorType == "pedigreeConflict") {
        errorMessage += "(for now it is only possible to add persons without an already existing pedigree to a family)";
    }
    if (replyJSON.errorType == "permissions") {
        errorMessage += "(you need to have edit permissions for the patient to be able to add it to a family)";
    }
    editor.getOkCancelDialogue().showError('<br>Unable to save pedigree: ' + errorMessage,
            'Error saving pedigree', "OK", undefined );
}