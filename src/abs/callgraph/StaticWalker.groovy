package abs.callgraph

import com.github.javaparser.JavaParser
@groovy.lang.Grab(group="com.google.guava", module="guava", version="21.0")
import com.google.common.graph.*

import java.nio.file.Paths

/**
 * Created by christophlaaber on 17/02/17.
 */
class StaticWalker implements CallGraphWalker {
    private final static String PARAM_SEP = ","
    private static final String GENERATED_FQN = "generated"
    private static final String GEN_SEP = "_"

    private final String libPath
    private final Set<String> jarPaths
    private final String pathPrefix
    private final projectPath
    private final Set<Method> kMethods
    private final Set<Method> interfaces
    private final Iterable<Method> benchmarks

    private MutableValueGraph<Method, CallType> cg = null;

    StaticWalker(String projectPath, String libPath, String pathPrefix, kMethods, Iterable<Method> benchmarks) {
        this.libPath = libPath
        this.pathPrefix = pathPrefix
        this.projectPath = projectPath
        this.kMethods = this.kMethods(kMethods)
        this.jarPaths = new HashSet<>()
        interfaces = new HashSet<>()
        this.benchmarks = benchmarks
    }

    private Set<Method> kMethods(kMethods) {
        def nodes = new HashSet<Method>()
        kMethods.each { f ->
            def file = new File("${Paths.get(projectPath, f.test_file).toString()}")
            def cu = JavaParser.parse(file)
            def className = f.test_file.substring(0, f.test_file.indexOf("."))
            className = className.substring(className.lastIndexOf("/") + 1, className.size())
            def fqnClassName = cu.getPackageDeclaration().get().name.asString() + "." + className

            f.methods.each { m ->
                def params = []
                def altered = false
                m.params.each { p ->
                    def tp = m.typeParams[p]
                    if (tp != null) {
                        params.add(tp)
                        altered = true
                    } else if (p.contains("<")) {
                        params.add(p.substring(0, p.indexOf("<")))
                        altered = true
                    } else {
                        params.add(p)
                    }
                }
                def node = new Method(className: fqnClassName, method: m.name, parameters: params)
                nodes.add(node)
            }
        }
        return nodes
    }

    @Override
    void addJar(String path) {
        if (!jarPaths.contains(path)) {
            createCallGraph(path)
        }
    }

    @Override
    void addInterfaceToClassEdges(InterfaceImplementer ii) {
        def cg = callGraph()
        ii.implementationOf(interfaces).forEach({ from, tos ->
            tos.each { to ->
                cg.putEdgeValue(from, to, CallType.INTERFACE_TO_CLASS)
            }
        })
    }

    @Override
    Iterable<Method> reachableMethods(Method method, Reachable r) {
        def cg = callGraph()
        def rms = new HashSet()
        for (def m : kMethods) {
            def reachable = r.reachable(cg, method, m)
            if (reachable) {
                rms.add(m)
            }
        }
        return rms
    }

    private MutableValueGraph<Method, CallType> callGraph() {
        if (cg == null) {
            synchronized (this) {
                if (cg == null) {
                    cg = ValueGraphBuilder.directed().allowsSelfLoops(true).build()
                }
            }
        }
        return cg
    }

    private Method benchmark(String className, String methodName) {
        Method ret = null
        for (def b : benchmarks) {
            if (b.className == className && b.method == methodName) {
                ret = b
                break
            }
        }
        return ret
    }

    private Method method(String[] arr) {
        def methodArr = arr[1].split("\\(")

        def className = arr[0]
        // replace nested class indication of '$' with '.'
        className = className.replace("\$", ".")
        def methodName = methodArr[0]

        //check whether method is generated
//        if (arr[0].contains(GENERATED_FQN)) {
////            println("old: " + className + " . " + methodName)
//            def oldClassName = className
//            def oldMethodName = methodName
//            def classNameArr = className.split("\\.")
//            def sb = new StringBuilder()
//            def counter = 1
//            classNameArr.each { el ->
//                try {
//                    if (GENERATED_FQN == el) {
//                        return
//                    }
//
//                    if (counter == classNameArr.size()) {
//                        def cn = el.split(GEN_SEP)[0]
//                        sb.append(cn)
//                    } else {
//                        sb.append(el)
//                        sb.append(".")
//                    }
//                } finally {
//                    counter++
//                }
//            }
//
//            className = sb.toString()
//            methodName = methodArr[0].split(GEN_SEP)[0]
//            if (methodName.trim() == "") {
//                return null
//            }
//
//            if (methodName == "alignedPrimitiveArgsProcessor" && className=="org.jctools.channels.mpsc.MpscProxyChannelBenchmark") {
//                println("--------------------")
//                println("old: " + oldClassName + " . " + oldMethodName)
//            }
//
//            def benchmark = benchmark(className, methodName)
//            if (benchmark != null) {
////                println(benchmark)
//                return benchmark
//            }
////            println("new: " + className + " . " + methodName)
////            println("------- generated but not a JMH test:\n   new: $className:$methodName;\n   old: $oldClassName:$oldMethodName")
//        }

        def params = methodArr[1].substring(0, methodArr[1].length()-1).trim()
        def paramsList = new ArrayList()
        if (params != "") {
            def paramsArr = params.split(PARAM_SEP)
            paramsArr.each { p ->
                // replace nested class indication of '$' with '.'
                paramsList.add(p.replace("\$", "."))
            }
        }
        return new Method(className: className, method: methodName, parameters: paramsList)
    }

    private void createCallGraph(String jarPath) {
        def sout = new StringBuilder()
        def serr = new StringBuilder()
        def execStmt = "java -jar $libPath $jarPath"
        def proc = execStmt.execute()
        proc.waitForProcessOutput(sout, serr)
        def err = serr.toString().trim()
        if (err != "") {
            println(err)
            return
        }

        def g = callGraph()

        def lines = sout.toString().split("\n").toList()
        lines.each { line ->
            if (line == "") {
                return
            }
            def elems = line.split(" ")
            if (elems.size() != 2) {
                println("CG parsing error: line expected to have only 1 whitespace, but had ${elems.size() - 1}")
                println("  '$line'")
                return
            }

            def fromArr = elems[0].split(":")
            if (fromArr.size() < 2 || fromArr[0] != 'M') {
                return
            }

            def type = elems[1].substring(1, 2)

            CallType ct = null
            switch (type) {
                case "M":
                    ct = CallType.VIRTUAL
                    break
                case "I":
                    ct = CallType.INTERFACE
                    break
                case "S":
                    ct = CallType.STATIC
                    break
                case "O":
                    ct = CallType.SPECIAL
                    break
                case "D":
                    ct = CallType.DYNAMIC
                    break
                default:
                    return
            }

            if (!fromArr[1].startsWith(pathPrefix)) {
                return
            }

            // from
            def fromArr2 = new String[fromArr.length-1]
            fromArr2 = fromArr[1..<fromArr.length].toArray(fromArr2)
            def from = method(fromArr2)
            if (from == null) {
                return
            }
            // to
            def toStr = elems[1].substring(3)
            def toArray = toStr.split(":")
            def to = method(toArray)
            if (to == null) {
                return
            }

//            println("cg: $from -> $to")

            g.putEdgeValue(from, to, ct)

            // save interface calls
            if (ct == CallType.INTERFACE) {
                interfaces.add(to)
            }
        }
    }
}
