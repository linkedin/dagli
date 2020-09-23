package com.linkedin.dagli.handle;

import com.linkedin.dagli.producer.Producer;


/**
 * Handle used to retrieve, from a prepared DAG, the result of preparing a transformer or transformer view in the
 * original preparable DAG.  It can also be used to retrieve root nodes (e.g.
 * {@link com.linkedin.dagli.placeholder.Placeholder}s and {@link com.linkedin.dagli.generator.Generator}s) but this
 * is equivalent to just using the root node's {@link ProducerHandle} because root nodes are not changed when the DAG
 * is prepared.
 *
 * Let's say transformer T is part of DAG D.  DAG D is then prepared, creating a new DAG, E, with a (possibly new)
 * prepared transformer U that was created from T.  For example, if T is a XGBoostClassification, U would be a new
 * XGBoostClassification.Prepared transformer.
 *
 * Notes:
 * (1) If the original transformer (in the DAG being prepared) was a prepared transformer, the corresponding prepared
 *     transformer in the prepared DAG can be (and often is) not the same as the original because the inputs to the
 *     transformer may be different.  However, it will be otherwise logically equivalent.
 * (2) For Generators and Placeholders, the corresponding producer in the prepared DAG is always the original Generator
 *     and Placeholder.  Consequently, the node retrieved using this handle is the same as would be retrieved using
 *     a {@link ProducerHandle}.
 * (3) For {@link com.linkedin.dagli.view.TransformerView}s, the prepared type is always a
 *     {@link com.linkedin.dagli.generator.Constant} (a type of {@link com.linkedin.dagli.generator.Generator})
 *
 * @param <T> the type of node that will be retrieved with this handle.  This may be a supertype of the
 *           actual type(s) that will be returned.
 */
public class PreparedHandle<T extends Producer>
  extends AbstractProducerUUIDHandle<T, PreparedHandle<T>> {
  private static final long serialVersionUID = 1;

  private final String _originalProducerClassName;

  /**
   * Gets the handle of the "original" producer that "prepared" into the producer to which this handle points.
   *
   * Note that it is possible for both the original and prepared producers to be the same instance.
   *
   * @return the handle to the "original" producer
   */
  public ProducerHandle<?> getOriginalProducerHandle() {
    return new ProducerHandle<>(_originalProducerClassName, this.getUUIDMostSignificantBits(),
        this.getUUIDLeastSignificantBits());
  }

  /**
   * Creates a new handle to prepared transformers that have been prepared from a transformer with the specified UUID.
   *
   * @param targetProducerClass the type of producer that will be addressed by this handle
   * @param originalProducerClassName the name of the original class from which the target producer was prepared
   * @param mostSignificantBits the most significant bits of the UUID
   * @param leastSignificantBits the least significant bits of the UUID
   */
  private PreparedHandle(Class<T> targetProducerClass, String originalProducerClassName, long mostSignificantBits, long leastSignificantBits) {
    super(targetProducerClass.getName(), mostSignificantBits, leastSignificantBits);
    _originalProducerClassName = originalProducerClassName;
  }

  /**
   * Creates a new handle to a "prepared" {@link Producer} based on the handle of the "original" producer.  Note that,
   * as explained in the documentation of this class, "prepared" refers to the node as it exists in a prepared DAG
   * created from a preparable DAG and not only the transformation of a
   * {@link com.linkedin.dagli.transformer.PreparableTransformer} to a
   * {@link com.linkedin.dagli.transformer.PreparedTransformer}.  For example, a
   * {@link com.linkedin.dagli.transformer.PreparedTransformer} can "prepare" to another
   * {@link com.linkedin.dagli.transformer.PreparedTransformer} because its inputs changed, and a
   * {@link com.linkedin.dagli.generator.Generator} will "prepare" to itself (because root nodes will remain the same).
   *
   * @param preparedProducerClass the class of the "prepared" producer that will be fetched by this handle
   * @param originalProducerHandle the handle of the original, pre-preparation producer
   */
  public PreparedHandle(Class<T> preparedProducerClass, ProducerHandle<?> originalProducerHandle) {
    this(preparedProducerClass, originalProducerHandle.getTargetClassName(),
        originalProducerHandle.getUUIDMostSignificantBits(), originalProducerHandle.getUUIDLeastSignificantBits());
  }
}