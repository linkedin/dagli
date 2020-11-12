# Dagli

[![Maven badge](https://maven-badges.herokuapp.com/maven-central/com.linkedin.dagli/core/badge.svg)](https://search.maven.org/search?q=g:com.linkedin.dagli)

Dagli is a machine learning framework that makes it easy to write bug-resistant, readable, efficient, maintainable and 
trivially deployable models in [Java 9+](documentation/java.md) (and other JVM languages).

Here's an introductory example of a text classifier implemented as a pipeline that uses the active leaves of a 
Gradient Boosted Decision Tree model (XGBoost) as well as a high-dimensional set of ngrams as features in a logistic 
regression classifier:

    Placeholder<String> text = new Placeholder<>();
    Placeholder<LabelType> label = new Placeholder<>(); 
    Tokens tokens = new Tokens().withInput(text);
    
    NgramVector unigramFeatures = new NgramVector().withMaxSize(1).withInput(tokens);
    Producer<Vector> leafFeatures = new XGBoostClassification<>()
        .withFeaturesInput(unigramFeatures)
        .withLabelInput(label)
        .asLeafFeatures();

    NgramVector ngramFeatures = new NgramVector().withMaxSize(3).withInput(tokens);
    LiblinearClassification<LabelType> prediction = new LiblinearClassification<LabelType>()
        .withFeaturesInput().fromVectors(ngramFeatures, leafFeatures)
        .withLabelInput(label);

    DAG2x1.Prepared<String, LabelType, DiscreteDistribution<LabelType>> trainedModel = 
        DAG.withPlaceholders(text, label).withOutput(prediction).prepare(textList, labelList);
    
    LabelType predictedLabel = trainedModel.apply("Some text for which to predict a label", null);
    // trainedModel now can be serialized and later loaded on a server, in a CLI app, in a Hive UDF...

This code is fairly minimal; Dagli also provides mechanisms to more elegantly encapsulate example data 
([@Structs](documentation/structs.md)), read in data (e.g. from delimiter-separated value or Avro files), evaluate model 
performance, and much more.  You can find demonstrations of these among the 
[many code examples provided with Dagli](documentation/examples.md).    

# Maven Coordinates
Dagli is [split into a number of modules](documentation/modules.md) that are published to 
[Maven Central](https://search.maven.org/search?q=g:com.linkedin.dagli); just add dependencies on those you need in your 
project.  For example, the dependencies for our above introductory example might look like this in Gradle:

    implementation 'com.linkedin.dagli:common:15.0.0-beta4'            // commonly used transformers: bucketization, model selection, ngram featurization, etc.
    implementation 'com.linkedin.dagli:text-tokenization:15.0.0-beta4' // the text tokenization transformer ("Tokens")
    implementation 'com.linkedin.dagli:liblinear:15.0.0-beta4'         // the Dagli Liblinear classification model
    implementation 'com.linkedin.dagli:xgboost:15.0.0-beta4'           // the Dagli XGBoost classification and regression models
    
If you're in a hurry, you can instead add a dependency on `all` (though training neural networks will still 
[require a few platform-specific dependencies](examples/neural-network/build.gradle)):

    implementation 'com.linkedin.dagli:all:15.0.0-beta4'  // not recommended for production due to classpath bloat 
    
# Benefits
- Write your machine learning pipeline as a directed acyclic graph (DAG) **once** for both training and inference.  No 
need to specify a pipeline for training and a separate pipeline for inference.  You define it, train it, and predict 
with a single pipeline definition.
- Bug-resiliency: easy-to-read ML pipeline definitions, ubiquitous static typing, and most things in Dagli are 
**immutable**.
- Portability: works on your server, in a Hadoop mapper, a CLI program, in your IDE, etc. on any platform
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
or other type rather than as multiple inputs.  DAGs are also themselves transformers and can thus be embedded within 
other, larger DAGs.

# Examples
Probably the easiest way to get a feel for how Dagli models are written and used is from the 
[numerous code examples](documentation/examples.md).  The example code is more verbose than would be seen in practice, 
but--combined with explanatory comments for almost every step--these can be an excellent pedagogic tool.

# Finding the Right Transformer
Dagli includes a large and growing library of transformers.  Other than the [examples](documentation/examples.md), the 
[module summary](documentation/modules.md) should give you a good idea of where to look.  In the future a "transformer
catalog" will make discovery more straightforward.

# Adding New Transformers
If an existing transformer doesn't do what you want, you can often wrap an existing function/method with a 
`FunctionResultX` transformer (where `X` is the function's arity, e.g. 1 or 4).  Otherwise, it's 
[easy to create your own transformers](documentation/transformers.md).  

# Documentation
- [Overview of Dagli Examples](documentation/examples.md)
- [Overview of Dagli Modules](documentation/modules.md)
- [How Dagli Represents ML Pipelines as DAGs](documentation/dag.md)
- [Usage and Creation of Transformers](documentation/transformers.md)
- [@Structs: Autogenerated, immutable convenience classes for storing fields](documentation/structs.md)
- [Using Avro Data with Dagli](documentation/avro.md)

# Alternative ML Solutions

Dagli lets Java (and JVM) developers easily define readable, reusable, bug-resistant models and train them efficiently on 
modern multicore, GPU-equipped machines.

Of course, there is no "one size fits all" ML framework.  Dagli provides a layer-oriented API for defining novel neural
networks, but for unusual architectures or cutting-edge research, TensorFlow, PyTorch, DeepLearning4J and others may be 
better options (Dagli supports the integration of arbitrary DeepLearning4J architectures into the model pipeline 
out-of-the-box, and, for example, pre-trained TensorFlow models can also be incorporated with a custom wrapper.)

Similarly, while Dagli models have been trained with *billions* of examples, extremely large scale training across 
multiple machines may be better served by platforms such as Hadoop, Spark, and Kubeflow.  Hadoop/Hive/Spark/Presto/etc. 
are of course commonly used to pull data to train and evaluate Dagli models, but it is also very feasible to, e.g. create
custom UDFs that train, evaluate or apply Dagli models.  

[Further discussion comparing extant pipelined and joint modeling with Dagli](documentation/comparison.md).


# Version History
- `15.0.0-beta4`: *11/11/20*: `xgboost` now bundles in [support for Windows](xgboost/README.md)
- `15.0.0-beta3`: *11/9/20*: Input Configurators and `MermaidVisualization`
    - This is a major version increment and may not be compatible with models from 14.*
    - Input configurators for more convenient, readable configuring of transformer inputs; e.g., 
      `new LiblinearClassification<LabelType>().withFeaturesInput().fromNumbers(numberInput1, numberInput2...)...`
    - New graph visualizer for rendering Dagli graphs as Mermaid markup
    - [Full list of improvements](documentation/v15.0.0-beta3.md)
- `14.0.0-beta2` *9/27/20*: update dependency metadata to prevent the annotation processors' dependencies from 
  transitively leaking into the client's classpath  
- `14.0.0-beta1`: initial public release

## Versioning Policy
Dagli's current public release is designated as "beta" due to extensive changes relative to previous 
(LinkedIn-internal) releases and the greater diversity of applications entailed by a public release. 

While in beta, releases with potentially breaking API or serialization changes will be accompanied by a major version 
increment (e.g. `14.0.0-beta2` to `15.0.0-beta3`).  After the beta period concludes, subsequent revisions will be backward
compatible to allow large projects to depend on multiple versions of Dagli without dependency shading.

# License
[Licensed under the BSD 2-Clause license](LICENSE).

Copyright 2020 LinkedIn Corporation.  All Rights Reserved.
