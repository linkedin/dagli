package com.linkedin.dagli.text;

import com.concurrentli.Singleton;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.ahocorasick.trie.handler.EmitHandler;


/**
 * Abstract class used to build transformers that efficiently find any of a list of substrings in the input string.
 * This is useful for, e.g. blacklisting or whitelisting certain specific phrases.
 *
 * @author Jeff Pasternack (jepasternack@linkedin.com)
 */
public abstract class AbstractSubstringSearch<R, S extends AbstractSubstringSearch<R, S>>
    extends AbstractPreparedTransformer1WithInput<String, R, S> {
  private static final long serialVersionUID = 1;

  protected boolean _caseSensitivity = true;
  protected boolean _wholePhrasesOnly = false;

  @Override
  public S clone(Consumer<S> modifier) {
    S res = super.clone(modifier);
    res._trie = new TrieSingleton(res);
    return res;
  }

  /**
   * Whether or not only whole phrases will be matched.  A "whole phrase" is defined by a match that does not have
   * a letter or digit on either side of the match.  E.g. "dog" is not a whole phrase in "doggy", but it is in
   * "the dog!".
   *
   * @param wholePhrasesOnly whether or not only whole phrases should be matched
   * @return a copy of this instance that will or will not match only whole phrases as specified
   */
  public S withWholePhrasesOnly(boolean wholePhrasesOnly) {
    return clone(c -> c._wholePhrasesOnly = wholePhrasesOnly);
  }

  protected abstract Collection<String> getSubstrings();
  protected abstract boolean getSingleResultOnly();
  protected abstract boolean getAllowOverlaps();

  // not serialized; partly because this allows the implementation to vary in the future, and mostly because Trie is
  // not serializable!
  protected transient TrieSingleton _trie = new TrieSingleton(this);

  private static class TrieSingleton extends Singleton<Trie> {
    private final AbstractSubstringSearch<?, ?> _owner;

    public TrieSingleton(AbstractSubstringSearch<?, ?> owner) {
      _owner = owner;
    }

    @Override
    protected Trie getValue() {
      Trie.TrieBuilder builder = Trie.builder();
      if (_owner.getSingleResultOnly()) {
        builder = builder.stopOnHit();
      }
      if (!_owner.getAllowOverlaps()) {
        builder = builder.ignoreOverlaps();
      }

      if (_owner._caseSensitivity) {
        return builder.addKeywords(_owner.getSubstrings()).build();
      } else {
        return builder.ignoreCase().addKeywords(_owner.getSubstrings()).build();
      }
    }
  }

  protected static class WordBoundaryEmitHandler implements EmitHandler {
    private final List<Emit> _result;

    private final String _str;

    public WordBoundaryEmitHandler(String str, int expectedResultCount) {
      _str = str;
      _result = new ArrayList<>(expectedResultCount);
    }

    public List<Emit> getResult() {
      return _result;
    }

    @Override
    public boolean emit(Emit emit) {
      if ((emit.getStart() == 0 || !Character.isLetterOrDigit(_str.charAt(emit.getStart() - 1))) && (
          emit.getEnd() == _str.length() - 1 || !Character.isLetterOrDigit(_str.charAt(emit.getEnd() + 1)))) {
        _result.add(emit);
        return true;
      }
      return false;
    }
  }

  protected Collection<Emit> getEmits(String str, int expectedResultCount) {
    if (_wholePhrasesOnly) {
      WordBoundaryEmitHandler wbeh = new WordBoundaryEmitHandler(str, expectedResultCount);
      _trie.get().parseText(str, wbeh);
      return wbeh._result;
    }

    return _trie.get().parseText(str);
  }
}
