# Dagli

Dagli is a machine learning framework that makes it easy to write bug-resistant, readable, efficient, maintainable and 
trivially deployable models in [Java 9+](documentation/java.md) (and other JVM languages).

To get started, here's an example of a text classifier implemented as a pipeline that uses the active leaves of a 
Gradient Boosted Decision Tree model (XGBoost) as well as a high-dimension set of ngrams as features in a logistic 
regression classifier:

    Placeholder<String> text = new Placeholder<>();
    Placeholder<LabelType> label = new Placeholder<>(); 
    Tokens tokens = new Tokens().withInput(text);
    
    NgramVector unigramFeatures = new NgramVector().withMaxSize(1).withInput(tokens);
    Producer<Vector> leafFeatures = new XGBoostClassification<>()
        .withSparseFeatureInput(unigramFeatures)
        .withLabelInput(label)
        .asLeafFeatures();

    NgramVector ngramFeatures = new NgramVector().withMaxSize(3).withInput(tokens);
    CompositeSparseVector combinedFeatures = new CompositeSparseVector().withInputs(ngramFeatures, leafFeatures);
    LiblinearClassification<String> prediction = new LiblinearClassification<String>().withFeatureInput(combinedFeatures).withLabelInput(label);

    DAG2x1<String, LabelType, DiscreteDistribution<String>> model = DAG.withPlaceholders(text, label).withOutput(prediction);   
    DAG2x1.Prepared<String, LabelType, DiscreteDistribution<String>> trainedModel = model.prepare(textList, labelList);
    LabelType prediction = trainedModel.apply("Some text for which to predict a label", null);
    // trainedModel can now be serialized and later loaded on a server, in a CLI app, within a Hive UDF, etc. 

This code is fairly minimal; Dagli also provides mechanisms to more elegantly encapsulate example data 
([@Structs](documentation/structs.md)), read in data (e.g. from delimiter-separated value or Avro files), evaluate model 
performance, and much more.  You can find demonstrations of these among the 
[many code examples provided with Dagli](documentation/examples.md).    

# Benefits
- Write your machine learning pipeline as a directed acyclic graph (DAG) **once** for both training and inference.  No 
need to specify a pipeline for training and a separate pipeline for inference.  You define it, train it, and predict 
with a single pipeline definition.
- Bug-resiliency: easy-to-read ML pipeline definitions, ubiquitous static typing, and most things in Dagli are 
**immutable**.
- Portability: works on your server, in a Hadoop mapper, a CLI program, in your IDE, etc. on any platform* <small>(some 
models, such as XGBoost, have native code dependencies that may have limited support beyond Linux/Windows/Mac)</small> 
- Deployability: an entire pipeline is serialized and deserialized as a single object
- Abstraction: creating new transformations and models is straightforward and these can be reused in any Dagli pipeline
- Speed: highly parallel multithreaded execution, graph (pipeline) optimizations, minibatching
- Inventory: many, many useful pipeline components ready to use, right out of the box.  Neural networks, logistic 
regression, gradient boosted decision trees, FastText, cross-validation, cross-training, feature selection, data 
readers, evaluation, feature transformations...
- Java: easily use from any JVM language with the support of your IDE's code completion, type hints, inline 
documentation, etc.

# Overview
As might be surmised from the name, 
[Dagli represents machine learning pipelines as directed acyclic graphs](documentation/dag.md) (DAGs).

- The "roots" of the graph 
    - `Placeholder`s (which represent the training and inference example data)
    - `Generator`s (which automatically generate a value for each example, such as a `Constant`, `ExampleIndex`, 
    `RandomDouble`, etc.)
- Transformers, the "child nodes" of the graph
    - Data transformations (e.g. `Tokens`, `BucketIndex`, `Rank`, `Index`, etc.)
    - Learned models (e.g. `XGBoostRegression`, `LiblinearClassifier`, `NeuralNetwork`, etc.)


