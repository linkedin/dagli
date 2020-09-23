# Transformers
Fundamentally, a `Transformer` accepts inputs from other components of an ML pipeline (often other transformers) and produces an output.  The number of inputs to the transformer is referred to as its *arity*.
 
Transformers will usually comprise the large majority of a pipeline, and include statistical models (e.g. `NeuralNetwork`, `FastTextClassification`...), feature transformations (e.g. `Tokens`, `BucketIndex`...), and many other operations (e.g. `MultinomialEvaluation`, which produces a `MultinomialEvaluationResult` summarizing model performance given the true and predicted labels as inputs).

All transformers are either `PreparedTransformer`s, which are immediately able to produce a result for each sequence of inputs, and `PreparableTransformer`s, transformers that examine their inputs across all examples and "prepare to" a `PreparedTransformer`.

[More about transformers' role in the ML pipeline DAG.](dag.md)

# Properties of Transformers
Transformers must be:
- Immutable: user-visible properties and behaviors of a given instance will always be consistent
- Thread-safe: can be safely used in parallel by multiple threads without exogenous locking
- Serializable
- Quasi-deterministic: the results of applying the transformer to the same inputs should always be equally valid<sup>1</sup>, no matter when or in what context the transformer is used.  A transformer that used the current time to generate its result would violate this rule, whereas a transformer that accepted a time as an input would not.

<sup>1</sup><small> In almost every case the results should, in fact, be `equals(...)`, but there may be exceptions; e.g. repeated FastText model training may result in different (but equally valid) learned parameters due to FastText's "hogwild" updates.</small>

# Arities
The arity of a transformers may be categorized into three categories:
- "Fixed-arity" transformers, e.g. `PreparedTransformer2`, accept a fixed number of inputs each of a known, static type.  These are (by far) the most common type of transformer.
- *Variadic* transformers, e.g. `PreparableTransformerVariadic`, accept any number of inputs of the same, static type.
- *Dynamic* transformers, e.g. `PreparedTransformerDynamic`, accept any number of inputs of types that are not statically known (by the compiler.)  Because of the increased burden on the implementor to ensure type safety, these should be avoided if possible.

Note that, as far as the **user** of a transformer is concerned, the "real" arity doesn't matter: a transformer might have an actual arity of 4 but offer only `withLabelInput(...)` and `withFeaturesInput(...)` methods through its public API (and thus appear to accept only two inputs).  This discrepancy is fairly common since transformers may want to set their "hidden" inputs themselves for a variety of reasons (e.g. a transformer might create inputs for the `Max` and `Min` of another, user-specified `Long` input to allow it know the maximum and minimum `Long` value provided). 
  
# Prepared Transformers
Prepared transformers are essentially functions: they accept inputs and produce an output.  However, they can sometimes be a little more complicated than this implies when they use minibatching or benefit from caching data across examples (e.g. to reuse thread-local buffers).  

Though prepared transformers are typically used only as part of a DAG, they may also be used directly via their `apply(...)` and `applyAll(...)` methods (the latter may be more efficient if you wish to transform multiple examples).  These methods are most commonly used on (prepared) DAG instances themselves to do prediction with the trained DAG (since DAGs are also transformers).  

## Creating a New Prepared Transformer

### Alternatives
Rather than creating a new transformer, it's possible to wrap a "safely-serializable" function (a function object or a method reference, but not a lambda) as a `FunctionResultX` transformer, like so:
    
    FunctionResult1<String, Integer> stringLength = new FunctionResult1<>(String::length).withInput(stringProducer);
    
This is convenient for transformers that correspond to existing, simple functions that have no configurable properties.

### An Example

Most transformers will extend `AbstractPreparedTransformerN` (where N ∈ [1, 10] is the arity), which we'll demonstrate here.  Examples of transformers extending `AbstractPreparedTransformerVariadic` and `AbstractPreparedTransformerDynamic` may be found in `dagli/common/src`.

    @ValueEquality // mark this transformer as being equal to another of the same class if all their fields compare as equals()
    public class TextLength extends AbstractPreparedTransformer1<CharSequence, Integer> {
        private static final long serialVersionUID = 1;
        
        private boolean _countLogicalCharacters = false;
        
        // recall that transformers are immutable: to configure a property, we return a (modified) clone:
        public TextLength withCountLogicalCharacters(boolean countLogicalCharacters) {
            return clone(c -> c._countLogicalCharacters = countLogicalCharacters);
        }
        
        // for transformers of arity 1, like this one, we could have instead extended the special base class 
        // AbstractPreparedTransformer1WithInput which would have defined this method for us:
        public TextLength withInput(Producer<? extends CharSequence> charSequence) {
            // use the base class method withInput1 to return a copy of this instance that accepts the passed input:
            return withInput1(charSequence)
        }
        
        // transformers often provide "convenience methods" for configuring inputs, like this one:
        public TextLength withObjectInput(Producer<?> objectInput) {
            return withInput(new FunctionResult1<>(Object::toString).withInput(objectInput));
        }
        
        public apply(CharSequence text) {
            return _countLogicalCharacters ? text.codePoints().count() : text.length();
        } 
    }

### Minibatching and Caching
Transformers that cache data and/or benefit from minibatching (multiple examples at once) inference should extend `AbstractPreparedStatefulTransformerN`, e.g. `AbstractPreparedStatefulTransformer3`.  Please see the class `dagli/examples/assorted/src/main/java/com/linkedin/dagli/assorted/IsPatternMatch.java` for an example.

# Preparable Transformers

`PreparableTransformer`s implement a `prepare(...)` method that provides a `Preparer`.  When a DAG is prepared (trained), the `Preparer` examines all the preparation/training data during DAG preparation and then creates a `PreparedTransformer`.

## "For Preparation Data" vs. "For New Data"
Usually, a `Preparer` only creates a single `PreparedTransformer`, which is then used to transform the data that was just used to prepare it (so the results may then be used as inputs to downstream transformers as DAG preparation continues) as well future, as-yet-unseen examples in the ultimate prepared DAG.  In rare cases, the transformer used for each of these cases needs to be different; please see the `KFoldCrossTrained` transformer for an example.

## *Batch* vs. *Stream* Preparers   

`Preparer`s may be either *batched* or *streaming*, e.g. `AbstractBatchPreparer3` and `AbstractStreamPreparer3` for transformers of arity 3.

- Batch preparers see each example one-by-one via their `process(...)` method and, in their `finish(...)` method, also receive an `ObjectReader` that allows them to iterate through all preparation data as many additional times as needed.
- Stream preparers only see examples via their `process(...)` method and have a parameterless `finish()` method. 

Prefer com.linkedin.dagli.util.stream preparers when you only need to see each example once: they are faster and may allow Dagli to avoid caching the preparable transformer's input data. 

## Example
Please see `dagli/examples/assorted/src/main/java/com/linkedin/dagli/assorted/NormalizedDouble.java`.

# Testing
Transformers may be conveniently tested via the `com.linkedin.dagli.tester.Tester` class, e.g.

    Tester.of(new NormalizedDouble())
        .input(5.0)
        .input(-5.0)
        .input(Double.POSITIVE_INFINITY)
        .output(0.0)
        .output(0.0)
        .output(Double.NaN)
        .test();

Please see `dagli/examples/assorted/src/test/java/com/linkedin/dagli/assorted/NormalizedDoubleTest.java` for a more complete example.

# Advanced Transformer Topics

## Always-Constant-Result
*Always-constant-result* transformers produce, within a given DAG execution, the same result for all examples.  Normally you would never implement an always-constant-result transformer yourself, but would rather use the `ConstantResultTransformationX` classes (where X is the arity: 1-10, Variadic or Dynamic).

The benefit of using `ConstantResultTransformerX` is that this may allow for certain aggressive DAG optimizations that will greatly reduce or entirely eliminate the computational cost of the transformer.

## Idempotent Preparers
Preparable transformers may override the `hasIdempotentPreparer()` method to return true when the transformer's `Preparer` is not affected by duplicate sets of inputs (for example, the `Min` and `Max` preparers produce the same prepared transformer no matter how many times the input value `6` is seen after the first `6`).

The advantage is that knowing a preparer is idempotent allows certain useful graph reductions (e.g. if Dagli can infer all the inputs to an idempotent preparable transformer are *constant-value*, it can prepare it without executing the DAG because the result will not depend on the number of examples). 

## Reductions
All Dagli nodes, including transformers, can provide *reducers* to optimize DAGs containing those nodes.  For example, `com.linkedin.dagli.object.ConditionalValue` has a reducer that replaces the transformer with either its if-true or if-false producer input in the DAG if the conditional input is a `Constant` (because Dagli does extensive "constant folding", any producer whose constant-value output can be determined before executing the DAG will be replaced by a `Constant` instance).