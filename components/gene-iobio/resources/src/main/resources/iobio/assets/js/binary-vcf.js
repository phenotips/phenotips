//--------------------------------------------------------------------------//
//                                                                          //
//                        B I N A R Y - V C F                               //
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
// https://raw.github.com/vjeux/jDataView/master/src/jdataview.js
// https://raw.github.com/vjeux/jParser/master/src/jparser.js
// inflate.js (fetch and place or fetch remotely)
// binary-vcf.js (this file)
//
// With files[0] == vcf file
//      files[1] == tabix file
//
// vcfR = new readBinaryVCF(files[1], files[0]);
// var x = undefined;
// vcfR.getRecords(11, 1000000, 1015808, function(rs){x = rs;});
// var chunks = vcfR.getChunks(11, 1000000, 1015808)




// Standard log base 2.  Used in tabix format bin level calculation
function log2 (x) {
    return Math.log(x) / Math.LN2;
}


// Take two array buffers BUFFER1 and BUFFER2 and, treating them as
// simple byte arrays, return a new byte array of their catenation.
function appendBuffer( buffer1, buffer2 ) {
  var tmp = new Uint8Array( buffer1.byteLength + buffer2.byteLength );
  tmp.set( new Uint8Array( buffer1 ), 0 );
  tmp.set( new Uint8Array( buffer2 ), buffer1.byteLength );
  return tmp.buffer;
}

