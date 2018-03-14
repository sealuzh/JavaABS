import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import abs.*

//import java.nio.file.Paths
//import abs.callgraph.BFS
//import abs.callgraph.BenchmarkFinderImpl
//import abs.callgraph.InterfaceImplementerAll
//import abs.callgraph.StaticWalker
//import abs.callgraph.TypeSolverFactory
//TODO: switch dependency as soon as version 0.5.3 comes out. This is a hacky solution that needs manually changing the grape/ivy repository
// @Grab(group='com.github.javaparser', module='java-symbol-solver-core', version='0.5.2-cl')


def writeToLog(def logfile, def run, def name, def result) {
    result.each { benchmark, data ->
      data.each { item ->
        logfile.append("$run;$name;$benchmark;$item\n")
      }
    }
}

def defrostResultsFromLog(def filename) {
    def results = [:]
    new File(filename).splitEachLine(";") { fields ->
        def run = fields[0] as int
        def method = fields[1]
        def benchmark = fields[2]
        def val = fields[3] as double
        if(!results.containsKey(run))
            results[run] = [:]
        if(!results[run].containsKey(method))
            results[run][method] = [:]
        if(!results[run][method].containsKey(benchmark))
            results[run][method][benchmark] = []
        results[run][method][benchmark] << val
    }
    return results
}

def do_run(def runnr) {

    BenchmarkRunner runner =
            (config.build_system && config.build_system == 'gradle') ?
            new GradleJMHBenchmarkRunner(config.project, config.benchmarks, config.benchmark_jar, config.custom_benchmark_config, config.gradle_target) :
            new MvnJMHBenchmarkRunner(config.project, config.benchmarks, config.benchmark_jar, config.custom_benchmark_config)

    if(config.benchmarks_to_execute) {
      def parsed = parseBenchmarksToExecute(config.benchmarks_to_execute)
      runner.setBenchmarksToExecute(parsed)
    }

    // config sanity check
    config.files.each { file ->
      file.methods.each { method ->
        RegressionInducer changer = new PerfRegressionInducer("${config.project}/${file.test_file}",
                method.name, method.params, config.degree_of_violation as double)
        changer.doUpdate()
        changer.resetChanges()
      }
    }
    println "##### Config seems ok #####"

    new File("codedumps/$runnr").mkdir()

    println "##### Baseline Run $runnr #####"
    // baseline run
    def baselineResult = runner.run(new EmptyRegressionInducer(), "")
    writeToLog(logfile, runnr, "Baseline", baselineResult)
    println "##### Baseline Run $runnr Finished #####"

    // test runs
    def results = [:]
    config.files.each { file ->
        if (file == null || file.test_file == null) {
            println "##### Empty file, not executing"
            return
        }
        println "##### Started Running $runnr for ${file.test_file} #####"
        def testfile = file.test_file
        def dumpFileName = testfile.replaceAll("/",".").replaceAll(".java","")
        new File("codedumps/$runnr/$dumpFileName").mkdir()
        file.methods.each { method ->
            println "##### Test run $runnr for ${method.name} #####"
            RegressionInducer changer = new PerfRegressionInducer("${config.project}/$testfile",
                    method.name, method.params, config.degree_of_violation as double)
            def testResult = runner.run(changer, "codedumps/$runnr/$dumpFileName/${method.name}")
            def fullname = "$testfile.${method.name}(${method.params})"
            results[fullname] = testResult
            writeToLog(logfile, runnr, fullname, testResult)
        }
    }
    println "##### Finished $runnr #####"

}

def parseBenchmarksToExecute(def listOfConfigs) {
  listOfConfigs.collect{ config ->
    new BenchmarkToExecute(pattern:config.pattern, params:config.params)
  }
}

def detectableSlowdown(def allresults, def method, def run) {

    def runresults = allresults[run]
    def baselineresults = collectBaselineResults(allresults)
    def testresults = runresults.subMap([method])

    def tester = new TTester()
    def pVals = tester.testForChanges(baselineresults, testresults[method])
    // Bonferroni correction
    def correctedAlpha = Double.parseDouble(config.confidence) / pVals.size()
    def activeTests = pVals.findAll{ _, entry ->
            entry["p"] < correctedAlpha && entry["dm"] > (config.min_effect_size as double)
        }
    println "For $method in run $run, ${activeTests.size()} benchmarks showed a difference (of ${pVals.size()})"
    if(activeTests.size() > 0) {
        println "Indicating benchmarks:"
        activeTests.each{m, r -> println "  $m" }
    }
    return activeTests
}

