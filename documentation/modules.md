# Dagli Module Overview
Dagli has a large number of modules; this overview is intended to help you find the class you're looking for (or the required JAR) when using Dagli.

# `annotation`
Defines annotations (like `@Struct` or `@ValueEquality`) used by Dagli.  Normally you do not need to explicitly depend on this module as it is transitively included by the `core` module.

# `annotation-processor`
Used to process some Dagli annotations such as `@Struct` and `@VisitedBy` to perform correctness checks or generate code.  If you're using a recent version of Gradle, you'll need to add these lines to your `dependency` section:

    annotationProcessor 'com.linkedin.dagli:annotation-processor:[version]'
    compileOnly 'com.linkedin.dagli:annotation-processor:[version]'
    
# `avro`
Contains the `AvroField` transformer, which extracts the value of a specified field from an inputted Avro object.

# `calibration`
Isotonic regression, which fits a non-decreasing piecewise linear function and is useful for finding calibrated probabilities from predicted scores.

# `clustering`
K-Means clustering.

# `common`
Most transformers included with Dagli live here, and this is first place to look for a transformer with a desired capability.

- `com.linkedin.dagli.array`: transformers for arrays
- `com.linkedin.dagli.distribution`: transformers for `DiscreteDistribution`s
- `com.linkedin.dagli.evaluation`: transformers that evaluate the performance of a model by comparing the predicted value(s) with the true value(s) (e.g. `MultinomialEvaluation`)
- `com.linkedin.dagli.list`: transformers for lists and, more generally, `Iterable`s
- `com.linkedin.dagli.map`: transformers for `Map`s and multisets
- `com.linkedin.dagli.meta`: "meta" transformers that typically wrap one or more other transformers, e.g. `BestModel` (finds the best model via cross-validation), `KFoldCrossTrained` (cross-trains a preparable transformer), `NullFiltered` (prepares a transformer with only sets of inputs containing no nulls), etc.
- `com.linkedin.dagli.object`: transformers that apply to objects in general, although many require the objects to be `Comparable`.  E.g. `Rank` finds the position of an input value in an ordered list (across all examples), `Index` assigns non-negative indices to distinct values, `Max` finds the highest value   `Convert` is not itself a transformer, but rather a static class whose methods create transformers that convert their inputs in various ways (e.g. by converting a `Number` to a `Long`).
- `com.linkedin.dagli.vector`: transformers for creating and manipulating `Vector`s (which are commonly used by Dagli to represent feature vectors and embeddings).

# `core`
All projects using Dagli should have a dependency on this module.  All basic Dagli components, base classes, and interfaces: DAGs, generators, placeholders, transformers, etc.  Besides DAG classes, this module defines only a small number of concrete transformer classes that may be interesting to clients: `ConstantResultTransformationX`, `MappedIterable`, `TriviallyPreparableTransformation`, `TupledX` and `ValueXFromTuple`.

# `data`
Provides `DSVReader` for reading delimiter-separated value files.

# `embedding-classification`
Classes supporting classification inference using simple embedding models, including FastText (`PreparedFastTextClassifiation`).  Note that these classes are inference-only and generally not used directly; e.g. to include a (trainable) FastText model in your own DAG, use the `FastTextClassification` transformer in the `fasttext` module.

# `fasttext`
Provides the FastText text classification model as the `FastTextClassification` transformer.  FastText is a shallow neural network that can be quickly trained with a CPU (no GPU required) and provides good "out of the box" performance in many text classification tasks.

# `liblinear`
Provides a logistic regression classifier backed by a Java port of the liblinear library.  Logistic regression is a staple of ML toolboxes and has a long history of successful use in industry.

# `math-distribution`
Dagli's sublibrary for discrete distributions (as defined by the `DiscreteDistribution` interface).  Discrete distributions are commonly used to represent the result of classification for both multinomial and multilabel classifiers (the probabilities of the events in the distribution can sum to more than 1).  Concrete implementations of distributiosn include `ArrayDiscreteDistribution` and `BinaryDistribution`; the sublibrary also incldes `AliasSampler` for asymptotically optimal sampling from distributions with replacement.

