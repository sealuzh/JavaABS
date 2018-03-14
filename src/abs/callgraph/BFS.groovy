package abs.callgraph

import com.google.common.graph.Graph

/**
 * Created by christophlaaber on 09/03/17.
 */
class BFS implements Reachable {
    public int exceptions = 0
    public int noExceptions = 0

    @Override
    boolean reachable(Graph<Method> graph, Method from, Method to) {
        def visited = new HashSet<Method>()
        def toVisit = new LinkedList<Method>()
        toVisit.add(from)

        Method current = null
        try {
            while (toVisit.peek() != null) {
                current = toVisit.poll()
                if (current == to) {
                    return true
                }

                for (Method n : graph.successors(current)) {
                    if (!visited.contains(n)) {
                        visited.add(n)
                        toVisit.add(n)
                    }
                }
            }
            noExceptions++
        } catch (Exception e) {
//            println("reachable: $from -> $to")
            exceptions++
        }
        return false
    }
}
