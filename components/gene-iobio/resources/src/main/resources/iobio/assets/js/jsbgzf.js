//--------------------------------------------------------------------------//
//                                                                          //
//                              J S B G Z F                                 //
//                                                                          //
//                                                                          //
// Copyright (c) 2014-2014 Trustees of Boston College                       //
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
// Author: Jon Anthony                                                      //
//                                                                          //
//--------------------------------------------------------------------------//
//

// Usage:
//
// Include the following libs:
//
// inflate.js (fetch and place or fetch remotely)
// pako_deflate.min.js (fetch and place or fetch remotely)
// jsbgzf.js (this file)
//
// API consists of top level functions - no constructor needed or
// wanted.
//
// * buffer2String - take a unsigned byte array (UBA) and convert to a
//   string. Only works for iso-latin-1 (or ascii - 8 bit bytes) - no
//   UTF-8 (unicode, ...) here
//
// * getChunk - low level function to obtain a chunk of a file as a UBA
//
// * getBGZFHD - parses and returns as a map bgzf headers (each
//   compressed block has a bgzf header)
//
// * nextBlockOffset - from a provided legal compressed block offset,
//   obtain the offset of the next compressed block
//
// * blockSize - from a provided legal compressed block offset,
//   compute size of contained compressed block
//
// * countBlocks - counts the total number of bgzf (compressed) blocks
//   in file
//
// * inflateBlock - from a provided legal compressed block offset,
//   inflate the blcck to its UBA representation.
//
// * inflateBlock2stg - same as inflateBlock but then use
//   buffer2String to convert inflated block to a string
//
// * inflateRegion - from a provided starting compressed block offset
//   and some ending offset (need not be a block offset), expand all
//   blocks covereed by region to a single UBA
//
// * inflateAllBlocks - inflate all blocks in file to a single UBA.
//   Likely not usable for large files.
//
// * inflateRegion2Stg - same as inflateBlock2stg, but for
//   inflateRegion
//
// * inflateAll2Stg - same as inflateRegioin2Stg, but where region is
//   the entire file
//
// * bgzf - takes a UBA of data and deflates to a bgzf compressed
//   block
//
// Appending buffers - used internally, but are intended for public
// use as well
// * appendBuffer - append two unsigned byte arrays (UBA)
// * appendBuffers - append vector of UBAs


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




// Take two array buffers BUFFER1 and BUFFER2 and, treating them as
// simple byte arrays, return a new byte array of their catenation.
function appendBuffer( buff1, buff2, asUint8) {
  var tmp = new Uint8Array( buff1.byteLength + buff2.byteLength );
  var b1 = (buff1 instanceof Uint8Array) ? buff1 : new Uint8Array(buff1);
  var b2 = (buff2 instanceof Uint8Array) ? buff2 : new Uint8Array(buff2);
  tmp.set(buff1, 0);
  tmp.set(buff2, buff1.byteLength);
  return (asUint8) ? tmp : tmp.buffer;
}

// Take a vector of array buffers and treating them as simple byte
// arrays, return a new byte array of their catenation.
function appendBuffers(bufferVec, asUint8) {
    var totalSize = 0;
    for (var i = 0; i < bufferVec.length; i++) {
        totalSize = totalSize + bufferVec[i].byteLength;
    };

    var tmp;
    if (bufferVec.length == 1) {
        var b = bufferVec[0];
        tmp = (b instanceof Uint8Array) ? b : new Uint8Array(b);
    } else {
        tmp = new Uint8Array(totalSize);
        var offset = 0;
        for (var i = 0; i < bufferVec.length; i++) {
            var b = bufferVec[i];
            var buff = (b instanceof Uint8Array) ? b :new Uint8Array(b);
            tmp.set(buff, offset);
            offset = offset + b.byteLength;
        };
    };
    return (asUint8) ? tmp : tmp.buffer;
}

// Take an array buffer considered as a byte stream, and return the
// string representation of the buffer.  This works only on latin 1
// character encodings (no UTF8).
function buffer2String (resultBuffer) {
    var s = '';
    var resultBB = new Uint8Array(resultBuffer);
    for (var i = 0; i < resultBB.length; ++i) {
        s += String.fromCharCode(resultBB[i]);
    }
    return s;
}


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



//===========================================================================//

