({
  mainConfigFile : '${basedir}/src/main/build/optimizerConfig.js',
  baseUrl: "${basedir}/src/main/resources/pedigree",
  name : 'pedigreeApp',
  out : '${project.build.outputDirectory}/pedigree/minified/pedigree.min.js',
  //Do not minify prototype key variable "$super"
  uglify: {
    except: ["$super"]
  }
})