def collectBaselineResults(def all) {
    def maps = all.collect{_, run ->
        run['Baseline']
    }
    def collectedMaps = [:]
    maps[0].each{ key, val ->
        collectedMaps[key] = maps.collect{ it[key] }.flatten()
    }
    return collectedMaps
}

def buildProject(config) {
    if (!config.scg.compile) {
        return
    }
    procStr = ""
    if (config.build_system && config.build_system == "gradle") {
        procStr = "./gradlew ${config.gradle_target}"
    } else {
        // assume maven
        procStr = "mvn clean install -DskipTests"
    }
    def proc = procStr.execute(null, new File(config.project))
    proc.in.eachLine { line -> println line }
    proc.out.close()
    proc.waitFor()
}


def parseArgs(args) {
    def cli = new CliBuilder(usage: 'copper')
    cli.d('run dynamic ptc')
    cli.s('run static callgraph ptc')
    cli.c('config file', required:true, args:1)
    def options = cli.parse(args)
    if (options == null) {
        return null
    }

    if (!options.getProperty('d') && !options.getProperty('s')) {
        println("error: Missing required option: either d or s")
        cli.usage()
        return null
    }
    return options
}

def options = parseArgs(this.args)
if (options == null) {
    return
}

def configPath = options.getProperty('c')
println("# load config file: $configPath")
def slurper = new JsonSlurper()
config = slurper.parse(new File(configPath))

// dynamic approach
if (options.getProperty('d')) {
    logfile = new File((String)config.log)
    logfile.write("")
    repeats = config.repeats as int

    println "##### Creating directory for code dumps #####"
    if(new File("codedumps").exists()) {
      new File("codedumps").deleteDir()
    }
    new File("codedumps").mkdir()

    repeats.times { run ->
        do_run(run)
    }

    allResults = defrostResultsFromLog((String)config.log)
    // allResults = defrostResultsFromLog("/Users/philipp/Downloads/results/trial_run_1/result_run3_0.5_0.4_3.csv")
    runs = allResults.keySet()
    methods = []
    allResults.each {run, results ->
        results.each { method, _ ->
            methods << method
        }
    }
    methods = methods.unique() - "Baseline"

    slowdownDetections = methods.count { method ->
        def slowdownsDetected = runs.collect { run ->
            detectableSlowdown(allResults, method, run)
        }

        def benchmarks = allResults[0]["Baseline"].collect{ it.key }
        def allDetected = benchmarks.any{benchmark ->
            slowdownsDetected.every{run ->
                run.keySet().contains(benchmark)
            }
        }
        return allDetected
    }
    ptc = slowdownDetections / (double)methods.size()
    println "Dynamic coverage value was ${ptc}"
}

// // static approach
// if (options.getProperty('s')) {
//     // compile project
//     buildProject(config)
//     // get jars
//     def filePattern = config.scg.filePattern
//     if (filePattern == null || filePattern == "") {
//         filePattern = ".*"
//     }
//
//     def jars = Project.jars(config.project, config.scg.jars, filePattern)
//     def typeSolver = TypeSolverFactory.get(jars)
//
//     def bf = new BenchmarkFinderImpl(typeSolver)
//     def benchmarkMethods = bf.all(Paths.get(config.project, config.benchmarks).toString())
//
//     // run call graph walker
//     def scg = new StaticWalker(config.project, config.scg.lib, config.scg.qualifiedPathPrefix, config.files, benchmarkMethods)
//     jars.each { jar ->
//         def path = jar.getAbsolutePath()
//         println("add jar '${path}'")
//         scg.addJar(path)
//     }
//     scg.addInterfaceToClassEdges(new InterfaceImplementerAll(config.project, typeSolver))
//     def finder = new BFS()
//     def rms = scg.reachableMethods(benchmarkMethods, finder)
//
//     def found = new HashSet()
//     rms.forEach({ _, fs ->
//         fs.each { f ->
//             found.add(f)
//         }
//     })
//     // calculate static performance test coverage
//     def sum = 0
//     config.files.each { f ->
//         sum += f.methods.size()
//     }
//     def ptc = 0
//     if (sum != 0) {
//         ptc = found.size()*1.0 / sum
//     }
//     println("Core Method Finder: ${finder.noExceptions} successful searches; ${finder.exceptions} exceptions")
//     println("Static call graph coverage value was ${ptc}")
//
//     // print results
//     String outPath = config.scg.out
//     if (outPath != null && outPath != "") {
//         def outValues = new HashMap()
//         outValues.put("coverage", ptc)
//         outValues.put("methods", rms)
//         def out = new File(outPath)
//         out.write(new JsonBuilder(outValues).toPrettyString())
//     }
// }
