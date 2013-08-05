package ru.autosome.jMacroape;

import com.sun.javaws.exceptions.InvalidArgumentException;

import java.util.*;

import static java.lang.Math.ceil;

public class PWM extends PM {
  private HashMap<Double,Double> count_distribution;
  public Integer max_hash_size;

  public PWM(double[][] matrix, double[] background, String name) throws IllegalArgumentException {
    super(matrix, background, name);
  }
  public double score_mean() {
    double result = 0.0;
    double probabilities[] = probabilities();
    for (double[] pos: matrix) {
      double mean_score_at_pos = 0.0;
      for (int letter = 0; letter < 4; ++letter) {
        mean_score_at_pos += pos[letter] * probabilities[letter];
      }
      result += mean_score_at_pos;
    }
    return result;
  }

  public double score_variance(){
    double variance = 0.0;
    double probabilities[] = probabilities();
    for (double[] pos: matrix) {
      double mean_sqare = 0.0;
      for (int letter = 0; letter < 4; ++letter) {
        mean_sqare += pos[letter] * pos[letter] * probabilities[letter];
      }
      double mean = 0.0;
      for (int letter = 0; letter < 4; ++letter) {
        mean += pos[letter] * probabilities[letter];
      }
      double squared_mean = mean*mean;
      variance += mean_sqare - squared_mean;
    }
    return variance;
  }

  double threshold_gauss_estimation(double pvalue) {
    double sigma = Math.sqrt(score_variance());
    double n_ = MathExtensions.inverf(1 - 2 * pvalue) * Math.sqrt(2);
    return score_mean() + n_ * sigma;
  }

  double score(String word) throws IllegalArgumentException {
    double probabilities[] = probabilities();
    word = word.toUpperCase();
    HashMap<Character, Integer> index_by_letter = index_by_letter();
    if (word.length() != length()){
      throw new IllegalArgumentException("word in PWM#score(word) should have the same length as matrix");
    }
    double sum = 0.0;
    for (int pos_index = 0; pos_index < length(); ++pos_index){
      char letter = word.charAt(pos_index);
      Integer letter_index = index_by_letter.get(letter);
      if (letter_index != null) {
        sum += matrix[pos_index][letter_index];
      } else if (letter == 'N'){
        double temp_accum = 0.0;
        for (int i = 0; i < 4; ++i) {
          temp_accum += matrix[pos_index][i] * probabilities[i];
        }
        sum += temp_accum;
      } else {
        throw new IllegalArgumentException("word in PWM#score(#{word}) should have only ACGT or N letters, but have '" + letter + "' letter");
      }
    }
    return sum;
  }

  public double best_score() {
    return best_suffix(0);
  }
  public double worst_score() {
    return worst_suffix(0);
  }

  // best score of suffix s[i..l]
  double best_suffix(int i) {
    double result = 0.0;
    for (int pos_index = i; pos_index < length(); ++pos_index) {
      result += ArrayExtensions.max(matrix[pos_index]);
    }
    return result;
  }

  double worst_suffix(int i) {
    double result = 0.0;
    for (int pos_index = i; pos_index < length(); ++pos_index) {
      result += ArrayExtensions.min(matrix[pos_index]);
    }
    return result;
  }


  /////////////////////////////

  HashMap<Double, Double> count_distribution_under_pvalue(double max_pvalue) {
    HashMap<Double, Double> cnt_distribution = new HashMap<Double, Double>();
    double look_for_count = max_pvalue * vocabulary_volume();

    while(!( HashExtensions.sum_values(cnt_distribution) >= look_for_count )) {
      double approximate_threshold;
      try {
        approximate_threshold = threshold_gauss_estimation(max_pvalue);
      } catch(ArithmeticException e) {
        approximate_threshold = worst_score();
      }
      cnt_distribution = count_distribution_after_threshold(approximate_threshold);
      max_pvalue *= 2; // if estimation counted too small amount of words - try to lower threshold estimation by doubling pvalue
    }

    return cnt_distribution;
  }

