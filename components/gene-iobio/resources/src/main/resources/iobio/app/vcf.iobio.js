//
//  vcfiobio
//  Tony Di Sera
//  October 2014
//
//  This is a data manager class for variant data.
//
//  Two file are used to generate the variant data:
//    1. the bgzipped vcf (.vcf.gz)
//    2. its corresponding tabix file (.vcf.gz.tbi).
//
vcfiobio = function module() {

  var debug =  false;

  var exports = {};

  var dispatch = d3.dispatch( 'dataReady', 'dataLoading');

  var SOURCE_TYPE_URL = "URL";
  var SOURCE_TYPE_FILE = "file";
  var sourceType = "url";


  var vcfURL;
  var vcfReader;
  var vcfFile;
  var tabixFile;
  var size16kb = Math.pow(2, 14);
  var refData = [];
  var refDensity = [];
  var refName = "";

  var regions = [];
  var regionIndex = 0;
  var stream = null;


var effectCategories = [
['coding_sequence_variant', 'coding'],
['chromosome' ,'chromosome'],
['inframe_insertion'  ,'indel'],
['disruptive_inframe_insertion' ,'indel'],
['inframe_deletion' ,'indel'],
['disruptive_inframe_deletion'  ,'indel'],
['downstream_gene_variant'  ,'other'],
['exon_variant' ,'other'],
['exon_loss_variant'  ,'exon_loss'],
['frameshift_variant' ,'frameshift'],
['gene_variant' ,'other'],
['intergenic_region'  ,'other'],
['conserved_intergenic_variant' ,'other'],
['intragenic_variant' ,'other'],
['intron_variant' ,'other'],
['conserved_intron_variant' ,'other'],
['miRNA','other'],
['missense_variant' ,'missense'],
['initiator_codon_variant'  ,'missense'],
['stop_retained_variant'  ,'missense'],
['rare_amino_acid_variant'  ,'rare_amino_acid'],
['splice_acceptor_variant'  ,'splice_acceptor'],
['splice_donor_variant' ,'splice_donor'],
['splice_region_variant'  ,'splice_region'],
['stop_lost'  ,'stop_lost'],
['5_prime_UTR_premature start_codon_gain_variant' ,'utr'],
['start_lost' ,'start_lost'],
['stop_gained'  ,'stop_gained'],
['synonymous_variant' ,'synonymous'],
['start_retained' ,'synonymous'],
['stop_retained_variant'  ,'synonymous'],
['transcript_variant' ,'other'],
['regulatory_region_variant'  ,'regulatory'],
['upstream_gene_variant'  ,'other'],
['3_prime_UTR_variant'  ,'utr'],
['3_prime_UTR_truncation +','utr'],
['5_prime_UTR_variant'  ,'utr'],
['5_prime_UTR_truncation +','utr']
];

  exports.isFile = function() {
    return sourceType != null && sourceType == SOURCE_TYPE_FILE;
  }

  exports.hasFileOrUrl = function() {
    return vcfURL != null || vcfFile !=null;
  }

  exports.clear = function() {
    vcfURL = null;
    vcfFile = null;
  }


  var errorMessageMap =  {
    "tabix Could not load .tbi": {
        regExp: /tabix\sError:\s.*:\sstderr\s-\sCould not load .tbi.*/,
        message:  "Unable to load the index (.tbi) file, which has to exist in same directory and be given the same name as the .vcf.gz with the file extension of .vcf.gz.tbi.  "
    },
     "tabix [E::hts_open]": {
        regExp:  /tabix\sError:\s.*:\sstderr\s-\s\[E::hts_open\]\sfail\sto\sopen\sfile/,
        message: "Unable to access the file.  "
     },
     "tabix [E::hts_open_format]": {
        regExp:  /tabix\sError:\s.*:\sstderr\s-\s\[E::hts_open_format\]\sfail\sto\sopen\sfile/,
        message: "Unable to access the file. "
     }
  }

  var ignoreMessages =  [
    /tabix\sError:\s.*:\sstderr\s-\s\[M::test_and_fetch\]\sdownloading\sfile\s.*/,
    /tabix\sError:\s.*:\sstderr\s-\s.*to local directory/
  ];



  exports.openVcfUrl = function(url, callback) {
    var me = this;
    sourceType = SOURCE_TYPE_URL;
    vcfURL = url;

    var rexp = /^(?:ftp|http|https):\/\/(?:(?:[^.]+|[^\/]+)(?:\.|\/))*?(vcf\.gz)$/;
    var parts = rexp.exec(url);
    // first element has entire url, the second element is the .vcf.gz extension.
    var extension = parts && parts.length == 2  ? parts[1] : null;
    if (extension == null) {
      callback(false, "Please specify a URL to a compressed, indexed vcf file with the file extension vcf.gz");
    } else {
      this.checkVcfUrl(url,function(success, message) {
          callback(success, message);
      });
    }

  }

  exports.getHeader = function(callback) {
    if (sourceType.toLowerCase() == SOURCE_TYPE_URL.toLowerCase() && vcfURL != null) {

      var buffer = "";
      var cmd = new iobio.cmd(
            IOBIO.tabix,
            ['-H', vcfURL]
        );

        cmd.on('data', function(data) {
          if (data != undefined) {
            success = true;
            buffer += data;
          }
        });

        cmd.on('end', function() {
          if (success == null) {
            success = true;
          }
          if (success && buffer.length > 0) {
            callback(buffer);
          }
        });

        cmd.on('error', function(error) {
          console.log(error);
        })
        cmd.run();
        
    } else if (vcfFile) {
        var vcfReader = new readBinaryVCF(tabixFile, vcfFile, function(tbiR) {
          vcfReader.getHeader( function(theHeader) {
            callback(theHeader);
          });
        });
    } else {
      callback(null);
    }

  }


  exports.checkVcfUrl = function(url, callback) {
    var me = this;
    var success = null;
    var buffer = "";
    var recordCount = 0;
    var cmd = new iobio.cmd(
        IOBIO.tabix,
        ['-H', url],
        {ssl: useSSL}
    );

    cmd.on('data', function(data) {
      if (data != undefined) {
        success = true;
        buffer += data;
      }
    });

    cmd.on('end', function() {
      if (success == null) {
        success = true;
      }
      if (success && buffer.length > 0) {
        callback(success);
      }
    });

    cmd.on('error', function(error) {
      if (me.ignoreErrorMessage(error)) {
      } else {
        if (success == null) {
          success = false;
          console.log(error);
          callback(success, me.translateErrorMessage(error));
        }
      }

    });

    cmd.run();
  }

  exports.ignoreErrorMessage = function(error) {
    var me = this;
    var ignore = false;
    ignoreMessages.forEach( function(regExp) {
      if (error.match(regExp)) {
        ignore = true;
      }
    });
    return ignore;

  }

  exports.translateErrorMessage = function(error) {
    var me = this;
    var message = null;
    for (key in errorMessageMap) {
      var errMsg = errorMessageMap[key];
      if (message == null && error.match(errMsg.regExp)) {
        message = errMsg.message;
      }
    }
    return message ? message : error;
  }

  exports.openVcfFile = function(event, callback) {
    sourceType = SOURCE_TYPE_FILE;

    if (event.target.files.length != 2) {
       callback(false, 'must select 2 files, both a .vcf.gz and .vcf.gz.tbi file');
       return;
    }

    if (endsWith(event.target.files[0].name, ".vcf") ||
        endsWith(event.target.files[1].name, ".vcf")) {
      callback(false, 'You must select a compressed vcf file (.vcf.gz), not a vcf file');
      return;
    }

    var fileType0 = /([^.]*)\.(vcf\.gz(\.tbi)?)$/.exec(event.target.files[0].name);
    var fileType1 = /([^.]*)\.(vcf\.gz(\.tbi)?)$/.exec(event.target.files[1].name);

    var fileExt0 = fileType0 && fileType0.length > 1 ? fileType0[2] : null;
    var fileExt1 = fileType1 && fileType1.length > 1 ? fileType1[2] : null;

    var rootFileName0 = fileType0 && fileType0.length > 1 ? fileType0[1] : null;
    var rootFileName1 = fileType1 && fileType1.length > 1 ? fileType1[1] : null;


    if (fileType0 == null || fileType0.length < 3 || fileType1 == null || fileType1.length <  3) {
      callback(false, 'You must select BOTH  a compressed vcf file (.vcf.gz) and an index (.tbi)  file');
      return;
    }


    if (fileExt0 == 'vcf.gz' && fileExt1 == 'vcf.gz.tbi') {
      if (rootFileName0 != rootFileName1) {
        callback(false, 'The index (.tbi) file must be named ' +  rootFileName0 + ".tbi");
        return;
      } else {
        vcfFile   = event.target.files[0];
        tabixFile = event.target.files[1];
      }
    } else if (fileExt1 == 'vcf.gz' && fileExt0 == 'vcf.gz.tbi') {
      if (rootFileName0 != rootFileName1) {
        callback(false, 'The index (.tbi) file must be named ' +  rootFileName1 + ".tbi");
        return;
      } else {
        vcfFile   = event.target.files[1];
        tabixFile = event.target.files[0];
      }
    } else {
      callback(false, 'You must select BOTH  a compressed vcf file (.vcf.gz) and an index (.tbi)  file');
      return;
    }

    callback(true);
    return;

  }


  function showFileFormatMessage() {
    alertify.set(
      {
        labels: {
          cancel     : "Show me how",
          ok         : "OK",
        },
        buttonFocus:  "cancel"
    });

    alertify.confirm("You must select a compressed vcf file and its corresponding index file in order to run this app. ",
        function (e) {
        if (e) {
            return;
        } else {
            window.location = 'http://IOBIO.io/2015/09/03/install-run-tabix/';
        }
     });
  }

  function endsWith(str, suffix) {
    return str.indexOf(suffix, str.length - suffix.length) !== -1;
  }

  exports.setSamples = function(sampleNames) {
    samples = sampleNames;
  }
  exports.getSamples = function() {
    return samples;
  }
  exports.getVcfFile = function() {
    return vcfFile;
  }

  exports.setVcfFile = function(file) {
    vcfFile = file;
  }

  exports.getVcfURL = function() {
    return vcfURL;
  }

  exports.setVcfURL = function(url) {
    vcfURL = url;
  }

  exports.getSourceType = function() {
    return sourceType;
  }

  exports.setSourceType = function(st) {
    sourceType = st;
  }



  function endsWith(str, suffix) {
    return str.indexOf(suffix, str.length - suffix.length) !== -1;
  }


  exports.stripChr = function(ref) {
    if (ref.indexOf("chr") == 0) {
      return ref.split("chr")[1];
    } else {
      return ref;
    }
  }


  exports.isNumeric = function(n) {
    return !isNaN(parseFloat(n)) && isFinite(n);
  }



  exports.getReferenceLengths = function(callback) {
    if (sourceType.toLowerCase() == SOURCE_TYPE_URL.toLowerCase()) {
      this._getRemoteReferenceLengths(callback);
    } else {
      this._getLocalReferenceLengths(callback);
    }
  }




  exports._getLocalReferenceLengths = function(callback, callbackError) {
    var me = this;
 
    vcfReader = new readBinaryVCF(tabixFile, vcfFile, function(tbiR) {
      var tbiIdx = tbiR;
      refDensity.length = 0;

      if (tbiIdx.idxContent.head.n_ref == 0) {
        var errorMsg = "Invalid index file.  The number of references is set to zero.  Try recompressing the vcf with bgzip and regenerating the index with tabix."
        if (callbackError) {
          callbackError(errorMsg);
        }
        console.log(errorMsg);
        return;
      }

      var referenceNames = [];
      for (var i = 0; i < tbiIdx.idxContent.head.n_ref; i++) {
        var ref   = tbiIdx.idxContent.head.names[i];
        referenceNames.push(ref);
      }      

      for (var i = 0; i < referenceNames.length; i++) {
        var ref   = referenceNames[i];

        var indexseq = tbiIdx.idxContent.indexseq[i];
        var calcRefLength = indexseq.n_intv * size16kb;


        // Load the reference density data.  Exclude reference if 0 points.
        refData.push( {"name": ref, "calcRefLength": calcRefLength, "idx": i});
      }

      // Sort ref data so that refs are ordered numerically
      refData = me.sortRefData(refData);
    
      if (callback) {
        callback(refData);
      }
    
    });

  }


  exports._getRemoteReferenceLengths = function(callback, callbackError) {
    var me = this;
    var buffer = "";
    var refName;

    var cmd = new iobio.cmd(
        IOBIO.vcfReadDepther,
        ['-i', vcfURL + '.tbi'],
        {ssl: useSSL}
    );

    cmd.on('data', function(data) {

      if (data == undefined) {
        return;
      }

      buffer += data;

    })

    // All data has been streamed.
    cmd.on('end', function() {


      var recs = buffer.split("\n");
      if (recs.length > 0) {
        for (var i=0; i < recs.length; i++)  {
          if (recs[i] == undefined) {
            return;
          }

          var success = true;
          if ( recs[i][0] == '#' ) {
            var tokens = recs[i].substr(1).split("\t");
            if (tokens.length >= 3) {
              var refNamePrev = refName;
              refIndex = tokens[0];
              refName = tokens[1];
              var refLength = tokens[2];

              // Zero fill the previous reference point data and callback with the
              // data we have loaded so far.
              if (refData.length > 0) {
                var refDataPrev = refData[refData.length - 1];
              }

              refData.push({"name": refName,  "calcRefLength": +refLength, "idx": +refIndex});


            } else {
                success = false;
            }
          }
          else {
            // We only care about getting the reference lengths, not the density data
          }
          if (success) {
            buffer = "";
          } else {
            buffer += recs[i];
          }
        }
      } else  {
        buffer += data;
      }

      // sort refData so references or ordered numerically
      refData = me.sortRefData(refData);


      // Zero fill the previous reference point data and callback with the
      // for the last reference that was loaded
      if (refData.length > 0) {
        var refDataPrev = refData[refData.length - 1];
      }
      if (callback) {
        callback(refData);
      }
    })

    // Catch error event when fired
    cmd.on('error', function(error) {
      console.log("Error occurred in loadRemoteIndex. " +  error);
      if (callbackError) {
        callbackError("Error occurred in loadRemoteIndex. " +  error);
      }
    })

    // execute command
    cmd.run();




  };



  exports.sortRefData = function(refData) {
    var me = this;
    return refData.sort(function(refa,refb) {
          var x = me.stripChr(refa.name);
          var y = me.stripChr(refb.name);
          if (me.isNumeric(x) && me.isNumeric(y)) {
            return ((+x < +y) ? -1 : ((+x > +y) ? 1 : 0));
          } else {
             if (!me.isNumeric(x) && !me.isNumeric(y)) {
                return ((+x < +y) ? -1 : ((+x > +y) ? 1 : 0));
             } else if (!me.isNumeric(x)) {
                return 1;
             } else {
                return -1;
             }
          }

      });
  }


  exports.promiseGetVariants = function(refName, geneObject, selectedTranscript, sampleName, annotationEngine, isRefSeq, hgvsNotation, getRsId, cache) {
    var me = this;


    return new Promise( function(resolve, reject) {

      if (sourceType == SOURCE_TYPE_URL) {
        me._getRemoteVariantsImpl(refName, geneObject, selectedTranscript, sampleName, annotationEngine, isRefSeq, hgvsNotation, getRsId, cache,
          function(annotatedData, data) {
            if (annotatedData && data) {
              resolve([annotatedData, data]);
            } else {
              reject();
            }
          });
      } else {
        //me._getLocalStats(refName, geneObject.start, geneObject.end, sampleName);

        me._getLocalVariantsImpl(refName, geneObject, selectedTranscript, sampleName, annotationEngine, isRefSeq, hgvsNotation, getRsId, cache,
          function(annotatedData, data) {
            if (annotatedData && data) {
              resolve([annotatedData, data]);
            } else {
              reject();
            }
          });

      }

    });
  }


  exports._getLocalVariantsImpl = function(refName, geneObject, selectedTranscript, sampleName, annotationEngine, isRefSeq, hgvsNotation, getRsId, cache, callback, errorCallback) {
    var me = this;

    // The variant region may span more than the specified region.
    // We will be keeping track of variant depth by relative position
    // of the region start, so to prevent a negative index, we will
    // keep track of the region start based on the variants.
    var variantRegionStart = geneObject.start;

    var vcfObjects = [];
    vcfObjects.length = 0;

    var headerRecords = [];
    vcfReader.getHeader( function(header) {
       headerRecords = header.split("\n");

    });

    // Get the vcf records for this region
    vcfReader.getRecords(refName, geneObject.start, geneObject.end, function(records) {

        var allRecs = headerRecords.concat(records);


        me.promiseAnnotateVcfRecords(allRecs, refName, geneObject, selectedTranscript, sampleName, annotationEngine, isRefSeq, hgvsNotation, getRsId)
        .then( function(data) {
            callback(data[0], data[1]);
        }, function(error) {
          console.log("_getLocalVariantsImpl() error - " + error);
          if (errorCallback) {
            errorCallback("_getLocalVariantsImpl() error - " + error);
          }
        });



    });



  }

  exports._getCacheKey = function(service, refName, geneObject, sampleName, miscObject) {
    var key =  "backend.gene.iobio"  
      + "-" + cacheHelper.launchTimestamp 
      + "-" + (vcfURL ? vcfURL : (vcfFile ? vcfFile.name : ""))
      + "-" + service
      + "-" + refName
      + "-" + geneObject.start.toString()
      + "-" + geneObject.end.toString()
      + "-" + geneObject.strand
      + "-" + sampleName;

    if (miscObject) {
      for (miscKey in miscObject) {
        key += "-" + miscKey + "=" + miscObject[miscKey];
      }
    }
    return key;
  }



  exports._getRemoteVariantsImpl = function(refName, geneObject, selectedTranscript, sampleName, annotationEngine, isRefSeq, hgvsNotation, getRsId, cache, callback, errorCallback) {

    var me = this;

    // Figure out the file location of the reference seq files
    var regionParm = ' ' + refName + ":" + geneObject.start + "-" + geneObject.end;
    var refFastaFile = genomeBuildHelper.getFastaPath(refName);


    var contigStr = "";
    getHumanRefNames(refName).split(" ").forEach(function(ref) {
        contigStr += "##contig=<ID=" + ref + ">\n";
    })
    var contigNameFile = new Blob([contigStr])

    // Create an iobio command get get the variants and add any header recs.
    var cmd = new iobio.cmd(IOBIO.tabix,['-h', vcfURL, regionParm], {ssl: useSSL})
      .pipe(IOBIO.bcftools, ['annotate', '-h', contigNameFile, '-'], {ssl: useSSL})

    // filter sample(s)
    if (sampleName != null && sampleName != "") {

      var sampleNameFile = new Blob([sampleName.split(",").join("\n")])
      cmd = cmd.pipe(IOBIO.vt, ["subset", "-s", sampleNameFile, '-'], {ssl: useSSL})
    }

    // normalize variants
    cmd = cmd.pipe(IOBIO.vt, ["normalize", "-n", "-r", refFastaFile, '-'], {ssl: useSSL})

    // get allele frequencies from 1000G and ExAC
    cmd = cmd.pipe(IOBIO.af, ["-b", genomeBuildHelper.getCurrentBuildName()], {ssl: useSSL});

    // Skip snpEff if RefSeq transcript set or we are just annotating with the vep engine
    if (isRefSeq || annotationEngine == 'vep') {
    } else {
      cmd = cmd.pipe(IOBIO.snpEff, [], {ssl: useSSL});
    }

    // VEP
    var vepArgs = [];
    vepArgs.push(" --assembly");
    vepArgs.push(genomeBuildHelper.getCurrentBuildName());
    vepArgs.push(" --format vcf");
    if (isRefSeq) {
      vepArgs.push("--refseq");
    }
    // Get the hgvs notation and the rsid since we won't be able to easily get it one demand
    // since we won't have the original vcf records as input
    if (hgvsNotation) {
      vepArgs.push("--hgvs");
    }
    if (getRsId) {
      vepArgs.push("--check_existing");
    }
    if (hgvsNotation || getRsId) {
      vepArgs.push("--fasta");
      vepArgs.push(refFastaFile);
    }

    //
    //  SERVER SIDE CACHING
    //
    var cacheKey = null;
    var urlParameters = {};
    if (cache) {
        cacheKey = me._getCacheKey(annotationEngine, refName, geneObject, sampleName, {refseq: isRefSeq, hgvs: hgvsNotation, rsid: getRsId});
        console.log(cacheKey);
        urlParameters.cache = cacheKey;
        urlParameters.partialCache = true;
        cmd = cmd.pipe("nv-dev-new.iobio.io/vep/", vepArgs, {ssl: useSSL, urlparams: urlParameters});
    } else {
        cmd = cmd.pipe(IOBIO.vep, vepArgs, {ssl: useSSL, urlparams: urlParameters});
    }




    var annotatedData = "";
    // Get the results from the iobio command
    cmd.on('data', function(data) {
         if (data == undefined) {
            return;
         }
         annotatedData += data;
    });

    // We have all of the annotated vcf recs.  Now parse them into vcf objects
    cmd.on('end', function(data) {
      var annotatedRecs = annotatedData.split("\n");
      var vcfObjects = [];
      var contigHdrRecFound = false;
      var vepFields = {};

      annotatedRecs.forEach(function(record) {
        if (record.charAt(0) == "#") {
          // Figure out how the vep fields positions
          if (record.indexOf("INFO=<ID=CSQ") > 0) {
            vepFields = me.parseHeaderFieldForVep(record);
          }
        } else {

          // Parse the vcf record into its fields
          var fields = record.split('\t');
          var pos    = fields[1];
          var id     = fields[2];
          var ref    = fields[3];
          var alt    = fields[4];
          var qual   = fields[5];
          var filter = fields[6];
          var info   = fields[7];
          var format = fields[8];
          var genotypes = [];
          for (var i = 9; i < fields.length; i++) {
            genotypes.push(fields[i]);
          }

          // Turn vcf record into a JSON object and add it to an array
          var vcfObject = {'pos': pos, 'id': 'id', 'ref': ref, 'alt': alt,
                           'qual': qual, 'filter': filter, 'info': info, 'format':format, 'genotypes': genotypes};
          vcfObjects.push(vcfObject);
        }
      });

      // Parse the vcf object into a variant object that is visualized by the client.
      var results = me.parseVcfRecords(vcfObjects, refName, geneObject, selectedTranscript, vepFields);


      callback(annotatedRecs, results);
    });

    cmd.on('error', function(error) {
       console.log(error);
    });

    cmd.run();

  }

  exports.parseHeaderFieldForVep = function(record) {
    var vepFields = {};
    var tokens = record.split("Format: ");
    if (tokens.length == 2) {
      var format = tokens[1];
      var fields = format.split("|");
      for(var idx = 0; idx < fields.length; idx++) {
        var fieldName = fields[idx];
        if (fieldName.indexOf("\"") == fieldName.length-1) {
          fieldName = fieldName.trim("\"");
        }
        vepFields[fieldName] = idx;
      }
    }
    return vepFields;
  }


  exports.getSampleNames = function(callback) {
    if (sourceType == SOURCE_TYPE_URL) {
      this._getRemoteSampleNames(callback);
    } else {
      this._getLocalSampleNames(callback);
    }
  }


  exports._getLocalSampleNames = function(callback) {
    var me = this;

    var vcfReader = new readBinaryVCF(tabixFile, vcfFile, function(tbiR) {
      var sampleNames = [];
      sampleNames.length = 0;

      var headerRecords = [];
      vcfReader.getHeader( function(header) {
         headerRecords = header.split("\n");
         headerRecords.forEach(function(headerRec) {
            if (headerRec.indexOf("#CHROM") == 0) {
              var headerFields = headerRec.split("\t");
              sampleNames = headerFields.slice(9);
              callback(sampleNames);
            }
         });

      });
   });

  }


  exports._getRemoteSampleNames = function(callback) {
    var me = this;

    var cmd = new iobio.cmd(
        IOBIO.tabix,
        ['-h', vcfURL, '1:1-1'],
        {ssl: useSSL});


    var headerData = "";
    // Use Results
    cmd.on('data', function(data) {
         if (data == undefined) {
            return;
         }
         headerData += data;
    });

    cmd.on('end', function(data) {
        var headerRecords = headerData.split("\n");
         headerRecords.forEach(function(headerRec) {
              if (headerRec.indexOf("#CHROM") == 0) {
                var headerFields = headerRec.split("\t");
                var sampleNames = headerFields.slice(9);
                callback(sampleNames);
              }
         });

    });

    cmd.on('error', function(error) {
      console.log(error);
    });

    cmd.run();

  }



  exports._getVariantCount = function(ref, start, end, sampleName, callback) {
    var me = this;

    var region = ref + ":" + start + "-" + end;

    var contigStr = "";
    getHumanRefNames(refName).split(" ").forEach(function(ref) {
        contigStr += "##contig=<ID=" + ref + ">\n";
    })
    var contigNameFile = new Blob([contigStr])

    var cmd = new iobio.cmd(IOBIO.tabix, ['-h', vcfURL, region], {ssl: useSSL});

    cmd  = cmd.pipe(IOBIO.bcftools, ['annotate', '-h', contigNameFile, '-'], {ssl: useSSL})

    if (sampleName != null && sampleName != "") {
      var sampleNameFile = new Blob([sampleName.split(",").join("\n")])
      cmd = cmd.pipe(IOBIO.vt, ['subset', '-s', sampleNameFile, '-'], {ssl: useSSL});
    }

    cmd = cmd.pipe(IOBIO.bcftools, ['stats'], {ssl: useSSL});


    var statsData = "";
    // Use Results
    cmd.on('data', function(data) {
         if (data == undefined) {
            return;
         }
         statsData += data;
    });

    cmd.on('end', function(data) {

        var recs = statsData.split("\n");
        var startIdx = null;
        for(var i =0; i < recs.length && startIdx == null; i++) {
          if (recs[i].indexOf("# SN, Summary numbers:") == 0) {
            startIdx = i + 2;
          }
        };

        var numRecs = null;
        if (startIdx) {
          for (var i = startIdx; i < recs.length && numRecs == null; i++) {
            var fields = recs[i].split("\t");
            if (fields.length > 3 && fields[2] == 'number of records:') {
              numRecs = +fields[3];
            }
          }
        }
        if (callback) {
          callback(numRecs);
        }

    });

    cmd.on('error', function(error) {
      console.log(error);
    });

    cmd.run();

  }

  exports.promiseParseVcfRecords = function(annotatedRecs, refName, geneObject, selectedTranscript, sampleIndex) {
    var me = this;

    return new Promise( function(resolve, reject) {
      // For each vcf records, call snpEff to get the annotations.
      // Each vcf record returned will have an EFF field in the
      // info field.
      var vcfObjects = [];
      var vepFields = {};

      annotatedRecs.forEach(function(record) {
        if (record == null || record == "") {

        } else if (record.charAt(0) == "#") {
          // Figure out how the vep fields positions
          if (record.indexOf("INFO=<ID=CSQ") > 0) {
            vepFields = me.parseHeaderFieldForVep(record);
          }
        } else {

          // Parse the vcf record into its fields
          var fields = record.split('\t');
          var pos    = fields[1];
          var id     = fields[2];
          var ref    = fields[3];
          var alt    = fields[4];
          var qual   = fields[5];
          var filter = fields[6];
          var info   = fields[7];
          var format = fields[8];
          var genotypes = [];
          for (var i = 9; i < fields.length; i++) {
            genotypes.push(fields[i]);
          }


          // Turn vcf record into a JSON object and add it to an array
          var vcfObject = {'pos': pos, 'id': 'id', 'ref': ref, 'alt': alt,
                           'qual': qual, 'filter': filter, 'info': info, 'format': format, 'genotypes': genotypes};
          vcfObjects.push(vcfObject);
        }
      });


      // Parse the vcf object into a variant object that is visualized by the client.
      var results = me.parseVcfRecords(vcfObjects, refName, geneObject, selectedTranscript, vepFields, sampleIndex);
      resolve([annotatedRecs, results]);

    });
  }

  exports.promiseAnnotateVcfRecords = function(records, refName, geneObject, selectedTranscript, sampleName, annotationEngine, isRefSeq, hgvsNotation, getRsId) {
    var me = this;

    return new Promise( function(resolve, reject) {
      // For each vcf records, call snpEff to get the annotations.
      // Each vcf record returned will have an EFF field in the
      // info field.
      me._annotateVcfRegion(records, refName, sampleName, annotationEngine, isRefSeq, hgvsNotation, getRsId, function(annotatedData) {

        var annotatedRecs = annotatedData.split("\n");
        var vcfObjects = [];
        var vepFields = {};

        annotatedRecs.forEach(function(record) {
          if (record.charAt(0) == "#") {
            // Figure out how the vep fields positions
            if (record.indexOf("INFO=<ID=CSQ") > 0) {
              vepFields = me.parseHeaderFieldForVep(record);
            }
          } else {

            // Parse the vcf record into its fields
            var fields = record.split('\t');
            var pos    = fields[1];
            var id     = fields[2];
            var ref    = fields[3];
            var alt    = fields[4];
            var qual   = fields[5];
            var filter = fields[6];
            var info   = fields[7];
            var format = fields[8];
            var genotypes = [];
            for (var i = 9; i < fields.length; i++) {
              genotypes.push(fields[i]);
            }


            // Turn vcf record into a JSON object and add it to an array
            var vcfObject = {'pos': pos, 'id': 'id', 'ref': ref, 'alt': alt,
                             'qual': qual, 'filter': filter, 'info': info, 'format': format, 'genotypes': genotypes};
            vcfObjects.push(vcfObject);
          }
        });

        // Parse the vcf object into a variant object that is visualized by the client.
        var results = me.parseVcfRecords(vcfObjects, refName, geneObject, selectedTranscript, vepFields);
        resolve([annotatedRecs, results]);
      });
    });
  }

  exports.promiseGetClinvarRecords = function(theVcfData, refName, geneObject, clinvarLoadVariantsFunction) {
    var me = this;

    return new Promise( function(resolve, reject) {
      var batchSize = 100;
      // For every 100 variants, make an http request to eutils to get clinvar records.  Keep
      // repeating until all variants have been processed.
      var numberOfBatches = Math.ceil(theVcfData.features.length / batchSize);
      if (numberOfBatches == 0) {
        numberOfBatches = 1;
      }
      var clinvarPromises = [];
      for( var i = 0; i < numberOfBatches; i++) {
        var start = i * batchSize;
        var end = start + batchSize;
        var batchOfVariants = theVcfData.features.slice(start, end <= theVcfData.features.length ? end : theVcfData.features.length);
        //var isLastBatch = (i == numberOfBatches - 1 ? true : false);

        if (isClinvarOffline) {
          var promise = me.promiseGetClinvarRecordsOffline(batchOfVariants, refName, geneObject, clinvarLoadVariantsFunction)
          .then(  function() {

          }, function(error) {
            reject("Unable to get clinvar annotations for variants");
          });
          clinvarPromises.push(promise);

        } else {
          var promise = me.promiseGetClinvarRecordsImpl(batchOfVariants, refName, geneObject, clinvarLoadVariantsFunction)
          .then(  function() {

          }, function(error) {
            reject("Unable to get clinvar annotations for variants");
          });
          clinvarPromises.push(promise);

        }
      }

      Promise.all(clinvarPromises).then(function() {
        resolve(theVcfData);
      });



    });
  }

  // When there is no internet, read the clinvar vcf to obtain clinvar annotations
  exports.promiseGetClinvarRecordsOffline= function(variants, refName, geneObject, clinvarLoadVariantsFunction) {
    var me = this;

    return new Promise( function(resolve, reject) {
      var regionStart = geneObject.start;
      var regionEnd = geneObject.end;


      var clinvarUrl = null;
      if (isOffline) {
        clinvarUrl = OFFLINE_CLINVAR_VCF_BASE_URL + genomeBuildHelper.getBuildResource(genomeBuildHelper.RESOURCE_CLINVAR_VCF_OFFLINE)
      } else {
        clinvarUrl = genomeBuildHelper.getBuildResource(genomeBuildHelper.RESOURCE_CLINVAR_VCF_S3);
      }


      var regionParm = ' ' + refName + ":" + regionStart + "-" + regionEnd;
      var cmd = new iobio.cmd(IOBIO.tabix, ['-h', clinvarUrl, regionParm], {ssl: useSSL});


      var clinvarData = "";
      // Parse results
      cmd.on('data', function(data) {
        if (data == undefined) {
            return;
        }
        clinvarData += data;
      });

      cmd.on('end', function(data) {
        var clinvarRecs = clinvarData.split("\n");
        var vcfObjects = [];

        clinvarRecs.forEach(function(record) {
          if (record.charAt(0) == "#") {

          } else {

            // Parse the vcf record into its fields
            var fields = record.split('\t');
            var pos    = fields[1];
            var id     = fields[2];
            var ref    = fields[3];
            var alt    = fields[4];
            var qual   = fields[5];
            var filter = fields[6];
            var info   = fields[7];
            var format = fields[8];
            var genotypes = [];
            for (var i = 9; i < fields.length; i++) {
              genotypes.push(fields[i]);
            }

            // Turn vcf record into a JSON object and add it to an array
            var vcfObject = {'pos': pos, 'id': 'id', 'ref': ref, 'alt': alt,
                             'qual': qual, 'filter': filter, 'info': info, 'format':format, 'genotypes': genotypes};
            vcfObjects.push(vcfObject);
          }
        });


        clinvarLoadVariantsFunction(vcfObjects);

        resolve();

      });

      cmd.on('error', function(error) {
        console.log(error);
      });

      cmd.run();
    });

  }


  exports.promiseGetClinvarRecordsImpl = function(variants, refName, geneObject, clinvarLoadVariantsFunction) {
    var me = this;

    return new Promise( function(resolve, reject) {

      var regionStart = geneObject.start;
      var regionEnd = geneObject.end;


      // Multiallelic input vcf records were assigned a number submission
      // index.  Create a map that ties the vcf record number to the
      // clinvar records number
      var sourceIndex = -1;
      var clinvarIndex = 0;
      var url = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=clinvar&usehistory=y&retmode=json&term=";
      url += "(" + refName + "[Chromosome]" + " AND ";
      // clinvarToSourceMap = new Object();
      variants.forEach(function(variant) {

        var pos    = variant.start;
        var ref    = variant.ref;
        var alt    = variant.alt;

        if (pos == null || ref == null || alt == null) {

        } else {
          // Get rid of the left most anchor base for insertions and
          // deletions for accessing clinvar
          var clinvarStart = +pos;
          if (alt == '.') {

          } else if (ref == '.') {

          } else if (ref.length > alt.length) {
            // deletion
            clinvarStart++;
          } else if (alt.length > ref.length) {
            // insertion
            clinvarStart++;
          }

          url += clinvarStart + ','
        }
      });

      var clinvarBuild = genomeBuildHelper.getBuildResource(genomeBuildHelper.RESOURCE_CLINVAR_POSITION);
      url = url.slice(0,url.length-1) + '[' + clinvarBuild + '])';

      var clinvarVariants = null;
      var requestClinvarSummaryTries = 0;
      requestClinvarSummary(url);

      function requestClinvarSummary(url) {
        $.ajax( url )
          .done(function(data) {
            if (data["esearchresult"]["ERROR"] != undefined) {
              if (requestClinvarSummaryTries < 2 ) {
                requestClinvarSummaryTries += 1;
                console.log('clinvar request failed ' + requestClinvarSummaryTries + ' times (' + data["esearchresult"]["ERROR"] + '). Trying again ...')
                requestClinvarSummary(url);
              } else {
                console.log('clinvar request failed 3 times (' + data.esearchresult.ERROR + '). Aborting ...')
              }
            } else {
              var webenv = data["esearchresult"]["webenv"];
              var queryKey = data["esearchresult"]["querykey"];
              var summaryUrl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=clinvar&query_key=" + queryKey + "&retmode=json&WebEnv=" + webenv + "&usehistory=y"
              $.ajax( summaryUrl )
                .done(function(sumData) {

                  if (sumData.result == null) {
                    if (sumData.esummaryresult && sumData.esummaryresult.length > 0) {
                      sumData.esummaryresult.forEach( function(message) {
                      });
                    }
                    sumData.result = {uids: []};
                    clinvarLoadVariantsFunction(sumData.result);
                    resolve();
                  } else {
                    var sorted = sumData.result.uids.sort(function(a,b){
                      var aStart = parseInt(sumData.result[a].variation_set[0].variation_loc.filter(function(v){return v["assembly_name"] == genomeBuildHelper.getCurrentBuildName()})[0].start);
                      var bStart = parseInt(sumData.result[b].variation_set[0].variation_loc.filter(function(v){return v["assembly_name"] == genomeBuildHelper.getCurrentBuildName()})[0].start);
                      if ( aStart > bStart)
                        return 1;
                      else
                        return -1;
                    })
                    sumData.result.uids = sorted;
                    if (clinvarLoadVariantsFunction) {
                      clinvarLoadVariantsFunction(sumData.result);
                    }
                    resolve();
                  }
                })
                .fail(function() {
                  console.log('Error: clinvar http request failed to get summary data');
                  reject('Error: clinvar http request failed to get summary data');
                })
            }
          })
          .fail(function() {
            console.log('Error: clinvar http request failed to get IDs');
            reject('Error: clinvar http request failed to get IDs');

          })
        }
      });

  }



  exports._annotateVcfRegion = function(records, refName, sampleName, annotationEngine, isRefSeq, hgvsNotation, getRsId, callback, callbackClinvar) {
    var me = this;

    //  Figure out the reference sequence file path
    var refFastaFile = genomeBuildHelper.getFastaPath(refName);


    var writeStream = function(stream) {
      records.forEach( function(record) {
        if (record.trim() == "") {
        } else {
          stream.write(record + "\n");
        }
      });

      stream.end();
    }

    //  Streamed vcf recs first go through contig appender to add mandatory header recs
    var contigStr = "";
    getHumanRefNames(refName).split(" ").forEach(function(ref) {
        contigStr += "##contig=<ID=" + ref + ">\n";
    })
    var contigNameFile = new Blob([contigStr])

    var cmd = new iobio.cmd(IOBIO.bcftools, ['annotate', '-h', contigNameFile, writeStream ], {ssl: useSSL})

    // Filter samples
    if (sampleName != null && sampleName != "") {
      var sampleNameFile = new Blob([sampleName.split(",").join("\n")])
      cmd = cmd.pipe(IOBIO.vt, ['subset', '-s', sampleNameFile, '-'], {ssl: useSSL});
    }

    // Normalize the variants (e.g. AAA->AAG becomes A->AG)
    cmd = cmd.pipe(IOBIO.vt, ['normalize', '-n', '-r', refFastaFile, '-'], {ssl: useSSL})

    // Get Allele Frequencies from 1000G and ExAC
    cmd = cmd.pipe(IOBIO.af, ["-b", genomeBuildHelper.getCurrentBuildName()], {ssl: useSSL})

    // Bypass snpEff if the transcript set is RefSeq or the annotation engine is VEP
    if (annotationEngine == 'vep' || isRefSeq) {
    } else {
      cmd = cmd.pipe(IOBIO.snpEff, [], {ssl: useSSL});
    }

    // VEP
    var vepArgs = [];
    vepArgs.push(" --assembly");
    vepArgs.push(genomeBuildHelper.getCurrentBuildName());
    vepArgs.push(" --format vcf");
    if (isRefSeq) {
      vepArgs.push("--refseq");
    }
    // Get the hgvs notation and the rsid since we won't be able to easily get it one demand
    // since we won't have the original vcf records as input
    if (hgvsNotation) {
      vepArgs.push("--hgvs");
    }
    if (getRsId) {
      vepArgs.push("--check_existing");
    }
    if (hgvsNotation || getRsId) {
      vepArgs.push("--fasta");
      vepArgs.push(refFastaFile);
    }
    cmd = cmd.pipe(IOBIO.vep, vepArgs, {ssl: useSSL});


    var buffer = "";
    // Get the results from the command
    cmd.on('data', function(data) {
         buffer = buffer + data;
    });

    cmd.on('end', function() {
         callback(buffer);
    });

    cmd.on('error', function(error) {
      console.log("error while annotating vcf records " + error);
    });

    // Run the iobio command
    cmd.run();

  }


  exports.parseVcfRecords = function(vcfRecs, refName, geneObject, selectedTranscript, vepFields, sampleIndex) {

      var me = this;
      var selectedTranscriptID = stripTranscriptPrefix(selectedTranscript.transcript_id);

      // Use the sample index to grab the right genotype column from the vcf record
      // If it isn't provided, assume that the first genotype column is the one
      // to be evaluated and parsed.
      if (sampleIndex == null) {
        sampleIndex = 0;
      }


      // The variant region may span more than the specified region.
      // We will be keeping track of variant depth by relative position
      // of the region start, so to prevent a negative index, we will
      // keep track of the region start based on the variants.
      var variantRegionStart = geneObject.start;

      var homCount = 0;
      var hetCount = 0;
      var sampleCount = -1;

      var variants = [];
      variants.length = 0;


      var appendTranscript = function(theObject, key, theTranscriptId) {
        var transcripts = theObject[key];
        if (transcripts == null) {
          transcripts = {};
        }
        transcripts[theTranscriptId] = theTranscriptId;
        theObject[key] = transcripts;
      }


      vcfRecs.forEach(function(rec) {
        if (rec.pos && rec.id) {
          var alts = [];
          if(rec.alt.indexOf(',') > -1) {
            // Done split apart multiple alt alleles for education edition
            if (isLevelEdu) {
              alts.push(rec.alt);
            } else {
              alts = rec.alt.split(",");
            }
          } else {
            alts.push(rec.alt);
          }
          var altIdx = 0;
          alts.forEach(function(alt) {
           var len = null;
            var type = null;
            var end = null;

            if (alt.indexOf("<") == 0 && alt.indexOf(">") > 0) {
              var annotTokens = rec.info.split(";");
              annotTokens.forEach(function(annotToken) {
                if (annotToken.indexOf("SVLEN=") == 0) {
                  len = Math.abs(+annotToken.substring(6, annotToken.length));
                } else if (annotToken.indexOf("SVTYPE=") == 0) {
                  type = annotToken.substring(7, annotToken.length);
                  //if (type && type.toLowerCase() == 'mnp') {
                  //  type = 'snp';
                  //}
                }
              });
              rec.ref = '';
              alt = '';
              end = +rec.pos + len;

            } else {
              len = alt.length;
              type = 'SNP';
              if (rec.ref == '.' || alt.length > rec.ref.length ) {
                type = 'INS';
                len = alt.length - rec.ref.length;
              } else if (rec.alt == '.' || alt.length < rec.ref.length) {
                type = 'DEL';
                len = rec.ref.length - alt.length;
              }
              end = +rec.pos + len;

            }

            // Determine the format of the genotype fields
            var gtTokens = {};
            var idx = 0;
            if (rec.format && rec.format != '') {
              var tokens = rec.format.split(":");
              tokens.forEach(function(token) {
                gtTokens[token] = idx;
                idx++;
              })
            }


            // svtype and snpEff annotations from the info field
            var effects = new Object();
            var impacts = new Object();
            var allSnpeff = new Object();
            var af = null;
            var typeAnnotated = null;
            var combinedDepth = null;
            var af1000G = '.';
            var afExAC = '.';
            var rs = null;
            var annotTokens = rec.info.split(";");

            // vep annotations from the info field
            //Allele|Consequence|IMPACT|SYMBOL|Gene|Feature_type|Feature
            // |BIOTYPE|EXON|INTRON|HGVSc|HGVSp|cDNA_position|CDS_position|
            // Protein_position|Amino_acids|Codons|
            // Existing_variation|DISTANCE|STRAND|SYMBOL_SOURCE|HGNC_ID|
            // SIFT|PolyPhen|HGVS_OFFSET|CLIN_SIG|SOMATIC|PHENO|
            // MOTIF_NAME|MOTIF_POS|HIGH_INF_POS|MOTIF_SCORE_CHANGE
            var allVep = new Object();
            var allSIFT = new Object();
            var allPolyphen = new Object();
            var vepConsequence = new Object();
            var vepImpact = new Object();
            var vepFeatureType = new Object();
            var vepFeature = new Object();
            var vepExon = new Object();
            var vepHGVSc = new Object();
            var vepHGVSp = new Object();
            var vepAminoAcids = new Object();
            var vepVariationIds = new Object();
            var vepSIFT = new Object();
            var vepPolyPhen = new Object();
            var sift = new Object();     // need a special field for filtering purposes
            var polyphen = new Object(); // need a special field for filtering purposes
            var regulatory = new Object(); // need a special field for filtering purposes

            var vepRegs = [];
            var vepRegBioTypeIndex = 7;
            var vepRegMotifNameIndex = 28;
            var vepRegMotifPosIndex = 29;
            var vepRegMotifHiInfIndex = 30;

            // Iterate through the annotation fields, looking for the
            // annotation EFF
            annotTokens.forEach(function(annotToken) {
              if (annotToken.indexOf("BGAF_1KG=") == 0) {
                af1000G = annotToken.substring(9, annotToken.length);
              } else if (annotToken.indexOf("BGAF_EXAC=") == 0) {
                afExAC = annotToken.substring(10, annotToken.length);
              } else if (annotToken.indexOf("RS=") == 0) {
                rs = annotToken.substring(3, annotToken.length);
              } else if (annotToken.indexOf("AF=") == 0) {
                // For now, just grab first af
                //af = me.parseAnnotForAlt(annotToken.substring(3, annotToken.length), altIdx);
                af = me.parseAnnotForAlt(annotToken.substring(3, annotToken.length), 0);
              } if (annotToken.indexOf("TYPE=") == 0) {
                typeAnnotated = me.parseAnnotForAlt(annotToken.substring(5, annotToken.length), altIdx);
              } if (annotToken.indexOf("DP=") == 0) {
                combinedDepth = annotToken.substring(3, annotToken.length);
              } else if (annotToken.indexOf("EFF=") == 0) {
                // We have found the EFF annotation. Now split
                // the EFF annotation into its parts.  Each
                // part represents the annotations for a given
                // transcript.
                annotToken = annotToken.substring(4, annotToken.length);
                var tokens = annotToken.split(",");
                var firstTime = true;
                tokens.forEach(function(token) {
                  // If we passed in an applicable transcript, grab the snpEff
                  // annotations pertaining to it.  Otherwise, just grab the
                  // first snpEff annotations listed.

                  //EFF= Effect ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_Length |
                  //              Gene_Name | Transcript_BioType | Gene_Coding | Transcript_ID | Exon_Rank  |
                  //              Genotype_Number [ | ERRORS | WARNINGS ] )

                  var stop = token.indexOf("(");
                  var theEffect = token.substring(0, stop);
                  var remaining = token.substring(stop+1,token.length);
                  var effectTokens = remaining.split("|");
                  var theImpact = effectTokens[0];
                  var theTranscriptId = effectTokens[8];


                  // Make sure that this annotation belongs to a transcript in the gene's transcript set.
                  var validTranscript = false;
                  geneObject.transcripts.forEach( function(transcript) {
                    if (transcript.transcript_id.indexOf(theTranscriptId) == 0) {
                      validTranscript = true;
                    }
                  });

                  if (validTranscript) {
                    // Determine if this is an annotation for the selected transcript
                    var parseForSelectedTranscript = false;
                    if (selectedTranscriptID && token.indexOf(selectedTranscriptID) > -1) {
                      parseForSelectedTranscript = true;
                    }


                    // Map all impact to effects so that we can determine
                    // the highest impact/effects for this variant, across
                    // ALL transcripts for this variant.
                    var effectsObject = allSnpeff[theImpact];
                    if (effectsObject == null) {
                      effectsObject = {};
                    }
                    appendTranscript(effectsObject, theEffect, theTranscriptId);
                    allSnpeff[theImpact] = effectsObject;

                    if (parseForSelectedTranscript) {
                      // Parse out the effect
                      effects[theEffect] = theEffect;

                      // Parse out the impact
                      impacts[theImpact] = theImpact;
                    }
                  } else {
                    //console.log(geneObject.gene_name + " " + theEffect + ": throwing out invalid transcript " + selectedTranscriptID)
                  }


                  firstTime = false;
                });
              } else if (annotToken.indexOf("CSQ") == 0) {
                // We have found the VEP annotation. Now split
                // the CSQ string into its parts.  Each
                // part represents the annotations for a given
                // transcript.
                annotToken = annotToken.substring(4, annotToken.length);
                var transcriptTokens = annotToken.split(",");
                //Allele|Consequence|IMPACT|SYMBOL|Gene|Feature_type|Feature|BIOTYPE|EXON|INTRON|HGVSc|GVSp
                //|cDNA_position|CDS_position|Protein_position|Amino_acids|Codons|Existing_variation
                //|DISTANCE|STRAND|SYMBOL_SOURCE|HGNC_ID|REFSEQ_MATCH|SIFT|PolyPhen|HGVS_OFFSET
                //|CLIN_SIG|SOMATIC|PHENO|MOTIF_NAME|MOTIF_POS|HIGH_INF_POS|MOTIF_SCORE_CHANGE
                transcriptTokens.forEach(function(transcriptToken) {
                    var vepTokens   = transcriptToken.split("|");
                    var feature     = vepTokens[vepFields.Feature];
                    var featureType = vepTokens[vepFields.Feature_type];

                    // If the transcript is the selected transcript, parse
                    // all of the vep fields.  We place these into maps
                    // because we can have multiple vep consequences for
                    // the same transcript.
                    // TODO:  Need to sort so that highest impact shows first
                    //        and is used for filtering and ranking purposes.
                    if (featureType == 'Transcript' && (feature == selectedTranscriptID || feature == selectedTranscript.transcript_id)) {
                      vepImpact[vepTokens[vepFields.IMPACT]] = vepTokens[vepFields.IMPACT];

                      var consequence = vepTokens[vepFields.Consequence];
                      consequence.split("&").forEach( function(token) {
                        vepConsequence[token] = token;
                      })

                      vepExon[vepTokens[vepFields.EXON]] = vepTokens[vepFields.EXON];
                      vepHGVSc[vepTokens[vepFields.HGVSc]] = vepTokens[vepFields.HGVSc];
                      vepHGVSp[vepTokens[vepFields.HGVSp]] = vepTokens[vepFields.HGVSp];
                      vepAminoAcids[vepTokens[vepFields.Amino_acids]] = vepTokens[vepFields.Amino_acids];
                      vepVariationIds[vepTokens[vepFields.Existing_variation]] = vepTokens[vepFields.Existing_variation];

                      var siftString = vepTokens[vepFields.SIFT];
                      var siftDisplay = siftString != null && siftString != "" ? siftString.split("(")[0] : "";
                      vepSIFT[siftDisplay] = siftDisplay;
                      sift['sift_'+ siftDisplay] = 'sift_' + siftDisplay;

                      var polyphenString = vepTokens[vepFields.PolyPhen];
                      var polyphenDisplay = polyphenString != null && polyphenString != "" ? polyphenString.split("(")[0] : "";
                      vepPolyPhen[polyphenDisplay] = polyphenDisplay;
                      polyphen['polyphen_' + polyphenDisplay] = 'polyphen_' + polyphenDisplay;

                    } else if (featureType == 'RegulatoryFeature' || featureType == 'MotifFeature' ) {
                      vepRegs.push( {
                        'impact' :  vepTokens[vepFields.IMPACT],
                        'consequence' : vepTokens[vepFields.Consequence],
                        'biotype': vepTokens[vepFields.BIOTYPE],
                        'motifName' : vepTokens[vepFields.MOTIF_NAME],
                        'motifPos'  : vepTokens[vepFields.MOTIF_POS],
                        'motifHiInf' : vepTokens[vepFields.HIGH_INF_POS]
                      });
                      var reg = vepTokens[vepFields.Consequence] == 'regulatory_region_variant' ? vepTokens[vepFields.BIOTYPE] : vepTokens[vepFields.Consequence];
                      var regKey = reg;
                      if (reg == "promoter") {
                        regKey = "the_promoter";
                      }

                      var valueUrl = "";
                      if (feature != "" && feature != null) {
                        var url = genomeBuildHelper.getBuildResource(genomeBuildHelper.RESOURCE_ENSEMBL_URL) + "Regulation/Context?db=core;fdb=funcgen;rf=" + feature;
                        valueUrl = '<a href="' + url + '" target="_reg">' + reg.split("_").join(" ").toLowerCase() + '</a>';
                      } else {
                        valueUrl = reg.split("_").join(" ").toLowerCase();
                      }
                      regulatory[(featureType == 'RegulatoryFeature' ? "reg_" : "mot_") + regKey.toLowerCase()] = valueUrl;
                    }
                    if (featureType == 'Transcript') {
                      var theTranscriptId = feature;

                      // Only keep annotations that are for transcripts that in the gene's list of known
                      // transcripts
                      var validTranscript = false;
                      geneObject.transcripts.forEach( function(transcript) {
                      if (transcript.transcript_id.indexOf(theTranscriptId) == 0) {
                        validTranscript = true;
                        }
                      });
                      if (validTranscript) {
                        // Keep track of all VEP impact and consequence so that we can determine the highest impact
                        // variant across all transcripts
                        var theImpact = vepTokens[vepFields.IMPACT];
                        var theConsequences = vepTokens[vepFields.Consequence];
                        var siftString = vepTokens[vepFields.SIFT];
                        var siftDisplay = siftString != null && siftString != "" ? siftString.split("(")[0] : "";
                        var siftScore = siftString != null && siftString != "" ? siftString.split("(")[1].split(")")[0] : 99;
                        var polyphenString = vepTokens[vepFields.PolyPhen];
                        var polyphenDisplay = polyphenString != null && polyphenString != "" ? polyphenString.split("(")[0] : "";
                        var polyphenScore = polyphenString != null && polyphenString != "" ? polyphenString.split("(")[1].split(")")[0] : -99;



                        var consequencesObject = allVep[theImpact];
                        if (consequencesObject == null) {
                          consequencesObject = {};
                        }
                        appendTranscript(consequencesObject, theConsequences, theTranscriptId);
                        allVep[theImpact] = consequencesObject;

                        var siftObject = allSIFT[siftScore];
                        if (siftObject == null) {
                          siftObject = {};
                        }
                        appendTranscript(siftObject, siftDisplay, theTranscriptId);
                        allSIFT[siftScore] = siftObject;

                        var polyphenObject = allPolyphen[polyphenScore];
                        if (polyphenObject == null) {
                          polyphenObject = {};
                        }
                        appendTranscript(polyphenObject, polyphenDisplay, theTranscriptId);
                        allPolyphen[polyphenScore] = polyphenObject;

                      } else {
                        var theConsequences = vepTokens[vepFields.Consequence];
                        //console.log(geneObject.gene_name + " " + theConsequences + ": throwing out invalid transcript " + theTranscriptId);
                      }


                    }

                });

              }

            });

            var effectCats = new Object();
            if ($.isEmptyObject(effects)) {
              effectCats['NOEFFECT'] = 'NOEFFECT';
            } else {
              var found = false;
              for (var y = 0; y < effectCategories.length; y++) {
                var cat = effectCategories[y];
                var eff = cat[0];
                var effCat = cat[1];

                if (effects[eff]) {
                  effectCats[effCat] = effCat;
                  found = true;
                }
              };
              if (!found) {
                effectCats['other'] = 'other';
              }

            }

            if ($.isEmptyObject(impacts)) {
              impacts["NOIMPACT"] = "NOIMPACT";
            }

            // Parse genotypes
            var genotypes = [];
            var genotypeDepths = [];
            var genotypeFilteredDepths = [];
            var genotypeAltCounts = [];
            var genotypeRefCounts = [];
            var genotypeAltForwardCounts = [];
            var genotypeAltReverseCounts = [];
            var genotypeRefForwardCounts = [];
            var genotypeRefReverseCounts = [];
            rec.genotypes.forEach(function(genotype) {
              if (genotype == ".") {

              } else {
                var tokens = genotype.split(":");
                gtFieldIndex = gtTokens["GT"];
                genotypes.push(tokens[gtFieldIndex]);

                gtDepthIndex = gtTokens["DP"];
                if (gtDepthIndex) {
                  genotypeFilteredDepths.push(tokens[gtDepthIndex]);
                } else {
                  genotypeFilteredDepths.push(null);
                }
                var gtAlleleCountIndex = gtTokens["AD"];
                var gtAltCountIndex = gtTokens["AO"];
                if (gtAlleleCountIndex) {
                  //
                  // GATK allele counts
                  //
                  var countTokens = tokens[gtAlleleCountIndex].split(",");
                  if (countTokens.length >= 2 ) {
                    var refAlleleCount = countTokens[0];
                    var altAlleleCounts = countTokens.slice(1).join(",");

                    var totalAllelicDepth = 0;
                    countTokens.forEach(function(allelicDepth) {
                      if (allelicDepth) {
                        totalAllelicDepth += +allelicDepth;
                      }
                    })

                    genotypeAltCounts.push(altAlleleCounts);
                    genotypeRefCounts.push(refAlleleCount);
                    genotypeDepths.push(totalAllelicDepth);
                  } else {
                    genotypeAltCounts.push(null);
                    genotypeRefCounts.push(null);
                    genotypeDepths.push(null);
                  }
                } else if (gtAltCountIndex) {
                  //
                  // Freebayes allele counts
                  //
                  var totalAllelicDepth = 0;

                  var altCount = tokens[gtAltCountIndex];
                  genotypeAltCounts.push(altCount);

                  var altCountTokens = altCount.split(",");
                  altCountTokens.forEach(function(allelicDepth) {
                    if (allelicDepth) {
                        totalAllelicDepth += +allelicDepth;
                    }
                  })

                  var refCount = 0;
                  var gtRefCountIndex = gtTokens["RO"];
                  if (gtRefCountIndex) {
                    refCount = tokens[gtRefCountIndex];
                    genotypeRefCounts.push(refCount);
                    totalAllelicDepth += +refCount;
                  } else {
                    genotypeRefCounts.push(null);
                  }

                  genotypeDepths.push(totalAllelicDepth);


                } else {
                  genotypeAltCounts.push(null);
                  genotypeRefCounts.push(null)
                }
                var strandAlleleCountIndex = gtTokens["SAC"]; // GATK
                var strandRefForwardIndex = gtTokens["SRF"]; // Freebayes
                var strandRefReverseIndex = gtTokens["SRR"]; // Freebayes
                var strandAltForwardIndex = gtTokens["SAF"]; // Freebayes
                var strandAltReverseIndex = gtTokens["SAR"]; // Freebayes
                if (strandAlleleCountIndex) {
                  //
                  // GATK Strand allele counts, comma separated
                  //
                  var countTokens = tokens[strandAlleleCountIndex].split(",");
                  if (countTokens.length == 4) {
                    genotypeRefForwardCounts.push(tokens[0]);
                    genotypeRefReverseCounts.push(tokens[1]);
                    genotypeAltForwardCounts.push(tokens[2]);
                    genotypeAltReverseCounts.push(tokens[3]);
                  } else {
                    genotypeRefForwardCounts.push(null);
                    genotypeRefReverseCounts.push(null);
                    genotypeAltForwardCounts.push(null);
                    genotypeAltReverseCounts.push(null);
                  }
                } else if (strandRefForwardIndex && strandRefReverseIndex && strandAltForwardIndex && strandAltReverseIndex ) {
                  //
                  // Freebayes Strand bias counts (SRF, SRR, SAF, SAR)
                  //
                  genotypeRefForwardCounts.push(tokens[strandRefForwardIndex]);
                  genotypeRefReverseCounts.push(tokens[strandRefReverseIndex]);
                  genotypeAltForwardCounts.push(tokens[strandAltForwardIndex]);
                  genotypeAltReverseCounts.push(tokens[strandAltReverseIndex]);
                } else {
                  genotypeRefForwardCounts.push(null);
                  genotypeRefReverseCounts.push(null);
                  genotypeAltForwardCounts.push(null);
                  genotypeAltReverseCounts.push(null);
                }
              }
            });

            var gtNumber = altIdx + 1;
            var genotypeForSample = null;
            var genotypeDepthForSample = null;
            var genotypeAltCountForSample = null;
            var genotypeRefCountForSample = null;
            var genotypeAltForwardCountForSample = null;
            var genotypeAltReverseCountForSample = null;
            var genotypeRefForwardCountForSample = null;
            var genotypeRefReverseCountForSample = null;
            var zygosity = null;
            var phased = null;


            // Only keep the alt if we have a genotype that matches.
            // For example
            // A->G    0|1 keep
            // A->G,C  0|1 keep A->G, but bypass A->C
            // A->G,C  0|2 bypass A->G, keep A->C
            // A->G,C  1|2 keep A->G, keep A->C
            var keepAlt = false;

            if (sampleCount == -1) {
              sampleCount = genotypes.length;
            }


            genotypeForSample = genotypes[sampleIndex];

            if (genotypeForSample == null) {
              keepAlt = true;
            } else {
              var delim = null;
              if (genotypeForSample.indexOf("|") > 0) {
                delim = "|";
                phased = true;
              } else {
                delim = "/";
                phased = false;
              }
              var tokens = genotypeForSample.split(delim);
              if (tokens.length == 2) {
                if (isLevelEdu && alt.indexOf(",") > 0) {
                  if ((tokens[0] == 1 ) && (tokens[1] == 2)) {
                    keepAlt = true;
                  } if (tokens[0] == tokens[1]) {
                    keepAlt = true;
                    theAltIdx = tokens[0] - 1;
                    alt = alt.split(',')[theAltIdx] + ',' + alt.split(',')[theAltIdx];
                  } else if (tokens[0] == 0 && tokens[1] != 0) {
                    var theAltIdx = +tokens[1] - 1;
                    alt = alt.split(',')[theAltIdx]
                  } else if (tokens[1] == 0 && tokens[0] != 0) {
                    var theAltIdx = +tokens[0] - 1;
                    alt = alt.split(',')[theAltIdx]
                  }
                  if (keepAlt) {
                    if (tokens[0] == tokens[1]) {
                      zygosity = "HOM";
                      homCount++;
                    } else {
                      zygosity = "HET";
                      hetCount++;
                    }
                  }

                } else if (tokens[0] == gtNumber || tokens[1] == gtNumber) {
                  keepAlt = true;
                  if (tokens[0] == tokens[1]) {
                    zygosity = "HOM";
                    homCount++;
                  } else {
                    zygosity = "HET";
                    hetCount++;
                  }
                } else if (tokens[0] == "0" && tokens[1] == "0" ) {
                  keepAlt = true;
                  zygosity = "HOMREF"
                }
              }
            }

            genotypeDepthForSample = genotypeDepths[sampleIndex];
            genotypeFilteredDepthForSample = genotypeFilteredDepths[sampleIndex];
            genotypeRefCountForSample = genotypeRefCounts[sampleIndex];
            genotypeRefForwardCountForSample = genotypeRefForwardCounts[sampleIndex];
            genotypeRefReverseCountForSample = genotypeRefReverseCounts[sampleIndex];

            genotypeAltCountForSample        = me.parseMultiAllelic(gtNumber-1, genotypeAltCounts[sampleIndex], ",");
            genotypeAltForwardCountForSample = genotypeAltForwardCounts[sampleIndex];
            genotypeAltReverseCountForSample = genotypeAltReverseCounts[sampleIndex];

            var eduGenotype = "";
            if (isLevelEdu) {
              if (alt.indexOf(",") > 0) {
                alt.split(",").forEach( function(altToken) {
                  if (eduGenotype.length > 0) {
                    eduGenotype += " ";
                  }
                  eduGenotype += altToken;
                });
              } else if (zygosity == "HET") {
                eduGenotype = rec.ref + " " + alt;
              } else if (zygosity == "HOM") {
                eduGenotype = alt + " " + alt;
              }
            }
            var eduGenotypeReversed = switchGenotype(eduGenotype);


            // Get rid of the left most anchor base for insertions and
            // deletions for accessing clinvar
            var clinvarStart = +rec.pos;
            var clinvarRef = rec.ref;
            var clinvarAlt = alt;
            if (clinvarAlt == '.') {
              clinvarAlt = '-';
            } else if (clinvarRef == '.') {
              clinvarRef = '-';
            } else if (clinvarRef.length > clinvarAlt.length) {
              // deletion
              clinvarStart++;
              clinvarAlt = clinvarAlt.length == 1 ? "-" : clinvarAlt.substr(1,clinvarAlt.length-1);
              clinvarRef = clinvarRef.substr(1,clinvarRef.length-1);
            } else if (clinvarAlt.length > clinvarRef.length) {
              // insertion
              clinvarStart++;
              clinvarRef = clinvarRef.length == 1 ? "-" : clinvarRef.substr(1,clinvarRef.length-1);
              clinvarAlt = clinvarAlt.substr(1,clinvarAlt.length-1);
            }

            var cullTranscripts = function(transcriptObject, theTranscriptId) {
              // If the current transcript is included in the list,
              // we don't have to identify individual transcripts.
              for (var key in transcriptObject) {
                var transcripts = transcriptObject[key];
                var found = false;
                for (var transcriptId in transcripts) {
                  var strippedTranscriptId = stripTranscriptPrefix(transcriptId);
                  if (theTranscriptId.indexOf(strippedTranscriptId) == 0) {
                    found = true;
                  }
                }
                if (found) {
                  transcriptObject[key] = {};
                }

              }
              return transcriptObject;
            }

            var getHighestImpact = function(theObject, cullFunction, theTranscriptId) {
              var theEffects = theObject['HIGH'];
              if (theEffects) {
                return {HIGH: cullFunction(theEffects, theTranscriptId)};
              }
              theEffects = theObject['MODERATE'];
              if (theEffects) {
                return {MODERATE: cullFunction(theEffects, theTranscriptId)};
              }
              theEffects = theObject['MODIFIER'];
              if (theEffects) {
                return {MODIFIER: cullFunction(theEffects, theTranscriptId)};
              }
              theEffects = theObject['LOW'];
              if (theEffects) {
                return {LOW: cullFunction(theEffects, theTranscriptId)};
              }
              return {};
            }

            var getLowestScore = function(theObject, cullFunction, theTranscriptId) {
              var minScore = 99;
              for( score in theObject) {
                if (+score < minScore) {
                  minScore = +score;
                }
              }
              // Now get other entries with the same SIFT/Polyphen category
              var categoryObject = theObject[minScore];
              for (var category in categoryObject) {
                for (var theScore in theObject) {
                  var theCategoryObject = theObject[theScore];
                  if (+theScore != +minScore && theCategoryObject[category] != null) {
                    var theTranscripts = theCategoryObject[category];
                    for (var transcriptId in theTranscripts) {
                      appendTranscript(categoryObject, category, transcriptId);
                    }
                  }
                }

              }
              theObject[minScore] = cullFunction(categoryObject, theTranscriptId);
              return theObject[minScore];
            }

            var getHighestScore = function(theObject, cullFunction, theTranscriptId) {
              var maxScore = -99;
              for( score in theObject) {
                if (+score > maxScore) {
                  maxScore = +score;
                }
              }
              // Now get other entries with the same SIFT/Polyphen category
              var categoryObject = theObject[maxScore];
              for (var category in categoryObject) {
                for (var theScore in theObject) {
                  var theCategoryObject = theObject[theScore];
                  if (+theScore != +maxScore && theCategoryObject[category] != null) {
                    var theTranscripts = theCategoryObject[category];
                    for (var transcriptId in theTranscripts) {
                      appendTranscript(categoryObject, category, transcriptId);
                    }
                  }
                }

              }
              theObject[maxScore] = cullFunction(categoryObject, theTranscriptId);
              return theObject[maxScore];
            }

            if (keepAlt) {

              var highestImpactSnpeff = getHighestImpact(allSnpeff, cullTranscripts, selectedTranscriptID);
              var highestImpactVep = getHighestImpact(allVep, cullTranscripts, selectedTranscriptID);
              var highestSIFT = getLowestScore(allSIFT, cullTranscripts, selectedTranscriptID);
              var highestPolyphen = getHighestScore(allPolyphen, cullTranscripts, selectedTranscriptID);

              variants.push( {'start': +rec.pos, 'end': +end, 'len': +len, 'level': +0,
                'strand': geneObject.strand,
                'chrom': refName,
                'type': typeAnnotated && typeAnnotated != '' ? typeAnnotated : type,
                'id': rec.id, 'ref': rec.ref,
                'alt': alt, 'qual': rec.qual, 'recfilter': rec.filter,
                'af': af,
                'combinedDepth': combinedDepth,
                'genotypes': genotypes,
                'genotype': genotypeForSample,
                'genotypeDepth' : genotypeDepthForSample,
                'genotypeFilteredDepth' : genotypeFilteredDepthForSample,
                'genotypeAltCount' : genotypeAltCountForSample,
                'genotypeRefCount' : genotypeRefCountForSample,
                'genotypeAltForwardCount' : genotypeAltForwardCountForSample,
                'genotypeAltReverseCount' : genotypeAltReverseCountForSample,
                'genotypeRefForwardCount' : genotypeRefForwardCountForSample,
                'genotypeRefReverseCount' : genotypeRefReverseCountForSample,
                'eduGenotype' : eduGenotype,
                'eduGenotypeReversed': eduGenotypeReversed,
                'zygosity': zygosity ? zygosity : 'gt_unknown',
                'phased': phased,
                'effect': effects,
                'impact': impacts,
                'highestImpactSnpeff': highestImpactSnpeff,
                'highestImpactVep': highestImpactVep,
                'highestSIFT': highestSIFT,
                'highestPolyphen': highestPolyphen,
                'consensus': rec.consensus,
                'inheritance': '',
                'af1000glevel': '',
                'afexaclevel:': '',
                'af1000G': me.parseAf(altIdx, af1000G),
                'afExAC': me.parseAf(altIdx, afExAC),
                'rsid' : (rs != null && rs != '' && rs != 0 ? rs : ''),
                'clinvarStart': clinvarStart,
                'clinvarRef': clinvarRef,
                'clinvarAlt': clinvarAlt,
                'vepConsequence': vepConsequence,
                'vepImpact': vepImpact,
                'vepExon': vepExon,
                'vepHGVSc':  vepHGVSc,
                'vepHGVSp': vepHGVSp,
                'vepAminoAcids': vepAminoAcids,
                'vepVariationIds' : vepVariationIds,
                'vepSIFT': vepSIFT,
                'sift' : sift,
                'vepPolyPhen':  vepPolyPhen,
                'polyphen' : polyphen,
                'vepRegs':  vepRegs,
                'regulatory' : regulatory
                }
              );

              if (rec.pos < geneObject.start ) {
                variantRegionStart = rec.pos;
              }

            }

            altIdx++;
          });
        }

      });

      // Here is the result set.  An object representing the entire region with a field called
      // 'features' that contains an array of variants for this region of interest.
      var results = {'ref': refName, 'gene': geneObject.gene_name, 'start': +geneObject.start, 'end': +geneObject.end, 'strand': geneObject.strand, 'transcript': selectedTranscript,
        'variantRegionStart': variantRegionStart, 'name': 'vcf track',
        'homCount': homCount, 'hetCount': hetCount, 'sampleCount' : sampleCount,
        'features': variants};

      return results;
  };

  exports.parseMultiAllelic = function(alleleIdx, genotypeValue, delim) {
    if (genotypeValue == null || genotypeValue == "" || genotypeValue.indexOf(delim) < 0) {
      return genotypeValue;
    } else {
      var tokens = genotypeValue.split(delim);
      if (tokens.length >= alleleIdx) {
        return tokens[alleleIdx];
      } else {
        return genotypeValue;
      }
    }
  };

  // If af returned from af is for multi-allelic variants, we need to parse out the
  // correct af from the comma separated string.
  exports.parseAf = function(altIdx, af) {
      // Handle multi-allelics
      if (af.indexOf(",") > 0) {
        var aftokens = af.split(",");
        var theAf = aftokens[+altIdx];
        return theAf;
      } else {
        return af;
      }
  };


  exports.parseAnnotForAlt = function(value, altIdx) {
    var annotValue = "";
    if (value.indexOf(",") > 0) {
      var tokens = value.split(",");
      if (tokens.length > altIdx) {
        annotValue = tokens[altIdx];
      } else {
        annotValue = value;
      }
    }  else {
      annotValue = value;
    }
    return annotValue;
  };

  exports.pileupVcfRecordsImproved = function(variants, regionStart, posToPixelFactor, widthFactor) {
    var pileup = pileupLayout().sort(null).size(800); // 1860
    var maxlevel = pileup(variants);
    return maxLevel;
  }

  exports.pileupVcfRecords = function(variants, regionStart, posToPixelFactor, widthFactor) {
      widthFactor = widthFactor ? widthFactor : 1;
      // Variant's can overlap each over.  Set a field called variant.level which determines
      // how to stack the variants vertically in these cases.
      var posLevels = {};
      var maxLevel = 0;
      var posUnitsForEachVariant = posToPixelFactor * widthFactor;
      variants.forEach(function(variant) {

        // get next available vertical spot starting at level 0
        var startIdx = (variant.start - regionStart);// + i;
        var posLevel = 0;
        var stackAtStart = posLevels[startIdx];
        if (stackAtStart) {
          for (var k = 0; k <= stackAtStart.length; k++ ) {
            if (stackAtStart[k] == undefined) {
              posLevel = k;
              break;
            }
          }
        }

        // Set variant level.
        variant.level = posLevel;

        // Now set new level for each positions comprised of this variant.
        for (var i = 0; i < variant.len + posUnitsForEachVariant; i++) {
          var idx = (variant.start - regionStart) + i;
          var stack = posLevels[idx] || [];
          stack[variant.level] = true;
          posLevels[idx] = stack;

          // Capture the max level of the entire region.
          if (stack.length - 1 > maxLevel) {
            maxLevel = stack.length - 1;
          }
        }
      });
      return maxLevel;
  }


  exports.compareVcfRecords = function(variants1, variants2, comparisonAttr, onMatchFunction, onNoMatchFunction) {

    var set1Label = 'unique1';
    var set2Label = 'unique2';
    var commonLabel = 'common';
    var comparisonAttribute = comparisonAttr;
    if (comparisonAttribute == null) {
      comparisonAttribute = 'consensus';
    }

    variants1.count = variants1.features.length;
    variants2.count = variants2.features.length;

    var features1 = variants1.features;
    var features2 = variants2.features;

    // Flag duplicates as this will throw off comparisons
    var ignoreDups = function(features) {
      for (var i =0; i < features.length - 1; i++) {
        var variant = features[i];
        var nextVariant = features[i+1];
        if (i == 0) {
          variant.dup = false;
        }
        nextVariant.dup = false;

        if (variant.start == nextVariant.start) {
             var refAlt = variant.type.toLowerCase() + ' ' + variant.ref + "->" + variant.alt;
             var nextRefAlt = nextVariant.type.toLowerCase() + ' ' + nextVariant.ref + "->" + nextVariant.alt;

             if (refAlt == nextRefAlt) {
                nextVariant.dup = true;
             }
        }
      }
    }
    ignoreDups(features1);
    ignoreDups(features2);


    // Iterate through the variants from the first set,
    // marking the consensus field based on whether a
    // matching variant from the second list is encountered.
    var idx1 = 0;
    var idx2 = 0;
    while (idx1 < features1.length && idx2 < features2.length) {
      // Bypass duplicates
      if (features1[idx1].dup) {
        idx1++;
      }
      if (features2[idx2].dup) {
        idx2++;
      }

      variant1 = features1[idx1];
      variant2 = features2[idx2];

      var refAlt1 = variant1.type.toLowerCase() + ' ' + variant1.ref + "->" + variant1.alt;
      var refAlt2 = variant2.type.toLowerCase() + ' ' + variant2.ref + "->" + variant2.alt;

      if (variant1.start == variant2.start) {

        if (refAlt1 == refAlt2) {
          variant1[comparisonAttribute] =  commonLabel;
          variant2[comparisonAttribute] =  commonLabel;

          if (onMatchFunction) {
            onMatchFunction(variant1, variant2);
          }
          idx1++;
          idx2++;
        } else if (refAlt1 < refAlt2) {
          variant1[comparisonAttribute] = set1Label;
          if (onNoMatchFunction) {
            onNoMatchFunction(variant1, null);
          }
          idx1++;
        } else {
          variant2[comparisonAttribute] = set2Label;
          if (onNoMatchFunction) {
            onNoMatchFunction(null, variant2);
          }
          idx2++;
        }
      } else if (variant1.start < variant2.start) {
        variant1[comparisonAttribute] = set1Label;
        if (onNoMatchFunction) {
            onNoMatchFunction(variant1, null);
        }
        idx1++;
      } else if (variant2.start < variant1.start) {
        variant2[comparisonAttribute] = set2Label;
        if (onNoMatchFunction) {
            onNoMatchFunction(null, variant2);
        }
        idx2++;
      }

    }


    // If we get to the end of one set before the other,
    // mark the remaining as unique
    //
    if (idx1 < features1.length) {
      for(x = idx1; x < features1.length; x++) {
        var variant1 = features1[x];
        variant1[comparisonAttribute] = set1Label;
        if (onNoMatchFunction) {
            onNoMatchFunction(variant1, null);
        }
      }
    }
    if (idx2 < features2.length) {
      for(x = idx2; x < features2.length; x++) {
        var variant2 = features2[x];
        variant2[comparisonAttribute] = set2Label;
        if (onNoMatchFunction) {
            onNoMatchFunction(null, variant2);
        }
      }
    }



  };



  //
  //
  //
  //  PRIVATE
  //
  //
  //


  // Allow on() method to be invoked on this class
  // to handle data events
  d3.rebind(exports, dispatch, 'on');

  // Return this scope so that all subsequent calls
  // will be made on this scope.
  return exports;
};
