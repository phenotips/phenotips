// Functions in this file provide functionality which is not present in
// one of { InternetExplorer 8, Chrome v28, Firefox 3 } but may be present
// in later verisons of the browsers and/or libriries such as Ext/Prototype
// However the goal was to release a dependency-free package, thus some
// of the mehtods available in other libraries were re-implemented.
// None of the methods modify any of the built-in type prototypes.
// Loop types are picked based on http://jsperf.com/loops/128

define ([], function(){

    var Helpers = {};
    
    // Fixes bug introduced after utilizing requirejs where the global console variable becomes undefined.
    if (!console) { var console = window.console; }
    
    // To allow debug code to run in IE7 && IE8
    if (!window.console) { var console = {log: function() {}}; }

    // For IE7 && IE8 again
    if(typeof String.prototype.trim !== 'function') {
      String.prototype.trim = function() {
        return this.replace(/^\s+|\s+$/g, '');
      }
    }

    // And again (IE7 && IE8 fix)
    if (!Array.prototype.forEach)
    {
      Array.prototype.forEach = function(fun)
      {
        var t = Object(this);
        var len = t.length >>> 0;
        if (typeof fun !== "function")
          throw new TypeError();

        var thisArg = arguments.length >= 2 ? arguments[1] : void 0;
        for (var i = 0; i < len; i++)
        {
          if (i in t)
            fun.call(thisArg, t[i], i, t);
        }
      };
    }

    // Fix for Safari v4 && v5
    if (typeof HTMLElement !== 'undefined' && !HTMLElement.prototype.click && document.createEvent) {
        HTMLElement.prototype.click = function()
        {
            var eventObj = document.createEvent('MouseEvents');
            eventObj.initEvent('click',true,true);
            this.dispatchEvent(eventObj);
        }
    }

    // Used for: cloning a 2D array of integers (i.e. no deep copy of elements is necessary)
    // Specific implementation is pciked based on http://jsperf.com/clone-2d-array/4
    Helpers.clone2DArray = function (arr2D) {
        var new2D = [];
        for (var i = 0; i < arr2D.length; ++i) {
            new2D.push(arr2D[i].slice());
        }
        return new2D;
    }

    // Creates a shallow copy of the given object
    // Specific implementation is picked based on http://jsperf.com/cloning-an-object/4
    Helpers.cloneObject = function(obj) {
        var target = {};
        for (var i in obj) {
            if (obj.hasOwnProperty(i))
                target[i] = obj[i];
        }
        return target;
    }

    // (Recursively) for every property in template check if data has the same property
    // and set template value to the one in data.
    Helpers.setByTemplate = function(template, data) {
        if (typeof template !== 'object' || typeof data !== 'object') {
            return;
        }
        for (var key in template) {
            if (data.hasOwnProperty(key) && template.hasOwnProperty(key)) {
                if (typeof template[key] === 'object') {
                    this.setByTemplate(template[key], data[key]);
                } else {
                    template[key] = data[key];
                }
            }
        }
    }

    // Assigns values to all properties which are set in Source to the Target
    Helpers.copyProperties = function(objectSource, objectTarget) {
      for (var p in objectSource) {
        if (objectSource.hasOwnProperty(p)) {
          objectTarget[p] = objectSource[p];
        }
      }
    }

    // Equivalent to (Array.indexOf() != -1)
    Helpers.arrayContains = function(array, item) {
        if (Array.prototype.indexOf) {
            return !(array.indexOf(item) < 0);
        }
        else {
            for (var i = 0, len = array.length; i < len; ++i) {
                if (array[i] === item)
                    return true;
            }
            return false;
        }
    }

    // Equivalent to Array.indexOf
    Helpers.arrayIndexOf = function(array, item) {
        if (Array.prototype.indexOf) {
            return (array.indexOf(item));
        }
        else {
            for (var i = 0, len = array.length; i < len; ++i) {
                if (array[i] === item)
                    return i;
            }
            return -1;
        }
    }

    Helpers.indexOfLastMinElementInArray = function(array) {
        var min      = array[0];
        var minIndex = 0;

        for (var i = 1, len = array.length; i < len; ++i) {
            if(array[i] <= min) {
                minIndex = i;
                min      = array[i];
            }
        }
        return minIndex;
    }

    // Returns an array of unique values from the given array
    // Specific implementation is picked based on http://jsperf.com/array-unique2/19
    Helpers.filterUnique = function(array) {
        var hash   = {},
            result = [],
            i      = array.length;
        while (i--) {
            if (!hash[array[i]]) {
                hash[array[i]] = true;
                result.push(array[i]);
            }
        }
        return result;
    }

    // Replaces the first occurence of `value` in `array` by `newValue`. Does nothing if `value` is not in `array`
    Helpers.replaceInArray = function(array, value, newValue) {
        for (var i = 0, len = array.length; i < len; ++i) {
            if (array[i] === value) {
                array[i] = newValue;
                break;
            }
        }
    }

    // Removes the first occurence of `value` in `array`. Does nothing if `value` is not in `array`
    Helpers.removeFirstOccurrenceByValue = function(array, item) {
        for (var i = 0, len = array.length; i < len; ++i) {
            if (array[i] == item) {
                array.splice(i,1);
                break;
            }
        }
    }

    // Returns true iff the object has no properties
    Helpers.isObjectEmpty = function(map) {
        for(var key in map) {
           if (map.hasOwnProperty(key)) {
              return false;
           }
        }
        return true;
    }

    // Returns num lines in a text block, or 0 for empty lines. Ignores leading and trailing whitespace
    Helpers.numTextLines = function(text) {
        if (text === null || text == "") { return 0; }
        var useText = text.replace(/^\s+|\s+$/g,'');
        var numLines = (useText.match(/\n/g) || []).length + 1;
        return numLines;
    }

    // Used for: user input validation
    Helpers.isInt = function(n) {
        //return +n === n && !(n % 1);
        //return !(n % 1);
        return (!isNaN(n) && parseInt(n) == parseFloat(n));
    }

    Helpers.toObjectWithTrue = function(array) {
      var obj = {};
      for (var i = 0; i < array.length; ++i)
        if (array[i] !== undefined) obj[array[i]] = true;
      return obj;
    }

    Helpers.romanize = function(num) {
        if (!+num)
            return false;
        var digits = String(+num).split(""),
            key = ["","C","CC","CCC","CD","D","DC","DCC","DCCC","CM",
                   "","X","XX","XXX","XL","L","LX","LXX","LXXX","XC",
                   "","I","II","III","IV","V","VI","VII","VIII","IX"],
            roman = "",
            i = 3;
        while (i--)
            roman = (key[+digits.pop() + (i * 10)] || "") + roman;
        return Array(+digits.join("") + 1).join("M") + roman;
    }

    /*function objectKeys(obj) {
        if (Object.keys)
            return Object.keyhs(obj);

        var keys = [];
        for (var i in obj) {
          if (obj.hasOwnProperty(i)) {
            keys.push(i);
          }
        }
        return keys;
    }*/

    //-------------------------------------------------------------
    // Used during ordering for bucket order permutations
    //-------------------------------------------------------------
    Helpers.makeFlattened2DArrayCopy = function(array) {
        var flattenedcopy = [].concat.apply([], array);
        return flattenedcopy;
    }

    Helpers.swap = function(array, i, j) {
        var b    = array[j];
        array[j] = array[i];
        array[i] = b;
    }

    Helpers.permute2DArrayInFirstDimension = function(permutations, array, from) {
       var len = array.length;

        if (from == len-1) {
            permutations.push(Helpers.makeFlattened2DArrayCopy(array));
            return;
        }

        for (var j = from; j < len; j++) {
            Helpers.swap(array, from, j);
            Helpers.permute2DArrayInFirstDimension(permutations, array, from+1);
            Helpers.swap(array, from, j);
        }
    }
    //-------------------------------------------------------------


    //-------------------------------------------------------------
    // Used for profiling code
    Helpers.Timer = function() {
        this.startTime = undefined;
        this.lastCheck = undefined;
        this.start();
    };

    Helpers.Timer.prototype = {

        start: function() {
            this.startTime = new Date().getTime();
            this.lastCheck = this.startTime;
        },

        restart: function() {
            this.start();
        },

        report: function() {
            var current = new Date().getTime();
            var elapsed = current - this.lastCheck;
            return elapsed;
        },

        printSinceLast: function( msg ) {
            var current = new Date().getTime();
            var elapsed = current - this.lastCheck;
            this.lastCheck = current;
            console.log( msg + elapsed + "ms" );
        },
    };
    //-------------------------------------------------------------


    //-------------------------------------------------------------
    Helpers.stringifyObject = function (obj) {
        return _printObjectInternal(obj, 1);
    }

    Helpers.printObject = function(obj) {
        console.log( _printObjectInternal(obj, 0) );
    }

    _printObjectInternal = function(o, level) {
        if (level > 10) return "...[too deep, possibly a recursive object]...";

        var output = '';

        if (typeof o === 'object')
        {

            if (Object.prototype.toString.call(o) === '[object Array]')
            {
                output = '[';
                for (var i = 0; i < o.length; i++) {
                    if (i > 0) output += ', ';
                    output += _printObjectInternal(o[i], level+1);
                }
                output += ']';
            }
            else
            {
                output = '{';
                var idx = 0;
                if (level == 0) output += '\n';
                for (var property in o) {
                    if (!o.hasOwnProperty(property)) continue;

                    if (level != 0 && idx != 0 )
                        output += ', ';
                    output += property + ': ' + _printObjectInternal(o[property], level+1);

                    if (level == 0)
                        output += '\n';
                    idx++;
                }
                output += '}';
            }
        }
        else if (typeof o === 'string') {
            output = "'" + o + "'";
        }
        else
            output = ''+o;

        return output;
    }

    Helpers.padString = function(string, width, padding, onLeft) {
        return (width <= string.length) ? string : Helpers.padString( (onLeft ? (padding + string) : (string + padding)), width, padding, onLeft);
    }
    //-------------------------------------------------------------

    return Helpers;
});
