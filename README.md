# Java API Benchmarking Score (ABS)
JavaABS is a tool to execute microbenchmarks written with JMH.

Moreover, it is used in Laaber and Leitner's paper "An Evaluation of Open-Source Software Microbenchmark Suites for Continuous Performance Assessment" published at Mining Software Repositories (MSR) in 2018.

## Execution
Run the following script to execute ABS:
```bash
groovy -cp "src:." copper.groovy -d -c config.json
```

### Arguments
* `-c` config file
* `-d` dynamic ABS metric

### Config File
Examplary configuration file for RxJava project:
```json
{
  "project" : "/home/ubuntu/projects/java/RxJava",
  "build_system" : "gradle",
  "gradle_target" : "clean build -x test",
  "benchmarks" : "build/libs",
  "benchmark_jar" : "rxjava-1.2.10-SNAPSHOT-benchmarks.jar",
  "custom_benchmark_config" : "-wi 10 -i20 -f 1",
  "degree_of_violation" : "0.6",
  "confidence" : "0.05",
  "min_effect_size" : "0.3",
  "log" : "tmp.csv",
  "repeats" : 2,
  "files" : [
    {
        "test_file": "src/main/java/rx/internal/util/SubscriptionList.java",
        "methods": [
            {
                "name": "add",
                "params": [
                    "Subscription"
                ],
                "typeParams": {}
            }
        ]
    },
    {
        "test_file": "src/main/java/rx/Observable.java",
        "methods": [
            {
                "name": "lift",
                "params": [
                    "Operator<? extends R, ? super T>"
                ],
                "typeParams": {
                    "R": "java.lang.Object",
                    "T": "java.lang.Object"
                }
            },
            {
                "name": "unsafeSubscribe",
                "params": [
                    "Subscriber<? super T>"
                ],
                "typeParams": {
                    "T": "java.lang.Object"
                }
            }
        ]
    }
    ]
}
```

JSON attributes (partial):
* `"project"` path to project directory
* `"build_system` mvn or gradle
* `"benchmarks` folder where JMH jar is placed
* `"benchmark_jar` name of JMH jar
* `"degree_of_violation"` inserted relative regression for ABS
* `"log"` output file path
* `"repeats"` repetitions of experiment (r in MSR paper)
* `"files"` methods to inject regressions into

### Output
JavaABS reports all results in CSV form to the file specified as `"log"`.
A sample output file is depicted below:
```csv
Run;Method altered;Microbenchmark;Result
0;Baseline;io.protostuff.benchmarks.RuntimeSchemaBenchmark.baseline;0.9870666624852784
0;Baseline;io.protostuff.benchmarks.RuntimeSchemaBenchmark.baseline;0.953202183493458
0;Baseline;io.protostuff.benchmarks.RuntimeSchemaBenchmark.generated_deserialize_10_int_field;80.25977955639304
0;Baseline;io.protostuff.benchmarks.RuntimeSchemaBenchmark.generated_deserialize_10_int_field;88.68216840394962

```


