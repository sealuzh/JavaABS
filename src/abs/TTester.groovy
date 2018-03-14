package abs

@groovy.lang.Grab(group = "org.apache.commons", module="commons-math3", version="3.6.1")
import org.apache.commons.math3.stat.inference.TTest

class TTester implements Tester {

    def testForChanges(def baseline, def update) {


        def benchmarks = generateBenchmarkList(baseline, update)

        def results = [:]
        benchmarks.each { benchmark, val ->

            def sample1 = update[benchmark]
            def sample2 = baseline[benchmark]

            def ttest = new TTest()
            def p = ttest.tTest(toDoubleArray(sample1), toDoubleArray(sample2))
            def m1 = getMean(sample1)
            def m2 = getMean(sample2)
            def s1 = getGroupVar(sample1, m1)
            def s2 = getGroupVar(sample2, m2)
            def n1 = sample1.size()
            def n2 = sample2.size()
            def s = Math.sqrt( ((n1 - 1)*s1 + (n2 - 1)*s2) / (n1 + n2 - 2) )
            def d = Math.abs(m1 - m2) / s
            // as effect size is not working out, let's also calculate this "home-grown" effect size
            // this is supposed to be "just" the relative difference in means, without taking the standard
            // deviation into account
            def dm = Math.abs(m1 - m2) / getMean(sample1 + sample2)
            results[benchmark] = ["p" : p, "d" : d, "dm" : dm]

        }
        return results

    }

    static def generateBenchmarkList(def baseline, def update) {
        baseline.findAll{ update.containsKey( it.key ) }
    }

    private double[] toDoubleArray(def l) {
        l.findAll{ (double)it }
    }

    private def getMean(def  sample) {
        sample.sum() / (double)sample.size()
    }

    private def getGroupVar(def  sample, def mean) {
        def sum = sample.sum { Math.pow( it - mean, 2) }
        return sum / ((double) sample.size() - 1)
    }

}
