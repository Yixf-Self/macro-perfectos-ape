package ru.autosome.perfectosape;

import ru.autosome.commons.backgroundModel.mono.Background;
import ru.autosome.commons.backgroundModel.mono.BackgroundModel;
import ru.autosome.commons.backgroundModel.mono.WordwiseBackground;
import ru.autosome.commons.importer.PWMImporter;
import ru.autosome.commons.model.Named;
import ru.autosome.commons.motifModel.mono.PWM;
import ru.autosome.commons.scoringModel.PWMOnBackground;
import ru.autosome.perfectosape.model.SequenceWithSNP;
import ru.autosome.perfectosape.model.encoded.mono.SequenceMonoEncoded;
import ru.autosome.perfectosape.model.encoded.mono.SequenceWithSNPMonoEncoded;

import java.util.List;

public class SNPScan extends ru.autosome.perfectosape.cli.generalized.SNPScan<SequenceMonoEncoded, SequenceWithSNPMonoEncoded, PWM, PWMOnBackground, BackgroundModel> {
  @Override
  protected String DOC_run_string(){
    return "java ru.autosome.perfectosape.SNPScan";
  }
  @Override
  protected String DOC_background_option() {
    return "ACGT - 4 numbers, comma-delimited(spaces not allowed), sum should be equal to 1, like 0.25,0.24,0.26,0.25";
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
  protected List<Named<PWM>> load_collection_of_pwms() {
    PWMImporter importer = new PWMImporter(background, dataModel, effectiveCount, transpose, pseudocount);
    return importer.loadMotifCollectionWithNames(path_to_collection_of_pwms);
  }

  protected SequenceWithSNPMonoEncoded encodeSequenceWithSNV(SequenceWithSNP sequenceWithSNV){
    return sequenceWithSNV.monoEncode();
  }

  protected static ru.autosome.perfectosape.cli.generalized.SNPScan from_arglist(String[] args) {
    ru.autosome.perfectosape.SNPScan result = new ru.autosome.perfectosape.SNPScan();
    result.setup_from_arglist(args);
    return result;
  }

  public static void main(String[] args) {
    try {
      ru.autosome.perfectosape.cli.generalized.SNPScan calculation = ru.autosome.perfectosape.SNPScan.from_arglist(args);
      calculation.process();
    } catch (Exception err) {
      System.err.println("\n" + err.getMessage() + "\n--------------------------------------\n");
      err.printStackTrace();
      System.err.println("\n--------------------------------------\nUse --help option for help\n\n" + new SNPScan().documentString());
      System.exit(1);

    }
  }
}
