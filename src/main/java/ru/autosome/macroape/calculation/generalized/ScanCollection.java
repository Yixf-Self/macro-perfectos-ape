package ru.autosome.macroape.calculation.generalized;

import ru.autosome.ape.calculation.findPvalue.CanFindPvalue;
import ru.autosome.ape.calculation.findPvalue.FindPvalueAPE;
import ru.autosome.ape.calculation.findThreshold.CanFindThreshold;
import ru.autosome.ape.calculation.findThreshold.FindThresholdAPE;
import ru.autosome.commons.backgroundModel.GeneralizedBackgroundModel;
import ru.autosome.commons.model.BoundaryType;
import ru.autosome.commons.model.Discretizer;
import ru.autosome.commons.motifModel.Alignable;
import ru.autosome.commons.motifModel.Discretable;
import ru.autosome.commons.motifModel.ScoreDistribution;
import ru.autosome.macroape.model.ComparisonSimilarityInfo;
import ru.autosome.macroape.model.ScanningSimilarityInfo;
import ru.autosome.macroape.model.ThresholdEvaluator;

import java.util.ArrayList;
import java.util.List;

public abstract class ScanCollection <ModelType extends Alignable<ModelType> & Discretable<ModelType> &ScoreDistribution<BackgroundType>, BackgroundType extends GeneralizedBackgroundModel> {

  protected final List<ThresholdEvaluator<ModelType>> thresholdEvaluators;

  public final ModelType queryPWM;
  public double pvalue;
  public Double queryPredefinedThreshold;
  public Discretizer roughDiscretizer, preciseDiscretizer;
  public BackgroundType queryBackground, collectionBackground;
  public BoundaryType pvalueBoundaryType;
  public Double similarityCutoff;
  public Double preciseRecalculationCutoff; // null means that no recalculation will be performed


  public ScanCollection(List<ThresholdEvaluator<ModelType>> thresholdEvaluators, ModelType queryPWM) {
    this.thresholdEvaluators = thresholdEvaluators;
    this.queryPWM = queryPWM;
  }

  abstract protected CompareModels<ModelType, BackgroundType> calculation(ModelType firstMotif, ModelType secondMotif,
                                                                          BackgroundType firstBackground, BackgroundType secondBackground,
                                                                          CanFindPvalue firstPvalueCalculator, CanFindPvalue secondPvalueCalculator,
                                                                          Discretizer discretizer);

  public ScanningSimilarityInfo similarityInfo(CanFindPvalue roughQueryPvalueEvaluator,
                                               CanFindPvalue preciseQueryPvalueEvaluator,
                                               double roughQueryThreshold,
                                               double preciseQueryThreshold,
                                               ThresholdEvaluator<ModelType> knownMotifEvaluator) {
    ComparisonSimilarityInfo<ModelType> info;
    boolean precise = false;
    CompareModels<ModelType,BackgroundType> roughCalculation = calculation(
        queryPWM, knownMotifEvaluator.pwm,
        queryBackground, collectionBackground,
        roughQueryPvalueEvaluator,
        knownMotifEvaluator.rough.pvalueCalculator,
        roughDiscretizer);

    Double roughCollectionThreshold = knownMotifEvaluator.rough.thresholdCalculator
                                          .thresholdByPvalue(pvalue, pvalueBoundaryType).threshold;

    info = roughCalculation.jaccard(roughQueryThreshold, roughCollectionThreshold);

    if (preciseRecalculationCutoff != null &&
            info.similarity() >= preciseRecalculationCutoff &&
            knownMotifEvaluator.precise.thresholdCalculator != null) {
      CompareModels<ModelType,BackgroundType> preciseCalculation = calculation(
          queryPWM, knownMotifEvaluator.pwm,
          queryBackground, collectionBackground,
          preciseQueryPvalueEvaluator,
          knownMotifEvaluator.precise.pvalueCalculator,
          preciseDiscretizer);

      Double preciseCollectionThreshold = knownMotifEvaluator.precise.thresholdCalculator
                                              .thresholdByPvalue(pvalue, pvalueBoundaryType).threshold;

      info = preciseCalculation.jaccard(preciseQueryThreshold, preciseCollectionThreshold);
      precise = true;
    }
    if (similarityCutoff == null || info.similarity() >= similarityCutoff) {
      return new ScanningSimilarityInfo<>(knownMotifEvaluator.pwm, knownMotifEvaluator.name, info, precise);
    } else {
      return null;
    }
  }

  public List<ScanningSimilarityInfo> similarityInfos() {
    List<ScanningSimilarityInfo> result;
    result = new ArrayList<>(thresholdEvaluators.size());

    CanFindPvalue roughQueryPvalueEvaluator = new FindPvalueAPE<>(queryPWM, queryBackground, roughDiscretizer);
    CanFindPvalue preciseQueryPvalueEvaluator = new FindPvalueAPE<>(queryPWM, queryBackground, preciseDiscretizer);

    double roughQueryThreshold = queryThreshold(roughDiscretizer);
    double preciseQueryThreshold = queryThreshold(preciseDiscretizer);

    for (ThresholdEvaluator<ModelType> knownMotifEvaluator: thresholdEvaluators) {
      ScanningSimilarityInfo info = similarityInfo(roughQueryPvalueEvaluator, preciseQueryPvalueEvaluator,
          roughQueryThreshold, preciseQueryThreshold,
          knownMotifEvaluator);
      if (info != null) {
        result.add(info);
      }
    }
    return result;
  }


  double queryThreshold(Discretizer discretizer) {
    if (queryPredefinedThreshold != null) {
      return queryPredefinedThreshold;
    } else {
      CanFindThreshold pvalue_calculator = new FindThresholdAPE<>(queryPWM, queryBackground, discretizer);
      return pvalue_calculator.thresholdByPvalue(pvalue, pvalueBoundaryType).threshold;
    }
  }

}
