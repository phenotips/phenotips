var PhenoTips = (function (PhenoTips) {
  var widgets = PhenoTips.widgets = PhenoTips.widgets || {};
  widgets.UnitConverter = Class.create({

     CONVERSION_META : {
        'weight': {
           'imperial_units' : ['lb', 'oz'],
           'metric_unit' : 'kg',
           'inter_unit_scale' : 16,
           'inter_system_scale' : 0.0283495
        },
        'length': {
           'imperial_units' : ['ft', 'in'],
           'metric_unit' : 'cm',
           'inter_unit_scale' : 12,
           'inter_system_scale' : 2.54
        }
     },

     DEFAULT_UNIT_SYSTEM : 'metric',

     initialize : function (container, selector, triggerInsertionElt, triggerInsertionPosition, system) {
       this._selector = selector;
       this._container = container || document.documentElement;
       if (!this._selector || !triggerInsertionElt) {return;}

       this.crtUnitSystem = system || this.DEFAULT_UNIT_SYSTEM;

       this.initializeElements = this.initializeElements.bind(this);
       this.attachConverter = this.attachConverter.bind(this);

       this.generateTrigger(triggerInsertionElt, triggerInsertionPosition || 'bottom');

       this.initializeElements();
       var _this = this;
       document.observe('xwiki:dom:updated', function(event) {
         if (event.memo && event.memo.elements) {
           event.memo.elements.each(_this.initializeElements.bind(_this));
         }
       });
     },

     generateTrigger : function(atElement, position) {
       this._trigger = new Element('select', {"class" : "unit-system-selector"});

       var optionMetric = new Element('option', {
                value : 'metric',
         }).update('Metric units (' + this.CONVERSION_META.weight.metric_unit + ', ' + this.CONVERSION_META.length.metric_unit + ')');
       if (this.crtUnitSystem == 'metric') {
          optionMetric.selected = 'selected';
       }
       var optionImperial = new Element('option', {
                value : 'imperial',
         }).update('Imperial units (' + this.CONVERSION_META.weight.imperial_units.join(' / ') + ', ' + this.CONVERSION_META.length.imperial_units.join(' / ') + ')');
       if (this.crtUnitSystem == 'imperial') {
          optionImperial.selected = 'selected';
       }
       this._trigger.insert(optionMetric).insert(optionImperial);

       insertionInfo = {};
       insertionInfo[position] = this._trigger;
       atElement.insert(insertionInfo);

       var _this = this;
       this._trigger.observe('change', function(event) {
         _this.crtUnitSystem = _this._trigger.options[_this._trigger.selectedIndex].value;
         _this.switchUnits(_this.crtUnitSystem);
       });
     },

     initializeElements : function(element) {
       container = element || this._container;
       if (container.__unitSwitcher || (!container.up('.measurements') && !container.up('#measurements') && !container.hasClassName('measurements') && !container.id == 'measurements')) {
         return;
       }
       container.__unitSwitcher = this;
       container.select(this._selector).each(this.attachConverter);
       this.switchUnits(this.crtUnitSystem, container);
     },

     switchUnits : function(type, element) {
       container = element || this._container;
       container.select('.unit-conversion-values .unit-type').each(function(item) {
          if (item.hasClassName(type)) {
            item.show();
          } else {
            item.hide();
          }
       });
     },

     attachConverter : function(element) {
       if (element.tagName.toLowerCase() != 'input' || element.type != 'text') {
         return;
       }
       var unitElt = element.next('.unit');
       var converterElement = new Element('div', {'class' : 'unit-conversion-values'});
       var imperialUnits;
       var type = (element.up('.weight')) ? 'weight' : 'length';

       converterElement.addClassName(type);
       converterElement._meta = this.CONVERSION_META[type];

       var values = this.metricToImperial(converterElement._meta, parseFloat(element.value) || 0);

       var metricZone = element.up('.metric');
       if (!metricZone) {
         metricZone = new Element('div', {'class' : 'unit-type metric'});
         metricZone.insert(element).insert(unitElt || converterElement._meta.metric_unit);
         element.insert({after: converterElement});
       } else {
         metricZone.addClassName('unit-type');
         metricZone.insert({after: converterElement});
       }
       var imperialZone = new Element('div', {'class' : 'unit-type imperial'});
       converterElement.insert(metricZone).insert(imperialZone);

       converterElement._meta.imperial_units.each(function (unit) {
         imperialZone.insert(new Element('label').insert(new Element('input', {'style' : 'width: auto', 'name' : unit, type: 'text', size : 3, value : (values[unit] || '')})).insert(unit));
       });

       this.enableSyncValues(converterElement);
     },

     enableSyncValues : function (element) {
       var _this = this;
       element.select('.imperial input').invoke('observe', 'change', function(event) {_this.syncMetricWithImperial(element);});
       element.select('.metric input').invoke('observe', 'change', function(event) {_this.syncImperialWithMetric(element);});
     },

     syncMetricWithImperial : function (element) {
        var metricInput =  element.down('.metric input');
        metricInput.value  = this.imperialToMetric(element._meta,
                                                      parseFloat(element.down('.imperial input[name="' + element._meta.imperial_units[0] + '"]').value) || 0,
                                                      parseFloat(element.down('.imperial input[name="' + element._meta.imperial_units[1] + '"]').value) || 0
                                                ) || '';
        Event.fire(metricInput, 'phenotips:measurement-updated');
     },

     syncImperialWithMetric : function (element) {
        var imperialValues = this.metricToImperial(element._meta, parseFloat(element.down('.metric input').value) || 0);
        element._meta.imperial_units.each(function (unit) {
          element.down('.imperial input[name="' + unit + '"]').value = imperialValues[unit] || '';
        });
     },

     metricToImperial : function (conversionMeta, value) {
        var result = {};
        var lowerUnitValue = value / conversionMeta.inter_system_scale;
        var higherUnitValue = Math.floor (lowerUnitValue / conversionMeta.inter_unit_scale);
        lowerUnitValue = lowerUnitValue - higherUnitValue * conversionMeta.inter_unit_scale;
        if (lowerUnitValue) {
          lowerUnitValue = lowerUnitValue.toFixed(2);
        }
        result[conversionMeta.imperial_units[0]] = higherUnitValue;
        result[conversionMeta.imperial_units[1]] = lowerUnitValue;
        return result;
     },

     imperialToMetric : function (conversionMeta, higherUnitValue, lowerUnitValue) {
        return ((conversionMeta.inter_unit_scale * higherUnitValue + lowerUnitValue) * conversionMeta.inter_system_scale).toFixed(2);
     }
  });
  return PhenoTips;
}(PhenoTips || {}));