//--------------------------------------------------------------------------//
//                                                                          //
//                        J S - L O C A L - B A M                           //
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

// Usage:
//
// Include the following libs:
//
// https://raw.github.com/vjeux/jDataView/master/src/jdataview.js
// https://raw.github.com/vjeux/jParser/master/src/jparser.js
// inflate.js (fetch and place or fetch remotely)
// pako_deflate.min.js (fetch and place or fetch remotely)
// jsbgzf.js (fetch and place or fetch remotely)
// binary-bam.js (this file)
//
// May also want bv-local-sampling.js
//
//
// Basic user level API.  There are two 'object' types with
// constructors:
//
// readBaiFile which takes a bam index (bai) filespec and initializes
// a bai reader.  Provides methods
//
//   * getIndex - builds the index information
//   * bin2Ranges - returns the chunk information for a [ref binid]
//   * bin2Beg - returns first chunk of bin
//   * bin2End - returns last chunk of bin
//   * getChunks - returns all chunks for bins covering region in ref
//
// Details below
//
// readBinaryBAM which takes a bai filespec and a BGZF BAM filespec
// initializes a bai reader and builds binary BAM reader.  Provides
// methods
//
//   * bamFront - obtain and return the bam front material:
//     - the header
//     - the reference list
//
//   * refName2Index - takes a string reference name and returns its
//     list index
//
//   * refsWithReads - returns a vector of all the listed references
//     which have actual read data
//
//   * getAlnUbas - Obtains unsigned byte arrays (UBAs) for a set of
//     alignments associated with a ref and region
//
//   * getAlns - obtains alignment information, in either text or
//     binary format, for a ref and region.  The binary format is for
//     lower level functions constructing new bam file chunks
//     (typically from a sampling)
//
//   * region2BAM - takes a region map defining a reference and region
//     and builds a vector of bgzf blocks covering the alignment data.
//
//   * regions2BAM - takes a vector of region maps and for each
//     invokes region2BAM, returning the resulting vector of vectors
//     of bam chunks
//
//   * throttledRegions2BAM - like regsion2BAM, but builds a
//     continuation that controls the stepping process and passes it
//     to user cbfn for control along with bam data
//
//   * getChunks - returns all chunks covered by region
//
//
// Examples:
//
// With files[0] == bam file
//      files[1] == bai file
//
// var bamR = new readBinaryBAM(files[1], files[0]);
// bamR.bamFront(function(){}); // Get bam front (head and refs)
// var withReads = bamR.refsWithReads().map(function(x) {return x[1]});
// var alns = []
// bamR.getAlns("20", 1000, 10000, function(alnseq){alns = alnseq})
//
// ...
//
// // Stream sampled regions somewhere
// var refsNregions = samplingRegions(withReads, {}).regions;
// var bamblks = [bamR.headUba];
//
// var bgzfHdr = bgzf(bamR.headUba);
// stream([bgzfHdr]); // Send header
//
// var regcnt = 0;
// var totcnt = bgzfHdr.length + EOFblk.length;
//
// bamR.throttledRegions2BAM(
//   refsNregions,
//   function(bgzfblks, fn, regmap){
//     // Only send two regions
//     if (bamblks && regcnt < 2) {
//       stream(bgzfblks);
//       totcnt = totcnt + bgzfblks.reduce(
//         function(S, x){return S+x.length},0);
//       regcnt = regcnt + 1;
//       fn.call(this, regmap);     // Step next region
//     } else {
//       stream(EOFblk);
//       console.log("FINISHED, total bytes sent:", totcnt)}})




