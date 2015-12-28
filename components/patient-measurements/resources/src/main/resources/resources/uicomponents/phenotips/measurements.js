var PhenoTips = (function(PhenoTips) {
  // Start PhenoTips augmentation
  var widgets = PhenoTips.widgets = PhenoTips.widgets || {};

  widgets.Measurements = Class.create({
    initialize: function(el) {
      this.el = el;
      this.measurementClassId = parseInt($('measurementclass-max-id').getAttribute('data-value'));

      this.getObject = this.getObject.bind(this);
      this._sets = [];

      var measurementSetsEls = this.el.select('.measurement-set:not(.proto)');
      if (!measurementSetsEls.length) {
        this._addMeasurementSet();
      } else {
        measurementSetsEls.each((function(el) {
          var newSet = new PhenoTips.widgets.MeasurementSet(el, this);
          this._sets.push(newSet);
        }).bind(this));
      }

      if (XWiki.contextaction == 'edit') {
        this._handleAddMeasurementSet();

        if (this.el.hasClassName('enable-unit-switch')) {
          new PhenoTips.widgets.UnitConverter(
             this.el.up(),
             '.measurement-row:not(.computed) input[type="text"]:not([name$="date"]):not([disabled])',
             this.el.up(),
             'before',
             this.el.hasClassName('imperial') ? 'imperial' : ''
          );
        }
      }

      // Create a warning when measurements are being entered but no birth date is specified
      var dateOfBirth = $(document.documentElement).down('input[name$=date_of_birth]');
      if (dateOfBirth) {
        _updateNoBirthDateWarning();
        dateOfBirth.observe('xwiki:date:changed', _updateNoBirthDateWarning);
      }
      function _updateNoBirthDateWarning(){
        birthdate = dateOfBirth.value;
        var warningInsertionPoint = $$('.measurement-info > .expand-tools')[0] ? $$('.measurement-info > .expand-tools')[0] : $('HMeasurements');
        if (!warningInsertionPoint) { return; }
        if (!birthdate && !$('no-birth-date-warning')) {
          warningInsertionPoint.insert({
            after:"<div id ='no-birth-date-warning' class='box infomessage'>$services.localization.render('phenotips.patientSheetCode.noBirthdateWarning')</div>"
          });
        } else if (birthdate && $('no-birth-date-warning')) {
          $('no-birth-date-warning').remove();
        }
      };
      // ------------------------------------------------------------------------------------------
      // And one more thing (not exactly related to measurements directly:
      // If we're collecting data about gestational age, select premature birth for low gestational ages
      var gaInput = $$('input[type="text"][name$="_gestation"]');
      var gaTermInput = $$('input[type="checkbox"][name$="_gestation"]');
      gaInput = gaInput.size() > 0 && gaInput[0] || '';
      gaTermInput = gaTermInput.size() > 0 && gaTermInput[0] || '';
      var pbInput = findFormElementForPhenotype('HP:0001622', false, 'prenatal');
      if (gaInput && pbInput) {
        gaInput.observe('blur', function (event) {
          var ga = parseInt(gaInput.value);
          var targetElt = pbInput;
          if (!isNaN(ga)) {
            if (ga < 37) {
              if (!targetElt.checked) {
                if (targetElt._ynpicker) {
                  targetElt._ynpicker._onClick(false, 'yes');
                } else {
                  targetElt.checked = true;
                }
                highlightChecked(targetElt);
              }
            } else {
              if (targetElt._ynpicker) {
                targetElt._ynpicker._onClick(false, 'no');
                highlightChecked(targetElt);
              }
            }
          } else {
            if (targetElt._ynpicker) {
              targetElt._ynpicker._onNA();
              highlightChecked(targetElt);
            }
          }
        });
      }
      // FIXME This code is a bit fragile... Both inputs should be handled in the same method
      if (gaTermInput && pbInput) {
        gaTermInput.observe('blur', function (event) {
          var targetElt = pbInput;
          if (gaTermInput.checked && targetElt._ynpicker) {
            targetElt._ynpicker._onClick(false, 'no');
            highlightChecked(targetElt);
          }
        });
      }
    
      this._charts = new PhenoTips.widgets.MeasurementsCharts($('charts-new'), this);

      return this;
    },
    _handleAddMeasurementSet: function() {
      var that = this;
      $('btn_add-measurement-set').observe('click', function(e) {
        that._addMeasurementSet();

        e.preventDefault(); 
        return false;
      });
    },
    _addMeasurementSet: function() {
      var clone = this.el.select('.measurement-set.proto')[0].clone(true);
      clone.removeClassName('proto');
      this.el.down('.list-actions').insert({before: clone});
      this._sets.push(new PhenoTips.widgets.MeasurementSet(clone, this));
      Event.fire(document, 'xwiki:dom:updated', {'elements' : [clone]});
    },

    getObject: function() {
      if (XWiki.contextaction == 'edit') {
        var sex = ($$('[name$=gender]:checked')[0] || {value:''}).value;
      } else {
        var sex = $('measurements-gender').readAttribute('data-value');
      }

      var obj = {
        sex: sex,
        measurementSets: []
      }

      for (var i = 0; i < this._sets.length; i++) {
        obj.measurementSets.push(this._sets[i].getObject());
      }

      return obj;
    }
  });

  widgets.MeasurementsCharts = Class.create({
    initialize: function(el, parent) {
      this.el = el; this.parent = parent;

      // DOM shortcuts
      this._thumbsEl = this.el.down('.chart-thumbs');

      // Bind methods
      this._setChartsWidth = this._setChartsWidth.bind(this);
      this._updateCharts = this._updateCharts.bind(this);
      this.selectChart = this.selectChart.bind(this);
      this._chartResourcesResponseHandler = this._chartResourcesResponseHandler.bind(this);
      this._addThumb = this._addThumb.bind(this);

      if (XWiki.contextaction == 'view') {
        this._setChartsWidth();
        this._updateCharts();
        Event.observe(window, 'resize', this._setChartsWidth);
      } else {
        // Event handlers
        $$('.measurement-info.chapter')[0].observe('chapter:show', (function() {
          if (!this._stickyBox) {
            this._setChartsWidth();
            this._initStickyBox();
          } else {
            this._stickyBox.makeDefault();
            this._setChartsWidth();
            this._stickyBox.resetPosition();
          }
        }).bind(this));
        ['change', 'age:change', 'measurementSet:delete'].each((function(ev) {
          $('measurements').observe(ev, this._updateCharts);
        }).bind(this));
        Event.observe(window, 'resize', this._setChartsWidth);

        $$('input[name$=gender]').each((function(el) {
          el.observe('click', this._updateCharts);
        }).bind(this));

        // Init using existing data
        this._updateCharts();

        if (!this._mainImgEl) {
          this._addPlaceholder();
        }
      }
    },

    _addPlaceholder: function() {
      var infoEl = new Element('div');
      infoEl.addClassName('infomessage');
      infoEl.innerHTML = "$services.localization.render('phenotips.patientSheet.measurements.chartPlaceholderMsg')";

      var url = new XWiki.Document("ChartService", "PhenoTips").getURL('get', 'a=-1&weight=1&standalone=1&n=weight&s=M&f=svg');
      var mainChartEl = new Element('a');
      mainChartEl.setAttribute('href', url.replace("&f=svg", ""));
      mainChartEl.setAttribute('target', "_blank");

      var mainChartImgEl = new Element('img');
      mainChartImgEl.setAttribute('src', url);
      mainChartImgEl.addClassName('main');
      mainChartEl.insert(mainChartImgEl);

      this.el.insert(infoEl);
      this.el.insert({bottom: mainChartEl});
      this._mainImgEl = mainChartEl;
    },

    _setChartsWidth: function() {
      var chartsWidth = $('measurements-container').getWidth() - $('measurements').getWidth();
      $('charts-new').style.width = chartsWidth - 20 + 'px';
    },

    _initStickyBox: function() {
      this._stickyBox = new StickyBox($('charts-new'), $('measurements-container'), {offsetTop: 30, offsetBottom: 40, resize: true});
    },

    _updateCharts: function() {
      var post = this.parent.getObject();

      var shouldSend = false;
      for (var i = 0; i < post.measurementSets.length; i++) {
        if (post.measurementSets[i].age.length && Object.keys(post.measurementSets[i].measurements).length) {
          shouldSend = true;
        }
      }
      shouldSend &= !!post.sex;

      if (shouldSend) {
        var ajx = new Ajax.Request(XWiki.contextPath+"/rest/measurements/chart-resources", {
          method: 'post',
          postBody: JSON.stringify(post),
          requestHeaders: {Accept : "application/json"},
          contentType: "application/json",
          onSuccess: this._chartResourcesResponseHandler,
          onFailure: function (response) {}
        });
      } else {
        this._chartResourcesResponseHandler({responseJSON: {charts: {}}});
      }
    },

    _chartResourcesResponseHandler: function(resp) {
      var charts = resp.responseJSON.charts;

      // Delete charts that are no longer present
      this.el.select('.chart-thumbs > div').each((function(el) {
        for (var i = 0; i < charts.length; i++) {
          if (charts[i].url == el.down('img').src) {
            return;
          }
        }
        if (this._mainImgEl && this._mainImgEl.down('img').src == el.down('img').src) {
          this._mainImgEl.remove(); this._mainImgEl = undefined;
        }
        el.remove();
      }).bind(this));

      // Add new charts
      var els = this.el.select('.chart-thumbs > div');
      var i = 0; j = 0;
      while (i < charts.length) {
        if (els && j < els.length && charts[i].url == els[j].down('img').src) {
          j++;
        } else {
          var below = (els && j < els.length) ? els[j] : undefined;

          var infoEl = this.el.down('.infomessage');
          if (infoEl) {
            infoEl.remove();
            this._mainImgEl.remove();
            this._mainImgEl = undefined;
          }

          var thumbEl = this._addThumb(charts[i], below);
          if (!this._mainImgEl) {
            this.selectChart(thumbEl);
          }
        }
        i++;
      }
    },

    _addThumb: function(chart, before) {
      var thumbEl = new Element('div');

      var titleEl = new Element('span');
      titleEl.innerHTML = chart.title;
      thumbEl.insert(titleEl);

      var imageEl = new Element('img');
      imageEl.setAttribute('src', chart.url);
      thumbEl.observe('click', (function(e) {
        this.selectChart(e.currentTarget);
      }).bind(this));
      thumbEl.insert(imageEl);

      if (before) {
        before.insert({before: thumbEl});
      } else {
        this._thumbsEl.insert(thumbEl);
      }

      return thumbEl;
    },

    selectChart: function(thumbEl) {
      thumbEl.addClassName('selected');
      thumbEl.siblings().invoke('removeClassName', 'selected');

      var url = thumbEl.down('img').src;

      var mainChartEl = new Element('a');
      mainChartEl.setAttribute('href', url.replace("&f=svg", ""));
      mainChartEl.setAttribute('target', "_blank");

      var mainChartImgEl = new Element('img');
      mainChartImgEl.setAttribute('src', url);
      mainChartImgEl.addClassName('main');
      mainChartEl.insert(mainChartImgEl);

      this._mainImgEl && this._mainImgEl.remove();
      this.el.insert({bottom: mainChartEl});
      this._mainImgEl = mainChartEl;
    },
  });

  widgets.MeasurementSet = Class.create({
    initialize: function(el, parent) {
      this.el = el;
      this.parent = parent;

      // DOM node shortcuts
      this._dateEl = this.el.select('input.measurement-date')[0];
      this._ageEl = this.el.select('input.measurement-age')[0];
      this._globalDobEl = $(document.documentElement).down('input[name$=date_of_birth]');
      this._moreContainer = this.el.select('div.expanded-measurements')[0];

      // Bind methods
      this._setAgeUsingDateAndDob = this._setAgeUsingDateAndDob.bind(this);
      this._updateHiddenFields = this._updateHiddenFields.bind(this);
      this._globalDobChangeHandler = this._globalDobChangeHandler.bind(this);
      this._moreToggleButtonHandler = this._moreToggleButtonHandler.bind(this);
      this._deleteHandler = this._deleteHandler.bind(this);
      this.getObject = this.getObject.bind(this);

      // Init rows
      this._rows = [];
      this.el.select('.measurement-row').each((function(el) {
        if (el.hasClassName('computed')) {
          this._rows.push(new widgets.ComputedMeasurementSetRow(el, this));
        } else {
          this._rows.push(new widgets.MeasurementSetRow(el, this));
        }
      }).bind(this));

      if (XWiki.contextaction == 'edit') {
        // Init datepicker
        if (!this._dateEl.hasClassName('initialized')) {
          this._datePicker = new XWiki.widgets.DateTimePicker(this._dateEl, this._dateEl.title);
        }

        // Init hidden inputs
        var _this = this;
        this.el.select('.measurement-row').each(function(measurementRow) {
          measurementRow.select('input').each(function(input) {
            input.name = input.name.replace('[id]', _this.parent.measurementClassId);
            input.disabled = false;
          });

          _this.parent.measurementClassId++;
        });

        // Initialize using existing values
        this._updateHiddenFields('date', this._dateEl.value);
        this._updateHiddenFields('age', this._ageEl.value);

        // Attach handlers
        this._dateEl.observe('xwiki:date:changed', function(e) {
          _this._updateHiddenFields('date', e.target.value);
          _this._ageEl.readOnly = e.target.alt.length > 0;

          _this._setAgeUsingDateAndDob();
        });
        ['input', 'keypress'].each(function(ev) {
          _this._ageEl.observe(ev, function(e) {
            _this._dateEl.readOnly = e.target.value.trim().length > 0;
          });
        });
        _this._ageEl.observe('duration:format', function(e) {
          _this._updateHiddenFields('age', e.target.value);
        });
        
        this._globalDobEl.observe('xwiki:date:changed', this._globalDobChangeHandler);
        this.el.select('.expand-buttons .buttonwrapper').each((function(el) {
          el.observe('click', this._moreToggleButtonHandler);
        }).bind(this));
        this.el.select('.delete')[0].observe('click', this._deleteHandler);

        // Hide (or show)
        var hasExpandedValue = false;
        this.el.select('.expanded-measurements input[name$=_value]').each(function(el) {
          if (el.value.trim().length > 0) {
            hasExpandedValue = true;
          }
        });
        if (hasExpandedValue) {
          this.el.select('.expand-buttons .buttonwrapper.show')[0].hide();
        } else {
          this._moreContainer.hide();
          this.el.select('.expand-buttons .buttonwrapper.hide')[0].hide();
        }
      }

      return this;
    },

    _globalDobChangeHandler: function() {
      if (this.el.select('.measurement-date')[0].alt.length) {
        this._setAgeUsingDateAndDob();
      }
    },

    _updateHiddenFields: function(fieldName, value) {
      this.el.select('input[name$=_'+fieldName+']').each(function(input) {
        input.value = value;
      });
    },

    _deleteHandler: function(event) {
      event.stop();
      var deleteTrigger = event.element();
      if (deleteTrigger.disabled) {
        return;
      }
      this.el.fire('measurementSet:delete');
      var removeElement = (function() {
        this.destroy();
        this.el.remove();
      }).bind(this);

      if (this._ageEl.value.length) {
        var qs = "document="+XWiki.Document.currentSpace+"."+XWiki.Document.currentPage;
        qs += "&age="+this._ageEl.value;
        var url = new XWiki.Document("MeasurementSetDeletionService", "PhenoTips").getURL('get', qs);
        new XWiki.widgets.ConfirmedAjaxRequest(url, {
          onCreate : function() {
            deleteTrigger.disabled = true;
          },
          onSuccess : removeElement,
          onComplete : function() {
            deleteTrigger.disabled = false;
          }
        },
        {
          confirmationText : "$services.localization.render('phenotips.patientSheet.measurements.confirmDeleteSet')"
        });
      } else {
        removeElement();
      }
    },

    _setAgeUsingDateAndDob: function() {
      var dateEl = this.el.select('.measurement-date')[0];
      if (dateEl.alt.length) {
        if (!this._globalDobEl.alt.length) return;

        var bday = Date.parseISO_8601(this._globalDobEl.alt || '');
        bday.setTimezoneOffset((new Date()).getTimezoneOffset() * -10/3);
        var age = new TimePeriod(bday, new Date(dateEl.alt));
        var dateDisplayParts = {
          y: age.years,
          m: age.months,
          d: age.days,
        };
        var ageStr = "";
        Object.keys(dateDisplayParts).each(function(k) {
          if (dateDisplayParts[k] > 0) ageStr += dateDisplayParts[k] + k;
        })
        this._ageEl.value = ageStr;
        this._ageEl.fire("age:change");
        this._updateHiddenFields('age', ageStr);
      } else {
        this._ageEl.value = '';
        this._ageEl.fire("age:change");
        this._updateHiddenFields('age', '');
      }
    },

    _moreToggleButtonHandler: function(e) {
      this._moreContainer.toggle();

      this.el.select('.expand-buttons .buttonwrapper').each(function(el) {
        el.toggle();
      })
    },

    destroy: function() {
      this._globalDobEl.stopObserving('xwiki:date:changed', this._globalDobChangeHandler);
    },

    getObject: function() {
      var obj = {
        age: XWiki.contextaction == 'view' ? this.el.down('div.age > span').innerHTML : this._ageEl.value,
        measurements: {}
      };

      for (var i = 0; i < this._rows.length; i++) {
        var row = this._rows[i];
        if (row.getValue().length) {
          obj.measurements[row.getMeasurementType()] = row.getValue();
        }
      }

      return obj;
    },
  });

  widgets.MeasurementSetRow = Class.create({
    initialize: function(el, parent) {
      this.el = el; this.parent = parent;

      // Bind methods
      this.fetchAndRenderPercentileSd = this.fetchAndRenderPercentileSd.bind(this);
      this.destroy = this.destroy.bind(this);
      this._genderChangeHandler = this._genderChangeHandler.bind(this);
      this.getMeasurementType = this.getMeasurementType.bind(this);
      this.getValue = this.getValue.bind(this);
      this._selectAssocPhenotypes = this._selectAssocPhenotypes.bind(this);
      this._selectMeasurementTerm = this._selectMeasurementTerm.bind(this);
      this._unselectMeasurementTerm = this._unselectMeasurementTerm.bind(this);
      this._selectFormElementForPhenotype = this._selectFormElementForPhenotype.bind(this);

      if (XWiki.contextaction == 'edit') {
        // DOM shortcuts
        this._valueEl = this.el.select('[name$=value]')[0];

        // Initialize using existing values
        this.fetchAndRenderPercentileSd();

        // Attach handlers
        ['input', 'phenotips:measurement-updated'].each((function(ev) {
          this._valueEl.observe(ev, this.fetchAndRenderPercentileSd);
        }).bind(this));
        ['age:change', 'duration:format'].each((function(ev) {
          this.parent._ageEl.observe(ev, this.fetchAndRenderPercentileSd);
        }).bind(this));
        $$('input[name$=gender]').each((function(el) {
          el.observe('click', this._genderChangeHandler);
        }).bind(this));
      }
    },

    getMeasurementType: function() {
      return this.el.down('[name$=type]').value;
    },

    getValue: function() {
      if (XWiki.contextaction == 'edit') {
        return this._valueEl.value;
      } else {
        return this.el.down('span.val').innerHTML;
      }
    },

    fetchAndRenderPercentileSd: function(e) {
      var pctlEl = this.el.select('.pctl')[0];
      var fetchParams = {
        'measurement': this.el.select('[name$=type]')[0].value,
        'value': this._valueEl.value,
        'age': this.parent._ageEl.value,
        'sex': ($$('[name$=gender]:checked')[0] || {value:''}).value,
      };
      // Normalize sex for the purposes of measurements, assuming other == male
      if (fetchParams.sex.length && fetchParams.sex.toLowerCase() != 'f') {
        fetchParams.sex == 'm';
      }

      if (this._valueEl.value.length && fetchParams.sex.length && fetchParams.age.length && fetchParams.sex.length) {
        var ajx = new Ajax.Request(XWiki.contextPath+"/rest/measurements/percentile", {
          method: 'get',
          parameters: fetchParams,
          requestHeaders: {Accept : "application/json"},
          onSuccess: (function(resp) {
            this._renderPercentileSd(pctlEl, resp.responseJSON);
            this._selectAssocPhenotypes(resp.responseJSON['associated-terms']);
          }).bind(this),
          onFailure: function (response) {}
        });
      } else {
        this._renderPercentileSd(pctlEl, null);
      }
    },

    _genderChangeHandler: function(e) {
      this.fetchAndRenderPercentileSd();
    },

    _renderPercentileSd: function(el, values) {
      var displayStr;
      if (values == null) {
        displayStr = '';
      } else {
        ['normal', 'below-normal', 'above-normal', 'extreme-below-normal', 'extreme-above-normal'].each(function(className) {
          el.removeClassName(className);
        });

        el.addClassName(values['fuzzy-value']);

        displayStr = values.percentile;
        displayStr += this._getOrdinalSuffix(values.percentile);
        displayStr += ' pctl'; // TODO: translate
        displayStr += ' (' + values.stddev.toFixed(2) + 'SD)'; // TODO: translate
      }

      el.innerText = displayStr;
    },

    _selectAssocPhenotypes: function(terms) {
      var allTerms = this.measurementTypeToAssocTerms[this.getMeasurementType()];
      if (allTerms && allTerms.length) {
        allTerms.each((function(term) {
          this._unselectMeasurementTerm(findFormElementForPhenotype(term));
        }).bind(this));
      }

      terms.each((function(term) {
        var targetEl = this._selectFormElementForPhenotype(term);
        this._selectMeasurementTerm(targetEl);
      }).bind(this));
    },

    _selectMeasurementTerm : function (elt, force) {
      if (elt && (!elt.checked || force)) {
        if (elt._ynpicker) {
          elt._ynpicker._select('yes');
        } else {
          elt.checked = true;
        }
        highlightChecked(elt);
        var addedItem = $('current-phenotype-selection').down('label input[value="' + elt.value + '"]');
        if (addedItem) {
          addedItem.up('label').insert({before : new Element('span', {
                                                               'class' : 'fa fa-bolt',
                                                               'title' : 'This phenotype was automatically added based on abnormal measurement values'
          }).update(' ')});
        }
      }
    },

    _unselectMeasurementTerm : function (elt) {
      if (elt && elt.checked) {
        if (elt._ynpicker) {
          elt._ynpicker._onUnselect();
        } else {
          elt.checked = false;
        }
      }
    },

    _selectFormElementForPhenotype: function(id) {
      if (!id) {
        return null;
      }
      var _this = this;
      var existingElement = findFormElementForPhenotype(id);
      if (existingElement == null) {
        var suggestWidget = $("quick-phenotype-search")._suggestWidget;
        if (!suggestWidget) {
           return null;
        }
        searchURL = new XWiki.Document('SolrService', 'PhenoTips').getURL("get", "q=" + id);
        new Ajax.Request(searchURL, {
          method: 'get',
          onSuccess: function(transport) {
            var response = transport.responseJSON.rows[0];
            if (response) {
              var categories = '<span class="hidden term-category">';
              response.term_category.each(function(category){categories += '<input type="hidden" value="' + category + '">'});
              categories + "</span>";
              var data = {'id':id, 'value':response.name, 'category': categories}
              var title =  response.name;
              suggestWidget.acceptEntry(data, title, title);
              _this._selectMeasurementTerm(findFormElementForPhenotype(id), true);
            }
          }
        });
        return null;
      } else {
        return existingElement;
      }
    },

    // Credit: http://stackoverflow.com/a/13627586
    _getOrdinalSuffix: function(i) {
      var j = i % 10,
          k = i % 100;
      if (j == 1 && k != 11) {
          return "st";
      }
      if (j == 2 && k != 12) {
          return "nd";
      }
      if (j == 3 && k != 13) {
          return "rd";
      }
      return "th";
    },

    measurementTypeToAssocTerms:
    #set ($handlers = $services.measurements.getAvailableMeasurementHandlers())
    #set ($map = {})
    #foreach ($handler in $handlers)
      #set ($assocTerms = $handler.getAssociatedTerms(null))
      #set ($assocTermStrings = [])
      #foreach ($term in $assocTerms)
        #set ($discard = $assocTermStrings.add($term.getId()))
      #end
      #set ($discard = $map.put($handler.getName(), $assocTermStrings))
    #end
    $jsontool.serialize($map)
    ,

    destroy: function() {
      $$('input[name$=gender]').each((function(el) {
        el.stopObserving('click', this._genderChangeHandler);
      }).bind(this));
    }
  });

  widgets.ComputedMeasurementSetRow = Class.create(widgets.MeasurementSetRow, {
    initialize: function($super, el, parent) {
      $super(el, parent);

      this.fetchAndRenderComputedValue = this.fetchAndRenderComputedValue.bind(this);
      this._getDependentMeasurementField = this._getDependentMeasurementField.bind(this);

      if (XWiki.contextaction == 'edit') {
      	this._displayEl = this.el.down('span.val');

        // Init field
        this._valueEl.readOnly = true;

        // Set up deps
        this._deps = this.measurementTypeToComputationDeps[this.getMeasurementType()];

        // Initialize using existing values
        this.fetchAndRenderComputedValue();

        // Attach deps handlers
        for (var i = 0; i < this._deps.length; i++) {
          ['input'].each((function(ev) {
            this._getDependentMeasurementField(this._deps[i]).observe(ev, this.fetchAndRenderComputedValue);
          }).bind(this));
        }
      }
    },

    _getDependentMeasurementField: function(measurement) {
      return this.parent.el.select('input[name$=type][value='+measurement+']')[0].up(0).select('input[name$=value]')[0];
    },

    fetchAndRenderComputedValue: function(e) {
      var canCalc = true;
      var fetchParams = {
        measurement: this.el.select('[name$=type]')[0].value,
      };
      for (var i = 0; i < this._deps.length; i++) {
        fetchParams[this._deps[i]] = this._getDependentMeasurementField(this._deps[i]).value;
        canCalc &= fetchParams[this._deps[i]].length > 0;
      }

      if (canCalc) {
        var ajx = new Ajax.Request(XWiki.contextPath+"/rest/measurements/computed", {
          method: 'get',
          parameters: fetchParams,
          requestHeaders: {Accept : "application/json"},
          onSuccess: (function(resp) {
            this._valueEl.value = resp.responseJSON.value.toFixed(2);
            this._displayEl.removeClassName('empty');
            this._displayEl.innerHTML = resp.responseJSON.value.toFixed(2);
            this.fetchAndRenderPercentileSd();
          }).bind(this),
          onFailure: function (response) {}
        });
      } else {
        this._valueEl.value = '';
        this._displayEl.innerHTML = '-';
        this._displayEl.addClassName('empty');
        this.fetchAndRenderPercentileSd();
      }
    },

    measurementTypeToComputationDeps:
    #set ($handlers = $services.measurements.getAvailableMeasurementHandlers())
    #set ($map = {})
    #foreach ($handler in $handlers)
      #if ($handler.getComputationDependencies())
        #set ($discard = $map.put($handler.getName(), $handler.getComputationDependencies()))
      #end
    #end
    $jsontool.serialize($map)
    ,
  });

  var init = function(event) {
    if (XWiki.contextaction == 'edit') {
      new PhenoTips.widgets.Measurements($('measurements'));
    }
  };

  (XWiki.domIsLoaded && init()) || document.observe("xwiki:dom:loaded", init);

  // End PhenoTips augmentation.
  return PhenoTips;
 }(PhenoTips || {}));