# `math-hashing`
Dagli's sublibrary for hashing, including MurmurHash variants and `StatelessRNG` implementations (which hash consecutive integers to well-distributed pseudorandom values).

# `math-mdarray`
Provides `MDArray` implementations, multi-dimensional arrays analogous to NumPy's NDArrays.  They are primarily used to provide or obtain multi-dimensional arrays (such as a weight matrix) to/from neural network models.

# `math-vector`
Dagli has an extensive inventory of `Vector` implementations and operations, which are used to, e.g. represent feature vectors and embeddings.  Vectors may be mutable or immutable, dense or sparse, have float or double elements, etc.

# `nn`
Dagli's neural network abstraction sublibrary, defining activation functions, loss functions, optimizers, and layers that may be used to easily construct neural network models.

Normally, clients will not include this module directly and will instead include a concrete neural network implementation module, such as `nn-dl4j`.

# `nn-dl4j`
A concrete implementation of Dagli's neural network abstraction (`NeuralNetwork`) backed by the DeepLearning4Java (DL4J) library.  There is also support (`CustomNeuralNetwork`) for arbitrary DL4J computational graphs not yet supported by Dagli's abstraction layer.

DL4J requires additional, platform-specific dependencies for ND4J (DL4J's accompanying linear algebra library); please see the [DeepLearning4Java website](https://deeplearning4j.org/) for more information, or `dagli/examples/neural-network` for examples using the basic CPU dependencies.  If you have a suitable GPU, the GPU-backed dependencies are a strongly recommended alternative to accelerate training and inference.

# `objectio-avro`
`ObjectReader` and `ObjectWriter` implementations for Avro-serialized objects: `AvroReader` and `AvroWriter`.  These will allow your Dagli pipelines to read Avro data and write Avro results. 

# `objectio-biglist`
`ObjectReader` and `ObjectWriter` implementations backed by `BigList`s (in-memory lists not limited to ~2 billion entries): `BigListReader` and `BigListWriter`.

# `objectio-core`
Provides the `ObjectReader` and `ObjectWriter` interfaces and many core implementations, e.g. `IterableReader` which wraps arbitrary `Iterable` instances to create corresponding `ObjectReader`s.

`ObjectReader`s are read through `ObjectIterator`s, which are akin to "`InputStream`s for objects" (rather than bytes).  `ObjectWriter`s are correspondingly destinations for written objects, akin to `OutputStream`s.

# `objectio-kryo`
Provides `KryoFileReader` and `KryoFileWriter` implementations for reading and writing Kryo-serialized object to files; these are often used when training DAGs with very large datasets to store intermediate transformer results and avoid exhausting RAM.

`KryoMemoryReader` and `KryoMemoryWriter` are also provided and read and write Kryo-serialized objects to (heap) memory.  Although this incurs a serialization and deserialization cost, Kryo-serialized objects may require substantially less RAM than their unserialized forms.

Please note that, although most objects are Kryo-serializable by default, this is not *always* the case, most commonly because the class is missing a parameterless constructor; this is why many Dagli classes have private parameterless constructors that seemingly serve no purpose.  Modifying the class or registering a custom Kryo serializer will fix this.

# `objectio-testing`
Provides the `Tester` class for basic testing of `ObjectReader`, `ObjectWriter` and `ObjectIterator` implementations.  Not useful unless you are implementing one of these classes yourself.

# `objectio-tuple`
Provides `TupleReader`, an `ObjectReader` that reads tuples comprised of objects read from one or more wrapped `ObjectReader`s.
 
# `text`
Dagli's collection of basic text-related transformers for `CharSequence`s (such as `String`s).  These range from the relatively sophisticated (efficiently replacing multiple target substrings with `ReplacedSubstrings`) to basic (`LowerCased` and `NormalizedUnicode`).

# `text-phone`
Provides the `ContainsPhoneNumber` transformer, which leverage's Google's Phone Number library to detect whether text contains a plausible phone number.

# `text-tokenization`
Provides `Tokens`, a transformer using JFlex to tokenize text into lists of words (and numbers and punctuation).  Currently languages using Latin characters are best supported; accuracy with logographic languages (e.g. Chinese or Japanese) will be poor.

### `text-tokenization-jflex`
Dependency of `text-tokenization` that stores the JFlex classes and rules defining the tokenizers used by `Tokens`.  Should not be directly used by client code.

# `tuple`
Dagli's Tuple sub-library.  Tuples are immutable sequences of statically-typed objects, e.g. `Tuple3<String, Integer, Double>`.  Dagli provides tuple classes for arities from 1 to 20, although more readable and bug-resistant alternatives such as [@Structs](structs.md) are often a far better alternative.  Dagli's tuples are efficient and can either wrap existing arrays (`ArrayTupleX`, where `X` is the arity) or store their values in fields (`FieldTupleX`), avoiding the overhead of an extra (array) object on the heap.

# `util`
Dagli's utility sub-library which transitively includes all of Dagli's `util-*` modules.  Only classes with non-trivial dependencies are defined within `util` itself.

# `util-array`
Provides the `ArraysEx` utility class containing numerous useful methods for primitive and object arrays.

# `util-collection`
The `Iterables`, `Lists`, and `Maps` utility classes, together with a number of specified data structures such as `LinkedNode`, `FixedCapacityArrayList`, etc.

# `util-core`
The "core" of Dagli's utility sub-library.
- `AbstractCloneable`: base class for cloneable derived classes
- `Closeables`: utility class for interacting with `Closeable` objects
- `Cryptography`: Dagli's cryptography utility class, useful when, e.g. temporary data being cached to disk needs to be protected.
- `DagliSystemProperties`: sets and gets Dagli's process-wide configuration, though these are normally set via command-line arguments to Java (e.g. `-Ddagli.tmpdir /tmp/`) 
- `ValueEqualityChecker`: checks if two objects are equal by comparing their fields
- `Exceptions`: utility class for exceptions
- `Arguments`: convenience methods for invariant-checking, especially of the arguments passed to a method
- `InputSubstream` and `Outputsubstream`: embed one byte stream within another
- `SerializableTempFile`: a temporary file whose data is serialized with an owning object and recreated when that object is deserialized
- `Patterns`: utility methods for `Pattern` objects (used by regular expressions)
- `StandardInputListener`: utility class for asynchronously and non-exclusively listening to stdin
- `AutoClosing*Stream`: streams that wrap an existing stream, closing it when a terminal method is called (useful when the stream is backed by something holding a system resource, e.g. the stream over the lines of a file returned by `Files.lines(...)`).
- `Classes`: utility methods for types

# `util-function`
Dagli's sublibrary of functional interfaces.  Functions with up to 10 typed parameters are supported, for all return types (including primitives) and void, e.g. `Function2` (a function accepted two arguments and returning an object), `IntFunction0` (a function accepting no arguments and returning an `int`), and `VoidFunction4` (a function accepting 4 parameters with no return value).

A major feature of this module is the focus on `Serializable` functions; every functional interface has a corresponding `Serializable` variant, e.g. `Function2.Serializable`.  Functions created from method references are not necessarily reliably serializable, but a *safely-serializable* copy of such functions can be made via their `safelySerializable()` method, e.g. `Function2.Serializable::safelySerializable()`.

# `visualization-ascii`
`AsciiVisualization` "draws" a DAG as ASCII art.  Please note that this module presently requires a Scala 2.10 dependency.

# `word2vec`
Provides the `Word2VecEmbedding` transformer that, given **pretrained** Word2Vec embeddings, embeds text provided as a list of tokens.  Pre-trained embeddings can be [downloaded here](https://code.google.com/archive/p/word2vec/) (in the section titled "Pre-trained word and phrase vectors").

# `xgboost`
Provides the `XGBoostClassification` and `XGBoostRegression` transformers that train gradient boosted decision tree classification and regression models, respectively.  Decision tree models like XGBoost have important practical advantages:
- They effectively find interesting non-linear conjunctions of the raw features (a common architecture is to use XGBoost to generate conjunctive features which are then fed to another, downstream model)
- They can naturally handle real-valued and ordinal features by finding good split points
- Forests of "decision stumps" (very low-depth trees) are resistant to overfitting     