// The BGZF header format for compressed blocks.  These blocks are all
// <= 2^16 (64KB) of uncompressed data.  The main header is the
// standard gzip header information, with the xlen field here set to 6
// (indicating an extra 6 bytes of subheader).  The subheader defines
// the specifics for the BGZ information.  The si* are required gzip
// subheader id information (can be basically anything fitting in two
// bytes, so basically two latin-1 characters), here they are 'BC'
// (code points 66 and 67).  SLEN indicates the size of the subheader
// data (here 2, indicating BSIZE is 2 bytes), and BSIZE is the actual
// 'real' extra data and is an unsigned 16 bit integer indicating the
// total block size - 1.  The actual compressed data follows this
// header and is BSIZE - header size - 8 (where the 8 accounts for 2
// 32 bit integers at the end holding the CRC and uncompressed size).
var bgzf_hd_fmt = {
    header: {
        id1:   'uint8',
        id2:   'uint8',
        cm:    'uint8',
        flg:   'uint8',
        mtime: 'uint32',
        xfl:   'uint8',
        os:    'uint8',
        xlen:  'uint16'
    },

    subheader: {
        si1:   'uint8',
        si2:   'uint8',
        slen:  'uint16',
        bsize: 'uint16'
    },

    bgzfHd: {head: 'header', subhead: 'subheader'}
};


// The size (in bytes) of a bgzf block header.
var hdSize = 18;

// The bgzf EOF marker block
EOFblk = [0x1f, 0x8b, 0x08, 0x04,
          0x00, 0x00, 0x00, 0x00,
          0x00, 0xff, 0x06, 0x00,
          0x42, 0x43, 0x02, 0x00,
          0x1b, 0x00, 0x03, 0x00,
          0x00, 0x00, 0x00, 0x00,
          0x00, 0x00, 0x00, 0x00];



// Low level binary file reader.  Reads bytes from base offset BEG to
// END inclusive as an array of unsigned bytes using a new FileReader
// for each read.  CBFN is the callback to call when read is finished
// and it is passed the FileReader object.
function getChunk (f, beg, end, cbfn) {
    var reader = new FileReader();
    reader.onloadend = function(evt) {
        if (evt.target.readyState == FileReader.DONE) {
            return cbfn.call(this, reader);
        } else {
            return alert('Bad read for ' + f + ' at ' + beg + ', ' + end +
                         'status: ' + evt.target.readyState);
        };
    };
    reader.readAsArrayBuffer(f.slice(beg, end));
}


// Low level function that obtains the BGZF header for the BGZF
// compressed file F at base byte offset OFFSET.  Decodes the header
// and passes the resulting JS object, representing the header
// information with fields as defined by template bgzf_hd_fmt, to
// CBFN.
function getBGZFHD (f, offset, cbfn) {
    var cb = function (r) {
        var a = new Uint8Array(r.result);
        var hdbuf = a.buffer;
        var parser = new jParser(hdbuf, bgzf_hd_fmt);
        var hdobj = parser.parse('bgzfHd');
        return cbfn.call(this, hdobj);
    };
    getChunk(f, offset, offset + hdSize, cb);
}

// Low level function that given BGZF file F, base offset OFFSET,
// obtains the offset of the next block and passes to CBFN
function nextBlockOffset (f, offset, cbfn) {
    var cb = function(hdobj) {
        var bsize = hdobj.subhead.bsize;
        return cbfn.call(this, offset + bsize + 1);
    };
    getBGZFHD(f, offset, cb);
}

// Low level function that given BGZF file F, base offset OFFSET,
// obtains the block size of block at OFFSET and passes to CBFN
function blockSize (f, offset, cbfn) {
    var cb = function(hdobj) {
        var blksize = hdobj.subhead.bsize + 1;
        return cbfn.call(this, blksize);
    };
    getBGZFHD(f, offset, cb);
}

// Low level function that given BGZF file F, obtains the total count
// of _gzip_ blocks in F.  Each of these will correspond to one of
// BGZF's 64KB uncompressed blocks.  NOTE: a chunk or interval may
// contain more than one of these blocks! Passes count to CBFN.
//
// WARNING: for large BGZF files this can take a looonnnggggg time.
function countBlocks (f, cbfn) {
    var blkCnt = 1;
    var cb = function(x) {
        if (x<files[0].size) {
            blkCnt = blkCnt+1;
            nextBlockOffset(f, x, cb);
        } else {
            cbfn.call(this, blkCnt);
        };
    };
    nextBlockOffset(f, 0, cb);
}


