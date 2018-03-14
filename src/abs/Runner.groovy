package abs

public abstract class Runner implements BenchmarkRunner {

    private def benchmarks

    public def run(def changer, def dumpFile) {
        def changedCode = changer.doUpdate()
        // dump to a file for debugging
        if(changedCode != "") {
          while(new File(dumpFile).exists())
            dumpFile += "N"
          new File(dumpFile+".java").write(changedCode)
        }
        compile()
        def nanoTimeBefore = System.nanoTime()
        def results = []
        if(benchmarks)
          results = benchmarks.collect{ runBenchmark(it) }
        else
          results << runBenchmark(null)
        def nanoTimeAfter = System.nanoTime()
        def seconds = (nanoTimeAfter - nanoTimeBefore) / 1000000000
        print "####### Running this benchmark suite took $seconds seconds"
        changer.resetChanges()
        return results.sum()
    }

    public void setBenchmarksToExecute(def benchmarks) {
        this.benchmarks = benchmarks
    }

    protected abstract void compile()
    protected abstract def runBenchmark(def benchmarkToRun)
}
