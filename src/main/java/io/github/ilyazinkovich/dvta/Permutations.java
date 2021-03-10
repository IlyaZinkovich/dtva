package io.github.ilyazinkovich.dvta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Permutations {

  static <T> Set<List<T>> generate(List<T> a, int size) {
    Set<List<T>> res = new HashSet<>();
    if (size == 1) {
      res.add(new ArrayList<>(a));
    }
    for (int i = 0; i < size; i++) {
      res.addAll(generate(a, size - 1));
      T temp;
      if (size % 2 == 1) {
        temp = a.get(0);
        a.set(0, a.get(size - 1));
      } else {
        temp = a.get(i);
        a.set(i, a.get(size - 1));
      }
      a.set(size - 1, temp);
    }
    return res;
  }
}
