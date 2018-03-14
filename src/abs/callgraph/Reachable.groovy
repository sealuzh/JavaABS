package abs.callgraph

import com.google.common.graph.Graph

/**
 * Created by christophlaaber on 09/03/17.
 */
interface Reachable {
    boolean reachable(Graph<Method> graph, Method from, Method to)
}