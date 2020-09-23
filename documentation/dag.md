# Models as Directed Acyclic Graphs
In Dagli, models are created as directed acyclic graphs (DAGs), from which Dagli takes its name ("**DAG**" + "**L**inked**I**n").  The roots of this graph (nodes with no parents) are either `Placeholder`s or `Generator`s that provide values for their "children".  These child nodes, `Transformer`s and `View`s, transform the inputs received from their parents into new results.  The *directed* edges between the nodes specify the flow of results from a parent node to its children.  Because the graph is *acyclic*, no node can have itself as an ancestor (which would imply an infinite loop of computation).

In the remainder of this document, we'll talk about the five types of nodes: `Placeholder`s, `Generator`s, `PreparedTransformer`s, `PreparableTransformer`s, and `View`s.

# Flow of Data in the DAG
During training and inference, one or more *examples* is provided to the DAG.  Each example provides a set of values that fill the `Placeholder` placeholders (and are roots of the graph).

Every type of node in Dagli *produces* a result value that becomes an input to its children (if any).  The "result" of a `Placeholder` root node is just the value provided as part of the example, while the result of a `Generator` root node is automatically generated.  The results of the `Transformer` and `View` child nodes are produced by transforming the input values received from their parent nodes.

A DAG always has one or more *output nodes*.  Any type of node can be an output.   The results produced by these nodes for each example become the results of the DAG.  In Dagli, it is always the case that:
- Every example has a result for every output node (e.g. no "skipping" or discarding examples is possible)
- The ordering of the results always matches the ordering of the examples.  Results are never "reordered".  E.g. the 5th result of a DAG always corresponds with the 5th example.

# Root Nodes
## Placeholders
A `Placeholder` is simply a placeholder for a value that will be provided for each example.  If we view examples as rows, `Placeholder`s would be the columns.  A very simple DAG for logistic regression to predict `String` labels might have, e.g. a `Placeholder<String>` for the label and a `Placeholder<Vector>` for the feature vector.

### Design best practice: keep the number of placeholders small, use [@Structs](structs.md)
Because DAGs have a maximum arity (number of inputs) of 10, a DAG cannot have more than 10 `Placeholder`s.  However, it's generally not a good idea to use more than ~3 for a given DAG because the lists of values for each `Placeholder` are provided as ordinal lists (e.g. `myDAG.prepare(valuesForPlaceholder1, valuesForPlaceholder2, ...)`: with more `Placeholder`s it becomes more likely that there could be two lists of values with the same type that could be incorrectly swapped, creating a logic bug.

Instead, use a smaller number of placeholders to provide richer, structured inputs.  In particular, in many cases you can use a single @Struct to define a type for your examples that contains all the needed fields, e.g.

	@Struct("Example")
	abstract class ExampleBase {
		Duration _age;
		float _annualIncomeInUSD;
		float _nauticalSpendingInUSD;
		
		@Optional
		boolean _ownsBoat; // the label of interest
	}

With this @Struct, our boat-ownership-prediction model needs only one placeholder: `Placeholder<Example>`, and the examples themselves are constructed using relatively foolproof builders, e.g.:

	Example trainingExample = Example.builder()
		.setAge(45)
		.setAnnualIncomeInUSD(100000)
		.setNauticalSpendingInUSD(10000)
		.setOwnsBoat(true)
		.build();

The examples are then provided to the DAG as a single list of values:

	boatOwnershipPredictor.prepare(trainingExamples);

Compare this to passing lists of these values to the DAG directly:

	boatOwnershipPredictor.prepare(ages, annualIncomes, nauticalSpendings, boatOwnerships);

(Note that the second and third lists could easily be swapped, creating a hard-to-find logic bug.)

@Structs are recommended because they provide a great deal of convenience (e.g. a transformer that extracts the annual income from an Example object will be automatically generated as an inner class called `Example.AnnualIncomeInUSD`), but of course they're not a requirement: your `Placeholder`s can provide any kind of input to your DAG that you'd like.

## Generators
A `Generator` is the other type of root node in Dagli.  Unlike a `Placeholder`, whose values come from data provided when training or doing inference (preparing or applying the DAG, respectively), a `Generator` produces its values automatically.

For example, a `Constant<String>` produces a single, constant `String` value, whereas an `IndexGenerator` produces an incrementing value for each subsequent example (0, 1, 2...).

Common uses for generators include:
- Providing a constant input to a transformer
- Using the output of `IndexGenerator` to partition examples into pseudorandomly assigned sets, e.g. for cross-training.
- Using `NullGenerator` to pass null values to an unused transformer input (for example, `LiblinearClassification.Prepared` does not use its "label" input and a null can be passed).

# Child Nodes
Child nodes, unlike root nodes, have parents.  The number of parents a child node has is called its *arity* (note that all types of nodes may have any number of children).  `Transformer`s can have a fixed arity from 1 to 10, a *variadic* arity (having any number of parents providing the same type of value), or a *dynamic* arity (any number of parents providing any types of values).

