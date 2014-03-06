package ru.autosome.perfectosape.calculations.findPvalue;

import ru.autosome.perfectosape.backgroundModels.DiBackgroundModel;
import ru.autosome.perfectosape.calculations.CountingDiPWM;
import ru.autosome.perfectosape.calculations.ScoringModelDistibutions;
import ru.autosome.perfectosape.motifModels.DiPWM;

public class DiPWMFindPvalueAPE extends FindPvalueByDiscretization<DiPWM, DiBackgroundModel> {
  public static class Builder extends FindPvalueBuilder<DiPWM> {
    Double discretization;
    DiBackgroundModel dibackground;
    Integer maxHashSize;

    public Builder(Double discretization, DiBackgroundModel dibackground, Integer maxHashSize) {
      this.discretization = discretization;
      this.dibackground = dibackground;
      this.maxHashSize = maxHashSize;
    }

    @Override
    public CanFindPvalue pvalueCalculator() {
      return new DiPWMFindPvalueAPE(motif, discretization, dibackground, maxHashSize);
    }
  }

  Integer maxHashSize;

  public DiPWMFindPvalueAPE(DiPWM dipwm, Double discretization, DiBackgroundModel dibackground, Integer maxHashSize) {
    super(dipwm, dibackground, discretization);
    this.maxHashSize = maxHashSize;
  }

  @Override
  ScoringModelDistibutions countingPWM() {
    return new CountingDiPWM(motif.discrete(discretization), background, maxHashSize);
  }
}
