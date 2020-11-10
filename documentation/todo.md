# Planned Improvements
## Near-Term
- Further expand documentation 
- More comprehensive logging
- ~~Improved visualization~~ (added in v15) and debugging tools
- Persistent caching of the intermediate results of DAG nodes to avoid recomputation of unchanged parts of the DAG when a modified DAG is executed with the same input data.
- New layer types for neural networks

## Long-Term
- Rewrite `MultithreadedDAGExecutor` to simplify the logic and further minimize inter-thread communication
- Add support for distributed DAG preparation (e.g. `HadoopDAGExecutor`)