Dynamic arity is intended for situations when the arity cannot be known at compile-time, usually because the transformer is "inheriting" its inputs from other, wrapped transformers.  For example, `BestModel` performs cross-validation to find the best model of a set of models, and its inputs are all the (aggregated, unique) inputs to those models.  If possible, avoid dynamic arity and used a fixed or variadic arity instead.

## PreparableTransformer
`Transformer`s in Dagli are either `PreparedTransformer`s or `PreparableTransformer`s.  A `PreparableTransformer` can't be directly applied to inputs to get a result, but it can instead be *prepared* to yield a `PreparedTransformer`.  Dagli formally calls this "preparation" rather than "training" because many things other than models (like `LiblinearClassifier`) can be prepared, such as `Index` (which maps every distinct object seen during preparation to a unique integer).

Any DAG that contains a `PreparableTransformer` is itself preparable (and, since DAGs are also transformers, also a `PreparableTransformer`).  A preparable DAG (and the `PreparableTransformer`s it contains) can be created and prepared as follows:

	myModel = DAG.withPlaceholders(myPlaceholder1, myPlaceholder2...).withOutputs(myOutput1, myOutput2, ...);
	myTrainedModel = myModel.prepare(inputList1, inputList2, ...)

The types of `myModel` and `myTrainedModel` are omitted as they depend on the number of placeholders and outputs; for example, with two placeholders and two outputs the types would be `DAG2x2<TPlaceholder1, TPlaceholder2, TOutput1, TOutput2>` and `DAG2x2.Prepared<TPlaceholder1, TPlaceholder2, TOutput1, TOutput2>`, respectively.  

Note that transformers always produce a **single** result value, and since DAGs are transformers, `DAG2x2<TPlaceholder1, TPlaceholder2, TOutput1, TOutput2>`  implements `PreparableTransformer2<TPlaceholder1, TPlaceholder2, Tuple2<TOutput1, TOutput2>>`.  Its result type when used as a transformer inside a larger graph is thus actually a `Tuple2<TOutput1, TOutput2>`.

### How Preparation Works
Under the hood, Dagli uses `DAGExecutor`s for both preparing (training) and applying (inference) on DAGs.  `PreparableTransformer`s provide a `getPreparer(...)` method that is called by the `DAGExecutor` to get a `Preparer` instance.  This `Preparer` is then fed all the values from the `PreparableTransformer`'s inputs, ultimately producing a new `PreparedTransformer` instance.  When all the `PreparableTransformer`s in the graph have been prepared and replaced by their resultant `PreparedTransformer`s, the (now prepared) DAG is returned.

## PreparedTransformers
`PreparedTransformer`s are transformers that are ready to transform their inputs into results immediately, without requiring further preparation on training data.  As discussed in the previous section, many `PreparedTransformer`s are the result of preparing corresponding `PreparableTransformer`s, but others (e.g. `NgramVector`) never require preparation, and, regardless, it's often possible to create the `PreparedTransformer` directly even if it is *usually* the result of preparation (e.g. `XGBoostClassification.Prepared` can be created from an existing XGBoost model [a `Booster` object]).

`PreparedTransformer`s themselves are essentially functions: their `apply(...)` method receives values for each of their inputs, and returns a result.

## Views
A `View` always has a single parent, a `PreparableTransformer`.  After the `PreparableTransformer` is prepared, the resulting `PreparedTransformer` is "observed" by the `View`, which produces some resultant value.  This value is then provided as a constant (same for every example) input to all the `View`'s children.

`View`s are a relatively simple mechanic, but allow information about `PreparedTransformer`s prepared from `PreparableTransformer`s to be processed by downstream nodes in the graph *during preparation*.  This is quite powerful and can enable functionality that would otherwise be impractical to implement.

### Example: FastTextClassification and ModelView
Oftentimes, `View`s are used "behind the scenes".  This is the case with many of `FastTextClassification`'s methods.  `FastTextClassification` is a `PreparableTransformer` that trains a FastText classifier (which predicts a a multinomial label for a sequence of text tokens).  During training, `FastTextClassification` learns embeddings for each token; these embeddings are provided as the result of a transformer obtained from by calling `FastTextClassification::asEmbeddedTokens()`, which transforms the token inputs to the original `FastTextClassification` transformer into their  embeddings.  This is done with the help of a `FastTextClassification.ModelView` (a private inner class) instance, using the original `FastTextClassification` instance as its input.  This `ModelView` extracts the trained model object from the classifier (which includes the embeddings), and an `Embedding.Features` transformer (which has two parents: the `ModelView` and the tokens) receives this model object as one of its inputs, together with the tokens, and essentially just looks up the embeddings for each token from the model and returns them as its result.

It may seem expensive (and perhaps somewhat odd) to pass the (same) model data from the `ModelView` to its `Embedding.Features` child for every example, but in fact this is extremely cheap: the `DAGExecutor` will compute the result of the `ModelView` once and the cached value is then fed to its children repeatedly as needed. 

# Immutability and Serializability
Like most Dagli objects, all nodes in the graph are both immutable (their logical state cannot be changed--this prevents many kinds of common bugs) and `Serializable` (this allows DAGs--both preparable and prepared--to be saved and loaded later using standard Java serialization).