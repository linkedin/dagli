package com.linkedin.dagli.text;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.ahocorasick.trie.Emit;


/**
 * Efficiently replaces substrings in the input with corresponding replacements; these substring-replacement pairs are
 * provided by a replacement map.
 *
 * Possible uses are normalization (replacing permutations of words with their lemmatized form), improper language
 * correction ("damn" -> "darn"), etc.
 *
 * @author Jeff Pasternack (jepasternack@linkedin.com)
 */
@ValueEquality
public class ReplacedSubstrings extends AbstractSubstringSearch<String, ReplacedSubstrings> {
  private static final long serialVersionUID = 1;

  private Map<String, String> _replacements = null;

  private static HashMap<String, String> lowercasedMap(Map<String, String> map) {
    HashMap<String, String> lowercased = new HashMap<>(map.size());
    map.forEach((key, value) -> lowercased.put(key.toLowerCase(), value));
    return lowercased;
  }

  /**
   * Sets the substring-replacement pairs that will be applied to the input.  The key in the provided map will be sought
   * in the input string; if found, it will be replaced by the corresponding value.
   *
   * The substrings sought must have the case as in the text.
   *
   * @param replacements the case-sensitive substring-replacement pairs to use
   * @return a copy of this instance that will use the specified replacements
   */
  public ReplacedSubstrings withReplacements(Map<String, String> replacements) {
    return withReplacements(replacements, true);
  }

  /**
   * Sets the substring-replacement pairs that will be applied to the input.  The key in the provided map will be sought
   * in the input string; if found, it will be replaced by the corresponding value.
   *
   * @param replacements the case-sensitive substring-replacement pairs to use
   * @param caseSensitive whether the substring-replacements will be case-sensitive; if false, case is ignored when
   *                      determining whether there is a match in the input text
   * @return a copy of this instance that will use the specified replacements
   */
  public ReplacedSubstrings withReplacements(Map<String, String> replacements, boolean caseSensitive) {
    return clone(c -> {
      c._replacements = caseSensitive ? new HashMap<>(replacements) : lowercasedMap(replacements);
      c._caseSensitivity = caseSensitive;
    });
  }

  @Override
  protected Collection<String> getSubstrings() {
    return _replacements.keySet();
  }

  @Override
  protected boolean getSingleResultOnly() {
    return false;
  }

  @Override
  protected boolean getAllowOverlaps() {
    return false;
  }

  private String getReplacement(String key) {
    return _replacements.get(key);
  }

  @Override
  public String apply(String value0) {
    Collection<Emit> emits = getEmits(value0, 8);
    if (emits.isEmpty()) {
      return value0;
    }

    Emit[] emitArray = emits.toArray(new Emit[0]);

    Arrays.sort(emitArray, (e1, e2) -> e1.getStart() - e2.getStart());

    StringBuilder builder = new StringBuilder(value0.length());
    int lastEnd = 0;

    for (Emit emit : emitArray) {
      builder.append(value0, lastEnd, emit.getStart());
      lastEnd = emit.getEnd() + 1;

      builder.append(getReplacement(emit.getKeyword()));
    }

    builder.append(value0, lastEnd, value0.length());

    return builder.toString();
  }

  @Override
  public void validate() {
    super.validate();
    if (_replacements == null) {
      throw new IllegalStateException("No replacements have been provided via withReplacements(...)");
    }
  }
}
