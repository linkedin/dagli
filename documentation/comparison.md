# Comparison with Other ML Solutions
Dagli is a way of assembling pipelined models as directed acyclic graphs of transformer "nodes" representing statistical 
models, feature generators, tokenizers, etc.  Other solutions that use a similar abstraction can be partitioned by 
whether they primarily target joint or pipelined models.  

### Joint Computation Graphs
Deep learning frameworks like TensorFlow, PyTorch and DeepLearning4J either implicitly or explicitly create DAGs that 
represent a (typically) joint model, such as a multilayer perceptron implemented as an input layer connected to one or 
more hidden layers connected to an output layer.

These are great for building statistical models which can be part of a larger pipelined model (such as one constructed 
in Dagli) but tend to be relatively complex to learn and may lack the efficiency of more specialized, purpose-built 
implementations like Liblinear.  Adding new functionality to the computational graph (such a new feature transformer or 
a new, non-neural statistical model) may also be relatively challenging.

### Pipelined Transformer Graphs
Conversely, platforms like Hadoop, Spark and Kubeflow schedule a set of interdependent transformations as a DAG.  These 
transformations are largely treated as "black boxes", which gives these approaches great flexibility, but also limits 
opportunities for optimization.  These systems often excel at distributed processing of and training on very large-scale 
data, which allows them to scale indefinitely but may also entail slower iteration and implementation of new model
architectures and transformers due to the ensuing overhead and boilerplate. 

### Dagli
Dagli also builds a graph of pipelined transformers, but with a much greater degree of transparency into the DAG.  Dagli 
graphs are all statically typed, and Dagli understands (at a high-level) what a node is doing and how it's doing it.  

Consequently, Dagli can provide faster single-machine training and inference by achieving better parallelism and, 
perhaps more importantly, Dagli models are easier to write, read, experiment with, train and deploy.  Through 
layer-oriented neural network support (backed by DeepLearning4J) Dagli also adds the convenience of defining a neural
(joint) network model architecture seamlessly within the wider pipelined DAG.
