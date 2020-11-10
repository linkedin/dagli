package com.linkedin.dagli.util.collection;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class LinkedStackTest {
  @Test
  public void test() {
    LinkedStack<Integer> stack1a = LinkedStack.<Integer>empty().push(0);
    LinkedStack<Integer> stack1b = stack1a.pushAll(Arrays.asList(1, 2));
    LinkedStack<Integer> stack1c = stack1b.push(3);

    LinkedStack<Integer> stack2a = LinkedStack.of(0);
    LinkedStack<Integer> stack2b = stack2a.push(1).push(2);
    LinkedStack<Integer> stack2c = stack2b.push(3);

    LinkedStack<Integer> stack3c = stack2b.push(4);

    Assertions.assertEquals(stack1a.pop(), LinkedStack.empty());
    Assertions.assertEquals(stack1a, stack2a);
    Assertions.assertEquals(stack1b, stack2b);
    Assertions.assertEquals(stack1c, stack2c);

    java.util.List<Integer> asStreamedList = stack2c.stream().collect(Collectors.toList());
    Collections.reverse(asStreamedList);
    Assertions.assertEquals(stack1c, LinkedStack.from(asStreamedList));
    Assertions.assertEquals(stack1c, LinkedStack.from(stack2c.toList()));

    Assertions.assertNotEquals(stack1a, stack1b);
    Assertions.assertNotEquals(stack1a, stack1c);
    Assertions.assertNotEquals(stack1b, stack1c);
    Assertions.assertNotEquals(stack1a, LinkedStack.empty());

    Assertions.assertNotEquals(stack1c, stack3c);
    Assertions.assertNotEquals(stack2c, stack3c);

    Assertions.assertEquals(0, LinkedStack.empty().size64());
    Assertions.assertEquals(1, stack1a.size64());
    Assertions.assertEquals(3, stack1b.size64());
    Assertions.assertEquals(4, stack1c.size64());
  }
}
