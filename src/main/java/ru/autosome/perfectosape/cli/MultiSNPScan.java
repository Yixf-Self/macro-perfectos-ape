package ru.autosome.perfectosape.cli;

import ru.autosome.perfectosape.backgroundModels.Background;
import ru.autosome.perfectosape.backgroundModels.BackgroundModel;
import ru.autosome.perfectosape.backgroundModels.WordwiseBackground;
import ru.autosome.perfectosape.calculations.findPvalue.CanFindPvalue;
import ru.autosome.perfectosape.calculations.findPvalue.FindPvalueAPE;
import ru.autosome.perfectosape.calculations.findPvalue.FindPvalueBsearchBuilder;
import ru.autosome.perfectosape.importers.MotifCollectionImporter;
import ru.autosome.perfectosape.importers.PWMImporter;
import ru.autosome.perfectosape.motifModels.PWM;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultiSNPScan extends MultiSNPScanGeneralized<BackgroundModel> {
  @Override
  protected String DOC_run_string(){
    return "java ru.autosome.perfectosape.cli.MultiSNPScan";
  }
  @Override
  protected String DOC_background_option() {
    return "ACGT - 4 numbers, comma-delimited(spaces not allowed), sum should be equal to 1, like 0.25,0.24,0.26,0.25";
  }

  private MultiSNPScan() {
    super();
  }

  @Override
  protected void extract_background(String str) {
    background = Background.fromString(str);
  }
  @Override
  protected void initialize_default_background() {
    background = new WordwiseBackground();
  }

  @Override
  protected void load_collection_of_pwms() throws FileNotFoundException {
    PWMImporter pwmImporter = new PWMImporter(background, dataModel, effectiveCount);
    MotifCollectionImporter importer = new MotifCollectionImporter<PWM>(pwmImporter);
    List<PWM> pwmList = importer.loadPWMCollection(path_to_collection_of_pwms);

    pwmCollection = new ArrayList<ThresholdEvaluator>();
    for (PWM pwm: pwmList) {
      CanFindPvalue pvalueCalculator;
      if (thresholds_folder == null) {
        pvalueCalculator = new FindPvalueAPE(pwm, background, discretization, max_hash_size);
      } else {
        pvalueCalculator = new FindPvalueBsearchBuilder(thresholds_folder).pvalueCalculator(pwm);
      }
      pwmCollection.add(new ThresholdEvaluator(pwm, pvalueCalculator, pwm.getName()));
    }
  }

  protected static MultiSNPScanGeneralized from_arglist(ArrayList<String> argv) throws FileNotFoundException {
    MultiSNPScan result = new MultiSNPScan();
    Helper.print_help_if_requested(argv, result.documentString());
    result.setup_from_arglist(argv);
    return result;
  }

  protected static MultiSNPScanGeneralized from_arglist(String[] args) throws FileNotFoundException {
    ArrayList<String> argv = new ArrayList<String>();
    Collections.addAll(argv, args);
    return from_arglist(argv);
  }

  public static void main(String[] args) {
    try {
      MultiSNPScanGeneralized calculation = MultiSNPScan.from_arglist(args);
      calculation.process();
    } catch (Exception err) {
      System.err.println("\n" + err.getMessage() + "\n--------------------------------------\n");
      err.printStackTrace();
      System.err.println("\n--------------------------------------\nUse --help option for help\n\n" + new MultiSNPScan().documentString());
      System.exit(1);

    }
  }
}
