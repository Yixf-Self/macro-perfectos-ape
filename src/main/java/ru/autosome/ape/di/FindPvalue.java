package ru.autosome.ape.di;

import ru.autosome.ape.calculation.findPvalue.CanFindPvalue;
import ru.autosome.ape.calculation.findPvalue.FindPvalueAPE;
import ru.autosome.ape.calculation.findPvalue.FindPvalueBsearch;
import ru.autosome.commons.backgroundModel.di.DiBackground;
import ru.autosome.commons.backgroundModel.di.DiBackgroundModel;
import ru.autosome.commons.backgroundModel.di.DiWordwiseBackground;
import ru.autosome.commons.importer.DiPWMFromMonoImporter;
import ru.autosome.commons.importer.DiPWMImporter;
import ru.autosome.commons.importer.MotifImporter;
import ru.autosome.commons.model.Named;
import ru.autosome.commons.motifModel.di.DiPWM;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public class FindPvalue extends ru.autosome.ape.cli.generalized.FindPvalue<DiPWM, DiBackgroundModel> {
  @Override
  protected String DOC_background_option() {
    return "ACGT - 16 numbers, comma-delimited(spaces not allowed), sum should be equal to 1, like 0.02,0.03,0.03,0.02,0.08,0.12,0.12,0.08,0.08,0.12,0.12,0.08,0.02,0.03,0.03,0.02";
  }
  @Override
  protected String DOC_run_string() {
    return "java ru.autosome.ape.di.FindPvalue";
  }

  @Override
  protected String DOC_additional_options() {
    return "  [--from-mono]  - obtain DiPWM from mono PWM/PCM/PPM.\n";
  }

  boolean fromMononucleotide;

  @Override
  protected CanFindPvalue calculator() {
    if (thresholds_folder == null) {
      return new FindPvalueAPE<>(motif.getObject(), background, discretizer);
    } else {
      File thresholds_file = new File(thresholds_folder, motif.getName() + ".thr");
      try {
        return new FindPvalueBsearch(thresholds_file);
      } catch (FileNotFoundException e) {
        System.err.println("Thresholds file `" + thresholds_file + "` not found. Fallback to APE-calculation of P-value");
        return new FindPvalueAPE<>(motif.getObject(), background, discretizer);
      }
    }
  }

  @Override
  protected void initialize_defaults() {
    super.initialize_defaults();
    fromMononucleotide = false;
  }

  @Override
  protected void initialize_default_background() {
    background = new DiWordwiseBackground();
  }

  @Override
  protected void extract_background(String str) {
    background = DiBackground.fromString(str);
  }

  protected boolean failed_to_recognize_additional_options(String opt, List<String> argv) {
    if (opt.equals("--from-mono")) {
      fromMononucleotide = true;
      return false;
    } else {
      return true;
    }
  }

  @Override
  protected Named<DiPWM> loadMotif(String filename) {
    MotifImporter<DiPWM> importer;
    if (fromMononucleotide) {
      importer = new DiPWMFromMonoImporter(background, data_model, effective_count, transpose, pseudocount);
    } else {
      importer = new DiPWMImporter(background, data_model, effective_count, transpose, pseudocount);
    }
    return importer.loadMotifWithName(filename);
  }

  protected static FindPvalue from_arglist(String[] args) {
    FindPvalue result = new FindPvalue();
    result.setup_from_arglist(args);
    return result;
  }

  public static void main(String[] args) {
    try {
      FindPvalue cli = ru.autosome.ape.di.FindPvalue.from_arglist(args);
      System.out.println(cli.report());
    } catch (Exception err) {
      System.err.println("\n" + err.getMessage() + "\n--------------------------------------\n");
      err.printStackTrace();
      System.err.println("\n--------------------------------------\nUse --help option for help\n\n" + new FindPvalue().documentString());
      System.exit(1);
    }
  }
}
