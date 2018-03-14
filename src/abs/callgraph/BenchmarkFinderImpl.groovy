package abs.callgraph

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.AnnotationExpr
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver
@Grab(group='commons-io', module='commons-io', version='2.5')
import org.apache.commons.io.*
@Grab(group='commons-io', module='commons-io', version='2.5')
import org.apache.commons.io.*

import java.nio.file.Paths

/**
 * Created by christophlaaber on 09/03/17.
 */
class BenchmarkFinderImpl implements BenchmarkFinder {
    private final TypeSolver TYPE_SOLVER

    BenchmarkFinderImpl(TypeSolver typeSolver) {
        this.TYPE_SOLVER = typeSolver
    }

    @Override
    Iterable<Method> all(String benchmarkProject) {
        def path = Paths.get(benchmarkProject)
        def methods = new HashSet<Method>()

        String[] exts = ["java"]
        FileUtils.listFiles(path.toFile(), exts, true).each { file ->
            CompilationUnit cu = JavaParser.parse(file)
            cu.getPackageDeclaration().ifPresent({ pd ->
                def v = new BenchVisitor(pd.name.toString(), methods, TYPE_SOLVER)
                cu.accept(v, null)
            })
        }

        return methods
    }

    private static class BenchVisitor extends VoidVisitorAdapter<Object> {
        private final BENCHMARK_ANNOTATION = "Benchmark"
        private final Set<Method> METHODS
        private final String PKG_DECL
        private final TypeSolver TYPE_SOLVER
        private String className = null

        BenchVisitor(String pkgDecl, Set<Method> methods, TypeSolver typeSolver) {
            this.PKG_DECL = pkgDecl
            this.METHODS = methods
            this.TYPE_SOLVER = typeSolver
        }

        @Override
        void visit(ClassOrInterfaceDeclaration n, Object arg) {
            try {
                if (className != null) {
                    return
                }
                className = n.getName()
            } finally {
                super.visit(n, arg)
            }
        }

        @Override
        void visit(MethodDeclaration n, Object args) {
            try {
                for (AnnotationExpr a : n.getAnnotations()) {
                    if (BENCHMARK_ANNOTATION == a.name.toString()) {
                        def m = new Method()
                        m.className = "$PKG_DECL.$className"
                        m.method = n.getName()
                        def l = new ArrayList<String>()
                        m.parameters = l
                        for (def pt : n.getParameters()) {
                            l.add(fqn(pt))
                        }
                        METHODS.add(m)
                    }
                }
            } finally {
                super.visit(n, args)
            }
        }

        private String fqn(Node n) {
            def type = JavaParserFacade.get(TYPE_SOLVER).getType(n)
            return type.describe()
        }
    }
}
