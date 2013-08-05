package ru.autosome.jMacroape;

import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.Math.ceil;

public class PM {
  double[][] matrix;
  double[] background;
  String name;

  public PM(PM pm) throws IllegalArgumentException {
    this.matrix = pm.matrix;
    this.background = pm.background;
  }
  public PM(double[][] matrix, double[] background, String name) throws IllegalArgumentException {
    for (double[] pos: matrix) {
      if (pos.length != 4) {
        throw new IllegalArgumentException("Matrix must have 4 elements in each position");
      }
    }
    if (background.length != 4) {
      throw new IllegalArgumentException("Background should contain exactly 4 nucleotides");
    }
    this.matrix = matrix;
    this.background = background;
    this.name = name;
  }

  public int length() {
    return matrix.length;
  }

  public double[] probabilities() {
    double sum = ArrayExtensions.sum(background);
    double[] probabilities = new double[4];
    for (int i = 0; i < 4; ++i) {
      probabilities[i] = background[i] / sum;
    }
    return probabilities;
  }
  public String toString() {
    String result;
    result = name + "\n";
    for (double[] pos: matrix) {
      result = result + pos[0] + "\t" + pos[1] + "\t" + pos[2] + "\t" + pos[3] + "\n";
    }
    return result;
  }

  public PM reverse_complement() {
    double[][] mat_result;
    mat_result = new double[length()][];
    for (int i = 0; i < length(); ++i) {
      mat_result[i] = new double[4];
      for (int j = 0; j < 4; ++j) {
        mat_result[i][j] = matrix[length() - 1  - i][4 - 1 - j];
      }
    }
    return new PM(mat_result, background, name);
  }

  public double vocabulary_volume() {
    return Math.pow(ArrayExtensions.sum(background), length());
  }

  public static HashMap<Character, Integer> index_by_letter() {
    HashMap<Character, Integer> result = new HashMap<Character,Integer>();
    result.put('A', 0);
    result.put('C', 1);
    result.put('G', 2);
    result.put('T', 3);
    return result;
  }
}