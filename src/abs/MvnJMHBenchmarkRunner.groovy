package abs

import groovy.json.JsonSlurper

public class MvnJMHBenchmarkRunner extends Runner {

//    public static final def MVN = "/Users/philipp/maven/bin/mvn"
//      public static final def MVN = "/Users/philipp/apache-maven-3.3.9/bin/mvn"
//    public static final def MVN = "C:\\Users\\leitn\\Desktop\\apache-maven-3.3.9\\bin\\mvn.cmd"
    public static final def MVN = "mvn"
    public static final def JAVA = "java"

    public static final def MX_SIZE = "-Xmx8g"
    public static final def RESULT_FILE = "tmp.json"

    def project
    def benchmarks
    def benchmarkJar
    def config

    public MvnJMHBenchmarkRunner(def project, def benchmarks, def benchmarkJar, def config) {
        this.project = project
        this.benchmarks = benchmarks
        this.benchmarkJar = benchmarkJar
        if(config)
            this.config = config
        else
            this.config = ""
    }

    protected void compile() {
        def proc = "$MVN clean install -DskipTests".execute(null, new File(this.project))
        proc.in.eachLine { line -> println line }
        proc.out.close()
        proc.waitFor()
    }

    protected def runBenchmark(def benchmarkToRun) {
        def cmd = "$JAVA $MX_SIZE -jar ${this.benchmarkJar} -rf json -rff $RESULT_FILE ${this.config}"
        if(benchmarkToRun) {
          if(benchmarkToRun.params)
            benchmarkToRun.params.each{ cmd += " -p $it" }
          cmd += " ${benchmarkToRun.pattern}"
        }
        println "#### Running command: $cmd"
        def proc = cmd.execute(null, new File("${this.project}/${this.benchmarks}"))
        proc.in.eachLine { line -> println line }
        proc.out.close()
        proc.waitFor()
        return parseResults(RESULT_FILE)
    }

    def parseResults(def resultsFile) {

        def slurper = new JsonSlurper()
        def results = slurper.parse(new File("${this.project}/${this.benchmarks}/$resultsFile"))
        def benchmarks = [:]
        results.each{result ->
            def name = result.benchmark
            if(result.params) {
                result.params.each {key, val ->
                    name += "($key-$val)"
                }
            }
            def data = result.primaryMetric.rawData[0]
            benchmarks[name] = data
        }
        return benchmarks

    }

}
