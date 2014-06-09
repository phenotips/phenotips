/**
 * NodeMenu is a UI Element containing options for AbstractNode elements
 *
 * @class NodeMenu
 * @constructor
 * @param {Array} data Contains objects corresponding to different menu items
 *
 {
 [
    {
        'name' : the name of the menu item,
        'label' : the text label above this menu option,
        'type' : the type of form input. (eg. 'radio', 'date-picker', 'text', 'textarea', 'disease-picker', 'select'),
        'values' : [
                    {'actual' : actual value of the option, 'displayed' : the way the option will be seen in the menu} ...
                    ]
    }, ...
 ]
 }
 
 Note: when an item is specified as "inactive" it is completely removed from the menu; when it
       is specified as "disabled" it is greyed-out and does not allow selection, but is still visible.
 */
NodeMenu = Class.create({
    initialize : function(data, otherCSSClass) {
        //console.log("nodeMenu initialize");
        this.canvas = editor.getWorkspace().canvas || $('body');
        var cssClass = 'menu-box';
        if (otherCSSClass) cssClass += " " + otherCSSClass;
        this.menuBox = new Element('div', {'class' : cssClass});

        this.closeButton = new Element('span', {'class' : 'close-button'}).update('×');
        this.menuBox.insert({'top': this.closeButton});
        this.closeButton.observe('click', this.hide.bindAsEventListener(this));

        this.form = new Element('form', {'method' : 'get', 'action' : ''});
        this.menuBox.insert({'bottom' : this.form});

        this.fieldMap = {};
        // Generate fields
        var _this = this;
        data.each(function(d) {
            if (typeof (_this._generateField[d.type]) == 'function') {
                _this.form.insert(_this._generateField[d.type].call(_this, d));
            }
        });

        // Insert in document
        this.hide();
        editor.getWorkspace().getWorkArea().insert(this.menuBox);

        this._onClickOutside = this._onClickOutside.bindAsEventListener(this);

        // Attach pickers
        // date
        var crtYear = new Date().getFullYear();
        window.dateTimePicker = new XWiki.widgets.DateTimePicker({
           year_range: [crtYear - 99, crtYear + 1],
           after_navigate : function(date) {
             this._selector.updateSelectedDate({day: date.getDate(), month: date.getMonth(), year : date.getYear() + 1900}, false);
           }
        });
        // disease
        this.form.select('input.suggest-omim').each(function(item) {
            if (!item.hasClassName('initialized')) {
                // Create the Suggest.
                item._suggest = new PhenoTips.widgets.Suggest(item, {
                    script: Disorder.getOMIMServiceURL() + "&",
                    varname: "q",
                    noresults: "No matching terms",
                    json: true,
                    resultsParameter : "rows",
                    resultId : "id",
                    resultValue : "name",
                    resultInfo : {},
                    enableHierarchy: false,
                    fadeOnClear : false,
                    timeout : 30000,
                    parentContainer : $('body')
                });
                if (item.hasClassName('multi') && typeof(PhenoTips.widgets.SuggestPicker) != "undefined") {
                    item._suggestPicker = new PhenoTips.widgets.SuggestPicker(item, item._suggest, {
                        'showKey' : false,
                        'showTooltip' : false,
                        'showDeleteTool' : true,
                        'enableSort' : false,
                        'showClearTool' : true,
                        'inputType': 'hidden',
                        'listInsertionElt' : 'input',
                        'listInsertionPosition' : 'after',
                        'acceptFreeText' : true
                    });
                }
                item.addClassName('initialized');
                document.observe('ms:suggest:containerCreated', function(event) {
                    if (event.memo && event.memo.suggest === item._suggest) {
                        item._suggest.container.setStyle({'overflow': 'auto', 'maxHeight': document.viewport.getHeight() - item._suggest.container.cumulativeOffset().top + 'px'})
                    }
                });
            }
        });
        // Update disorder colors
        this._updateDisorderColor = function(id, color) {
          this.menuBox.select('.field-disorders li input[value="' + id + '"]').each(function(item) {
             var colorBubble = item.up('li').down('.disorder-color');
             if (!colorBubble) {
               colorBubble = new Element('span', {'class' : 'disorder-color'});
               item.up('li').insert({top : colorBubble});
             }
             colorBubble.setStyle({background : color});
          });
        }.bind(this);
        document.observe('disorder:color', function(event) {
           if (!event.memo || !event.memo.id || !event.memo.color) {
             return;
           }
           _this._updateDisorderColor(event.memo.id, event.memo.color);
        });
        this._setFieldValue['disease-picker'].bind(this);
    },

    _generateEmptyField : function (data) {
        var result = new Element('div', {'class' : 'field-box field-' + data.name});
        var label = new Element('label', {'class' : 'field-name'}).update(data.label);
        result.inputsContainer = new Element('div', {'class' : 'field-inputs'});
        result.insert(label).insert(result.inputsContainer);
        this.fieldMap[data.name] = {
            'type' : data.type,
            'element' : result,
            'default' : data["default"] || '',
            'crtValue' : data["default"] || '',
            'function' : data['function'],
            'inactive' : false
        };
        return result;
    },

    _attachFieldEventListeners : function (field, eventNames, values) {
      var _this = this;
      eventNames.each(function(eventName) {
        field.observe(eventName, function(event) {
          if (_this._updating) return; // otherwise a field change triggers an update which triggers field change etc
          var target = _this.targetNode;
          if (!target) return;
          _this.fieldMap[field.name].crtValue = field._getValue && field._getValue()[0];
          var method = _this.fieldMap[field.name]['function'];

          if (target.getSummary()[field.name].value == _this.fieldMap[field.name].crtValue)
              return;

          if (method.indexOf("set") == 0 && typeof(target[method]) == 'function') {
              var properties = {};
              properties[method] = _this.fieldMap[field.name].crtValue;
              var event = { "nodeID": target.getID(), "properties": properties };
              document.fire("pedigree:node:setproperty", event);
          }
          else {
              var properties = {};
              properties[method] = _this.fieldMap[field.name].crtValue;
              var event = { "nodeID": target.getID(), "modifications": properties };
              document.fire("pedigree:node:modify", event);
          }
          field.fire('pedigree:change');
        });
      });
    },

    update: function(newTarget) {
        //console.log("Node menu: update");
        if (newTarget)
            this.targetNode = newTarget;

        if (this.targetNode) {
            this._updating = true;   // needed to avoid infinite loop: update -> _attachFieldEventListeners -> update -> ...
            this._setCrtData(this.targetNode.getSummary());
            this.reposition();
            delete this._updating;
        }
    },

    _attachDependencyBehavior : function(field, data) {
        if (data.dependency) {
            var dependency = data.dependency.split(' ', 3);
            var element = this.fieldMap[dependency[0]].element;
            dependency[0] = this.form[dependency[0]];
            element.inputsContainer.insert(field.up());
            this.fieldMap[field.name].element = element;
            this._updatedDependency(field, dependency);
            dependency[0].observe('pedigree:change', function() {
                this._updatedDependency(field, dependency);
                field.value = '';
                //console.log("dependency observed: " + field + ", dep: " + dependency);
            }.bindAsEventListener(this));
        }
    },

    _updatedDependency : function (field, dependency) {
        switch (dependency[1]) {
            case '!=':
                field.disabled =  (dependency[0].value == dependency[2]);
                break;
            default:
                field.disabled =  (dependency[0].value == dependency[2]);
                break;
        }
    },

    _generateField : {
        'radio' : function (data) {
            var result = this._generateEmptyField(data);
            var columnClass = data.columns ? "field-values-" + data.columns + "-columns" : "field-values";
            var values = new Element('div', {'class' : columnClass});
            result.inputsContainer.insert(values);
            var _this = this;
            var _generateRadioButton = function(v) {
                var radioLabel = new Element('label', {'class' : data.name + '_' + v.actual}).update(v.displayed);
                var radioButton = new Element('input', {type: 'radio', name: data.name, value: v.actual});
                radioLabel.insert({'top': radioButton});
                radioButton._getValue = function() { return [this.value]; }.bind(radioButton);
                values.insert(radioLabel);
                _this._attachFieldEventListeners(radioButton, ['click']);
                _this._attachDependencyBehavior(radioButton, data);
            };
            data.values.each(_generateRadioButton);

            return result;
        },
        'checkbox' : function (data) {
            var result = this._generateEmptyField(data);
            var checkbox = new Element('input', {type: 'checkbox', name: data.name, value: '1'});
            result.down('label').insert({'top': checkbox});
            checkbox._getValue = function() { return [this.checked];}.bind(checkbox);
            this._attachFieldEventListeners(checkbox, ['click']);
            return result;
        },
        'text' : function (data) {
            var result = this._generateEmptyField(data);
            var text = new Element('input', {type: 'text', name: data.name});
            if (data.tip) {
                text.placeholder = data.tip;
            }
            result.inputsContainer.insert(text);
            text.wrap('span');
            text._getValue = function() { return [this.value]; }.bind(text);
            //this._attachFieldEventListeners(text, ['keypress', 'keyup'], [true]);
            this._attachFieldEventListeners(text, ['keyup'], [true]);
            this._attachDependencyBehavior(text, data);
            return result;
        },
        'textarea' : function (data) {
            var result = this._generateEmptyField(data);
            var properties = {name: data.name};
            properties["class"] = "textarea-"+data.rows+"-rows"; // for compatibiloity with older browsers not accepting {class: ...}
            var text = new Element('textarea', properties);
            result.inputsContainer.insert(text);
            //text.wrap('span');
            text._getValue = function() { return [this.value]; }.bind(text);
            this._attachFieldEventListeners(text, ['keyup'], [true]);
            this._attachDependencyBehavior(text, data);
            return result;
        },
        'date-picker' : function (data) {
            var result = this._generateEmptyField(data);
            var datePicker = new Element('input', {type: 'text', 'class': 'xwiki-date', name: data.name, 'title': data.format, alt : '' });
            result.insert(datePicker);
            datePicker._getValue = function() { return [this.alt && Date.parseISO_8601(this.alt)]; }.bind(datePicker);
            this._attachFieldEventListeners(datePicker, ['xwiki:date:changed']);
            return result;
        },
        'disease-picker' : function (data) {
            var result = this._generateEmptyField(data);
            var diseasePicker = new Element('input', {type: 'text', 'class': 'suggest multi suggest-omim', name: data.name});
            result.insert(diseasePicker);
            diseasePicker._getValue = function() {
              var results = [];
              var container = this.up('.field-box');
              if (container) {
                container.select('input[type=hidden][name=' + data.name + ']').each(function(item){
                  results.push(new Disorder(item.value, item.next('.value') && item.next('.value').firstChild.nodeValue || item.value));
                });
              }
              return [results];
            }.bind(diseasePicker);
            // Forward the 'custom:selection:changed' to the input
            var _this = this;
            document.observe('custom:selection:changed', function(event) {
              if (event.memo && event.memo.trigger && event.findElement() != event.memo.trigger && !event.memo.trigger._silent) {
                 Event.fire(event.memo.trigger, 'custom:selection:changed');
                _this.reposition();
              }
            });
            this._attachFieldEventListeners(diseasePicker, ['custom:selection:changed']);
            return result;
        },
        'select' : function (data) {
            var result = this._generateEmptyField(data);
            var select = new Element('select', {'name' : data.name});
            result.inputsContainer.insert(select);
            select.wrap('span');
            var _generateSelectOption = function(v) {
              var option = new Element('option', {'value' : v.actual}).update(v.displayed);
              select.insert(option);
            };
            if(data.nullValue) {
                _generateSelectOption({'actual' : '', displayed : '-'});
            }
            if (data.values) {
                data.values.each(_generateSelectOption);
            } else if (data.range) {
                $A($R(data.range.start, data.range.end)).each(function(i) {_generateSelectOption({'actual': i, 'displayed' : i + ' ' + data.range.item[+(i!=1)]})});
            }
            select._getValue = function() { return [(this.selectedIndex >= 0) && this.options[this.selectedIndex].value || '']; }.bind(select);
            this._attachFieldEventListeners(select, ['change']);
            return result;
        },
        'hidden' : function (data) {
            var result = this._generateEmptyField(data);
            result.addClassName('hidden');
            var input = new Element('input', {type: 'hidden', name: data.name, value: ''});
            result.update(input);
            return result;
        }
    },

    show : function(node, x, y) {
        this._onscreen = true;
        //console.log("nodeMenu show");
        this.targetNode = node;
        this._setCrtData(node.getSummary());
        this.menuBox.show();
        this.reposition(x, y);
        document.observe('mousedown', this._onClickOutside);
    },
        
    hide : function() {
        this._onscreen = false;
        //console.log("nodeMenu hide");
        document.stopObserving('mousedown', this._onClickOutside);
        if (this.targetNode) {
            this.targetNode.onWidgetHide();
            delete this.targetNode;
        }
        this.menuBox.hide();
        this._clearCrtData();
    },

    isVisible: function() {
        return this._onscreen;
    },

    _onClickOutside: function (event) {
        //console.log("nodeMenu clickoutside");
        if (!event.findElement('.menu-box') && !event.findElement('.calendar_date_select') && !event.findElement('.suggestItems')) {
            this.hide();
        }
    },

    reposition : function(x, y) {
      x = Math.floor(x);
      y = Math.floor(y);
      if (x !== undefined ) {
          if (this.canvas && x + this.menuBox.getWidth() > (this.canvas.getWidth() + 10)) {              
              var delta = x + this.menuBox.getWidth() - this.canvas.getWidth();
              editor.getWorkspace().panByX(delta, true);
              x -= delta;              
          }              
          this._currentX = x;
          this._currentY = y;
          this._height   = this.menuBox.getHeight();
          this.menuBox.style.height   = '';
          this.menuBox.style.overflow = '';
          this.menuBox.style.left = x + 'px';
      } else {
          if (this.menuBox.getHeight() <= this._height) return;
          this._height = this.menuBox.getHeight();
          x = this._currentX;
          y = this._currentY;
      }
      // Make sure the menu fits inside the screen
      if (this.canvas && this.menuBox.getHeight() >= (this.canvas.getHeight() + 5)) {
        this.menuBox.style.top = 0;
        this.menuBox.style.height = this.canvas.getHeight() + 'px';
        this.menuBox.style.overflow = 'auto';
      } else if (this.canvas.getHeight() < y + this.menuBox.getHeight() + 25) {   // fix a bug (?) in firefox & chrome where getHeight() is sligtly incorrect
        var diff = y + this.menuBox.getHeight() - this.canvas.getHeight() + 25;
        this.menuBox.style.top = (y - diff) + 'px';
      } else {
        this.menuBox.style.top = y + 'px';
      }
    },
    
    _clearCrtData : function () {
        var _this = this;
        Object.keys(this.fieldMap).each(function (name) {
            _this.fieldMap[name].crtValue = _this.fieldMap[name]["default"];
            _this._setFieldValue[_this.fieldMap[name].type].call(_this, _this.fieldMap[name].element, _this.fieldMap[name].crtValue);
            _this.fieldMap[name].inactive = false;
        });
    },

    _setCrtData : function (data) {
        var _this = this;
        Object.keys(this.fieldMap).each(function (name) {
            _this.fieldMap[name].crtValue = data && data[name] && typeof(data[name].value) != "undefined" ? data[name].value : _this.fieldMap[name].crtValue || _this.fieldMap[name]["default"];
            _this.fieldMap[name].inactive = (data && data[name] && (typeof(data[name].inactive) == 'boolean' || typeof(data[name].inactive) == 'object')) ? data[name].inactive : _this.fieldMap[name].inactive;
            _this.fieldMap[name].disabled = (data && data[name] && (typeof(data[name].disabled) == 'boolean' || typeof(data[name].disabled) == 'object')) ? data[name].disabled : _this.fieldMap[name].disabled;
            _this._setFieldValue[_this.fieldMap[name].type].call(_this, _this.fieldMap[name].element, _this.fieldMap[name].crtValue);
            _this._setFieldInactive[_this.fieldMap[name].type].call(_this, _this.fieldMap[name].element, _this.fieldMap[name].inactive);
            _this._setFieldDisabled[_this.fieldMap[name].type].call(_this, _this.fieldMap[name].element, _this.fieldMap[name].disabled);
            //_this._updatedDependency(_this.fieldMap[name].element, _this.fieldMap[name].element);
            //console.log("name = " + name + ", data = " + stringifyObject(data[name]) + ", inactive: " + stringifyObject(_this.fieldMap[name].inactive));            
        });
    },

    _setFieldValue : {
        'radio' : function (container, value) {
            var target = container.down('input[type=radio][value=' + value + ']');
            if (target) {
                target.checked = true;
            }
        },
        'checkbox' : function (container, value) {
          var checkbox = container.down('input[type=checkbox]');
          if (checkbox) {
           checkbox.checked = value;
          }
        },
        'text' : function (container, value) {
            var target = container.down('input[type=text]');
            if (target) {
                target.value = value;
            }
        },
        'textarea' : function (container, value) {
            var target = container.down('textarea');
            if (target) {
                target.value = value;
            }
        },
        'date-picker' : function (container, value) {
            var target = container.down('input[type=text].xwiki-date');
            if (target) {
                target.value = value && value.toFormattedString({'format_mask' : target.title}) || '';
                target.alt = value && value.toISO8601() || '';
                //Event.fire(target, 'xwiki:date:changed');
            }
        },
        'disease-picker' : function (container, values) {
            var _this = this;
            var target = container.down('input[type=text].suggest-omim');
            if (target && target._suggestPicker) {
                target._silent = true;
                target._suggestPicker.clearAcceptedList();
                if (values) {
                    values.each(function(v) {
                        target._suggestPicker.addItem(v.id, v.value, '');
                        _this._updateDisorderColor(v.id, editor.getDisorderLegend().getDisorderColor(v.id));
                    })
                }
                target._silent = false;
            }
        },
        'select' : function (container, value) {
            var target = container.down('select option[value=' + value + ']');
            if (target) {
                target.selected = 'selected';
            }
        },
        'hidden' : function (container, value) {
            var target = container.down('input[type=hidden]');
            if (target) {
                target.value = value;
            }
        }
    },

    _toggleFieldVisibility : function(container, doHide) {
        if (doHide) {
          container.addClassName('hidden');
        } else {
          container.removeClassName('hidden');
        }
    },

    _setFieldInactive : {
        'radio' : function (container, inactive) {
            if (inactive === true) {
                container.addClassName('hidden');
            } else {
                container.removeClassName('hidden');
                container.select('input[type=radio]').each(function(item) {                    
                    if (inactive && Object.prototype.toString.call(inactive) === '[object Array]') {                        
                        item.disabled = (inactive.indexOf(item.value) >= 0);
                        if (item.disabled)
                            item.up().addClassName('hidden');
                        else
                            item.up().removeClassName('hidden');
                    } else if (!inactive) {
                        item.disabled = false;
                        item.up().removeClassName('hidden');
                    }
                });
            }
        },
        'checkbox' : function (container, inactive) {
            this._toggleFieldVisibility(container, inactive);
        },
        'text' : function (container, inactive) {
            this._toggleFieldVisibility(container, inactive);
        },
        'textarea' : function (container, inactive) {
            this._toggleFieldVisibility(container, inactive);
        },
        'date-picker' : function (container, inactive) {
            this._toggleFieldVisibility(container, inactive);
        },
        'disease-picker' : function (container, inactive) {
            this._toggleFieldVisibility(container, inactive);
        },
        'select' : function (container, inactive) {
            this._toggleFieldVisibility(container, inactive);
        },
        'hidden' : function (container, inactive) {
            this._toggleFieldVisibility(container, inactive);
        }
    },

    _setFieldDisabled : {
        'radio' : function (container, disabled) {
            if (disabled === true) {
                container.addClassName('hidden');
            } else {
                container.removeClassName('hidden');
                container.select('input[type=radio]').each(function(item) {
                    if (disabled && Object.prototype.toString.call(disabled) === '[object Array]')
                        item.disabled = (disabled.indexOf(item.value) >= 0);
                    if (!disabled)
                        item.disabled = false;
                });
            }            
        },
        'checkbox' : function (container, disabled) {
            var target = container.down('input[type=checkbox]');
            if (target) {
                target.disabled = disabled;
            }
        },
        'text' : function (container, disabled) {
            var target = container.down('input[type=text]');
            if (target) {
                target.disabled = disabled;
            }
        },
        'textarea' : function (container, inactive) {
            // FIXME: Not implemented
        },
        'date-picker' : function (container, inactive) {
            // FIXME: Not implemented
        },
        'disease-picker' : function (container, inactive) {
            // FIXME: Not implemented
        },
        'select' : function (container, inactive) {
            // FIXME: Not implemented
        },
        'hidden' : function (container, inactive) {
            // FIXME: Not implemented
        }
    }
});