//--------------------------------------------------------------------------//
//                                                                          //
//                        J S - B V - C O M M O N                           //
//                                                                          //
//                                                                          //
// Permission is hereby granted, free of charge, to any person obtaining    //
// a copy of this software and associated documentation files (the          //
// "Software"), to deal in the Software without restriction, including      //
// without limitation the rights to use, copy, modify, merge, publish,      //
// distribute, sublicense, and/or sell copies of the Software, and to       //
// permit persons to whom the Software is furnished to do so, subject to    //
// the following conditions:                                                //
//                                                                          //
// The above copyright notice and this permission notice shall be           //
// included in all copies or substantial portions of the Software.          //
//                                                                          //
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,          //
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF       //
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND                    //
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE   //
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION   //
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION    //
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.          //
//                                                                          //
//                                                                          //
// Author: Jon Anthony (2014)                                               //
//                                                                          //
//--------------------------------------------------------------------------//
//



// Takes a ref and binid and builds a return vector mapped from the
// chunk sequence of bin, where each element is a two element vector
// defining the region of a chunk.  The begin and end of each region
// are the base virtual file offsets (the 16 bit right shifted values)
// and the offset within the INflated block (the lower 16 bits).
// Returns a vector [[[vfbeg, bobeg], [vfend, boend]], ...] where
//
// * vfbeg is the virtual file offset of beginning bgzf block
// * bobeg is the offset within the inflated block of that block
// * vfend is the virtual file offset of ending bgzf block
// * boend is the offset of last byte in that block
function bin2Ranges (indexReader, ref, binid) {
    var res = [];
    var bs = indexReader.idxContent.indexseq[ref].binseq;
    var cnkseq = bs[indexReader.bhash[ref][binid]].chunkseq;

    for (var i = 0; i < cnkseq.length; i++) {
        var cnk = cnkseq[i];
        var cnkBeg = cnk.cnk_beg.valueOf();
        var cnkEnd = cnk.cnk_end.valueOf();
        res.push([[rshift16(cnkBeg), low16(cnkBeg)],
                  [rshift16(cnkEnd), low16(cnkEnd)]]);
    };
    return res;
};


// First chunk region of binid.
function bin2Beg (indexReader, ref, binid) {
    var range = bin2Ranges(indexReader, ref, binid);
    return range[0];
};


// Last chunk region of binid.
function bin2End (indexReader, ref, binid) {
    var range = bin2Ranges(indexReader, ref, binid);
    return range[range.length-1];
};


// For a reference REF region defined by BEG and END return the set of
// chunks of all bins involved as a _flat_ vector of two element
// vectors, each defining a region of a bin.
function getChunks (indexReader, ref, beg, end) {

    var bids = reg2bins(beg, end+1).filter(
        function(x){
            return (indexReader.bhash[ref][x] != undefined);
        }, indexReader);
    var bcnks = bids.map(
        function(x){
            return bin2Ranges(indexReader, ref, x);
        }, indexReader);
    var cnks = bcnks.reduce(
        function(V, ranges) {
            ranges.forEach(function(item) {V.push(item);});
            return V;
        }, []);

    return cnks;
};



// Standard log base 2.  Used in bai format bin level calculation
function log2 (x) {
    return Math.log(x) / Math.LN2;
}


// Simple hash for mapping items to indices.  ITEMS is a seq of
// objects, each having field denoted by KEY.  Map item.key to item's
// index in ITEMS.  Returns resulting association map (hash map).
function simpleHash (items, key) {
    var itemhash = {};
    var i = 0;
    for (var o in items) {
        var x = items[o][key];
        itemhash[x]=i;
        i = i + 1;
    }
    return itemhash;
};


// Some of the binary encoded strings are straight C null terminated
// type strings - no length attribute available in the encoding, so we
// need to have a C string reader over a buffer (vector, Uint8Array)
// of unsigned bytes.  NOTE: this only works on iso-latin-1 - no UTF8,
// but since BAM adheres to this, that should not be an issue here.
//
// BUF is an unsigned byte collection which can be at least 1) a
// vector and 2) a Uint8Array.  INDEX is the starting index into BUF
// to start a string read.
//
// Returns a vector [newIndex, stg], where newIndex is the index of
// BUF past the null terminating character of the string just read, or
// the length of BUF if no null found.  STG is the string obtained.
//
function cstg (index, buf) {
  var s = "";
  var i = index;
  while (i < buf.length && buf[i] != 0) {
    s += String.fromCharCode(buf[i]);
    i++;
  };
  return [(i<buf.length) ? i+1 : i, s];
}


