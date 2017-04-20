//--------------------------------------------------------------------------//
//                                                                          //
//                         J S - B V - S A M P L I N G                      //
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
// License information here                                                 //
//                                                                          //
// Author: Jon Anthony (2014)                                               //
//                                                                          //
//--------------------------------------------------------------------------//
//



// General JS sort predicate (this should really go in some general
// util pkg).
function _spred (l, r) {
    return ((l < r) ? -1 : ((l > r) ? 1 : 0));
}




// An estimator of coverage depth of corresponding data file (for the
// index reader - bam or vcf) which uses bytes in region segments as
// the indicator.  Returns the estimations as an object of 'segment
// information' maps, where each key is a reference id and each such
// map is:
//
// {relBinNum: (segment-bin-num - start16kbBinid)
//  pos:       (relBinNum * 16KB)
//  depth:     total-byte-count}
//
function estimateCoverageDepth (indexReader, cb) {
    var rdrThis = indexReader;

    if (rdrThis.estimates) {
        if (cb) {cb.call(rdrThis, rdrThis.estimates)};
        return rdrThis.estimates;
    } else {
        var indexWreads =
            rdrThis.idxContent.indexseq.reduce(
                function(V, x, i, _){
                    if (x.n_bin != 0 || x.n_intv != 0)
                        V.push([x, i]);
                    return V;
                }, []);

        rdrThis.estimates =
            indexWreads.reduce(
                function(RD, I) {
                    var ref = I[1];
                    var withLeafBins = I[0].binseq.filter(
                        function(o){
                            return (o.bin >= start16kbBinid &&
                                    o.bin <= end16kbBinid);});

                    refsegs =
                        withLeafBins.reduce(
                            function(segs, o){
                                var b = o.bin;
                                var relBinNum = (b - start16kbBinid);
                                var position =  (relBinNum * _16KB);
                                var readDepth =
                                    o.chunkseq.reduce(
                                        function(bc, chunk) {
                                            var b = chunk.cnk_beg.valueOf();
                                            var e = chunk.cnk_end.valueOf();
                                            
                                            var bShift = rshift16(b);
                                            var eShift = rshift16(e);
                                            // If the difference between the right shifted begin and
                                            // end positions is zero, yet the end position (not right shifted)
                                            // is greater than zero, something is wrong with the 
                                            // values.  In this case, bypass right shifting.
                                            if ((eShift - bShift) == 0 && e > 0) {
                                                return bc + 1;
                                            } else {
                                                return bc + (eShift - bShift);
                                            }

                                        }, 0);
                                segs.push(
                                    {relBinNum: relBinNum,
                                     pos: position, depth: readDepth});
                                return segs;
                            }, []);
                    RD[ref] = refsegs;
                    return RD;
                }, {});
        return estimateCoverageDepth(indexReader, cb);
    };
}


// Mapping function over the estimate read coverage segments of
// reference REFID.  See estimateCoverageDepth for more on segments.
// Requires that estimateCoverageDepth has been run; if not will
// implicitly run it.  FN is a function of a segment information map.
// For the sq of segment information maps for REFID [segmap...],
// returns the sq [fn(seqmap)...].  If keepNils is true, returns fn
// results which are nil (undefined), otherwise, returns only non nil
// results.
function mapSegCoverage (indexReader, refid, fn, keepNils) {
    var info = function (i) {
        var e = indexReader.estimates[refid][i];
        return (e) ? e : {relBinNum: i, pos: i * 16384, depth: 0}
    };

    var res = [];
    for (var i = 0; i < (end16kbBinid - start16kbBinid); i++) {
        var user_res = fn.call(indexReader, info(i))
        if (keepNils) {
            res.push(user_res);
        } else if (user_res) {
            res.push(user_res);
        }
    };
    return res;
}


// For a sq of reference objects REFS (as obtained from file header
// information), and option set OPTIONS (a map of parameter options),
// obtains a set of region samples for each reference in refs.  The
// resulting set is intended to be a 'representative sample' of the
// base file.
//
// Tuning parameters in options are binSize, the max size covered by a
// bin; binNumber, the max number of bins to consider for a ref;
// start, the starting position of the sequence associated with a ref
// for a sampling.  Defaults are binSize = 40000 bases, binNumber =
// 20, start = 1.
//
// Returns a map {regions: [reginfo...], regstr: json-string-for-regions}
//
// where reginfo = {name: ref.name, start: int, end: int} and regions
// is sorted ascendingly by name by numerical start
//
//
function samplingRegions (refs, options) {
    var bsize = options.binSize || 40000;
    var bcnt  = options.binNumber || 20;
    var start = options.start || 1;
    var regions =
        refs.reduce(
            function(regs, ref) {
                var len = ref.l_ref - start;
                if (len > 0 && len < (bcnt * bsize)) {
                    regs.push({name: ref.name, start: start, end: ref.l_ref});
                } else if (len > 0) {
                    // There are cases where it is too painful to
                    // write functional code in js...
                    for (var i = 0; i < bcnt; i++) {
                        var nstart = start + Math.floor(Math.random() * len);
                        regs.push(
                            {name: ref.name,
                             start: nstart,
                             end: nstart + bsize});
                    };
                };
                return regs;
            }, []);

    regions = regions.sort(function(l,r){
        if (l.name ==  r.name) {
            return parseInt(l.start) - parseInt(r.start)
        } else {
            return _spred(l.name, r.name);
        }});

    return {regions: regions,
            regstr: JSON.stringify(regions.map(
                function(r){
                    // This is broken - SERVERs should change names if needed!
                    return {start: r.start, end: r.l_ref, chr: r.name}}))};
}


// Convert ref objects to old format.  Servers should be upgraded to
// use the actual bai index names, but this will provide interim
// support.  Takes a sq REFS of reference objs, each of format {name:
// string, l_ref: int, l_name: int} and converts to sq of objs of
// format {name: string, end: int}
function new2oldRefs (refs) {
    return refs.map(function(ref) {return {name: ref.name, end: ref.l_ref}});
}


// Obtain a bamstats-alive url encoding for a region sampling of REFS
// a sq of reference objects as obtained from file header information.
// Performs a samplingRegions operation to obtain the region
// information and then generates a corresponding url for the
// bamstats-alive server.  OPTIONS is as for samplingRegions.
// bsaliveURL is the base http address for a bamstats-alive server.
function bamStatsAliveSamplingURL (refs, options, bsaliveURL) {
    var regionInfo = samplingRegions(refs, options);
    var regions = regionInfo.regions;
    var regStr = regionInfo.regStr;
    return encodeURI(
        bsaliveURL +
            '?cmd=-u 3000 -r \'' + regStr + '\' ' +
            encodeURIComponent(getBamRegionsUrl(regions)));
}