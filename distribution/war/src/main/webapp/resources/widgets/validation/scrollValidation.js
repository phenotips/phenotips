require(['jquery'], function ($)
{
    $(document).ready(function ()
    {
        var mandatoryFields = $(".mandatory input:not([type='hidden'])");
        var saveButtons = $("input[name='action_save']");

        //This will happen if the page is not in edit mode
        if (mandatoryFields.length < 1) {
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
                    /* Only one section can be expanded at a time. */
                    sectionToExpand == null ? sectionToExpand = fieldObject['parent'] : null;
                    fieldObject['field'][0].__validation.validate();
                }
            });
            if (sectionToExpand) {
                sectionToExpand.removeClass('collapsed');
            }
        });

        //In case somebody decides to resize the window, and through off visibility calculations
        _window.resize(function ()
        {
            viewportHeight = $(window).height();
        });

        saveButtons.on("click", function(){
            $.each(fieldsByPosition, function (index, fieldObject)
            {
                if (!fieldObject['field'][0].__validation.validate()) {
                    fieldObject['parent'].removeClass('collapsed');
                    var position = fieldObject['field'].offset().top - (viewportHeight / 3);
                    position < 0 ? position = 0 : null;
                    _window.scrollTop(position);
                }
            });
        });
    });
});