// We use JParser for binary decoding.  JParser provides a declarative
// frame work  for decoding binary encoded data.   In such declarative
// techniques, the  structure of the  file is defined  using recursive
// templates that mirror very  closely the formal specification of the
// file.  For the bai format, this template fully describes the
// decoder for it.
//
var bai_fmt = {

    // Bai strings are C type 0 terminated.  This removes the 0.
    string0: function(size) {
        return this.parse(['string', size]).replace(/\0+$/, '');
    },

    // For some reason, uint64 is not defined in JParse, but JDataView
    // has the base level decoder, so we just make our own.
    uint64: function(){
        return this.view.getUint64();
    },


    // Bai file header template.  This is the part that comes before
    // bin and linear indices.  Note how we can refer back to the
    // current containing data for information on how to proceed with
    // the parse.
    header: {
        magic:   ['string', 4],
        n_ref:   'int32'
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

    // A Bai file is comprised of a header and a sequence of indices,
    // the number of which is given by the header field n_ref.
    bai: {
        head:     'header',
        indexseq: ['array', 'index',
                   function(){return this.current.head.n_ref;}]
    }

};


// Bam file format parser encoding.  This needs to be split into two
// separate parsers.  The first, here, defines the overall structure
// of the file blocking.  But these blocks cannot be fully resolved
// with a one pass parse, since the alignment information contains
// structure (the 'auxilliary data' for tags) that is ad hoc, and not
// discernable until runtime.  Hence the size of a file level block is
// _not_ encoded in the block itself, and further the alignment
// structure includes a size for it _only_ and _only_ as a count of
// unsigned bytes for the alignment sub-block.  Lastly, there is not
// an end marker for the sub-block.  Hence, the alignment sub-block is
// first parsed to get the 'raw' unsigned byte array for it.  The
// contents of which must then be parsed in a second pass.  See
// aln_fmt for the definition of that sub-block parser.
//
var bam_fmt = {

    // BAM strings are C type _possibly_ 0 terminated.  This removes
    // the 0 if there.
    string0: function(size) {
        return this.parse(['string', size]).replace(/\0+$/, '');
    },

    // The alignment information blocks need to be read as chunks of
    // unsigned bytes from the input stream as the size is otherwise
    // ambiguous.  From these chunks, we can then parse the actual
    // data fields as needed.  So, we first need a parse reader for
    // unsigned byte arrays.
    ubarray: function(size) {
        var x = this.parse(['array', 'uint8', size]);
        return new Uint8Array(x);
    },

    // The header has a free form plain header text in SAM format of
    // size l_text and the number of references in the file.
    header: {
        magic:  ['string', 4],
        l_text: 'int32',
        text:   ['string0', function(){return this.current.l_text;}],
        n_ref:  'int32'
    },

    // References are structures with a string name for the reference
    // of size l_name, plus the length of the reference sequence.
    reference: {
        l_name: 'int32',
        name:   ['string0', function(){return this.current.l_name;}],
        l_ref:  'int32'
    },

    // This is the alignment information in 'raw' unsigned byte
    // format.  This is the only way to ensure we get the correct size
    // and data in this first pass parse.  A second pass parse will
    // then be able to decode the actual information.  See ALN_FMT
    // below.
    uba_aln: {
        block_size:   'int32',
        uba_aln_info: ['ubarray', function(){return this.current.block_size}]
    },

    // Fixed BAM file front data section.  Includes header and list of
    // reference information (reference structures).
    bam_front: {
        head: 'header',
        refs: ['array', 'reference',
               function(){return this.current.head.n_ref;}]
    },

    alnUbas: function () {
        var aln_ubas = [];
        while (this.tell() < this.len) {
            aln_ubas.push(this.parse('uba_aln'));
        };
        return aln_ubas;
    },

    bam: {
        front: 'bam_front',
        aln_ubas: 'alnUbas'
    }
};


// BAM integer type encoding to jParser integer parse types
//
var BAMintType2jParserType = {
    A: 'char',
    c: 'int8',
    C: 'uint8',
    s: 'int16',
    S: 'uint16',
    i: 'int32',
    I: 'uint32',
    f: 'float32'
};

// Alignments are structures encoding the alignment information for a
// reference, as identified by refID which may be -1 indicating no
// reference - no mapping position.  They consist of a well defined
// (though variable) header, and a totally adhoc set of 'auxillary'
// information.  This fact is why this parser is split from the main
// BAM parser and constitutes a second pass over the chunk of unsigned
// bytes obtained for a given alignment record.  Because of this, this
// parser is intended to be used (and must be so used) as part of the
// top level parse for the containing record.
//
var aln_fmt = {

    // BAM strings are C type _possibly_ 0 terminated.  This removes
    // the 0 if there.
    string0: function(size) {
        return this.parse(['string', size]).replace(/\0+$/, '');
    },

    header: {
        // block_size:  'int32', *** NOTE already parsed - see UBA_ALN above
        refID:       'int32',
        pos:         'int32',

        // bin_mq_nl: 'uint32',  We split the encoding in the parse here:
        l_read_name: 'uint8',
        mapq:        'uint8',
        bin:         'uint16',

        //flag_nc: 'uint32', We split the encoding in the parse here:
        n_cigar_op:  'uint16',
        flag:        'uint16',

        l_seq:       'int32',
        next_refID:  'int32',
        next_pos:    'int32',
        tlen:        'int32',
        read_name:   ['string0', function(){return this.current.l_read_name;}],

        // Note we need to split the cigars after parse [28bits,4bits]
        cigar:       ['array', 'uint32',
                      function(){return this.current.n_cigar_op;}],

        // Note these must be changed to strings after parse [4bits,4bits] AAs
        seq:         ['array', 'uint8',
                      function(){return Math.floor((this.current.l_seq+1)/2);}],

        qual:        ['string0', function(){return this.current.l_seq;}]
    },


    // Auxilliary information is adhoc with its format determined at
    // runtime and then the data decoded.  To help with this, we
    // define the various possible parsers for the auxilliary content
    // here.

    // Each auxilliary data has at least this much header to start
    aux_hd: {
        tag:      ['string0', 2],
        val_type: 'char'
    },

    // 'Z' types are standard null terminated C strings (but only
    // printable characters should be in them...).
    Cstring: function() {
        var res = cstg(this.tell(), this.aln_buf);
        this.seek(res[0]);
        return res[1];
    },

    // Numeric arrays ('B' types), have this parse structure, where
    // val_type codes for the type (and even more importantly, size)
    // of an array data element.  The count field is the number of
    // elements.
    Barray: {
        val_type: 'char',
        count:    'int32',
        value:    ['array',
                   function(){
                       return BAMintType2jParserType[this.current.val_type];
                   },
                   function(){return this.current.count;}]
    },

    // An auxilliary data element has only a runtime structure, making
    // a function description the simplest sub parser description.
    // This retuns a structure (map/'Object') that has the header
    // information (aux_hd) augmented with the actual value.
    aux: function () {
        var hd = this.parse('aux_hd');

        switch (hd.val_type) {
          case "B":
            var B = this.parse('Barray');
            hd.value = B.value;
            break;
          case "H":
            // As far as I can tell, these things are really just C
            // strings where each two char substring (starting at
            // index 0) codes for a Hex number in the usual scheme.
            hd.value = this.parse('Cstring');
            break;
          case "Z":
            hd.value = this.parse('Cstring');
            break;
          default:
            hd.value = this.parse(BAMintType2jParserType[hd.val_type]);
        }
        return hd;
    },

    //
    auxs: function () {
        var aux_data = [];
        while (this.tell() < this.aln_end) {
            aux_data.push(this.parse('aux'));
        };
        return aux_data;
    },

    aln: {head: 'header', aux_data: 'auxs'}
}




//===========================================================================//
// Simple evt callback for testing.
var files = undefined;
function readBinaryFile(evt) {
    files = evt.target.files;
    var f = files[0];
    var cb = function (x) {console.log(x);};
    var stgcb = function (x) {console.log(buffer2String(x));};
    this.theFile = f;

    console.log(f.name);
    var isBai = (f.name.split(".").slice(-1)[0] == "bai");

    if (f) {
        if (isBai) {
            var r = new readBaiFile(f);
            r.getIndex(function (bai) {
                           console.log(r.idxContent);
                           console.log(r.bhash);});
        } else {
            //var r = new readBinaryBAM(f1, f2);
            getBGZFHD(f, 0, cb);
            blockSize(f, 0, cb);
            nextBlockOffset(f, 0, cb);
        }

    } else {
        alert("Failed to load file");
    }
}
//===========================================================================//




// Start of main API.  There are two 'object' types with constructors:
//
// readBaiFile which takes a filespec and initializes a bai
// reader.  Provides methods
//
// readBinaryBAM which takes a bai filespec and a BGZF BAM filespec
// initializes a bai reader and builds binary BAM reader.
//

// Constructor for bai reader and decoder.  baifile is a bai binary
// index file.
function readBaiFile(baiFile) {
    if (!(this instanceof arguments.callee)) {
        throw new Error("Constructor may not be called as a function");
    }
    this.theFile = baiFile;
    this.baiThis = this;
}

// Main function for a bai reader.  Obtains and decodes the index
// and caches information on it used by other methods.  So, must be
// called before others.
readBaiFile.prototype.getIndex =
    function(cb){
        var cbfn = (cb != undefined) ? cb : function (x) {console.log(x);};
        var baiThis = this;
        var f = this.theFile;

        var bins2hash =
            function (binseq) {
                var bhash = {};
                var i = 0;
                for (var x in binseq) {
                    var b = binseq[x].bin;
                    bhash[b]=i;
                    i = i + 1;
                }
                return bhash;
            };

        var parseBAI =
            function(reader) {
                var resultBuffer = new Uint8Array(reader.result);
                var parser =  new jParser(resultBuffer, bai_fmt);
                baiThis.idxContent = parser.parse('bai');
                // Create bin xref hash table
                var n_ref = baiThis.idxContent.head.n_ref;
                baiThis.bhash = {};
                for (i = 0; i < n_ref; i++) {
                    baiThis.bhash[i] =
                        bins2hash(baiThis.idxContent.indexseq[i].binseq);
                };
                cbfn.call(baiThis, baiThis);
            };

        var reader = new FileReader();
        reader.onloadend =
            function (evt) {
                if (evt.target.readyState == FileReader.DONE) {
                    return parseBAI(reader);
                } else {
                    return alert('Bad read for ' + f +
                                 ' status: ' + evt.target.readyState);
                };
            };
        reader.readAsArrayBuffer(f.slice(0, f.size));
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
readBaiFile.prototype.bin2Ranges =
    function  (ref, binid) {
        return bin2Ranges(this, ref, binid)
    };

// First chunk region of binid.
readBaiFile.prototype.bin2Beg =
    function (ref, binid) {
        return bin2Beg(this, ref, binid);
    };

// Last chunk region of binid.
readBaiFile.prototype.bin2End =
    function (ref, binid) {
        return bin2End(thi, ref, binid);
    };


// For a reference REF region defined by BEG and END return the set of
// chunks of all bins involved as a _flat_ vector of two element
// vectors, each defining a region of a bin.
readBaiFile.prototype.getChunks =
    function (ref, beg, end) {
        return getChunks(this, ref, beg, end);
    };



//===========================================================================//




// Constructor for BGZF BAM reader and decoder.  baifile is a bai
// binary index file for BAMFILE, a BGZF encoded BAM file.  Inits and
// builds index and initializes the BAM reader.
function readBinaryBAM (baiFile, bamFile, cb) {
    if (!(this instanceof arguments.callee)) {
        throw new Error("Constructor may not be called as a function");
    }

    var r = new readBaiFile(baiFile);

    this.theFile = bamFile;
    this.baiR = r;
    this.bamThis = this;

    r.getIndex(cb);
}


// Obtain the front (apriori schema) data section of a BAM from its
// reader.  The front section consists of a static fixed size
// 'header', and the more important variable length sequence of
// variable sized references.  Each reference has a name, length
// (l_ref:), and implicitly its index location in the reference
// sequence.
//
// Parses out the header and references, adds attributes 'head' and
// 'refs' respectively to the BAM reader for them, and computes a
// reference name to reference index map (hash map) and places this on
// attribute 'refhash'.  Finally, calls cb with the header and refs:
// cb(head, refs).
readBinaryBAM.prototype.bamFront =
    function (cb) {
        var bamRthis = this;

        if (this.head) {
            cb.call(this, this.head, this.refs);
        } else {
            wrappedJParser(
                this.theFile, 0, bam_fmt, function(bhp){
                    bamRthis.bhp = bhp;
                    bamRthis.head = bhp.head = bhp.parse("header");
                    getBamRefs(bhp, function(refs){
                        bamRthis.refs = refs;
                        bamRthis.refhash = simpleHash(refs, "name");
                        bamRthis.headUba = bhp.curBuf.subarray(0, bhp.tell());
                        bamRthis.bamFront(cb);
                    });
                });
        };
    };


// Converts a reference name to its bam index.  References in bam
// processing always need to be by their index.  Requires that
// bamFront has run.
readBinaryBAM.prototype.refName2Index =
    function (name) {
        return this.refhash[name];
    };


// Return the set of references that have reads in this bam. Requires
// that bamFront has run or if RUNFRONT is true, will implicitly run
// bamFront.  If runfront is not true and bamFront has not yet run,
// calls CB with undefined.
//
// Returns a [[ref-bin-info, ref-name-info]...]
//
// Where ref-bin-info = {binseq: .., intervseq: ...} is the index file
// information for the reference, and ref-bame-info = {name: ...,
// l_ref: ...} is the corresponding BAM file reference name info in
// the BAM header.
//
readBinaryBAM.prototype.refsWithReads =
    function (cb, runfront) {
        bamRthis = this;
        if (bamRthis.refsWreads) {
            if (cb) {cb.call(bamRthis, bamRthis.refsWreads)};
            return bamRthis.refsWreads;
        } else if (bamRthis.refs) {
            var rswr =
                bamRthis.baiR.idxContent.indexseq.reduce(
                    function(V, x, i, _){
                        if (x.n_bin != 0 || x.n_intv != 0)
                            V.push([x, bamRthis.refs[i]]);
                        return V;
                    }, []);
            bamRthis.refsWreads = rswr;
            if (cb) {cb.call(bamRthis, rswr)};
            return rswr;
        } else if (runfront) {
            bamRthis.bamFront(function(hd, refs){bamRthis.refsWithReads(cb)});
        } else {
            if (cb) {cb.call(bamRthis, undefined)};
            return undefined;
        };
    };


// Obtains the set of raw unsigned byte arrays containing the
// alignments of REF in the region [beg, end).  Ref may be a reference
// index or reference name (in which case it is implicitly converted
// to its index - see refName2Index).  On finish, calls CB with the
// vector of ubas.  Note cb is called with this == the bam reader.
//
// Due to the adhoc structure of aln information, alignment parsing
// needs to be split between obtaining (via parse) the raw byte arrays
// containing the alignments and the alignments contained in the these
// arrays via secondary parse - see getAlns.
readBinaryBAM.prototype.getAlnUbas =
    function (ref, beg, end, cbfn, binary) {

        var ref = (typeof(ref) == "number") ? ref : this.refName2Index(ref);
        var bamRthis = this;
        var baiR = this.baiR
        var cnks = baiR.getChunks(ref, beg, end);

        var aln_ubas = [];
        var fn1 = function (ckbeg, ckend) {
            var vfbeg = ckbeg[0]; // Virtural file offset of beg bvzf blk
            var bobeg = ckbeg[1]; // Beg offset in beg inflated data
            var vfend = ckend[0]; // Virtural file offset of end bvzf blk
            var boend = ckend[1]; // End offset into end inflated data
            wrappedRegionJParser(
                bamRthis.theFile, vfbeg, vfend, bam_fmt,
                function(bap) {
                    bap.seek(bobeg);
                    bap.len = bap.curBuf.byteLength - bap.endBlkSz + boend;
                    bamRthis.bap = bap;
                    ubaInfo = _getRegAlnUba(bap);
                    ubaInfo.uba = bap.curBuf;
                    aln_ubas.push(ubaInfo);
                    if (cnks.length > 0) {
                        var rng = cnks.pop();
                        fn1(rng[0], rng[1]);
                    } else {
                        cbfn.call(bamRthis, aln_ubas);
                    };
                });
        };

        if (cnks.length > 0) {
            var rng = cnks.pop();
            fn1(rng[0], rng[1]);
        } else {
            cbfn.call(bamRthis, []);
        };
    };


// Main function for BAM reader.  For a reference REF alignment region
// defined by BEG and END, obtains the set of bins and chunks covering
// the region, inflates the corresponding data blocks, obtains the raw
// unsigned byte arrays for each contained alignment, parses each such
// array for the detailed alignement information in it, producing a
// vector of all alignments for ref in the region.  Calls CBFN with
// the vector.  Note cbfn is called with this == the bam reader.
//
// Makes use of getAlnUbas as the intermediary parse for the set of
// unsigned byte arrays for the region contained alignments.
readBinaryBAM.prototype.getAlns =
    function (ref, beg, end, cbfn, binary) {
        var bamRthis = this;

        bamRthis.getAlnUbas(
            ref, beg, end,
            function (ubas) {
                var alns = [];
                ubas.forEach(function(ubaInfo){
                    if (binary) {
                        alns.push(ubaInfo);
                    } else {
                        var aln_blksizes = ubaInfo.aln_blksizes;
                        var offset = ubaInfo.beg + 4;
                        var p = new jParser(ubaInfo.uba, aln_fmt);
                        p.aln_buf = ubaInfo.uba;
                        for (var i = 0; i < aln_blksizes.length; i++){
                            p.seek(offset);
                            p.aln_end = offset + aln_blksizes[i];
                            alns.push(p.parse('aln'));
                            offset = p.tell() + 4;
                        };
                    };
                });
                cbfn.call(bamRthis, alns);
            });
    };


// Takes a region map regmap, representing a reference and region of
// form {name:, start:, end:}, where name denotes a reference
// (typically the name of a reference from the reference index), and
// start and end denote the beginning and end of a region on
// reference.
//
// Computes the set of unsigned byte array block chunks covering the
// region, ensures ubas are maximal blocks for bgzf deflation (see
// coalesce65KBCnks for more information), and bgzf deflates each
// block to a corresponding legal bgzf block.
//
// Calls CBFN with the resulting vector of bgzf blocks for the region.
//
// Can be used to write custom bam renderers from segments of the
// containing bam.  See regions2BAM for an example use that works
// across a set of ref/region maps as returned by samplingRegions.
readBinaryBAM.prototype.region2BAM =
    function (regmap, cbfn) {
        var bamRthis = this;

        bamRthis.getAlns(// IO here, so must use CBs...
            regmap.name, regmap.start, regmap.end,
            function(alninfos) {
                console.log("Done");
                //alninfos.reverse();
                var bgzfBlks =
                    getBgzfBlocks(coalesce65KBCnks(alnCnkUbas(alninfos)));
                cbfn.call(bamRthis, bgzfBlks);
            }, true);
    };

// Takes a vector refsNregions of region maps [regmap, ...] and for
// each such regmap, calls readBinaryBAM.region2BAM(regmap,
// cbfn). Each such call will be synchronized to the required io
// involved and thus calls will be ordered by order of regmaps in
// refsNregions.  Details of regmap format can be found at region2BAM.
//
// Example of use: (bamR is assumed to be a readBinarBAM reader)
//
// var withReads = bamR.refsWithReads().map(function(x) {return x[1]});
// var refsNregions = samplingRegions(withReads, {}).regions;
// var totcnt = 0;
// var bamblks = [bamR.headUba];
//
// bamR.regions2BAM(
//   refsNregions,
//   function(bgzfblks){
//     if (bgzfblks) {
//       console.log(bgzfblks);
//       bamblks.push(bgzfblks);
//       totcnt = totcnt + bgzfblks.reduce(
//         function(S, x){return S+x.length},0)
//     } else {
//       bamblks.push(EOFblk);
//       console.log("FINISHED")}});
//
// *** NOTE: in this variant _all_ regmaps in refsNregions are
// *** processed.  See throttledRegions2BAM for variant where the
// *** user, via cbfn / continuation, has more control.
readBinaryBAM.prototype.regions2BAM =
    function (refsNregions, cbfn) {
        var bamRthis = this;
        var refregmaps = refsNregions.reverse();

        var reduce = function (regmap) {
            bamRthis.region2BAM(
                regmap,
                function (bgzfBlks){
                    if (refregmaps.length > 0) {
                        cbfn.call(bamRthis, bgzfBlks);
                        reduce(refregmaps.pop());
                    } else {
                        cbfn.call(bamRthis, undefined);
                    };
                });
        };
        if (refregmaps.length > 0) {
            reduce(refregmaps.pop());
        } else {
            cbfn.call(bamRthis, undefined);
        };
    };

// Similar to regions2BAM but where CBFN controls when and how much of
// refsNregions to step through. CBFN must have the following
// signature to make this work:
//
// function (bgzfBlks, contfn, regmap) {...}
//
// The first argument is just as for cbfn for regions2BAM.  The next
// two provide the throttling effect. contfn closes over the control
// state of the processing (basically the refmaps left) to enable user
// determined stepping.  To make the next step through refsNregions,
// cbfn would call the continuation function contfn with regmap as its
// argument:
//
// function (bgzfBlks, contfn, regmap){
//  ...
//  if (continue) {
//    ...
//    contfn.call(this, regmap); // NOTE 'this' here is the binary bam reader
//  } else {
//    ...
//  }
// }
//
// Example of use: (bamR is assumed to be a readBinaryBAM reader)
//
// var withReads = bamR.refsWithReads().map(function(x) {return x[1]});
// var refsNregions = samplingRegions(withReads, {}).regions;
// var bamblks = [bamR.headUba];
//
// var bgzfHdr = bgzf(bamR.headUba);
// stream([bgzfHdr]); // Send header
//
// var regcnt = 0;
// var totcnt = bgzfHdr.length + EOFblk.length;
//
// bamR.throttledRegions2BAM(
//   refsNregions,
//   function(bgzfblks, fn, regmap){
//     // Only send two regions
//     if (bamblks && regcnt < 2) {
//       stream(bgzfblks);
//       totcnt = totcnt + bgzfblks.reduce(
//         function(S, x){return S+x.length},0);
//       regcnt = regcnt + 1;
//       fn.call(this, regmap);     // Step next region
//     } else {
//       stream(EOFblk);
//       console.log("FINISHED, total bytes sent:", totcnt)}})
//
readBinaryBAM.prototype.throttledRegions2BAM =
    function (refsNregions, cbfn) {
        var bamRthis = this;
        var refregmaps = refsNregions.reverse();

        var contfn = function (regmap) {
            bamRthis.region2BAM(
                regmap,
                function (bgzfBlks){
                    if (refregmaps.length > 0) {
                        cbfn.call(bamRthis, bgzfBlks,
                                  contfn, refregmaps.pop());
                    } else {
                        cbfn.call(bamRthis, undefined);
                    };
                });
        };
        if (refregmaps.length > 0) {
            contfn(refregmaps.pop());
        } else {
            cbfn.call(bamRthis, undefined);
        };
    };



// Synonym for bai getChunks.  Directly callable on a bamReader.
readBinaryBAM.prototype.getChunks =
    function (ref, beg, end) {
        var baiR = this.baiR;
        return baiR.getChunks(ref, beg, end);
    };




//===========================================================================//

// Low level function (mostly for use by higher level methods attached
// to readBinaryBAM.  Obtain the sequence of references in BAM encoded
// in _wrapped_ parser P.  Calls CB with the resulting sequence of
// references.
function getBamRefs (p, cb) {
  var rcnt = 0;
  var refs = [];

  var getRef = function(x){
    //console.log("rcnt " + rcnt, "x", x, "refs", refs);
    if (rcnt < p.head.n_ref){
      rcnt++;
      refs.push(x);
      nextParseRes(p, "reference", getRef);
    } else {
      cb.call(this, refs);
    };
  };

  nextParseRes(p, "reference", getRef);
}


// Low level function (mostly private for use by higher level methods
// attached to readBinaryBAM.  Given a wrapped parser P, for a ref and
// region inflated buffer, obtain the set of unsigned byte arrays for
// the alignments for the region.  See the method getAlnUbas for
// setup.
function _getAlnUbas (p, cb) {
  var aln_ubas = [];

  var getAlnUba = function(x){
    aln_ubas.push(x);
    if (p.tell() < p.len){
      nextParseRes(p, "uba_aln", getAlnUba);
    } else {
      cb.call(this, aln_ubas);
    };
  };

  nextParseRes(p, "uba_aln", getAlnUba);
}

// Low level function (mostly private for use by higher level methods
// attached to readBinaryBAM.  Given a wrapped parser P, for a ref and
// region inflated buffer, obtain the _full_ unsigned byte arrays for
// all the contained alignments for the region.  See the method
// getAlnUbas for setup.
function _getRegAlnUba (p) {
    var beg = p.tell();
    var bsize = p.parse('int32');
    var end = beg + bsize + 4;
    var aln_blksizes = [bsize];

    while (end < p.len) {
        //console.log(end);
        p.seek(p.tell() + bsize);
        bsize = p.parse('int32');
        aln_blksizes.push(bsize);
        end = end + bsize + 4;
    };

    return {beg: beg, aln_blksizes: aln_blksizes, end: end};
}




// The next set of functions support the construction of ~65KB bam
// alignment blocks and their deflation to bgzf format for
// construction of new representative bam file or to be streamed to
// another service (for example, bam-stats-alive).


// Takes a vector of alignment information maps [alninfo-map, ...], where
//
// alinfo-map = {beg, end, aln_blksizes, uba}, where
//
// * uba: is the unsigned byte array containing the alignments
// * beg: is the beginning offset into uba of the first of interest
// * end: is the ending offset into uba of the last byte of last of interest
// * aln_blksizes: is a vector of all the block sizes of the
//   alignments of interest.  NOTE these sizes do NOT reflect the
//   extra 4 bytes of header size (from which each block size is
//   obtained).
//
// The API here provides these maps by bamBinaryReader.getAlns, with
// the binary flag set true.  Further, the typical use case is to
// obtain a sampling region(s) via samplingRegions, and for each such
// region use getAlns to obtain its alninfo map vector.
//
// Reduces input by transforming each alninfo-map alninfo by:
// (-> alninfo.aln_blksizes
//     get65KBCnkBlkSizes
//     get65KBCnkUbas(%, alninfo.beg, alninfo.uba)
//     getBgzfBlocks)
//
// Returns the resulting vector of bgzf blocks
//
// *** NOTE: no longer currently used in the internal api, but may be
// *** of use to end user.
function alnInfo2BgzfBlks (alninfos) {
    var alninfos = (alninfos instanceof Array) ? alninfos : [alninfos];
    var bgzf_blks = alninfos.reduce(
        function (Bs, alninfo) {
            return Bs.concat(
                getBgzfBlocks(
                    get65KBCnkUbas(
                        get65KBCnkBlkSizes(
                            alninfo.aln_blksizes),
                        alninfo.beg,
                        alninfo.uba)[0]));
        }, []);
    return bgzf_blks;
}


// Takes a vector of alignment information maps [alninfo-map, ...], where
//
// alinfo-map = {beg, end, aln_blksizes, uba}, where
//
// * uba: is the unsigned byte array containing the alignments
// * beg: is the beginning offset into uba of the first of interest
// * end: is the ending offset into uba of the last byte of last of interest
// * aln_blksizes: is a vector of all the block sizes of the
//   alignments of interest.  NOTE these sizes do NOT reflect the
//   extra 4 bytes of header size (from which each block size is
//   obtained).
//
// The API here provides these maps by bamBinaryReader.getAlns, with
// the binary flag set true.  Further, the typical use case is to
// obtain a sampling region(s) via samplingRegions, and for each such
// region use getAlns to obtain its alninfo map vector.
//
// Reduces input by transforming each alninfo-map alninfo by:
// (-> alninfo.aln_blksizes
//     get65KBCnkBlkSizes
//     get65KBCnkUbas(%, alninfo.beg, alninfo.uba))
//
// Returns the vector of corresponding <= 65kb uba alignment cnks.
// However, note that 'scattered' alignments can produce corresponding
// uba cnks of much smaller size than 65kb optimum for bgzf deflation.
// Hence, see coalesce65KBCnks below.
function alnCnkUbas (alninfos) {
    var alninfos = (alninfos instanceof Array) ? alninfos : [alninfos];
    var alnubas = alninfos.reduce(
        function (Bs, alninfo) {
            return Bs.concat(
                get65KBCnkUbas(
                    get65KBCnkBlkSizes(
                        alninfo.aln_blksizes),
                    alninfo.beg,
                    alninfo.uba)[0]);
        }, []);
    return alnubas;
}

// Takes a vector of alignment unsigned byte array (UBA) cnks and
// 'coalesces' adjacent chunks up to a maximum of 65kb cnks.  The idea
// here is to merge small (potentially as small as one alignment) ubas
// into a maximal bgzf input block uba.  See alnCnkUbas for more
// details and information.
function coalesce65KBCnks (alnubas) {
    var grp65kbUbas = alnubas.reduce(
        function(Cnks, uba) {
            var cur = Cnks.pop();
            cur = (cur) ? cur : [];
            var tsm = cur.reduce(function(S,x){return S+x.length},0);
            if ((tsm + uba.length) < _65kb) {
                cur.push(uba);
                Cnks.push(cur);
            } else {
                Cnks.push(cur);
                Cnks.push([uba]);
            };
            return Cnks;
        }, []);
    var coalescedUbas = grp65kbUbas.reduce(
        function (Ubas, grp) {
            Ubas.push(appendBuffers(grp, true));
            return Ubas;
        });
    return coalescedUbas;
}



// Take a set of alignment block sizes (NOT including the block's size
// header word, of size 4 bytes), and reduce these to a vector
// containing vectors of these sizes where the total size of each such
// vector is <= 65KB.  Order of block sizes is maintained in and
// across these vectors.
//
// Typically, blksizes comes from a random sampling of regions in the
// bam file.  See samplingRegions in bv-local-sampling.js
//
// Returns the vector of these vectors
function get65KBCnkBlkSizes (blksizes) {
    return blksizes.reduce(
        function(Cnks, bsz) {
            var cur = Cnks.pop();
            cur = (cur) ? cur : [];
            var tsm = cur.reduce(function(S,x){return S+x+4;},0);
            if ((tsm + bsz + 4) < _65kb) {
                // This is lame - push should NOT return the value,
                // but the new array!!!
                cur.push(bsz);
                Cnks.push(cur);
            } else {
                Cnks.push(cur);
                Cnks.push([bsz]);
            };
            return Cnks;
        }, []);
}

// Take a vector of vectors [blksizes1, blksizes2, ...], where each
// contained blksizes vector is a set of alignment block sizes (NOT
// including the block's size header word, of length 4 bytes), plus a
// unsigned byte array UBA containing at least all such alignments,
// and a beginning offset into this UBA, BEG.  Reduces the blksizes
// vectors to actual subarrays of UBA of size <= 65KB (corresponding
// to the total size represented by the blksizes vector).  Returns:
//
// [[sububa-1, ..., sububa-n], endoffset], where
//
// endoffset is the final offset into UBA of the ending byte of
// sububa-n
function get65KBCnkUbas (sets65KBblksizes, beg, uba) {
    return sets65KBblksizes.reduce(
        function(Bs, szs) {
            var bufs = Bs[0];
            var offset = Bs[1];
            // NOTE, must use initial value to ensure all get extra 4
            // bytes for header word
            var sz = szs.reduce(function(S,x){return S+x+4},0);
            var end = offset + sz;
            var nbuf = uba.subarray(offset, end);
            bufs.push(nbuf);
            //console.log(offset, end, sz);
            Bs[0] = bufs;
            Bs[1] = end;
            return Bs;
        }, [[], beg]);
}

// Take a vector of ~65kb 'cnk' blocks and reduces it to deflate each
// cnk to bgzf format.  Any concatenation of these represents a valid
// bgzf file.  Adding a bam header and 'eof' block will produce a
// valid bam file.  Returns [bgzfuba-1, ..., bgzfuba-n]
function getBgzfBlocks (cnkubas) {
    return cnkubas.reduce(
        function (bgzfUbas, cnkuba) {
            bgzfUbas.push(bgzf(cnkuba));
            return bgzfUbas;
        }, []);
}

// End of functions for construction of new bam from containing bam.




// Functions to decode cigar information from an alignment head.
//
// cigarOp returns the cigar op encoded in a uint32 cigar value
// cigarInfo returns a map {op: the-op, op_len: the-op-len} from a
// cigar value
function cigarOp(n){return "MIDNSHP=X"[n]}
function cigarInfo(cigar) {
  return {op: cigarOp(cigar & 0xF),
          op_len: (cigar >>> 4)};
}

// Binary BAM reader methods to convert an alignment cigar sequence
// into a sequence of cigarInfo maps.  ALN is a _parsed_ alignment.
readBinaryBAM.prototype.cigarInfo =
    function (aln) {
        return _.map(aln.head.cigar, cigarInfo);
    };


// Functions to convert an alignment's parsed seq to AA or NT seq
// (depending on what was encoded in seq).  aln.head.seq is encodes AA
// or NT codes in 4 bits - two per uint8 byte in seq.
//
// nyble2aa returns an AA or NT code for the 4 bit value N.
// byte2aas returns the pair of AA or NT codes in the byte B
//
function nyble2aa (n) {return "=ACMGRSVTWYHKDBN"[n]}
function byte2aas (b) {
  var x = [nyble2aa(b >>> 4), nyble2aa(b & 0xF)];
  return x.join("");
}

// Takes a seq (array of uint8), from the header of an alignment and
// converts all bytes and then joins them to create bio seq.
function seq2aas (seq) {
  var aapairs = _.map(seq, byte2aas);
  return aapairs.join("");
}

// Binary BAM reader method to convert an alignment header seq to its
// corresponding biological sequence.  ALN is a _parsed_ alignment.
readBinaryBAM.prototype.getSeq =
    function (aln) {
        return seq2aas(aln.head.seq);
    };




//var baiR = new readBaiFile(files[1]);
//baiR.getIndex(function(r){console.log(r.idxContent)})
// baiR.idxContent.indexseq[0].binseq
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