// Take a vector of array buffers and treating them as simple byte
// arrays, return a new byte array of their catenation.
function appendBuffers(bufferVec) {
    var totalSize = 0;
    for (var i = 0; i < bufferVec.length; i++) {
        totalSize = totalSize + bufferVec[i].byteLength;
    };
    var tmp = new Uint8Array(totalSize);
    var offset = 0;
    for (var i = 0; i < bufferVec.length; i++) {
        tmp.set(new Uint8Array(bufferVec[i]), offset);
        offset = offset + bufferVec[i].byteLength;
    };
    return tmp.buffer;
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




// We use JParser for binary decoding.  JParser provides a declarative
// frame work  for decoding binary encoded data.   In such declarative
// techniques, the  structure of the  file is defined  using recursive
// templates that mirror very  closely the formal specification of the
// file.  For the tabix format, this template fully describes the
// decoder for it.
//
var tabi_fmt = {

    // Tabix strings are C type 0 terminated.  This removes the 0.
    string0: function(size) {
        return this.parse(['string', size]).replace(/\0+$/, '');
    },

    // For some reason, uint64 is not defined in JParse, but JDataView
    // has the base level decoder, so we just make our own.
    uint64: function(){
        return this.view.getUint64();
    },


    // Tabix file header template.  This is the part that comes before
    // bin and linear indices.  Note how we can refer back to the current
    // containing data for information on how to proceed with the parse.
    header: {
        magic:   ['string', 4],
        n_ref:   'int32',
        format:  'int32',
        col_seq: 'int32',
        col_beg: 'int32',
        col_end: 'int32',
        meta:    'int32',
        skip:    'int32',
        l_nm:    'int32',
        names:   ['string0', function(){return this.current.l_nm;}]
    },


    // Bins are structures with a bin id, and a sequence of chunks,
    // the size of the sequence being given by the n_chunk field.
    chunk: {
        cnk_beg: 'uint64',
        cnk_end: 'uint64'
    },

    bin: {
        bin:      'uint32',
        n_chunk:  'int32',
        chunkseq: ['array', 'chunk', function(){return this.current.n_chunk;}]
    },

    // An index is composed of a sequence of bins (the number of which
    // is given by n_bin), and an interval sequence of 64 bit virtual
    // addresses (the 'linear index'), whose size is given by n_intv.
    index: {
        n_bin:     'int32',
        binseq:    ['array', 'bin', function(){return this.current.n_bin;}],
        n_intv:    'int32',
        intervseq: ['array', 'uint64', function(){return this.current.n_intv;}]
    },

    // A tabix file is comprised of a header and a sequence of
    // indices, the number of which is given by the header field
    // n_ref.
    tabix: {
        head:     'header',
        indexseq: ['array', 'index',
                   function(){return this.current.head.n_ref;}]
    }

};


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
// the _gzip_ blocks in the region into a single array buffer and
// passes to CBFN.
function inflateRegion (f, begOffset, endOffset, cbfn) {
    var blockOffset = begOffset;
    var res = [];
    var cb = function (x) {
        if (blockOffset < endOffset) {
            inflateBlock(f, blockOffset, function(x) {res.push(x);});
            nextBlockOffset(f, blockOffset, function(x){blockOffset = x;});
            return inflateBlock(f, blockOffset, cb);
        } else {
            var resBuf = appendBuffers(res);
            return cbfn.call(this, resBuf);
        };
    };
    inflateBlock(f, blockOffset, cb);
}

// Mid level function that given a BGZF file F, inflates all the
// contained _gzip blocks, appends them all together into a single
// array buffer and passes that to CBFN.  Calling this on any 'large'
// BGZF _data_ file (tabix should be fine) will likely blow up with
// memory exceeded.
function inflateAllBlocks(f, cbfn) {
    return inflateRegion(f, 0, f.size, cbfn);
}


// Mid level function that given a BGZF file F, a region defined by
// offsets BEGOFFSET and ENDOFFSET, fetches, inflates, appends
// together and converts to a string all the gzip blocks in region.
// Passes the string to CBFN
function inflateRegion2Stg (f, begOffset, endOffset, cbfn) {
    var cb = function (resBuf) {
        var res = buffer2String(resBuf);
        return cbfn.call(this, res);
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



// Simple evt callback for testing.
var files = undefined;
function readBinaryFile(evt) {
    files = evt.target.files;
    var f = files[0];
    var cb = function (x) {console.log(x);};
    var stgcb = function (x) {console.log(buffer2String(x));};
    this.theFile = f;

    console.log(f.name);
    var isTabi = (f.name.split(".").slice(-1)[0] == "tbi");

    if (f) {
        if (isTabi) {
            var r = new readTabixFile(f);
            r.getIndex(function (tbx) {
                           console.log(r.tabixContent);
                           console.log(r.bhash);});
        } else {
            //var r = new readBinaryVCF(f1, f2);
            getBGZFHD(f, 0, cb);
            blockSize(f, 0, cb);
            nextBlockOffset(f, 0, cb);
        }

    } else {
        alert("Failed to load file");
    }
}



// Start of main API.  There are two 'object' types with constructors:
//
// readTabixFile which takes a filespec and initializes a tabix
// reader.  Provides methods
//
//   * getIndex - builds the index information
//   * bin2Ranges - returns the chunk information for a [ref binid]
//   * bin2Beg - returns first chunk of bin
//   * bin2End - returns last chunk of bin
//   * getChunks - returns all chunks for bins covering region in ref
//
// Details below
//
// readBinaryVCF which takes a tabix filespec and a BGZF VCF filespec
// initializes a tabix reader and builds binary VCF reader.  Provides
// methods
//
//   * getRecords - obtains the data records in a reference region and
//                  returns as a vector of strings to provided callback
//   * getChunks - returns all chunks covered by region
//
// Details below

// first and last 16kb bin ids.  Not currently used.
var start16kbBinid = 4681;
var end16kbBinid = 37449;

// Constructor for tabix reader and decoder.  tabixfile is a bgzipped
// tabix binary index file.
function readTabixFile(tabixFile) {
    if (!(this instanceof arguments.callee)) {
        throw new Error("Constructor may not be called as a function");
    }
    this.theFile = tabixFile;
    this.tabixThis = this;
}

// Main function for a tabix reader.  Obtains and decodes the index
// and caches information on it used by other methods.  So, must be
// called before others.
readTabixFile.prototype.getIndex =
    function(cb){
        var cbfn = (arguments.length>0) ? cb : function (x) {console.log(x);};
        var tbxThis = this;

        var bins2hash = function (binseq) {
            var bhash = {};
            var i = 0;
            for (var x in binseq) {
                var b = binseq[x].bin;
                bhash[b]=i;
                i = i + 1;
            }
            return bhash;
        };

        inflateAllBlocks(
            this.theFile,
            function(resultBuffer) {
                var parser =  new jParser(resultBuffer, tabi_fmt);
                tbxThis.tabixContent = parser.parse('tabix');
                // Names to array of names
                tbxThis.tabixContent.head.names =
                    tbxThis.tabixContent.head.names.split('\0');
                // Create bin xref hash table
                var n_ref = tbxThis.tabixContent.head.n_ref;
                tbxThis.bhash = {};
                for (i = 0; i < n_ref; i++) {
                    tbxThis.bhash[i] =
                        bins2hash(tbxThis.tabixContent['indexseq'][i]['binseq']);
                };
                cbfn.call(tbxThis, tbxThis);
            });};

// Takes a ref and binid and builds a return vector mapped from the
// chunk sequence of bin, where each element is a two element vector
// defining the region of a chunk.  The begin and end of each region
// are the base file offsets (the 16 bit right shifted values)
readTabixFile.prototype.bin2Ranges =
    function  (ref, binid) {
        var res = [];
        var bs = this.tabixContent.indexseq[ref].binseq;
        var cnkseq = bs[this.bhash[ref][binid]].chunkseq;

        for (var i = 0; i < cnkseq.length; i++) {
            var cnk = cnkseq[i];
            var cnkBeg = cnk.cnk_beg.valueOf();
            var cnkEnd = cnk.cnk_end.valueOf();
            res.push([Math.floor(cnkBeg / Math.pow(2, 16)),
                      Math.floor(cnkEnd / Math.pow(2, 16))]);
        };
        return res;
    };

// First chunk region of binid.
readTabixFile.prototype.bin2Beg =
    function (binid) {
        var range = bin2Range(binid);
        return range[0];
    };

// Last chunk region of binid.
readTabixFile.prototype.bin2End =
    function (binid) {
        var range = bin2Range(binid);
        return range[range.length-1];
    };

// For a reference REF region defined by BEG and END return the set of
// chunks of all bins involved as a _flat_ vector of two element
// vectors, each defining a region of a bin.
readTabixFile.prototype.getChunks =
    function (ref, beg, end) {

        var bids = reg2bins(beg, end+1).filter(
            function(x){
                return (this.bhash && this.bhash[ref] && this.bhash[ref][x] != undefined);
            }, this);
        var bcnks = bids.map(
            function(x){
                return this.bin2Ranges(ref, x);
            }, this);
        var cnks = bcnks.reduce(
            function(V, ranges) {
                ranges.forEach(function(item) {V.push(item);});
                return V;
            }, []);

        return cnks;
    };



// Constructor for BGZF VCF reader and decoder.  tabixfile is a
// bgzipped tabix binary index file for VCFFILE, a BGZF encoded VCF
// file.  Inits and builds index and initializes the VCF reader.
function readBinaryVCF (tbxFile, vcfFile, callback) {
    if (!(this instanceof arguments.callee)) {
        throw new Error("Constructor may not be called as a function");
    }

    var r = new readTabixFile(tbxFile);

    this.theFile = vcfFile;
    this.tbxR = r;
    this.vcfThis = this;

    r.getIndex(callback);
}

// Main function for VCF reader.  For a record region defined by BEG
// ad END, obtains the set of bins and chunks covering the region,
// inflates the corresponding data blocks, converts to a vector of
// strings (one for each record) and filters these to ensure only
// those in the range are kept.  The resulting filtered vector of
// strings is returned by calling CBFN with the vector.
readBinaryVCF.prototype.getRecords =
    function (ref, beg, end, cbfn) {
        var vcfFile = this.theFile;
        var TbxR = this.tbxR;

        // vector.pop, pops from the _end_
        var cnks = TbxR.getChunks(ref,beg,end).reverse();

        var stg = "";
        var cb = function (x) {
            stg = stg.concat(x);
            if (cnks.length > 0) {
                var range = cnks.pop();
                inflateRegion2Stg(vcfFile, range[0], range[1], cb);
            } else {
                var stgRecs = stg.split("\n").filter(
                    function (rec) {
                        var n = parseInt(rec.split("\t")[1]);
                        return ((beg <= n) && (n <= end));
                    });
                cbfn.call(this,stgRecs);
            };
        };

        if (cnks.length == 0) {
            console.log("no cnks found for ref " + ref + " start=" + beg + " end=" + end ); 
            cbfn.call(this, null);
        }  else {
            var rng0 = cnks.pop();
            inflateRegion2Stg(this.theFile, rng0[0], rng0[1], cb);
        }
    };

readBinaryVCF.prototype.getHeader = 
    function (cbfn) {
        var vcfRthis = this;
        var f = vcfRthis.theFile;
        var offset = 0;

        var hdstgs = [];
        var cb = function (nxtStg) {
            var stgs = nxtStg.split("\n");
            gdstgs = stgs.filter(
                function (s) {return (s[0] == "#");});
            hdstgs = hdstgs.concat(gdstgs);
            if (stgs[stgs.length-1][0] == "#") {
                nextBlockOffset(f, offset, function(o) {
                    offset = o;
                    inflateBlock2stg(f, offset, cb);
                });
            } else {
                vcfRthis.head = hdstgs.join("\n");
                cbfn.call(vcfRthis, vcfRthis.head);
            };
        };

        if (vcfRthis.head) {
            cbfn.call(vcfRthis, vcfRthis.head);
        } else {
            inflateBlock2stg(f, offset, cb);
        };
    };

readBinaryVCF.prototype.getHeaderRecords =
    function (cbfn) {
        var vcfFile = this.theFile;
        var TbxR = this.tbxR;


        // Find the lowest start offset
        var start = 0;
        var end = 0;
        for (var i = 0; i < this.tbxR.tabixContent.head.n_ref; i++) {
            var ref   = this.tbxR.tabixContent.head.names[i];

            var indexseq = this.tbxR.tabixContent.indexseq[i];
            var interval = indexseq.intervseq[0];
            var fileOffset = interval.valueOf();
            if (fileOffset == 0) {
                continue;
            }

            if (end == 0 ) {
                end = fileOffset;
            } else if (fileOffset < end) {
                end = fileOffset;
            }
        }

        var stg = "";
        var cb = function (x) {
            stg = stg.concat(x);
            var stgRecs = stg.split("\n").filter(
                    function (rec) {
                        return rec.substring(0, 1) == "#";
                    });
            cbfn.call(this,stgRecs);
        };
        inflateRegion2Stg(this.theFile, start, end, cb);
    };


// Synonym for tabix getChunks.  Directly callable on a vcfReader.
readBinaryVCF.prototype.getChunks =
    function (ref, beg, end) {
        var TbxR = this.tbxR;
        return TbxR.getChunks(ref, beg, end);
    };




// Tabix bin half closed half open record interval calculator.
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




//var TbxR = new readTabixFile(files[1]);
//TbxR.getIndex(function(r){console.log(r.tabixContent)})
// TbxR.tabixContent.indexseq[0].binseq
//
//
//resStgs = []
//inflateBlock2stg(files[0], 0, function(stg){resStgs.push(stg.split("\n"));})
//resStgs[0][resStgs[0].length-1]
//
//inflateBlock2stg(files[0],9717,function(stg){resStgs.push(stg.split("\n"));})
//resStgs[1][0]
//
// var x = "";
// var resStgs = []
// inflateBlock2stg(files[0], 0, function(stg){x = x.concat(stg);})
// resStgs = x.split("\n")
