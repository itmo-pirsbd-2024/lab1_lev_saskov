```bash
java -jar ./build/libs/ldr-vector-db-1.0.0-jmh.jar jmh.read.ReadBenchmark -o ./jmh/results/after_optimization.txt
```

TODO: Make shortcut for asprof
/Users/lsaskov/Programms/async-profiler-3.0-macos/bin/asprof -d 60 -e cpu -o flamegraph=total 9678 -f ./tmp/cpu_tmp.html

/Users/lsaskov/Programms/async-profiler-3.0-macos/bin/asprof -d 60 -e alloc -o flamegraph=total 11142 -f ./tmp/alloc_tmp.html


TODO: На профилях аллоков видно, что больше всего аллоков уходит на Integer и Double,  
можно попробовать сделать декодеры через примитивы. 

Нужно мапы переделать на FastUtil