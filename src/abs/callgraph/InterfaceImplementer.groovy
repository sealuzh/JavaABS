package abs.callgraph

/**
 * Created by christophlaaber on 16/03/17.
 */
trait InterfaceImplementer {
    Iterable<Method> implementationOf(Method i) {
        def m = [i]
        def implementations = implementationOf(m)
        return implementations.get(i)
    }

    abstract Map<Method, Iterable<Method>> implementationOf(Iterable<Method> interfaces)
}