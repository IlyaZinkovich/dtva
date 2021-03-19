package io.github.ilyazinkovich.dvta.dynamic;

import java.util.ArrayList;
import java.util.List;

class InsertionPoints {

  static List<int[]> generate(int start, int end) {
    List<int[]> points = new ArrayList<>();
    for (int i = start; i <= end; i++) {
      for (int j = i + 1; j <= end + 1; j++) {
        points.add(new int[]{i, j});
      }
    }
    return points;
  }
}
