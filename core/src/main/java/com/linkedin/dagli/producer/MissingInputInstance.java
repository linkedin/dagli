package com.linkedin.dagli.producer;

import com.linkedin.dagli.dag.Graph;
import com.linkedin.dagli.handle.ProducerHandle;
import com.linkedin.dagli.producer.internal.ProducerInternalAPI;
import com.linkedin.dagli.reducer.ClassReducerTable;
import com.linkedin.dagli.reducer.Reducer;
import java.util.Collection;


/**
 * An enum which is used to provide the {@link MissingInput} singleton.
 */
enum MissingInputInstance implements MissingInput<Object> {
  INSTANCE;

  // the handle UUID is fixed (because this is a singleton) as an arbitrary value (that was generated randomly)
  private static final ProducerHandle<? extends com.linkedin.dagli.producer.MissingInput> MY_HANDLE =
      new ProducerHandle<>(com.linkedin.dagli.producer.MissingInput.class, 7561596932544414700L, -6205292413973495837L);

  private static final ProducerInternalAPI<Object, MissingInputInstance> MY_API =
      new ProducerInternalAPI<Object, MissingInputInstance>() {
        @Override
        public boolean hasAlwaysConstantResult() {
          return false;
        }

        @Override
        public Collection<Reducer<? super MissingInputInstance>> getGraphReducers() {
          throw new UnsupportedOperationException("Attempting to reduce a graph containing a MissingInput");
        }

        @Override
        public ClassReducerTable getClassReducerTable() {
          throw new UnsupportedOperationException("Attempting to reduce a graph containing a MissingInput");
        }

        @Override
        public ProducerHandle<MissingInputInstance> getHandle() {
          return (ProducerHandle<com.linkedin.dagli.producer.MissingInputInstance>) MY_HANDLE;
        }

        @Override
        public Graph<Object> subgraph() {
          return null;
        }
      };

  @Override
  public String getName() {
    return "Unused Input";
  }

  @Override
  public boolean hasConstantResult() {
    return false;
  }

  @Override
  public String toString() {
    return getName();
  }

  @Override
  public ProducerInternalAPI<Object, MissingInputInstance> internalAPI() {
    return MY_API;
  }


}