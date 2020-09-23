package com.linkedin.dagli.transformer;

import com.linkedin.dagli.list.FilteredList;
import com.linkedin.dagli.tester.Tester;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.junit.jupiter.api.Test;


public class FilteredListTest {
  @Test
  public void test() {
    HashSet<String> set = new HashSet<>();
    set.add("pig");

    ArrayList<String> list = new ArrayList<>();
    list.add("dog");
    list.add("cat");
    list.add("pig");

    FilteredList<String> fl = new FilteredList<>();

    Tester.of(fl.withInclusionSet(set))
        .input(list)
        .output(Collections.singletonList("pig"))
        .test();
    Tester.of(fl.withExclusionSet(set))
        .input(list)
        .output(Arrays.asList("dog", "cat"))
        .test();
  }
}
