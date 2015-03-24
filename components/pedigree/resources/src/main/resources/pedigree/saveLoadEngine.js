/**
 * SaveLoadEngine is responsible for automatic and manual save and load operations.
 *
 * @class SaveLoadEngine
 * @constructor
 */

var ProbandDataLoader = Class.create( {
    initialize: function() {
        this.probandData = {};
    },

    load: function(callWhenReady) {
        var probandID = XWiki.currentDocument.page;
        var patientJsonURL = new XWiki.Document('ExportPatient', 'PhenoTips').getURL('get', 'id='+probandID);
        // IE caches AJAX requests, use a random URL to break that cache (TODO: investigate)
        patientJsonURL += "&rand=" + Math.random();
        new Ajax.Request(patientJsonURL, {
            method: "GET",
            onSuccess: this.onProbandDataReady.bind(this),
            onComplete: callWhenReady ? callWhenReady : {}
        });
    },

    onProbandDataReady : function(response) {
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

        if (!noUndo) {
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

        if (!noUndo) {
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

        var jsonData = this.serialize();

        console.log("[SAVE] data: " + stringifyObject(jsonData));

        var image = $('canvas');
        var background = image.getElementsByClassName('panning-background')[0];
        var backgroundPosition = background.nextSibling;
        var backgroundParent =  background.parentNode;
        backgroundParent.removeChild(background);
        var bbox = image.down().getBBox();
        var savingNotification = new XWiki.widgets.Notification("Saving", "inprogress");

        var familyServiceURL = new XWiki.Document('FamilyPedigreeInterface', 'PhenoTips').getURL('get');
        new Ajax.Request(familyServiceURL, {
                method: "POST",
        //new Ajax.Request(XWiki.currentDocument.getRestURL('objects/PhenoTips.PedigreeClass/0', 'method=PUT'), {
        //    method: 'POST',
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
                var actionAfterSave = editor.getAfterSaveAction();
                actionAfterSave && actionAfterSave();
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
            onSuccess: function() { editor.getActionStack().addSaveEvent();
                                    savingNotification.replace(new XWiki.widgets.Notification("Successfuly saved"));
                                  },
            parameters: {"json": jsonData, "image": image.innerHTML.replace(/xmlns:xlink=".*?"/, '').replace(/width=".*?"/, '').replace(/height=".*?"/, '').replace(/viewBox=".*?"/, "viewBox=\"" + bbox.x + " " + bbox.y + " " + bbox.width + " " + bbox.height + "\" width=\"" + bbox.width + "\" height=\"" + bbox.height + "\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"")}
        });
        backgroundParent.insertBefore(background, backgroundPosition);
    },

    load: function() {
        console.log("initiating load process");

        // IE caches AJAX requests, use a random URL to break that cache
        var probandID = XWiki.currentDocument.page;
        var pedigreeJsonURL = new XWiki.Document('ExportPatient', 'PhenoTips').getURL('get', 'id='+probandID);
        pedigreeJsonURL += "&data=pedigree";
        // IE caches AJAX requests, use a random URL to break that cache (TODO: investigate)
        pedigreeJsonURL += "&rand=" + Math.random();
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
