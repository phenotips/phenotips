document.observe("projects:projectselection:popupcreated", function(event) {
   new PhenoTips.widgets.SelectProject();
});

var PhenoTips = (function (PhenoTips) {
   var widgets = PhenoTips.widgets = PhenoTips.widgets || {};
   widgets.SelectProject = Class.create({

   initialize : function ()
   {
      _this = this;
      _this._checkboxes = $('projectsSection').select(':input');
      _this._registerListeners();

      // This will be run when the user opens the projects/template selection pop-up for update and
      // will narrow down the list of templates.
      document.observe("projects:projectselection:popupupdated", function(event) {
         _this._projectSelected();
      });
   },

   _registerListeners : function(sectionId, documentName)
   {
      _this._checkboxes.map(function(item) {
         item.observe('click', function() {
             _this._projectSelected();
         });
      });
   },

   _projectSelected : function() {
      var selected = "";
      _this._checkboxes.map(function(item) {
         if (item.checked) {
            if (selected.length > 0) {
               selected += ",";
            }
            selected += item.name;
         }
      });

      var templatesSelect = $('templatesSelect');
      new Ajax.Request(new XWiki.Document('StudyBindingClass', 'PhenoTips').getURL('get'), {
         parameters : {'input' : selected},
         onCreate : function() {
            templatesSelect.addClassName("loading");
         },
         onSuccess : function(response) {
            templatesSelect.update(response.responseText);
         },
         onFailure : function(response) {
            var failureReason = response.responseText || response.statusText;
            if (response.statusText == '' /* No response */ || response.status == 12031 /* In IE */) {
               failureReason = "$services.localization.render('phenotips.studyBindingClass.failure.noResponse')";
            }
            _this._messages.update(new Element('div', {'class' : 'errormessage'}).update("$services.localization.render('phenotips.studyBindingClass.failureMessage')" + ' ' + failureReason));
          },
          on0 : function (response) {
             response.request.options.onFailure(response);
          },
          onComplete : function () {
             templatesSelect.removeClassName("loading");
          }
        });
   }
   });
   return PhenoTips;
}(PhenoTips || {}));