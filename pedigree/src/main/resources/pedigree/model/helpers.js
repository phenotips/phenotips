// Functions in this file provide functionality which is not present in
// one of { InternetExplorer 8, Chrome v28, Firefox 3 } but may be present
// in later verisons of the browsers and/or libriries such as Ext/Prototype
// However the goal was to release a dependency-free package, thus some
// of the mehtods available in other libraries were re-implemented.
// None of the methods modify any of the built-in type prototypes.
// Loop types are picked based on http://jsperf.com/loops/128


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
function clone2DArray (arr2D) {
    var new2D = [];
    for (var i = 0; i < arr2D.length; ++i) {
        new2D.push(arr2D[i].slice());
    }
    return new2D;
}

// Creates a shallow copy of the given object
// Specific implementation is picked based on http://jsperf.com/cloning-an-object/4
function cloneObject(obj) {
    var target = {};
    for (var i in obj) {
        if (obj.hasOwnProperty(i))
            target[i] = obj[i];
    }
    return target;
}

// Equivalent to (Array.indexOf() != -1)
function arrayContains(array, item) {
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
function arrayIndexOf(array, item) {
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

function indexOfLastMinElementInArray(array) {
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
function filterUnique(array) {
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
function replaceInArray(array, value, newValue) {
    for (var i = 0, len = array.length; i < len; ++i) {
        if (array[i] === value) {
            array[i] = newValue;
            break;
        }
    }
}

// Removes the first occurence of `value` in `array`. Does nothing if `value` is not in `array`
function removeFirstOccurrenceByValue(array, item) {
    for (var i = 0, len = array.length; i < len; ++i) {
        if (array[i] == item) {
            array.splice(i,1);
            break;
        }
    }
}

// Used for: user input validation
function isInt(n) {
    //return +n === n && !(n % 1);
    //return !(n % 1);
    return (!isNaN(n) && parseInt(n) == parseFloat(n));
}

function toObjectWithTrue(array) {
  var obj = {};
  for (var i = 0; i < array.length; ++i)
    if (array[i] !== undefined) obj[array[i]] = true;
  return obj;
}

function romanize (num) {
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
function makeFlattened2DArrayCopy (array) {
    var flattenedcopy = [].concat.apply([], array);
    return flattenedcopy;
}

function swap (array, i, j) {
    var b    = array[j];
    array[j] = array[i];
    array[i] = b;
}

function permute2DArrayInFirstDimension (permutations, array, from) {
   var len = array.length;

   if (from == len-1) {
       permutations.push(makeFlattened2DArrayCopy(array));
       return;
   }

   for (var j = from; j < len; j++) {
      swap(array, from, j);
      permute2DArrayInFirstDimension(permutations, array, from+1);
      swap(array, from, j);
   }
}
//-------------------------------------------------------------


//-------------------------------------------------------------
// Used for profiling code
Timer = function() {
    this.startTime = undefined;
    this.lastCheck = undefined;
    this.start();
};

Timer.prototype = {

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
function stringifyObject(obj) {
    return _printObjectInternal(obj, 1);
}

function printObject(obj) {
    console.log( _printObjectInternal(obj, 0) );
}

function _printObjectInternal(o, level) {
    if (level > 10) return "...[too deep, possibly a recursive object]...";

    var output = '';

    if (typeof o == 'object')
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
    else if (typeof o == 'string') {
        output = "'" + o + "'";
    }
    else
        output = ''+o;

    return output;
}

function padString (string, width, padding, onLeft) {
    return (width <= string.length) ? string : padString( (onLeft ? (padding + string) : (string + padding)), width, padding, onLeft);
}
//-------------------------------------------------------------

