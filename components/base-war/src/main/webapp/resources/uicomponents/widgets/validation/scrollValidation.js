require(['jquery'], function ($)
{
    $(document).ready(function ()
    {
        var _mandatoryFields = $(".mandatory :input:not([type='hidden'])");
        var _checkedFields = $(".checked :input:not([type='hidden'])");
        var externalId = $("#PhenoTips\\.PatientClass_0_external_id");
        var _checkedDates = $(".checked-date input.fuzzy-date");
        var _mandatoryDates = $(".mandatory-date input.fuzzy-date");
        var mandatoryFields = _mandatoryFields.add(_checkedFields).add(externalId).add(_checkedDates).add(_mandatoryDates);
        var saveButtons = $("input[name='action_save']");

        //This will happen if the page is not in edit mode
        if ($("#inline").length < 1) {
            return;
        }

        var fieldsByPosition = [];
        mandatoryFields.each(function (index, field)
        {
            var jField = $(field);
            var position = jField.offset().top;
            var parent = $(jField.parents('.chapter')[0]);
            //Position could be 0, so in that case take the position of the parent section
            if (position == 0) {
                position = parent.offset().top;
            }
            var positionObject = {'field': jField, 'position': position, 'parent': parent};
            fieldsByPosition.push(positionObject);
        });

        /* Listeners */
        //Scroll
        var _window = $(window);
        var viewportHeight = $(window).height();
        _window.scroll(function ()
        {
            var windowScroll = _window.scrollTop();
            var lowerVisibilityBound = windowScroll + (viewportHeight / 3);
            var sectionToExpand = null;
            $.each(fieldsByPosition, function (index, fieldObject)
            {
                //Unfortunately, it seems that the position for all objects has to be recalculated on each scroll event
                var position = fieldObject['field'].offset().top;
                if (fieldObject['parent'].hasClass('collapsed')) {
                    position = fieldObject['parent'].offset().top;
                }
                if (position > windowScroll && position < lowerVisibilityBound) {
                    /* Only one section can be expanded at a time. Expands only if the fields inside fail validation. */
                    if (fieldObject['field'][0].__validation && fieldObject['field'][0].__validation.validate() == false) {
                        sectionToExpand == null ? sectionToExpand = fieldObject['parent'] : null;
                    }
                }
            });
            if (sectionToExpand) {
                sectionToExpand.removeClass('collapsed');
            }
        });

        //In case somebody decides to resize the window, and throw off visibility calculations
        _window.resize(function ()
        {
            viewportHeight = $(window).height();
        });

        saveButtons.on("click", function(){
            var numberMissing = 0;
            $.each(fieldsByPosition, function (index, fieldObject)
            {
                if (fieldObject['field'].parents('body').length == 0) {
                    // The element has been removed, also remove the validation
                    fieldObject['field'][0].__validation && fieldObject['field'][0].__validation.destroy();
                    delete fieldObject['field'][0].__validation;
                } else if (fieldObject['field'][0].__validation && !fieldObject['field'][0].__validation.validate()) {
                    fieldObject['parent'].removeClass('collapsed');
                    var position = fieldObject['field'].offset().top - (viewportHeight / 3);
                    position < 0 ? position = 0 : null;
                    _window.scrollTop(position);
                    numberMissing++;
                }
            });
            if (numberMissing > 0) {
                var dialog = new PhenoTips.widgets.ModalPopup("<div class='box errormessage'>Some fields are missing or invalid. Please review and correct the entered data. Missing or invalid fields: " + numberMissing + "</div>",
                    false, {'title': 'Invalid input', 'verticalPosition': 'top', 'removeOnClose': true});
                dialog.showDialog();
                document.fire("phenotips:scrollValidation:invalid");
            }
        });

        var addNewElement = function(field) {
            var jField = $(field);
            var position = jField.offset().top;
            var parent = $(jField.parents('.chapter')[0]);
            //Position could be 0, so in that case take the position of the parent section
            if (position == 0) {
                position = parent.offset().top;
            }
            var positionObject = {'field': jField, 'position': position, 'parent': parent};
            fieldsByPosition.push(positionObject);
        };

        document.observe('xwiki:dom:updated', function (event) {
            ((event && event.memo.elements) || [$('body')]).each(function(element) {
                element.select(".mandatory :input:not([type='hidden'])").each(function(field) {
                    addNewElement(field);
                });
                element.select(".checked :input:not([type='hidden'])").each(function(field) {
                    addNewElement(field);
                });
                // Also validate any date elements.
                element.select(".mandatory-date input.fuzzy-date").each(function(field) {
                  addNewElement(field);
                });
                element.select(".checked-date input.fuzzy-date").each(function(field) {
                  addNewElement(field);
                });
            });
        });
    });
});
