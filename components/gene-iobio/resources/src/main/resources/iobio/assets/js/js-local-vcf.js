//--------------------------------------------------------------------------//
//                                                                          //
//                       J S - L O C A L - V C F                            //
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
//
// There are two 'object' types with constructors:
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
//   * getHeader - obtains and returns the VCF header lines
//   * getRecords - obtains the data records in a reference region and
//                  returns as a vector of strings to provided callback
//   * getChunks - returns all chunks covered by region
//
// Details below
//
// Example:
//
// With files[0] == vcf file
//      files[1] == tabix file
//
// vcfR = new readBinaryVCF(files[1], files[0]);
// var x = undefined;
// vcfR.getRecords(11, 1000000, 1015808, function(rs){x = rs;});
// var chunks = vcfR.getChunks(11, 1000000, 1015808)




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
                           console.log(r.idxContent);
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
//   * getHeader - obtains and returns the VCF header lines
//   * getRecords - obtains the data records in a reference region and
//                  returns as a vector of strings to provided callback
//   * getChunks - returns all chunks covered by region
//
// Details below


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
        var cbfn = cb ? cb : function (x) {console.log(x);};
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
                tbxThis.idxContent = parser.parse('tabix');
                // Names to array of names
                var names = tbxThis.idxContent.head.names.split('\0');
                tbxThis.idxContent.head.names = names
                // Ref name to index map (hash map)
                tbxThis.idxContent.namehash =
                    simpleHash(
                        names.map(function(n){return {name: n}}), "name");
                // Create bin xref hash table
                var n_ref = tbxThis.idxContent.head.n_ref;
                tbxThis.bhash = {};
                for (i = 0; i < n_ref; i++) {
                    tbxThis.bhash[i] =
                        bins2hash(tbxThis.idxContent.indexseq[i].binseq);
                };
                cbfn.call(tbxThis, tbxThis);
            });};

// Converts a reference name to its tabix index.  References in vcf
// processing always need to be by their index.  Requires that
// getIndex has been run
readTabixFile.prototype.refName2Index =
    function (name) {
        return this.idxContent.namehash[name];
    };

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
readTabixFile.prototype.bin2Ranges =
    function  (ref, binid) {
        var res = [];
        var bs = this.idxContent.indexseq[ref].binseq;
        var cnkseq = bs[this.bhash[ref][binid]].chunkseq;

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
                return (this.bhash[ref][x] != undefined);
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
function readBinaryVCF (tbxFile, vcfFile, cb) {
    if (!(this instanceof arguments.callee)) {
        throw new Error("Constructor may not be called as a function");
    }

    var cb = cb ? cb : function(){};
    var r = new readTabixFile(tbxFile);

    this.theFile = vcfFile;
    this.tbxR = r;
    this.vcfThis = this;

    r.getIndex(cb);
}


// Obtain and return the VCF header information as a vector of
// strings.  Calls cbfn with this vector.  All header lines begin with
// a "#" and start as the first line of the file and stop at first
// line starting without a "#" in char pos 0.
readBinaryVCF.prototype.getHeader =
    function (cbfn) {
        var vcfRthis = this;
        var f = vcfRthis.theFile;
        var offset = 0;

        var hdstgs = [];
        var buffer = "";
        var cb = function (nxtStg) {
            buffer += nxtStg;
            var recsFromChunk = nxtStg.split("\n");       
            if (recsFromChunk[recsFromChunk.length-1][0] == "#") {
                // If last record is a header record, continue reading,
                // the file, concatenating the header into string buffer
                nextBlockOffset(f, offset, function(o) {
                    offset = o;
                    inflateBlock2stg(f, offset, cb);
                });
            } else {
                // If the last record is not a header record, we are
                // done reading the vcf file.  Now parse the string
                // buffer to filter out any non-header rows
                // and perform callback.
                var stgs = buffer.split("\n");
                var gdstgs = stgs.filter(
                    function (s) {return (s[0] == "#");});
                hdstgs = hdstgs.concat(gdstgs);                
                vcfRthis.head = hdstgs.join("\n");
                cbfn.call(vcfRthis, vcfRthis.head);
            }
        };

        if (vcfRthis.head) {
            cbfn.call(vcfRthis, vcfRthis.head);
        } else {
            inflateBlock2stg(f, offset, cb);
        };
    };


// Main function for VCF reader.  For a record region defined by BEG
// ad END, obtains the set of bins and chunks covering the region,
// inflates the corresponding data blocks, converts to a vector of
// strings (one for each record) and filters these to ensure only
// those in the range are kept.  The resulting filtered vector of
// strings is returned by calling CBFN with the vector.
readBinaryVCF.prototype.getRecords =
    function (ref, beg, end, cbfn) {
        vcfRthis = this;
        var vcfFile = this.theFile;
        var TbxR = this.tbxR;
        var ref = (typeof(ref) == "number") ? ref : TbxR.refName2Index(ref);

        // vector.pop, pops from the _end_
        var cnks = TbxR.getChunks(ref,beg,end).reverse();

        var stg = "";
        var fn1 = function(ckbeg, ckend) {
            var vfbeg = ckbeg[0]; // Virtural file offset of beg bvzf blk
            var bobeg = ckbeg[1]; // Beg offset in beg inflated data
            var vfend = ckend[0]; // Virtural file offset of end bvzf blk
            var boend = ckend[1]; // End offset into end inflated data
            inflateRegion2Stg(
                vcfFile, vfbeg, vfend,
                function(x, ebsz) {
                    var last = x.length - ebsz + boend;
                    stg = stg.concat(x.slice(bobeg,last));
                    if (cnks.length > 0) {
                        var rng = cnks.pop();
                        fn1(rng[0], rng[1]);
                    } else {
                        //console.log(stg.length);
                        var stgRecs = stg.split("\n").filter(
                            function (rec) {
                                var n = parseInt(rec.split("\t")[1]);
                                return ((beg <= n) && (n <= end));
                            });
                        cbfn.call(vcfRthis,stgRecs);
                    };
                });
        };

        if (cnks.length > 0) {
            var rng = cnks.pop();
            fn1(rng[0], rng[1]);
        } else {
            cbfn.call(vcfRthis, []);
        }
    };


// Synonym for tabix getChunks.  Directly callable on a vcfReader.
readBinaryVCF.prototype.getChunks =
    function (ref, beg, end) {
        var TbxR = this.tbxR;
        return TbxR.getChunks(ref, beg, end);
    };




//var TbxR = new readTabixFile(files[1]);
//TbxR.getIndex(function(r){console.log(r.idxContent)})
// TbxR.idxContent.indexseq[0].binseq
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
