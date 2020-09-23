package com.linkedin.dagli.annotation.processor.struct;

import com.linkedin.dagli.annotation.processor.ProcessorConstants;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.WildcardTypeName;
import java.util.Collections;
import java.util.HashSet;


/**
 * Static utility class that supports @Struct annotation processing, defining various name and class constants.
 */
abstract class StructConstants {
  private StructConstants() { }

  // Fields that should be copied from an extended class to the derived class for reasons that are likely stupid.
  // SCHEMA$: used by Avro, which checks for this field via getDeclaredField(...) rather than getField(...)
  static final HashSet<String> COPIED_STATIC_FIELDS = new HashSet<>(Collections.singletonList("SCHEMA$"));

  static final String COMPLETED_BUILDER_INTERFACE_NAME = "CompletedBuilder";
  static final String COMPLETED_ASSEMBLED_BUILDER_INTERFACE_NAME = "CompletedAssembledBuilder";
  static final String ITERABLE_BUILDER_SETTER_INTERFACE_NAME = "ReaderBuilder";
  static final String COMPLETED_ITERABLE_BUILDER_INTERFACE_NAME = "CompletedReaderBuilder";
  static final String BUILDER_CLASS_NAME = "Builder";
  static final String ASSEMBLED_BUILDER_CLASS_NAME = "AssembledBuilder";
  static final String DEFAULT_GENERATOR_INTERFACE_NAME = "DefaultGenerator";
  static final String HELPER_CLASS_NAME = "Helper";
  static final String BUILDER_SETTER_INTERFACE_NAME = "Builder";
  static final String SCHEMA_CLASS_NAME = "Schema";
  static final String SCHEMA_BUILDER_SETTER_INTERFACE_NAME = "SchemaBuilder";
  static final String COMPLETED_SCHEMA_BUILDER_INTERFACE_NAME = "CompletedSchemaBuilder";

  static final String DAGLI_TRANSFORMER_PACKAGE = "com.linkedin.dagli.transformer";
  static final ClassName PREPARED_TRANSFORMER_INTERFACE = ClassName.get(DAGLI_TRANSFORMER_PACKAGE, "PreparedTransformer");
  static final ClassName ABSTRACT_PREPARED_TRANSFORMER_DYNAMIC =
      ClassName.get(DAGLI_TRANSFORMER_PACKAGE, "AbstractPreparedTransformerDynamic");
  static final ClassName ABSTRACT_PREPARED_TRANSFORMER_1 =
          ClassName.get(DAGLI_TRANSFORMER_PACKAGE, "AbstractPreparedTransformer1");
  static final ClassName TRANSFORMER_CLASS = ClassName.get(DAGLI_TRANSFORMER_PACKAGE, "Transformer");

  static final String DAGLI_UTIL_PACKAGE = "com.linkedin.dagli.util";
  static final ClassName CLASSES = ClassName.get(DAGLI_UTIL_PACKAGE + ".types", "Classes");
  static final ClassName ABSTRACT_CLONEABLE = ClassName.get(DAGLI_UTIL_PACKAGE + ".cloneable", "AbstractCloneable");

  static final String DAGLI_UTIL_FUNCTION_PACKAGE = "com.linkedin.dagli.util.function";
  static final ClassName FUNCTION1_INTERFACE = ClassName.get(DAGLI_UTIL_FUNCTION_PACKAGE, "Function1");

  static final String DAGLI_GENERATOR_PACKAGE = "com.linkedin.dagli.generator";
  static final ClassName GENERATOR_INTERFACE = ClassName.get(DAGLI_GENERATOR_PACKAGE, "Generator");
  static final ClassName ABSTRACT_GENERATOR = ClassName.get(DAGLI_GENERATOR_PACKAGE, "AbstractGenerator");

  static final String DAGLI_PLACEHOLDER_PACKAGE = "com.linkedin.dagli.placeholder";
  static final ClassName DAGLI_PLACEHOLDER = ClassName.get(DAGLI_PLACEHOLDER_PACKAGE, "Placeholder");

  static final ClassName PRODUCER_INTERFACE = ClassName.get(ProcessorConstants.DAGLI_PRODUCER_PACKAGE, "Producer");
  static final ParameterizedTypeName PRODUCER_INTERFACE_WILDCARD =
      ParameterizedTypeName.get(PRODUCER_INTERFACE, WildcardTypeName.subtypeOf(Object.class));
  static final ClassName MISSING_INPUT = ClassName.get(ProcessorConstants.DAGLI_PRODUCER_PACKAGE, "MissingInput");
  static final ClassName PRODUCER_HANDLE = ClassName.get(ProcessorConstants.DAGLI_PRODUCER_PACKAGE, "ProducerHandle");
  static final ClassName ABSTRACT_PRODUCER = ClassName.get(ProcessorConstants.DAGLI_PRODUCER_PACKAGE, "AbstractProducer");

  static final String DAGLI_PREPARER_PACKAGE = "com.linkedin.dagli.preparer";
  static final ClassName PREPARER_CONTEXT = ClassName.get(DAGLI_PREPARER_PACKAGE, "PreparerContext");
  static final ClassName TRIVIAL_PREPARER = ClassName.get(DAGLI_PREPARER_PACKAGE, "TrivialPreparer");
  static final ClassName PREPARER_INTERFACE = ClassName.get(DAGLI_PREPARER_PACKAGE, "Preparer");

  static final String BATCHABLE_PACKAGE = "com.linkedin.dagli.objectio";
  static final ClassName BATCH_ITERATOR = ClassName.get(BATCHABLE_PACKAGE, "ObjectIterator");
  static final ClassName BATCH_ITERABLE = ClassName.get(BATCHABLE_PACKAGE, "ObjectReader");

  static final String DAGLI_DATA_SCHEMA_PACKAGE = "com.linkedin.dagli.data.schema";
  static final ClassName ROW_SCHEMA_FIELD_SCHEMA_INTERFACE = ClassName.get(DAGLI_DATA_SCHEMA_PACKAGE, "RowSchema", "FieldSchema");
  static final ClassName ROW_SCHEMA_INDEXED_FIELD_INTERFACE = ClassName.get(DAGLI_DATA_SCHEMA_PACKAGE, "RowSchema", "Field", "Indexed");
  static final ClassName ROW_SCHEMA_NAMED_FIELD_INTERFACE = ClassName.get(DAGLI_DATA_SCHEMA_PACKAGE, "RowSchema", "Field", "Named");
  static final ClassName ROW_SCHEMA_ALL_FIELDS_INTERFACE = ClassName.get(DAGLI_DATA_SCHEMA_PACKAGE, "RowSchema", "AllFields");
  static final ClassName ROW_SCHEMA_INTERFACE = ClassName.get(DAGLI_DATA_SCHEMA_PACKAGE, "RowSchema");

  static final String DAGLI_REDUCER_PACKAGE = "com.linkedin.dagli.reducer";
  static final ClassName REDUCER = ClassName.get(DAGLI_REDUCER_PACKAGE, "Reducer");
  static final ClassName INVERSE_CLASS_REDUCER = ClassName.get(DAGLI_REDUCER_PACKAGE, "InverseClassReducer");
}
