package ru.autosome.macroape.calculation.mono;

import gnu.trove.iterator.TDoubleDoubleIterator;
import gnu.trove.iterator.TDoubleObjectIterator;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import gnu.trove.map.hash.TDoubleObjectHashMap;
import ru.autosome.commons.backgroundModel.di.DiBackgroundModel;
import ru.autosome.commons.motifModel.di.DiPWM;
import ru.autosome.macroape.model.PairAligned;
import ru.autosome.commons.model.Position;
import ru.autosome.commons.backgroundModel.mono.BackgroundModel;
import ru.autosome.commons.backgroundModel.GeneralizedBackgroundModel;
import ru.autosome.ape.model.exception.HashOverflowException;
import ru.autosome.commons.cli.ResultInfo;
import ru.autosome.commons.motifModel.mono.PWM;

public class AlignedModelIntersection extends ru.autosome.macroape.calculation.generalized.AlignedModelIntersection<PWM, BackgroundModel> {

  public AlignedModelIntersection(PairAligned<PWM> alignment, BackgroundModel firstBackground, BackgroundModel secondBackground) {
    super(alignment, firstBackground, secondBackground);
  }

  public AlignedModelIntersection(PWM firstPWM, PWM secondPWM, BackgroundModel firstBackground, BackgroundModel secondBackground, Position relativePosition) {
    super(firstPWM, secondPWM, firstBackground, secondBackground,relativePosition);
  }


  private int summarySize(TDoubleObjectHashMap<TDoubleDoubleHashMap> scores) {
    int sum = 0;
    TDoubleObjectIterator<TDoubleDoubleHashMap> iterator = scores.iterator();
    while (iterator.hasNext()) {
      iterator.advance();
      sum += iterator.value().size();
    }
    return sum;
  }

  // 2d-score hash before first step
  private TDoubleObjectHashMap<TDoubleDoubleHashMap> initialScoreHash() {
    TDoubleObjectHashMap<TDoubleDoubleHashMap> scores = new TDoubleObjectHashMap<TDoubleDoubleHashMap>();
    scores.put(0.0, new TDoubleDoubleHashMap(new double[] {0},
                                             new double[] {1}) );
    return scores;
  }

  @Override
  protected double get_counts(double threshold_first, double threshold_second, BackgroundModel background) throws HashOverflowException {
    // scores_on_first_pwm, scores_on_second_pwm --> count
    TDoubleObjectHashMap<TDoubleDoubleHashMap> scores = initialScoreHash();

    for (int pos = 0; pos < alignment.length(); ++pos) {
      double[] firstColumn = alignment.firstModelAligned.matrix[pos];
      double[] secondColumn = alignment.secondModelAligned.matrix[pos];
      double leastSufficientScoreFirst = threshold_first - alignment.firstModelAligned.best_suffix(pos + 1);
      double leastSufficientScoreSecond = threshold_second - alignment.secondModelAligned.best_suffix(pos + 1);

      if (background.is_wordwise()) {
      scores = recalc_score_hash_wordwise(scores,
                                          firstColumn, secondColumn,
                                          leastSufficientScoreFirst, leastSufficientScoreSecond);
      } else {
        scores = recalc_score_hash(scores,
                                   firstColumn, secondColumn,
                                   leastSufficientScoreFirst, leastSufficientScoreSecond,
                                   background);
      }

      if (maxPairHashSize != null && summarySize(scores) > maxPairHashSize) {
        throw new HashOverflowException("Hash overflow in AlignedModelIntersection#get_counts");
      }
    }

    return combine_scores(scores);
  }

  double combine_scores(TDoubleObjectHashMap<TDoubleDoubleHashMap> scores) {
    double sum = 0;
    TDoubleObjectIterator<TDoubleDoubleHashMap> iterator = scores.iterator();
    while (iterator.hasNext()){
      iterator.advance();
      TDoubleDoubleIterator second_iterator = iterator.value().iterator();
      while (second_iterator.hasNext()) {
        second_iterator.advance();
        sum += second_iterator.value();
      }
    }
    return sum;
  }

