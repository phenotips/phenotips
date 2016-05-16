/**
 * An extension manager for pedigree editor.
 *
 * Extensions are added by modifying global `window.pedigreeExtensions` array. It is assumed
 * that the extension code is run before the pedigree editor is loaded (which will be the
 * case for extensions created in XWiki and loaded "on every page") and adds extension description
 * to this array. before pedigree starts.
 *
 * Each extension definition in the `window.pedigreeExtensions` array should be an object in the following format:
 *  {
 *    "name" (optional): {String},        // displayed for the user. If two extensions have the same name
 *                                        // and extend the same extension point the one with higher
 *                                        // version (using case-sensitive string comparison) will be used.
 *                                        // If one has no version and another has, the one with version is used
 *
 *    "version" (optional): {String},     // displayed for the user + see above
 *
 *    "apiVersion" (optional): {String},  // extension api version. Defaults to "1.0".
 *
 *    "extensions": {Array of Objects}    // each extension must extend one or more extension points
 *    [
 *        "extensionPoint": {String},      // non-supported extension points will be ignored
 *
 *        "code": {function},              // the function should take an Object as the only parameter
 *                                         // (contents of the object depend on the extension point) and
 *                                         // return an object with two fields:
 *                                         //   { "parameters": {Object},                    // in the same format as parameters passed into the function,
 *                                         //                                                // but possibly modified, depending on extension point
 *                                         //     "pedigreeChanged" (optional): {Boolean} }  // true iff pedigree data has been changed. Some extension
 *                                         //                                                // points do not care, but some may depend on this being
 *                                         //                                                // set correctly
 *
 *        "priority" (optional): {Integer} // -Inf..Inf, extension with a higher numeric priority will
 *                                         // be called before an extension with a lower priority
 *                                         // Extensions with no priority are assumed to have priority 0
 *                                         // This priority will only aply to this extension point
 *    ]
 *  }
 *
 * If the same extension implements multiple extension points it should create multiple entries
 * in the extrensions list.
 *
 * =================================================
 *
 * List of curently supported extension points:
 *
 *  - "personNodeMenu" (nodeMenuFields.js)
 *  - "relationshipNodeMenu" (nodeMenuFields.js)
 *  - "groupNodeMenu" (nodeMenuFields.js)
 *
 *  - "newPedigreeState" (undoRedoManager.js)   // not called during undo-redo operations, only
 *                                              // when a pedigree state is encountered for the first time.
 *
 *  - "personNodeCreated" (person.js)           // after other initializations have been completed - to enable set default value
 *  - "personNodeRemoved" (person.js)           // before remove before any other data is removed - to enable cleanup, e.g. remove data from legend
 *  - "personGetNodeMenuData" (person.js)       // after all other data has been added - to add new data no node menu parameters
 *  - "personToModel" (person.js)               // after all other data has been dumped
 *  - "modelToPerson" (person.js)               // after all other data has been loaded
 *
 * =================================================
 *
 * Using an extension in pedigree editor:
 *   var dataForExtension = { "foo": "bar", "currentPedigreeNode": node };
 *   var modifiedData = editor.getExtensionManager().callExtensions("extensionPointName", dataForExtension).extendedData;
 *
 * @class PedigreeExtensionManager
 */
