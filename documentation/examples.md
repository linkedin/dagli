# Code Examples
Dagli provides a number of well-commented examples in the [`dagli/examples/`](../examples) directory.  These are 
generally toy problems but can be a useful guide when getting started with Dagli.

## FastText and Avro
Located in [`dagli/examples/fasttext-and-avro`](../examples/fasttext-and-avro/src/main/java/com/linkedin/dagli/examples/fasttextandavro), 
this example reads dialog from Shakespearean plays from an Avro record using a @Struct, and then learns a FastText model 
to predict a character given their dialog (e.g. "A rose by any other name would smell as sweet" -> "Juliet").
- FastText text classification model
- Reading Avro data with @Structs
- Evaluating, serializing, deserializing and applying the trained model

## Complex Pipelined Model
Located in [`dagli/examples/complex-model`](../examples/complex-model/src/main/java/com/linkedin/dagli/examples/complexmodel), 
this example reads dialog from Shakespearean plays from a tab-separated value file and also learns to predict a 
character given their dialog.
- Pipelined text classification model employing both FastText and XGBoost
- Creating and combining feature vectors
- Using `BestModel` for model selection
- Reading delimited-separated values (DSV) with `DSVReader`
- Evaluating, serializing, deserializing and applying the trained model

## Neural Network
Located in [`dagli/examples/neural-network`](../examples/neural-network/src/main/java/com/linkedin/dagli/examples/neuralnetwork), 
several types of neural network classification models are demonstrated:
- A bag-of-ngrams feed-forward model
- A pseudo-Transformer-encoder model using multi-headed attention
- A letter-by-letter LSTM (RNN) model

## XGBoost + Liblinear
Located in [`dagli/examples/xgboost-and-liblinear`](../examples/xgboost-and-liblinear/src/main/java/com/linkedin/dagli/examples/complexmodel), 
this example demonstrates a common pipelined model in industry: training an XGBoost classifier and then using the active 
leaves for each example as features in a downstream logistic regressor (together with many other features).

## Assorted
Located in [`dagli/examples/assorted`](../examples/assorted/src/main/java/com/linkedin/dagli/assorted), this is a 
collection of smaller and simpler examples:
- Definition of new, prepared transformers
- A pipelined model that uses FastText embeddings in a logistic regression classifier
- A logistic regression model using Word2Vec pretrained embeddings
- A straightforward XGBoost model