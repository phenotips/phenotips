/**
 * A doalogue which can display various selectors in tabs
 *
 * @class TabbedSelector
 */
define([
        "pedigree/model/helpers"
    ], function(
        Helpers
    ){
    var TabbedSelector = Class.create( {

        initialize: function(title, tabContents) {
            var _this = this;
            this.tabContents = tabContents;

            this.mainDiv = new Element('div', {'class': 'multitab-selector-modal'});

            this.topMessage = new Element('div', {'class': 'multitab-selector-top-message'});
            this.mainDiv.insert(this.topMessage);

            this.tabs = [];
            for (var i = 0; i < tabContents.length; i++) {
                var tabHeader = new Element('dd', {"class": (i == 0 ? "active" : "")}).insert("<a>" + tabContents[i].getTitle() + "</a>");

                var tabInfo = {"header": tabHeader, "content": tabContents[i].getContentDiv()};

                tabContents[i].getContentDiv().addClassName("tab-content");

                this.tabs.push(tabInfo);
            }

            var switchTab = function(index) {
                return function() {
                    _this.switchTab(index);
                }
            }

            var tabTop = new Element('dl', {'class':'multitab-selector-tabs'});
            this.tabs.each(function(item, index) {
                item.header.observe('click', switchTab(index))
                tabTop.insert(item.header);
            });
            this.mainDiv.insert(tabTop);

            this.tabs.each(function(item) {
                _this.mainDiv.insert(item.content);
            });

            var closeShortcut = ['Esc'];
            this.dialog = new PhenoTips.widgets.ModalPopup(this.mainDiv, {close: {method : this.hide.bind(this), keys : closeShortcut}}, {extraClassName: "multitab-selector-main", title: title, displayCloseButton: true, verticalPosition: "top"});

            var closeFunction = function() {
                _this.hide();
            };
            for (var i = 0; i < tabContents.length; i++) {
                tabContents[i].setParent(this.dialog.getBoxId(), closeFunction);
            }
        },

        switchTab: function(tabID) {
            var _this = this;
            var needCallOnActivate = false;
            this.tabs.each(function(item, index) {
                if (index == tabID) {
                    if (!_this.tabs[index].header.hasClassName('active')) {
                        needCallOnActivate = true;
                    }
                    _this.tabs[index].content.addClassName("active");
                    _this.tabs[index].header.addClassName('active');
                } else {
                    _this.tabs[index].header.removeClassName('active');
                    _this.tabs[index].content.removeClassName("active");
                }
            });
            // call this AFTER redraw has been done (other tab(s) hidden, this tab displayed -
            // so that all on-screen elements have final/correct sizes and positions
            if (needCallOnActivate) {
                this.tabContents[tabID].onActivatedTab();
            }
        },

        /**
         * Displays the template selector
         *
         * @method show
         */
        show: function(tabID, allowCancel, topMessage, topMessageCssClasses) {
            if (topMessage) {
                this.topMessage.update(new Element('div', {'class': topMessageCssClasses ? topMessageCssClasses : ""}).update(topMessage));
                this.topMessage.addClassName("margin-bottom-10px");
            } else {
                this.topMessage.update("");
                this.topMessage.removeClassName("margin-bottom-10px");
            }

            this.switchTab(tabID);

            this.dialog.show();

            if (!allowCancel) {
                // hide close button and disable keyboard close shortcut
                $(this.dialog.getBoxId()).down('.msdialog-close').hide();
                this.dialog.unregisterShortcuts("close");
            } else {
                // show close button and enable keyboard close shortcut
                $(this.dialog.getBoxId()).down('.msdialog-close').show();
                this.dialog.registerShortcuts("close");
            }

            for (var i = 0; i < this.tabContents.length; i++) {
                this.tabContents[i].onShow(allowCancel);
            }
        },

        /**
         * Removes the the template selector
         *
         * @method hide
         */
        hide: function() {
            for (var i = 0; i < this.tabContents.length; i++) {
                this.tabContents[i].onHide();
            }
            this.dialog.closeDialog();
        }
    });

    return TabbedSelector;
});