Transformers may be *preparable* or *prepared*.  Dagli uses the word "preparation" rather than "training" because many 
`PreparableTransformer`s are not statistical models; e.g. `BucketIndex` examines all the preparation examples to find 
the optimal bucket boundaries with the most even distribution of values amongst the buckets.

When a DAG is prepared with training/preparation data, the `PreparableTransformer`s (like `BucketIndex` or 
`XGBoostRegression`) become `PreparedTransformer`s (like `BucketIndex.Prepared` or `XGBoostRegression.Prepared`) which 
are then subsequently used to actually transform the input values (both during DAG preparation so the results may be fed
to downstream transformers and later, during inference in the prepared DAG).

Of course, many transformers are already "prepared" and don't require preparation; a prepared DAG containing no 
preparable transformers may be created directly (e.g. `DAG.Prepared.withPlaceholders(...).withOutputs(...)`) and used to
transform data without any preparation/training step. 

DAGs are encapsulated by a `DAG` class corresponding to their input and output arities, e.g. `DAG2x1<String, Integer, 
Double>` is a pipeline that accepts examples with a `String` and `Integer` feature and outputs a `Double` result.  
Generally, it's better design to provide all the example data together as a single [@Struct](documentation/structs.md) 
or other type rather than as multiple inputs.  DAGs are themselves transformers and can thus be embedded within other, 
larger DAGs.

# Examples
Probably the easiest way to get a feel for how Dagli models are written and used is from the 
[numerous code examples](documentation/examples.md).  The example code is more verbose than would be seen in practice, 
but--combined with explanatory comments for almost every step--these can be an excellent pedagogic tool.

# Finding the Right Transformer
Dagli includes a large and growing library of transformers; unfortunately, this can sometimes make it challenging to 
find one that does what you want.

Other than the [examples](documentation/examples.md), the [module summary](documentation/modules.md) should give you a 
good idea of where to look.  We plan to eventually catalog available transformers in a more comprehensive way to make 
discovery more straightforward.

# Adding New Transformers
If an existing transformer doesn't do what you want, you can often wrap an existing function/method with a 
`FunctionResultX` transformer (where `X` is the function's arity, e.g. 1 or 4).  Otherwise, it's 
[easy to create your own transformers](documentation/transformers.md).  

# Documentation
- [Overview of Dagli Examples](documentation/examples.md)
- [Overview of Dagli Modules](documentation/modules.md)
- [How Dagli Represents ML Pipelines as DAGs](documentation/dag.md)
- [Usage and Creation of Transformers](documentation/transformers.md)
- [@Structs](documentation/structs.md)
- [Using Avro Data with Dagli](documentation/avro.md)
- [Planned Improvements](documentation/todo.md)

# Comparison with Other ML Solutions
Other solutions also offer the ability to define computation graphs as DAGs, either joint models (TensorFlow, PyTorch,
DeepLearning4J...) or pipelined models (Hadoop, Spark, Kubeflow...).  Dagli is primarily a framework for pipelined 
models exploiting the high parallelism possible on high-core-count modern hardware, but with the ability to seamlessly
define novel (joint) neural network model architectures within the pipeline and a focus on experimental velocity, ease
of use and resistance to bugs (e.g. static typing).  [Further discussion](documentation/comparison.md).


# Versioning
Our initial public release is `14.0.0-beta1`.  We begin with a "beta" designation due to extensive changes relative to 
the previous (LinkedIn-internal) version and the greater diversity of applications entailed by a public release.

While in beta, releases with potentially breaking API or serialization changes will be accompanied by a major version 
increment (e.g. `15.0.0-beta1`).  However, once the beta designation is removed, subsequent revisions will maintain full
backward compatibility, allowing large projects to transitively depend on multiple versions of Dagli without dependency 
shading.

# License
Copyright 2020 LinkedIn Corporation.  All Rights Reserved.

Licensed under the BSD 2-Clause license; please see the LICENSE file in this directory for details.