// Low level function that given BGZF file F, base off BLOCKOFFSET,
// inflates the single _gzip_ compressed block at that location and
// passes the base array buffer obtained to CBFN.  NOTE: this uses the
// JSZlib library.
function inflateBlock(f, blockOffset, cbfn) {
    var cb2 = function (r) {
        var a = new Uint8Array(r.result);
        var inBuffer = a.buffer;
        var resBuf = jszlib_inflate_buffer(inBuffer, hdSize, a.length - hdSize);
        return cbfn.call(this, resBuf);
    };
    var cb = function (blksize) {
        //console.log(blockOffset, blksize);
        getChunk(f, blockOffset, blockOffset + blksize, cb2);
    };
    blockSize(f, blockOffset, cb);
}

// Low level function that given BGZF file F, base offset BLOCKOFFSET,
// inflates the single _gzip_ compressed block at that location,
// converts the array buffer so obtained to a string (latin-1) and
// passes that to CBFN
function inflateBlock2stg(f, blockOffset, cbfn) {
    var cb = function (resBuf) {
        var res = buffer2String(resBuf);
        return cbfn.call(this, res);
    };
    inflateBlock(f, blockOffset, cb);
}


// Mid level function that given a BGZF file F, a region defined by
// offsets BEGOFFSET and ENDOFFSET, fetches, inflates and appends all
// (_inclusively_) the _gzip_ blocks in the region into a single array
// buffer and passes to CBFN.
function inflateRegion (f, begOffset, endOffset, cbfn) {
    var blockOffset = begOffset;
    var res = [];
    var cb = function (x) {
        res.push(x);
        nextBlockOffset(
            f, blockOffset,
            function(x){
                blockOffset = x;
                if (blockOffset <= endOffset) {
                    return inflateBlock(f, blockOffset, cb);
                } else {
                    var resBuf = appendBuffers(res);
                    return cbfn.call(this, resBuf, res.slice(-1)[0].byteLength);
                };
            });
    };
    inflateBlock(f, blockOffset, cb);
}

// Mid level function that given a BGZF file F, inflates all the
// contained _gzip blocks, appends them all together into a single
// array buffer and passes that to CBFN.  Calling this on any 'large'
// BGZF _data_ file (bai should be fine) will likely blow up with
// memory exceeded.
function inflateAllBlocks(f, cbfn) {
    return inflateRegion(f, 0, f.size-1, cbfn);
}


// Mid level function that given a BGZF file F, a region defined by
// offsets BEGOFFSET and ENDOFFSET, fetches, inflates, appends
// together and converts to a string all the gzip blocks in region.
// Passes the string to CBFN
function inflateRegion2Stg (f, begOffset, endOffset, cbfn) {
    var cb = function (resBuf, ebsz) {
        var res = buffer2String(resBuf);
        return cbfn.call(this, res, ebsz);
    };
    inflateRegion(f, begOffset, endOffset, cb);
}

// Mid level function.  Inflates the entire BGZF file F, converts to a
// total string and passes to CBFN.  Calling this on any 'large' BGZF
// _data_ file will likely blow off with memory exceeded.
function inflateAll2Stg (f, cbfn) {
    var cb = function (resBuf) {
        var res = buffer2String(resBuf);
        return cbfn.call(this, res);
    };
    inflateAllBlocks(f, cb);
}



// BGZF deflation; takes an unsigned byte array UBA, which is taken as
// not yet deflated (though you could try deflating an already
// deflated block, but this will likely lose) and deflate it to a
// legal BGZF formatted compressed uba (including BSIZE payload).
function bgzf (uba) {
    var bgzfUba =
        new pako.gzip(uba,
                      {header: {os: 255, time: 0,
                                extra: new Uint8Array([66,67,2,0,255,255])}});
    var bsize = bgzfUba.length - 1;
    var b0 = (bsize & 0xff);
    var b1 = (bsize >> 8);
    bgzfUba[16] = b0;
    bgzfUba[17] = b1;
    return bgzfUba;
}

// Takes a uba composed of a series of bgzf blocks and appends the EOF
// block.
function addEOFblk (bgzfUba) {
    return appendBuffer(bgzfUba, EOFblk);
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
              p.curBuf = appendBuffer(p.curBuf, new Uint8Array(b));
              p.view.buffer = p.curBuf;
              p.view.byteLength = p.curBuf.byteLength;
              p.view._view = new DataView(
                p.curBuf, 0, p.view.byteLength);
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


// Bai bin half closed half open record interval calculator.
// Returns a vector, [interval, k, l, sl, ol], where
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
