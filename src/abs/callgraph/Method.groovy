package abs.callgraph

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Created by christophlaaber on 08/03/17.
 */
@EqualsAndHashCode
class Method {
    String className
    String method
    List<String> parameters

    @Override
    String toString() {
        def sb = new StringBuilder()
        def first = true
        for (def p : parameters) {
            if (first) {
                first = false
            } else {
                sb.append(",")
            }
            sb.append(p)
        }

        return "$className:$method(${sb.toString()})"
    }
}
