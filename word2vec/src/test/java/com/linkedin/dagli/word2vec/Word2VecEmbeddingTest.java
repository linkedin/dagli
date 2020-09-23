package com.linkedin.dagli.word2vec;

import com.linkedin.dagli.math.vector.DenseFloatBufferVector;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class Word2VecEmbeddingTest {
  @Test
  @Disabled // requires embedding file to be present as a resource
  public void testBasic() {
    Word2VecEmbedding embed = new Word2VecEmbedding().withMaxDictionarySize(50000);

    List<DenseFloatBufferVector> sanEmbed = embed.apply(Arrays.asList("San"));
    List<DenseFloatBufferVector> sfEmbed = embed.apply(Arrays.asList("San", "Francisco"));
    List<DenseFloatBufferVector> sfrEmbed = embed.apply(Arrays.asList("San", "Francisco", "rules"));

    assertEquals(sanEmbed.size(), 1);
    assertEquals(sfEmbed.size(), 1);
    assertEquals(sfrEmbed.size(), 2);

    assertTrue(sanEmbed.get(0).size64() == 300);
    assertTrue(sfEmbed.get(0).size64() == 300);
    assertTrue(sfrEmbed.get(0).size64() == 300);
    assertEquals(sfEmbed.get(0), sfrEmbed.get(0));
  }
}
