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
        var collaboratorsJSON = this._getCollaboratorsJSON(formData);
        var templates = formData.templates || [];

        new Ajax.Request(_this.ajaxService, {
          parameters : {'projectId'     : _this.projectId,
                        'xaction'       : 'update',
                        'collaborators' : collaboratorsJSON,
                        'templates'     : templates},
          onComplete: function(response) {
             _this._readCollaboratorsAndTemplates();
          }
        });
      }.bind(_this));
    },

    _initializeCollaborators : function(type, searchUsers, searchGroups) {
      var params = "&searchUsers="+searchUsers+"&searchGroups="+searchGroups+"&"
      var suggestInput = $('suggest'+type);
      suggestInput._suggest = new PhenoTips.widgets.Suggest(suggestInput, {
        script: "$xwiki.getURL('PhenoTips.SearchUsersAndGroups', 'get', 'outputSyntax=plain')" + params,
        varname: "input",
        noresults: "$services.localization.render('phenotips.projectSheet.noUsersResults')",
        resultsParameter: "matched",
        json: true,
        resultIcon: "icon",
        enableHierarchy: false,
        fadeOnClear: true,
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
             var newCollaborator = {'id' : values[0] , 'name' : name , 'type' : values[1], 'icon' : event.memo.icon};
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
        noresults: "$services.localization.render('phenotips.projectSheet.noTemplateResults')",
        resultsParameter: "matchedTemplates",
        json: true,
        resultId: "id",
        resultValue: "textSummary",
        enableHierarchy: false,
        fadeOnClear: true,
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
		var leadersList = $('leaderslist');
		if (_this._isItemInList(c.id, leadersList) || _this._isItemInList(c.id, list)) {
          return;
        }

        if (_this.editmode || collaboratorType != 'leaders') {
            var row = new Element('tr', {'class' : (highlight === true ? 'new' : '')});
			var image = new Element('img', { 'src' : c.icon, 'height': '25px', 'class' : 'icon project-contributor-avatar' }).update(' ');
            row.insert(new Element('td').insert(new Element('div', { 'class' : 'user-avatar-wrapper' }).insert(image)));
            row.insert(new Element('td', {'class' : 'mainTd'})
                .insert(new Element('div', {'class': 'user-name'}).update(c.name))
                .insert(new Element('input', {'type': 'hidden', 'name' : collaboratorType, 'value' : c.id}))
            )

           if (_this.editmode) {
               var deleteTool = new Element('span', {'class' : 'tool delete fa fa-times'});
               row.insert(deleteTool.wrap('td'));
               deleteTool.observe('click', function(event) {
                  if (_this.openForContribution && collaboratorType == 'contributors') { //contributors-div is disabled
                      return;
                  }
                  event.findElement('tr').remove();
               });
           }
        } else {
            var row = new Element('tr', { 'class' : (highlight === true ? 'new' : '') });
            var image = new Element('a', { 'href' : c.link }).insert(new Element('img', { 'src' : c.icon, 'width' : '80px', 'class' : 'project-leader-avatar' }).update(' '));
            var name = new Element('span', { 'class' : 'project-leader-name' }).insert(new Element('a', { 'href' : c.link }).insert(new Element('h4', { 'style' : 'margin: 0;' }).update(c.name)));
            var email = new Element('span', { 'class' : 'project-leader-email' }).insert(new Element('a', { 'href' : 'mailto:'+c.email }).update(c.email));
            var bio = new Element('span', { 'class' : 'project-leader-bio' }).update(c.comment);
            var td = new Element('td', {'class' : 'project-leader-info mainTd'}).insert(image).insert(name);
            if (c.email != "") {
                td.insert(email).insert(new Element('br'));
            }
            td.insert(new Element('br')).insert(bio).insert(new Element('input', {'type': 'hidden', 'name' : collaboratorType, 'value' : c.id}));
            row.insert(td);
        }
        list.insert(row);
    },

    _loadCollaboratorsAndTemplates : function(observers, contributors, leaders, templates) {
      if (observers.length > 0 && _this.editmode) {
		$('observerslist').update('');
        for (var i = 0; i < observers.length ; i++) {
          this._addCollaborator(observers[i], false, 'observers');
        }
      }

      if (contributors.length == 0 && !_this.editmode) {
        $('contributorslist').up('.project-contributors-block').hide();
      } else {
		$('contributorslist').update('');
        for (var i = 0; i < contributors.length ; i++) {
          this._addCollaborator(contributors[i], false, 'contributors');
        }
      }

      if (leaders.length == 0 && !_this.editmode) {
        $('leaderslist').up('.project-leaders-block').hide();
      } else {
		$('leaderslist').update('');
        for (var i = 0; i < leaders.length ; i++) {
          this._addCollaborator(leaders[i], false, 'leaders');
        }
      }

      if (_this.editmode) {
        $('templatesList').update('');
        for (var i = 0; i < templates.length ; i++) {
          this._addTemplate(templates[i], false);
        }
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
       this._addCollaborator({type: "user", name: currentUser.name, id: "$xcontext.mainWikiName"+":"+currentUser.id, icon : "$xwiki.getSkinFile('icons/xwiki/noavatar.png')"}, true, 'leaders');
    },

    _getCollaboratorsJSON : function(formData) {
        var leaders = [].concat(formData.leaders || []);
        var contributors = [].concat(formData.contributors || []);
        var observers = [].concat(formData.observers || []);
        if (_this.openForContribution) {
           contributors = [];
        }

        var collaborators = [];
        this._addCollaboratorsToJSON(collaborators, leaders, 'leader');
        this._addCollaboratorsToJSON(collaborators, contributors, 'contributor');
        this._addCollaboratorsToJSON(collaborators, observers, 'observer');

        var collaboratorsJSON = {};
        collaboratorsJSON.collaborators = collaborators;
        return JSON.stringify(collaboratorsJSON);
    },

    _addCollaboratorsToJSON : function(collaborators, array, accessLevel) {
        for (var i=0; i<array.length; i++) {
            var item = new Object();
            item.userOrGroup = array[i];
            item.accessLevel = accessLevel;
            collaborators.push(item);
        }
    }

  });
  return PhenoTips;
})(PhenoTips || {});

document.observe("xwiki:dom:loaded", function () {
   new PhenoTips.widgets.ProjectSheet(document.documentElement.down('meta[name="page"]').content);
});

document.observe("xwiki:livetable:displayComplete", function () {
   var patientsTable = $('content-project-contributors');
      if (patientsTable) {
         patientsTable = $('content-project-contributors').remove();
         var tableBlock = $('project-patients-block');
         if (tableBlock) {
            tableBlock.down('dd').insert(patientsTable);
         }
      }
});