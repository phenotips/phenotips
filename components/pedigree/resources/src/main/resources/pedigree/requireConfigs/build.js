({
  mainConfigFile : '${basedir}/src/main/resources/pedigree/requireConfigs/optimizerConfig.js',
  baseUrl: "${basedir}/src/main/resources/pedigree",
  name : 'pedigreeApp',
  out : '${basedir}/target/classes/pedigree/minified/pedigree.min.js',
  //Do not minify prototype key variable "$super"
  uglify: {
    except: ["$super"]
  }
})