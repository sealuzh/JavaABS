package abs

public class GradleJMHBenchmarkRunner extends MvnJMHBenchmarkRunner {

    public static final def GRADLEW = "./gradlew"

    def gradleConfig

    public GradleJMHBenchmarkRunner(def project, def benchmarks, def benchmarkJar, def jmhConfig, def gradleConfig) {
        super(project, benchmarks, benchmarkJar, jmhConfig)
        this.gradleConfig = gradleConfig
    }

    protected void compile() {
        def proc = "$GRADLEW ${this.gradleConfig}".execute(null, new File(this.project))
        proc.in.eachLine { line -> println line }
        proc.out.close()
        proc.waitFor()
    }

}
