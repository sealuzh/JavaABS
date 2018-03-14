package abs.callgraph

/**
 * Created by christophlaaber on 17/02/17.
 */
trait CallGraphWalker {
    abstract void addJar(String path)
    abstract void addInterfaceToClassEdges(InterfaceImplementer ii)
    abstract Iterable<Method> reachableMethods(Method method, Reachable r)

    Map<Method, Iterable<Method>> reachableMethods(Iterable<Method> methods, Reachable r) {
        def ret = new HashMap()
        methods.each { m ->
            ret.put(m, reachableMethods(m, r))
        }
        return ret
    }
}