({
  mainConfigFile : 'requireConfigs/optimizerConfig.js',
  baseUrl: "../pedigree",
  name : 'pedigreeApp',
  out : 'minified/pedigree.min.js',
  //Do not minify prototype key variable "$super"
  uglify: {
    except: ["$super"]
  }
})