  // 2d-hash which has first level keys initialized, but second level is empty
  TDoubleObjectHashMap<TDoubleDoubleHashMap> seedHashToRecalc(TDoubleObjectHashMap<TDoubleDoubleHashMap> scores,
                                                              double[] firstColumn, double leastSufficientScoreFirst) {
    TDoubleObjectHashMap<TDoubleDoubleHashMap> result = new TDoubleObjectHashMap<TDoubleDoubleHashMap>();

    TDoubleObjectIterator<TDoubleDoubleHashMap> iterator = scores.iterator();
    while (iterator.hasNext()) {
      iterator.advance();
      double score_first = iterator.key();
      for (int letter = 0; letter < PWM.ALPHABET_SIZE; ++letter) {
        double new_score_first = score_first + firstColumn[letter];
        if (new_score_first >= leastSufficientScoreFirst) {
          result.put(new_score_first, new TDoubleDoubleHashMap());
        }
      }
    }
    return result;
  }

  // Step of dynamic programming algorithm which recalculates score distribution `scores`
  // for matrices of length augmented with one column (different for each matrix).
  // leastSufficientScore makes it possible to reject lots of prefixes which start words that can't overcome thresholds
  TDoubleObjectHashMap<TDoubleDoubleHashMap> recalc_score_hash(TDoubleObjectHashMap<TDoubleDoubleHashMap> scores,
                                                               double[] firstColumn, double[] secondColumn,
                                                               double leastSufficientScoreFirst, double leastSufficientScoreSecond,
                                                               BackgroundModel background) {
    TDoubleObjectHashMap<TDoubleDoubleHashMap> new_scores = seedHashToRecalc(scores, firstColumn, leastSufficientScoreFirst);

    TDoubleObjectIterator<TDoubleDoubleHashMap> iterator = scores.iterator();
    while (iterator.hasNext()) {
      iterator.advance();
      double score_first = iterator.key();

      TDoubleDoubleHashMap second_scores = iterator.value();

      TDoubleDoubleIterator second_iterator = second_scores.iterator();
      while (second_iterator.hasNext()) {
        second_iterator.advance();
        double score_second = second_iterator.key();
        double count = second_iterator.value();

        for (int letter = 0; letter < PWM.ALPHABET_SIZE; ++letter) {
          double new_score_first = score_first + firstColumn[letter];

          if (new_score_first >= leastSufficientScoreFirst) {
            double new_score_second = score_second + secondColumn[letter];

            if (new_score_second >= leastSufficientScoreSecond) {
              double add = background.count(letter) * count;
              new_scores.get(new_score_first).adjustOrPutValue(new_score_second, add, add);
            }
          }
        }

      }
    }
    return new_scores;
  }

  // optimized version for a case of wordwise background
  TDoubleObjectHashMap<TDoubleDoubleHashMap> recalc_score_hash_wordwise(TDoubleObjectHashMap<TDoubleDoubleHashMap> scores,
                                                                        double[] firstColumn, double[] secondColumn,
                                                                        double leastSufficientScoreFirst, double leastSufficientScoreSecond) {
    TDoubleObjectHashMap<TDoubleDoubleHashMap> new_scores = seedHashToRecalc(scores, firstColumn, leastSufficientScoreFirst);

    TDoubleObjectIterator<TDoubleDoubleHashMap> iterator = scores.iterator();
    while (iterator.hasNext()) {
      iterator.advance();
      double score_first = iterator.key();

      TDoubleDoubleHashMap second_scores = iterator.value();

      TDoubleDoubleIterator second_iterator = second_scores.iterator();
      while (second_iterator.hasNext()) {
        second_iterator.advance();
        double score_second = second_iterator.key();
        double count = second_iterator.value();

        for (int letter = 0; letter < PWM.ALPHABET_SIZE; ++letter) {
          double new_score_first = score_first + firstColumn[letter];

          if (new_score_first >= leastSufficientScoreFirst) {
            double new_score_second = score_second + secondColumn[letter];

            if (new_score_second >= leastSufficientScoreSecond) {
              new_scores.get(new_score_first).adjustOrPutValue(new_score_second, count, count);
            }
          }
        }

      }
    }
    return new_scores;
  }
}