// Internal call back function for wrapped parsers.  FILE is the file
// associated with the parser, x1 and x2 are the new offsets (cur and
// nxt file offsets), and cb is the user level cb.  Create and wrap a
// jParser p for buffer B and init all its required wrapping attributes:
//
// p.theFile : the file being parsed - assumed to be a bgzf file
// p.curFileOffset : the current virtual block into theFile
// p.nxtFileOffset : the next virtual block offset into theFile
// p.offset : the current parse location offset for current parse field
// p.curBuf : the current ArrayBuffer reflecting the slice of theFile being
//            parsed.
//
// Calls cb with the resulting parser intance.
function _wrapperCB (ctxThis, file, b, ebsz, fmt, x1, x2, cb) {
    var buf = new Uint8Array(b);
    var p = new jParser(buf, fmt);
    p.curBuf = buf;
    p.endBlkSz = ebsz;
    p.theFile = file;
    p.offset = p.tell();
    p.curFileOffset = x1;
    p.nxtFileOffset = x2;
    cb.call(ctxThis, p);
}

// Single block inflation wrapped parser.  FILE is the file to
// associate with parser, START is the virtual offset for the first
// block, FMT is the parser definition, and CB is the user level call
// back to call with the resulting wrapped parser.
//
// Inflates block starting at start, obtains the next two virtual
// (bgzf) block offsets after this block for the initial
// cur|nxt(FileOffset), creates a jParser for the buffer resulting
// from the block inflation and adds wrapping attributes via
// _wrapperCB (see above).
//
// Calls the user call back CB with the resulting parser
function wrappedJParser (file, start, fmt, cb) {
    inflateBlock(
        file, start,
        function (b, ebsz) {
            nextBlockOffset(
                file, start,
                function(x1) {
                    nextBlockOffset(
                        file, x1,
                        function (x2) {
                            _wrapperCB(this, file, b, ebsz, fmt, x1, x2, cb)
                        });
                });
        });
}

// Region inflation wrapped parser.  FILE is the file to associate
// with parser, BEG is the starting virtual offset for the first block
// in the region, END is the ending virtual offset for the last block
// in the region, FMT is the parser definition, and CB is the user
// level call back to call with the resulting wrapped parser.
//
// Inflates the region of file from beg to end, uses end as the
// initial curFileOffset, obtains the next virtual (bgzf) block offset
// after end for the initial nxtFileOffset, creates a jParser for the
// buffer resulting from the region inflation and adds wrapping
// attributes via _wrapperCB (see above).
//
// Calls the user call back CB with the resulting parser
function wrappedRegionJParser (file, beg, end, fmt, cb) {
    if (beg >= end) {
        wrappedJParser(file, beg, fmt, cb);
    } else {
        inflateRegion(
            file, beg, end,
            function (b, ebsz) {
                nextBlockOffset(
                    file, end,
                    function (x2) {
                        _wrapperCB(this, file, b, ebsz,  fmt, end, x2, cb)
                    });
            });
    };
};


