package abs.callgraph

import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import com.github.javaparser.utils.Pair
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.DirectoryFileFilter
import org.apache.commons.io.filefilter.RegexFileFilter

/**
 * Created by christophlaaber on 07/04/17.
 */
class TypeSolverFactory {

    static TypeSolver get(String path) {
        CombinedTypeSolver s = new CombinedTypeSolver()
        s.add(new ReflectionTypeSolver())
        def f = new File(path)
        javaDirs(f).each { dir ->
            println(dir.absolutePath)
            s.add(new JavaParserTypeSolver(dir))
        }
        s.add(new JavaParserTypeSolver(new File(path)))
        return s
    }

    private static List<File> javaDirs(File file) {
        def ret = new ArrayList<File>()
        if (file.isFile()) {
            return ret
        }

        def containsJava = false
        file.eachFile { f ->
            if (f.isFile() && f.name.endsWith(".java")) {
                if (!containsJava) {
                    // only add once per dir
                    ret.add(file)
                }
                containsJava = true
            } else if (f.isDirectory()) {
                ret.addAll(javaDirs(f))
            }
        }
        return ret
    }

    private static Pair<Boolean, List<File>> dirContent(File file) {
        if (file.isFile()) {
            return new Pair<Boolean, List<File>>(Boolean.FALSE, new ArrayList<File>())
        }

        def containsJava = Boolean.FALSE
        def list = new ArrayList<File>()
        file.eachFile { f ->
            if (f.isDirectory()) {
                list.add(f)
            } else if (f.name.endsWith(".java")) {
                containsJava = Boolean.TRUE
            }
        }
        return new Pair<Boolean, List<File>>(containsJava, list)
    }

    static TypeSolver get(Iterable<File> jars) {
        CombinedTypeSolver s = new CombinedTypeSolver()
        s.add(new ReflectionTypeSolver())
        jars.forEach { j ->
            s.add(new JarTypeSolver(j.absolutePath))
        }
        return s
    }

}