define([
        "pedigree/model/helpers"
    ], function(
        Helpers
    ){
    var PedigreeExtensionManager = Class.create({

        /**
         * Loads all already defined extensions, and orders them by priority.
         */
        initialize: function() {
            this.APIVERSION = "1.0";

            this._extensions = {};   // map { extensionPointName -> [ array_of_extensions_ordered_by_priority ] }

            if (!window.pedigreeExtensions) {
                // no extensions are defined
                window.pedigreeExtensions = [];
                return;
            }

            // gather all extensions and sort them by extension point, and by
            // priority within the extension point
            //
            // if more than one extension has the same name and extends the same extension point - pick
            // the one with the latest version (using case-sensitive string comparision)

            var _this = this;
            window.pedigreeExtensions.forEach(function(extension) {
                if ( !extension.hasOwnProperty("extensions") || !Array.isArray(extension.extensions) || extension.extensions.length == 0 ) {
                    console.log("[EXTENSIONMANAGER] Ignoring improperly defined extension: [" + stringifyObject(extension) + "]");
                    return;
                }

                // TODO: if API version changes, need to check if extension is compatible with the current version

                var extensionDescription = extension.hasOwnProperty("name") ? "extension [" + extension.name + "]" : "unnamed extension";
                extension.hasOwnProperty("version") && (extensionDescription += ", version: [" + extension.version + "]");
                console.log("[EXTENSIONMANAGER] Found " + extensionDescription);

                extension.extensions.forEach(function(extensionElement) {
                    if ( !extensionElement.hasOwnProperty("extensionPoint")
                         || !extensionElement.hasOwnProperty("code")
                         || !(typeof extensionElement.extensionPoint === "string")
                         || !(typeof extensionElement.code === "function") ) {
                        console.log("[EXTENSIONMANAGER] Ignoring improperly defined extension point: [" + stringifyObject(extensionElement) + "]");
                        return;
                    }
                    if (!_this._extensions.hasOwnProperty(extensionElement.extensionPoint)) {
                        _this._extensions[extensionElement.extensionPoint] = [];
                    }
                    extension.hasOwnProperty("name") && (extensionElement.name = extension.name);
                    extension.hasOwnProperty("version") && (extensionElement.version = extension.version);
                    _this._extensions[extensionElement.extensionPoint].push(extensionElement);
                    console.log("[EXTENSIONMANAGER] - adding a handler for extension point " + extensionElement.extensionPoint);
                });
            });

            for (var extensionPoint in this._extensions) {
                if (this._extensions.hasOwnProperty(extensionPoint)) {
                    this._extensions[extensionPoint] = this._removeDeprecatedVersions(this._extensions[extensionPoint]);
                    this._extensions[extensionPoint] = this._orderExtensions(this._extensions[extensionPoint]);
                }
            }

        },

        /**
         * Calls all extensions extending the given extension point (in the priority order),
         * passing the data from the original pedigree as the input to the first, and then
         * chaining the output.
         *
         * Returns an object with 4 fields:
         *  1) "extensionsDefined": {boolean} true if there is at least one extension, false ow
         *
         *  2) "pedigreeChanged": {boolean} true iff pedigree data has been changed by an extension, false ow
         *
         *  3) "extendedData": {object} parameters as modified by all extensions, or original
         *                              parameters if no extensions were defined
         */
        callExtensions: function(extensionPoint, parameters)
        {
            // if no extensions extend this extension point return the unmodified result
            if (!this._extensions.hasOwnProperty(extensionPoint)) {
                return { "extensionsDefined": false,
                         "pedigreeChanged": false,
                         "paramsChanged": false,
                         "extendedData": parameters };
            }

            var pedigreeDataChanged = false;
            // chain call all available extensions (which are already ordered by priority),
            // passing output of the first as the input to the second, etc.
            for (var i = 0; i < this._extensions[extensionPoint].length; i++) {
                var extension = this._extensions[extensionPoint][i];
                if (true || editor.DEBUG_MODE) {
                    console.log("[EXTENSIONMANAGER] [Extension point " + extensionPoint + "] " +
                                "Calling extension [" + (extension.hasOwnProperty("name") ? extension.name : "unnamed") + "]");
                }
                var extensionRunResult = extension.code(parameters);
                parameters = extensionRunResult.parameters;
                extensionRunResult.hasOwnProperty("pedigreeChanged")
                    && extensionRunResult.pedigreeChanged
                    && (pedigreeDataChanged = true);
            }

            return { "extensionsDefined": true,
                     "pedigreeChanged": pedigreeDataChanged,
                     "extendedData": parameters };
        },

        /**
         * Removes duplicate extensions with the same name but different versions
         * (only the last version is kept, last defined as the largest string
         * according to case-sensitive string comparision).
         */
        _removeDeprecatedVersions: function(extensionList) {
            var filteredExtensionList = [];

            var extensionNames = {};
            extensionList.forEach(function(extension) {
                // keep all extensions with no name
                if (!extension.hasOwnProperty(name)) {
                    filteredExtensionList.push(extension);
                    return;
                }
                var name = extension.name;
                if (!extensionNames.hasOwnProperty(name)) {
                    extensionNames[name] = extension;
                } else {
                    console.log("[EXTENSIONMANAGER] Multiuple versions of extension + [" + name + "] are found, keeping only one version");

                    // If next extension with the same name has no version defined keep previous
                    if (!extension.hasOwnProperty("version")) {
                        return;
                    }
                    // If next has version defined and previous does not
                    // OR if both have and new version is larger
                    // => replace previous with next
                    if (!extensionNames[name].hasOwnProperty("version") || extension.version > extensionNames[name].version) {
                        extensionNames[name] = extension;
                        return;
                    }
                }
            });
            for (var name in extensionNames) {
                if (extensionNames.hasOwnProperty(name)) {
                    filteredExtensionList.push(extensionNames[name]);
                }
            }
            return filteredExtensionList;
        },

        /**
         * Orders extensions by priority.
         */
        _orderExtensions: function(extensionList) {
            var byPriority = function(e1, e2) {
                var p1 = e1.hasOwnProperty("priority") ? e1.priority : 0;
                var p2 = e2.hasOwnProperty("priority") ? e2.priority : 0;
                return p2 - p1;
            };
            extensionList.sort( byPriority );
            return extensionList;
        }
    });

    return PedigreeExtensionManager;
});
