package abs.callgraph

import com.github.javaparser.JavaParser
import com.github.javaparser.ParseProblemException
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver
import com.github.javaparser.symbolsolver.model.resolution.UnsolvedSymbolException
import org.apache.commons.io.FileUtils

/**
 * Created by christophlaaber on 16/03/17.
 */
class InterfaceImplementerAll implements InterfaceImplementer {
    private final String PROJECT_PATH
    private final JavaParserFacade JPF

    InterfaceImplementerAll(String projectPath, TypeSolver typeSolver) {
        this.PROJECT_PATH = projectPath
        this.JPF = JavaParserFacade.get(typeSolver)
    }

    @Override
    Map<Method, Iterable<Method>> implementationOf(Iterable<Method> interfaces) {
        def f = new File(PROJECT_PATH)
        def methods = new HashMap<Method, Set<Method>>()

        String[] exts = ["java"]
        FileUtils.listFiles(f, exts, true).each { file ->
            try {
                CompilationUnit cu = JavaParser.parse(file)
                cu.getPackageDeclaration().ifPresent({ pd ->
                    def v = new ImplementingVisitor(pd.name.asString(), methods, interfaces)
                    cu.accept(v, JPF)
                })
            } catch (ParseProblemException e) {
                println("Could not parse ${file.absolutePath}")
            }
        }

        return methods
    }

    private static class ImplementingVisitor extends VoidVisitorAdapter<JavaParserFacade> {
        private final Iterable<Method> I
        private final Map<Method, Set<Method>> MS
        private final String PKG

        ImplementingVisitor(String pkg, Map<Method, Set<Method>> methods, Iterable<Method> interfaces) {
            this.I = interfaces
            this.MS = methods
            this.PKG = pkg
        }

        void visit(ClassOrInterfaceDeclaration n, JavaParserFacade jpf) {
            try {
                I.each { i ->
                    n.implementedTypes.each { implementers ->
                        String implementerClassName
                        String interfaceClassName
                        try {
                            implementerClassName = jpf.convertToUsage(implementers).describe()
                            def genIx = implementerClassName.indexOf("<")
                            if (genIx >= 0) {
                                implementerClassName = implementerClassName.substring(0, genIx)
                            }
                            interfaceClassName = i.className
                        } catch (com.github.javaparser.symbolsolver.javaparsermodel.UnsolvedSymbolException e) {
                            implementerClassName = implementers.getName().toString()
                            interfaceClassName = i.className.substring(i.className.lastIndexOf(".")+1)
                            println("${n.name}: Could not resolve $implementerClassName (${implementers})")
                        }

//                        def implementerClassName = implementers.getName().toString()
//                        def interfaceClassName = i.className.substring(i.className.lastIndexOf(".")+1)

                        if (interfaceClassName == implementerClassName) {
                            def params = new ArrayList()
                            params.addAll(i.parameters)
                            def m = new Method(className: "$PKG.${n.nameAsString}", method: i.method, parameters: params)
                            def implementations = MS.get(i)
                            if (implementations != null) {
                                implementations.add(m)
                            } else {
                                Set<Method> ms = [m]
                                MS.put(i, ms)
                            }
                        }
                    }
                }
            } finally {
                super.visit(n, jpf)
            }
        }
    }
}