  HashMap<Double,Double> count_distribution_after_threshold(double threshold) {
    if (count_distribution != null) {
      HashMap<Double,Double> result = new HashMap<Double, Double>();
      for(Map.Entry<Double,Double> entry: count_distribution.entrySet()) {
        double score = entry.getKey();
        double count = entry.getValue();
        if (score >= threshold) {
          result.put(score, count);
        }
      }
      return result;
    }

    HashMap<Double,Double> scores = new HashMap<Double,Double>();
    scores.put(0.0, 1.0);
    for (int column = 0; column < length(); ++column){
      scores = recalc_score_hash(scores, matrix[column], threshold - best_suffix(column + 1));
      if (max_hash_size != null && scores.size() > max_hash_size) {
        throw new IllegalArgumentException("Hash overflow in PWM::ThresholdByPvalue#count_distribution_after_threshold");
      }
    }
    return scores;
  }

  public HashMap<Double,Double> recalc_score_hash(HashMap<Double,Double> scores, double[] column, double least_sufficient) {
    HashMap<Double,Double> new_scores = new HashMap<Double,Double>();
    for(Map.Entry<Double,Double> entry: scores.entrySet()) {
      double score = entry.getKey();
      double count = entry.getValue();
      for(int letter = 0; letter < 4; ++letter) {
        double new_score = score + column[letter];
        if (new_score >= least_sufficient) {
          if (! new_scores.containsKey(new_score) ) {
            new_scores.put(new_score, 0.0);
          }
          new_scores.put(new_score, new_scores.get(new_score) + count * background[letter]);
        }
      }
    }
    return new_scores;
  }

  public HashMap<Double,Double> count_distribution() {
    if(this.count_distribution == null) {
      this.count_distribution = count_distribution_after_threshold(worst_score());
    }
    return this.count_distribution;
  }

  public HashMap<Double,Double> counts_by_thresholds(double ... thresholds) {
    HashMap<Double,Double> scores = count_distribution_after_threshold(ArrayExtensions.min(thresholds));
    HashMap<Double,Double> result = new HashMap<Double,Double>();
    for (double threshold: thresholds) {
      double accum = 0.0;
      for(Map.Entry<Double,Double> entry: scores.entrySet()) {
        double score = entry.getKey();
        double count = entry.getValue();
        if(score >= threshold) {
          accum += count;
        }
      }
      result.put(threshold, accum);
    }
    return result;
  }

  public double count_by_threshold(double threshold) {
    return counts_by_thresholds(threshold).get(threshold);
  }

  public double pvalue_by_threshold(double threshold) {
    return count_by_threshold(threshold) / vocabulary_volume();
  }

  public HashMap<Double, double[]> thresholds(double ... pvalues) {
    HashMap<Double, double[]> results = new HashMap<Double, double[]>();
    HashMap<Double,double[][]> thresholds_by_pvalues = thresholds_by_pvalues(pvalues);
    for (double pvalue: thresholds_by_pvalues.keySet()) {
      double thresholds[] = thresholds_by_pvalues.get(pvalue)[0];
      double counts[] = thresholds_by_pvalues.get(pvalue)[1];
      double threshold = thresholds[0] + 0.1 * (thresholds[1] - thresholds[0]);
      double real_pvalue = (double)counts[1] / vocabulary_volume();
      double result_entries[] = {threshold, real_pvalue};
      results.put(pvalue, result_entries);
    }
    return results;
  }

  // "weak" means that threshold has real pvalue not less than given pvalue, while usual threshold not greater
  public HashMap<Double, double[]> weak_thresholds(double ... pvalues) {
    HashMap<Double, double[]> results = new HashMap<Double, double[]>();
    HashMap<Double,double[][]> thresholds_by_pvalues = thresholds_by_pvalues(pvalues);
    for (double pvalue: thresholds_by_pvalues.keySet()) {
      double thresholds[] = thresholds_by_pvalues.get(pvalue)[0];
      double counts[] = thresholds_by_pvalues.get(pvalue)[1];
      double threshold = thresholds[0];
      double real_pvalue = (double)counts[0] / vocabulary_volume();
      double result_entries[] = {threshold, real_pvalue};
      results.put(pvalue, result_entries);
    }
    return results;
  }

