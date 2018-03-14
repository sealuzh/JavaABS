package abs

public interface Tester {

    def testForChanges(def baseline, def update)

}

//tester = new TTester()
//runner = new MvnJMHBenchmarkRunner('/Users/philipp/Downloads/protostuff', 'protostuff-benchmarks', '')
//f1 = runner.parseResults('tmp.json')
//f2 = runner.parseResults('tmp.json')
//print tester.testForChanges(f1, f2)