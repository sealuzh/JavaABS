package abs.callgraph

import abs.callgraph.Method

/**
 * Created by christophlaaber on 09/03/17.
 */
interface BenchmarkFinder {
    Iterable<Method> all(String benchmarkProject)
}