  public HashMap<Double,double[][]> thresholds_by_pvalues(double ... pvalues) {
    HashMap<Double,Double> scores_hash = count_distribution_under_pvalue(ArrayExtensions.max(pvalues));

    Double[] scores_keys = scores_hash.keySet().toArray(new Double[0]);
    java.util.Arrays.sort(scores_keys);
    Double[] sorted_scores_keys = ArrayExtensions.reverse(scores_keys);
    double scores[] = ArrayExtensions.toPrimitiveArray(sorted_scores_keys);

    double counts[] = new double[scores.length];
    for (int i = 0; i < scores.length; ++i) {
      counts[i] = scores_hash.get(scores[i]);
    }

    double partial_sums[] = ArrayExtensions.partial_sums(counts, 0.0);

    HashMap<Double, double[][]> results = new HashMap<Double, double[][]>();

    double sorted_pvalues[] = pvalues.clone();
    Arrays.sort(sorted_pvalues);
    HashMap<Double, Double> pvalue_counts = new HashMap<Double, Double>();
    for (double pvalue: sorted_pvalues) {
      pvalue_counts.put(pvalue, pvalue * vocabulary_volume());
    }
    for (Map.Entry<Double, Double> entry: pvalue_counts.entrySet()) {
      double pvalue = entry.getKey();
      double look_for_count = entry.getValue();
      int ind = 0;
      for (int i = 0; i < partial_sums.length; ++i) {
        if (partial_sums[i] >= look_for_count) {
          ind = i;
          break;
        }
      }
      double minscore = scores[ind];
      double count_at_minscore = partial_sums[ind];
      double maxscore;
      double count_at_maxscore;
      if (ind>0) {
        maxscore = scores[ind-1];
        count_at_maxscore = partial_sums[ind-1];
      } else {
        maxscore = best_score() + 1.0;
        count_at_maxscore = 0.0;
      }

      double[][] resulting_ranges = {{minscore, maxscore},{count_at_minscore, count_at_maxscore}};
      results.put(pvalue, resulting_ranges);
    }
    return results;
  }

  double threshold(double pvalue) {
    return threshold_and_real_pvalue(pvalue)[0];
  }
  double[] threshold_and_real_pvalue(double pvalue) {
    return thresholds(pvalue).get(pvalue);
  }
  double weak_threshold(double pvalue) {
    return weak_threshold_and_real_pvalue(pvalue)[0];
  }
  double[] weak_threshold_and_real_pvalue(double pvalue) {
    return weak_thresholds(pvalue).get(pvalue);
  }
  public PWM discrete(double rate) {
    double[][] mat_result;
    mat_result = new double[length()][];
    for (int i = 0; i < length(); ++i) {
      mat_result[i] = new double[4];
      for (int j = 0; j < 4; ++j){
        mat_result[i][j] = ceil(matrix[i][j] * rate);
      }
    }
    return new PWM(mat_result, background, name);
  }

  double[] zero_column() {
    double[] result = {0.0,0.0,0.0,0.0};
    return result;
  }

  public PWM left_augment(int n) {
    double[][] mat_result;
    mat_result = new double[length() + n][];
    for (int i = 0; i < n; ++i) {
      mat_result[i] = zero_column();
    }
    for (int i = 0; i < length(); ++i) {
      mat_result[n + i] = matrix[i].clone();
    }
    return new PWM(mat_result, background, name);
  }

  public PWM right_augment(int n) {
    double[][] mat_result;
    mat_result = new double[length() + n][];
    for (int i = 0; i < length(); ++i) {
      mat_result[i] =  matrix[i].clone();
    }
    for (int i = 0; i < n; ++i) {
      mat_result[length() + i] = zero_column();
    }
    return new PWM(mat_result, background, name);
  }
}