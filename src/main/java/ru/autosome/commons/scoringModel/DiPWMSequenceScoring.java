package ru.autosome.commons.scoringModel;

import ru.autosome.commons.backgroundModel.di.DiBackgroundModel;
import ru.autosome.commons.model.Orientation;
import ru.autosome.commons.model.indexingScheme.DiIndexingScheme;
import ru.autosome.commons.model.indexingScheme.DiIndexingSchemeIUPAC;
import ru.autosome.commons.motifModel.ScoreStatistics;
import ru.autosome.commons.motifModel.di.DiPWM;
import ru.autosome.perfectosape.model.encoded.di.SequenceDiEncoded;

import static ru.autosome.commons.model.indexingScheme.DiIndexingSchemeIUPAC.N_index;

public class DiPWMSequenceScoring implements ScoreStatistics, SequenceScoringModel<SequenceDiEncoded> {

  private final DiPWM dipwm;
  private final DiBackgroundModel dibackground;
  private final double[][] matrixIUPAC;
  public DiPWMSequenceScoring(DiPWM dipwm, DiBackgroundModel dibackground) {
    this.dipwm = dipwm;
    this.dibackground = dibackground;
    this.matrixIUPAC = calculateMatrixIUPAC();
  }

  // Extract merging scheme
  private double[][] calculateMatrixIUPAC() {
    double[][] result = new double[dipwm.getMatrix().length][];
    for (int posIndex = 0; posIndex < dipwm.getMatrix().length ; ++posIndex) {
      result[posIndex] = new double[25];
      for (int firstLetterIndex = 0; firstLetterIndex < 4; ++firstLetterIndex) {
        // AA,AC,AG,AT, CA,CC,CG,CT, GA,GC,GG,GT, TA,TC,TG,TT
        for (int secondLetterIndex = 0; secondLetterIndex < 4; ++secondLetterIndex) {
          result[posIndex][DiIndexingSchemeIUPAC.diIndex(firstLetterIndex, secondLetterIndex)] =
           dipwm.getMatrix()[posIndex][DiIndexingScheme.diIndex(firstLetterIndex, secondLetterIndex)];
        }
        // AN,CN,GN,TN
        result[posIndex][DiIndexingSchemeIUPAC.diIndex(firstLetterIndex, N_index)] =
         dibackground.average_by_second_letter(dipwm.getMatrix()[posIndex], firstLetterIndex);
      }
      for (int secondLetterIndex = 0; secondLetterIndex < 4; ++secondLetterIndex) {
        // NA,NC,NG,NT
        result[posIndex][DiIndexingSchemeIUPAC.diIndex(N_index, secondLetterIndex)] =
         dibackground.average_by_first_letter(dipwm.getMatrix()[posIndex], secondLetterIndex);
      }
      // NN
      result[posIndex][DiIndexingSchemeIUPAC.diIndex(N_index, N_index)] = dibackground.mean_value(dipwm.getMatrix()[posIndex]);
    }
    return result;
  }

  @Override
  public int length() {
    return dipwm.length(); // It is model length, not matrix length
  }

  public double score(SequenceDiEncoded word) {
    return score(word, Orientation.direct, 0);
  }

  @Override
  public double score(SequenceDiEncoded word, Orientation orientation, int position) {
    byte[] seq;
    int startPos;
    if (orientation == Orientation.direct) {
      seq = word.directSequence;
      startPos = position;
    } else  {
      seq = word.revcompSequence;
      startPos = word.length() - (position + length());
    }

    double sum = 0.0;
    for (int pos_index = 0; pos_index < matrixIUPAC.length; ++pos_index) {
      byte letter = seq[startPos + pos_index];
      sum += matrixIUPAC[pos_index][letter];
    }
    return sum;
  }

  @Override
  public double score_mean() {
    double result = 0.0;
    for (double[] pos : dipwm.getMatrix()) {
      result += dibackground.mean_value(pos);
    }
    return result;
  }

  @Override
  public double score_variance() {
    double variance = 0.0;
    for (double[] pos : dipwm.getMatrix()) {
      variance += dibackground.variance(pos);
    }
    return variance;
  }
}
