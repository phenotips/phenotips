/**
 * The UI Element for displaying prompts to the user in a movable/semi-transparent dialogue for study selection
 *
 * @class OkCancelDialogue
 */
define([], function(){
  var StudySelectionDialog = Class.create( {

      initialize: function(studies) {
          var _this = this;

          this.studies = studies;

          var mainDiv = new Element('div', {'class' : "study-selection-dialog"});
          var container = new Element('div', {'class' : 'xform'});

          mainDiv.insert(container);

          var submitButton = new Element('input', {type : 'button', value : "Submit", 'class' : 'button'});
          mainDiv.insert(submitButton);
          submitButton.observe('click', function(event) {
              selectedStudies = $$('.study-selection-dialog input[type="radio"].study-input');
              var studyName = "";
              selectedStudies.each( function(item) {
                  if (item.checked) {
                      studyName = item.value;
                  }
              });
              _this.submit(studyName);
              _this.hide();
          });

          var cancelButton = new Element('input', {type : 'button', value : "Cancel", 'class' : 'button secondary'});
          mainDiv.insert(' ').insert(cancelButton);
          cancelButton.observe('click', function(event) {
        	  _this.hide();
          });

          var studiesContainer = new Element('dl');
          var header = new Element('dt').update('Please select one of the available studies');
          studiesContainer.insert(header);

          var studiesList = new Element('dd');
          var noneStudy = new Element('div', {'class' : 'form-template-option'});
          var input = new Element('input', {'type' : 'radio', 'name' : 'study-selection'});
          input.checked = true;
          var label = new Element('label').insert(input).insert(' None');
          noneStudy.insert(label);
          noneStudy.insert(new Element('p', {'class' : 'xHint'}).insert('Use the system\'s default form configuration: a generic form covering basic patient information and covering phenotypes related to major anatomic systems.'));
          studiesList.insert(noneStudy);

          this.studies.each( function(study) {
              var newStudy = new Element('div', {'class' : 'form-template-option'});
              var label = new Element('label').insert(new Element('input', {'type' : 'radio', 'value' : study.id, 'name' : 'study-selection', 'class' : 'study-input'})).insert(study.name);
              newStudy.insert(label).insert(new Element('p', {'class' : 'xHint'}).insert(study.description));
              studiesList.insert(newStudy);
          });

          studiesContainer.insert(studiesList);
          container.update(studiesContainer);

          var closeShortcut = ['Esc'];
          this.dialog = new PhenoTips.widgets.ModalPopup(mainDiv, {close: {method : this.hide.bind(this), keys : closeShortcut}}, {'title': "Study selection", 'removeOnClose': false, 'extraClassName': "study-selection-dialog", displayCloseButton: false});
      },

      /**
       * Displays the dialogue (vanilla OK\Cancel sytyle)
       *
       * @method show
       */
      show: function(onOKFunction) {
          this.dialog.show();
          if (onOKFunction) {
        	  this.submit = onOKFunction;
          }
      },

      /**
       * Hides the dialogue
       *
       * @method hide
       */
      hide: function() {
          this.dialog.closeDialog();
      },
      
      /**
       * Default 
       *
       * @method hide
       */
      submit: function() {
          this.dialog.closeDialog();
      }
  });
  return StudySelectionDialog;
});