// Mid level function.  Takes a jParser instance P and a parser
// production / rule PARSE_RULE, a string naming a parse production in
// P, and attempts to advance the current parse with parse_rule.  On
// success, calls cb with the result of the parse. P must have several
// attributes:
//
// p.theFile : the file being parsed - assumed to be a bgzf file
// p.curFileOffset : the current virtual block in theFile
// p.nxtFileOffset : the next virtual block offset in theFile
// p.offset : the current parse location offset for current parse field
// p.curBuf : the current ArrayBuffer reflecting the slice of theFile being
//            parsed.
//
// These attributes are in addition to those for jParser instances,
// and must be initialized on P before the first call to nextParseRes.
//
// On failure, checks to see if a RangeError was raised during the
// parse attempt.  This indicates that the parse field(s) for rule
// parse_rule extends into the next compressed blcok. If so, will
// attempt to:
//
// * Obtain and inflate the next block in the file
// * Append this to p.curBuf creating an extended ArrayBuffer file slice
// * Update the jDataview corresponding buffer in p.view (a jDataView)
// * Update the byteLength for the view to reflect new slice size
// * Create new DataView for new file slice ArrayBuffer
// * Obtain next virtual offset passed the newly inflated block
// * Update curFileOffset, and nxtFileOffset to reflect slice extension
// * Re-seek to the beginning of failed parse
// * Retry the parse by recursion.
//
// Note that all asynchronous file actions are set to synchronize with
// next parse attempt.
//
function nextParseRes (p, parse_rule, cb) {
  try {
    p.offset = p.tell();
    cb.call(this, p.parse(parse_rule));
  } catch (e) {
    if (!(e instanceof RangeError)) {
      throw e;
    } else {
      // get next chunk and reparse
      inflateBlock(
        p.theFile, p.curFileOffset,
        function(b){
          nextBlockOffset(
            p.theFile, p.nxtFileOffset,
            function(x) {
              p.curBuf = appendBuffer(p.curBuf, new Uint8Array(b), true);
              p.view.buffer = p.curBuf.buffer;
              p.view.byteLength = p.view.buffer.byteLength;
              p.view._view = new DataView(
                p.view.buffer, 0, p.view.byteLength);
              p.curFileOffset = p.nxtFileOffset;
              p.nxtFileOffset = x;
              p.seek(p.offset);
              nextParseRes(p, parse_rule, cb);
            })
        });
    };
  };
}



// first and last 16kb bin ids.
var start16kbBinid = 4681;
var end16kbBinid = 37449;
var _16KB = 16384;
var _65kb = Math.pow(2, 16);


// Right shift val 16 bits.  For a chunk beg/end value, this gives the
// virtual file offset address.
function rshift16 (val) {
    return Math.floor(val / Math.pow(2, 16));
}

// Get the lower 16 bits of val as an integer.  For a chunk beg/end
// value, this gives the starting or ending offset into the inflated
// chunk.
function low16 (val) {
    return val & 0Xffff;
}


// Bin half closed half open record interval calculator.  Returns a
// vector, [interval, k, l, sl, ol], where
//
// * interval is the half closed half open record interval
// * k is the corresponding binid covering the interval
// * l is the level of the bin
// * sl is the size of the bin (at level l)
// * ol is the offset of the bin (at level l)
//
function bin2Recs (binid) {
    var k = binid;
    var l = Math.floor(log2((7*k)+1) / 3);
    var sl = Math.pow(2, (29-(3*l)));
    var ol = (Math.pow(2, 3*l)-1) / 7;
    var interval = [(k-ol)*sl, (k-ol+1)*sl-1];
    return [interval, k, l, sl, ol];
}


/* calculate bin given an alignment covering [beg,end) (zero-based,
 * half-close-half-open) */
function reg2bin(beg, end)
{
    --end;
    if (beg>>14 == end>>14) return ((1<<15)-1)/7 + (beg>>14);
    if (beg>>17 == end>>17) return ((1<<12)-1)/7 + (beg>>17);
    if (beg>>20 == end>>20) return ((1<<9)-1)/7 + (beg>>20);
    if (beg>>23 == end>>23) return ((1<<6)-1)/7 + (beg>>23);
    if (beg>>26 == end>>26) return ((1<<3)-1)/7 + (beg>>26);
    return 0;
}

/* calculate the list of bins that may overlap with region [beg,end)
 * (zero-based) */
var MAX_BIN = (((1<<18)-1)/7);
function reg2bins(beg, end)
{
    var i = 0, k, list = [];
    --end;
    list.push(0);
    for (k = 1 + (beg>>26); k <= 1 + (end>>26); ++k) list.push(k);
    for (k = 9 + (beg>>23); k <= 9 + (end>>23); ++k) list.push(k);
    for (k = 73 + (beg>>20); k <= 73 + (end>>20); ++k) list.push(k);
    for (k = 585 + (beg>>17); k <= 585 + (end>>17); ++k) list.push(k);
    for (k = 4681 + (beg>>14); k <= 4681 + (end>>14); ++k) list.push(k);
    return list;
}
