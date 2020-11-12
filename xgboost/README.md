# The XGBoost Module
This module provides Dagli's XGBoost transformers, such as `XGBoostClassification` (by transitive inclusion of the 
`xgboost-core` module), and support for:
- Linux
- Windows (64-bit)
- Mac OSX (single-threaded training only; for multithreaded training, please see below) 
 
## Dependency on XGBoost4J
This module uses "official" XGBoost4J release (at Maven coordinates `ml.dmlc:xgboost4j:[version]`), which includes the
native libraries required by both Linux and Mac OSX.  To support Windows, we've added the Windows binary as a resource
file (`lib/xgboost4j.dll`) from [Criteo forks](https://github.com/criteo-forks/xgboost-jars).

### Windows
Loading the Windows library depends, effectively, on a class from one JAR being able to read a resource file in another
JAR; if using non-standard class loaders there is a theoretical possibility that this might not be the case (in which 
case you'll see an error to the effect of "can't find xgboost4j.dll").  In this unlikely event, use the `xgboost-core` 
module instead and add a dependency on the XGBoost4J Windows JAR from 
[Criteo forks](https://github.com/criteo-forks/xgboost-jars).

### Mac OSX
Mac OSX is limited to a single-thread because the included XGBoost4J native library for OSX is not built with OpenMP 
support (presumably to avoid compatibility issues where libomp is not available).  It **is** very possible to [build
the library with multi-threading support](https://xgboost.readthedocs.io/en/latest/jvm/#enabling-openmp-for-mac-os), 
which can then be used as a dependency alongside `xgboost-core`.