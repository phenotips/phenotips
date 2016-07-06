var PhenoTips = (function (PhenoTips) {
  var widgets = PhenoTips.widgets = PhenoTips.widgets || {};
  widgets.ProjectSheet = Class.create({

    initialize : function (projectId) {
      _this = this;
      _this.projectId = projectId;
      _this.form = $('inline');
      _this.ajaxService = '/bin/get/Projects/SaveProjectHandler';

      var editmode = false;
      if ($('editmode')) {
         editmode = $('editmode').value;
      }
      _this.editmode = editmode;
      if (_this.editmode) {
         _this._initializeEditMode();
      } else {
         _this._initializeViewMode();
      }
    },

    _initializeViewMode : function() {
       this._readCollaboratorsAndTemplates();
    },

    _initializeEditMode : function() {
      $('PhenoTips.ProjectClass_0_openProjectForContribution').observe('click', function() {
         _this._openForContributionCheckboxClicked();
      });
      _this._openForContributionCheckboxClicked();

      _this._initializeCollaborators('leaders', true, false);
      _this._initializeCollaborators('contributors', true, true);
      _this._initializeCollaborators('observers', true, true);
      _this._initializeSuggestTemplates();

      var url = window.location.href.toLowerCase();
      var newProject = (url.indexOf("newproject") > 0);
      if (newProject) {
         this._addCurrentUserAsLeader();
      } else {
         this._readCollaboratorsAndTemplates();
      }

      // Save collaborators and templates when project is saved
      document.observe("xwiki:actions:save", function(event) {
        var formData = _this.form.serialize(true);
        var leaders = formData.leaders || [];
        var templates = formData.templates || [];
        var contributors = formData.contributors || [];
        var observers = formData.observers || [];
        if (_this.openForContribution) {
           contributors = [];
        }

        new Ajax.Request(_this.ajaxService, {
          parameters : {'projectId'    : _this.projectId,
                        'xaction'      : 'update',
                        'observers'    : observers,
                        'contributors' : contributors,
                        'leaders'      : leaders,
                        'templates'    : templates},
          onComplete: function(response) {
             _this._readCollaboratorsAndTemplates();
          }
        });
      });
    },

    _initializeCollaborators : function(type, searchUsers, searchGroups) {
      var params = "&searchUsers="+searchUsers+"&searchGroups="+searchGroups+"&"
      var suggestInput = $('suggest'+type);
      suggestInput._suggest = new PhenoTips.widgets.Suggest(suggestInput, {
        script: "$xwiki.getURL('PhenoTips.SearchUsersAndGroups', 'get', 'outputSyntax=plain')" + params,
        varname: "input",
        noresults: "No matching terms",
        resultsParameter: "matched",
        json: true,
        resultIcon: "icon",
        enableHierarchy: false,
        fadeOnClear: false,
        timeout: 30000,
        parentContainer: $('body')
      });

      suggestInput.observe("focus", function (event) {
        Form.Element.clear(suggestInput);
      });

      var collaboratorsList = $(type+'list');
      var collaboratorType = type;
      suggestInput.observe("ms:suggest:selected", function(event) {
         if (event.memo) {
           var name = event.memo.value;
           var existingCollaborator = collaboratorsList.down('tr input[value="' + name.replace('"', '\\"') + '"]');
           if (existingCollaborator) {
             existingCollaborator.up('tr').addClassName('highlight');
             // remove highlight after a while
             new PeriodicalExecuter(function(pe) {
                existingCollaborator.up('tr').removeClassName('highlight');
                pe.stop();
             }, 5);
           } else {
             var values = event.memo.id.split(";");
             var newCollaborator = {'id' : values[0] , 'name' : name , 'type' : values[1]};
             _this._addCollaborator(newCollaborator, true, collaboratorType);
             event.findElement().value = '';
           }
         }
      });
    },

    _initializeSuggestTemplates: function() {
      var suggestTemplates = $('suggestTemplates');
      suggestTemplates._suggest = new PhenoTips.widgets.Suggest(suggestTemplates, {
        script: "$xwiki.getURL('Studies.StudiesSearch', 'get', 'outputSyntax=plain')" + "&",
        varname: "input",
        noresults: "No matching terms",
        resultsParameter: "matchedTemplates",
        json: true,
        resultId: "id",
        resultValue: "textSummary",
        enableHierarchy: false,
        fadeOnClear: false,
        timeout: 30000,
        parentContainer: $('body')
      });

      suggestTemplates.observe("focus", function (event) {
        Form.Element.clear(suggestTemplates);
      });

      suggestTemplates.observe("ms:suggest:selected", function (event) {
        _this._addTemplate(event.memo, true);
      });

    },

    _openForContributionCheckboxClicked : function() {
       _this.openForContribution = $('PhenoTips.ProjectClass_0_openProjectForContribution').checked;
       $('suggestcontributors').disabled=_this.openForContribution;
       if (_this.openForContribution) {
          $('contributorsTable').hide();
       } else {
          $('contributorsTable').show();
       }
    },

    _addTemplate : function (template, highlight) {
       var list = $('templatesList');
       if (_this._isItemInList(template.id, list)) {
          return;
       }
       var row = new Element('tr', {'class' : (highlight === true ? 'new' : '')});
       row.insert(new Element('td', {'class' : 'mainTd'}).insert(template.value)
               .insert(new Element('input', {'type': 'hidden', 'name' : 'templates', 'value' : template.id}))
       )
       if (_this.editmode) {
          var deleteTool = new Element('span', {'class' : 'tool delete fa fa-times'});
          row.insert(deleteTool.wrap('td'));
          deleteTool.observe('click', function(event) {
             event.findElement('tr').remove();
          });
       }
       list.insert(row);
    },

    _isItemInList : function(item, list) {
      var children = list.childElements();
      for (var i=0; i < children.size(); i++) {
         var v = children[i].down(".mainTd input").value;
         if (v == item) {
            return true;
         }
      }
      return false;
    },

    _addCollaborator : function (c, highlight, collaboratorType) {
      var list = $(collaboratorType + 'list');
      if (_this._isItemInList(c.id, list)) {
         return;
      }
 
      var row = new Element('tr', {'class' : (highlight === true ? 'new' : '')});
      row.insert(new Element('td').insert(new Element('span', {'class' : 'fa fa-' + c.type}).update(' ')));
      row.insert(new Element('td', {'class' : 'mainTd'})
              .insert(c.name)
              .insert(new Element('input', {'type': 'hidden', 'name' : collaboratorType, 'value' : c.id}))
      )
      if (_this.editmode) {
         var deleteTool = new Element('span', {'class' : 'tool delete fa fa-times'});
         row.insert(deleteTool.wrap('td'));
         deleteTool.observe('click', function(event) {
            if (_this.openForContribution &amp;&amp; collaboratorType == 'contributors') { //contributors-div is disabled
               return;
            }
            event.findElement('tr').remove();
         });
      }
      list.insert(row);
    },
    
    _loadCollaboratorsAndTemplates : function(observers, contributors, leaders, templates) {
      $('observerslist').update('');
      $('contributorslist').update('');
      $('leaderslist').update('');

      for (var i = 0; i < observers.length ; i++) {
        this._addCollaborator(observers[i], false, 'observers');
      }
      for (var i = 0; i < contributors.length ; i++) {
        this._addCollaborator(contributors[i], false, 'contributors');
      }
      for (var i = 0; i < leaders.length ; i++) {
        this._addCollaborator(leaders[i], false, 'leaders');
      }

      $('templatesList').update('');
      for (var i = 0; i < templates.length ; i++) {
        this._addTemplate(templates[i], false);
      }
    },

    _readCollaboratorsAndTemplates : function() {
      new Ajax.Request(_this.ajaxService, {
        parameters : {'xaction'   : 'get',
                      'projectId' : _this.projectId},
        onSuccess : function(response) {
           if (!("error" in response.responseJSON)) {
              var observers = response.responseJSON.observers;
              var contributors = response.responseJSON.contributors;
              var leaders = response.responseJSON.leaders;
              var templates = response.responseJSON.templates;
              _this._loadCollaboratorsAndTemplates(observers, contributors, leaders, templates);
           }
        }
      });
    },

    _addCurrentUserAsLeader : function() {
       var currentUser = PhenoTips.currentUser;
       this._addCollaborator({type: "user", name: currentUser.name, id: currentUser.id}, true, 'leaders');
    }

  });
  return PhenoTips;
})(PhenoTips || {});

document.observe("xwiki:dom:loaded", function () {
   new PhenoTips.widgets.ProjectSheet(document.documentElement.down('meta[name="page"]').content);
});