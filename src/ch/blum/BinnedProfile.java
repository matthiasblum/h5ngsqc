package ch.blum;

/**
 * @author  Matthias Blum <mat.blum@gmail.com>
 */

import java.io.*;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;
import ch.systemsx.cisd.hdf5.*;

public class BinnedProfile {
    private final File bedFile;
    private final File tableFile;
    private final HashMap<String, Section> sections;


    public BinnedProfile(File bedFile, File tableFile, Assembly assembly, int wigSpan) {
        this.bedFile = bedFile;
        this.tableFile = tableFile;
        this.sections = new HashMap<>();

        for (String chrom: assembly.getChroms()) {
            this.sections.put(chrom, new Section(assembly.getChromSize(chrom), wigSpan));
        }
    }

    public void loadLocalQCs(int backgroundThreshold, boolean useFiveReps, boolean skip, boolean quiet) {
        if (useFiveReps) {
            this.loadLocalQCs5(backgroundThreshold, skip, quiet);
        } else
            this.loadLocalQCs(backgroundThreshold, skip, quiet);
    }

    public void loadWiggles(int readExtension, boolean skip, boolean quiet) {
        int lineNumber = 0;

        /**
         * todo: read from stdin with:
         * br = new BufferedReader(new InputStreamReader(System.in));
         */
        try (FileInputStream fis = new FileInputStream(this.bedFile)) {

            try (BufferedReader br = new BufferedReader(isGzipped(this.bedFile) ?
                    new InputStreamReader(new GZIPInputStream(fis)) : new InputStreamReader(fis))
            ) {
                String line;
                String prevChrom = null;
                int prevPos1 = -1;
                boolean sawFwRead = false;
                boolean sawRvRead = false;

                while ((line = br.readLine()) != null) {
                    lineNumber++;

                    if (! quiet && lineNumber % 1000000 == 0)
                        System.err.format("\t%d reads parsed\n", lineNumber);

                    if (line.charAt(0) == '#' || line.toLowerCase().startsWith("track") || line.toLowerCase().startsWith("broser"))
                        continue;

                    // chr2L	995	1068	ERR393678.1060596	1	+
                    String[] cols = line.trim().split("\t");
                    String chrom = cols[0];

                    if (this.sections.get(chrom) == null) {
                        if (skip)
                            continue;
                        else {
                            System.err.format("%s: unknown chromosome '%s' at line %d\n",
                                    this.bedFile.getPath(), chrom, lineNumber);
                            System.exit(1);
                        }
                    }

                    int pos1 = Integer.parseInt(cols[1]);
                    int pos2 = Integer.parseInt(cols[2]);
                    boolean isFwRead = cols[5].charAt(0) == '+';

                    if (readExtension != 0) {
                        if (isFwRead)
                            pos2 = pos1 + readExtension;
                        else
                            pos1 = pos2 - readExtension;
                    }

                    boolean isUniqueRead = true;
                    if (prevChrom != null && chrom.equals(prevChrom) && pos1 == prevPos1) {
                        if (isFwRead) {
                            if (sawFwRead)
                                isUniqueRead = false;
                            else
                                sawFwRead = true;
                        } else if (sawRvRead) {
                            isUniqueRead = false;
                        } else {
                            sawRvRead = true;
                        }
                    } else if (isFwRead) {
                        sawFwRead = true;
                        sawRvRead = false;
                    } else {
                        sawFwRead = false;
                        sawRvRead = true;
                    }

                    this.sections.get(chrom).addRead(pos1, pos2, isUniqueRead);
                    prevChrom = chrom;
                    prevPos1 = pos1;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.format("%s: 6 columns expected at line %d\n", this.bedFile.getPath(), lineNumber);
                System.exit(1);
            } catch (NumberFormatException e) {
                System.err.format("%s: invalid number at line %d\n", this.bedFile.getPath(), lineNumber);
                System.exit(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void loadLocalQCs5(int backgroundThreshold, boolean skip, boolean quiet) {
        int lineNumber = 0;

        try (FileInputStream fis = new FileInputStream(this.tableFile)) {

            try (BufferedReader br = new BufferedReader(isGzipped(this.tableFile) ?
                    new InputStreamReader(new GZIPInputStream(fis)) : new InputStreamReader(fis))
            ) {
                String line;

                while ((line = br.readLine()) != null) {
                    lineNumber++;

                    if (! quiet && lineNumber % 1000000 == 0)
                        System.err.format("\t%d lines parsed\n", lineNumber);

                    String[] cols = line.trim().split("\t");

                    // chr1	3000500	3001000	4	3	3	2	4	3	4   3   2	3	3	0	0	2	1	1
                    String chrom = cols[0];

                    if (this.sections.get(chrom) == null) {
                        if (skip)
                            continue;
                        else {
                            System.err.format("%s: unknown chromosome '%s' at line %d\n",
                                    this.tableFile.getPath(), chrom, lineNumber);
                            System.exit(1);
                        }
                    }

                    int position = Integer.parseInt(cols[1]);
                    int intensity = Integer.parseInt(cols[3]);

                    if (intensity < backgroundThreshold)
                        continue;

                    // Arrays of counts for random samplings
                    int[] intensity90 = new int[5];
                    int[] intensity70 = new int[5];
                    int[] intensity50 = new int[5];

                    // Fill the arrays
                    for (int i = 0; i < 5; i++) {
                        intensity90[i] = Integer.parseInt(cols[4+i]);
                        intensity70[i] = Integer.parseInt(cols[9+i]);
                        intensity50[i] = Integer.parseInt(cols[14+i]);
                    }

                    // Sums of 50% dispersions for replicate with dRCI < 10%
                    double sum_disp50 = 0;

                    // Number of replicates having dRCI < 10%
                    int n_valid = 0;

                    // Verify for each replicate if its passes the 10% dispersion thresholds
                    for (int i = 0; i < 5; i++) {
                        double disp90 = Math.abs(90 - 100. * intensity90[i] / intensity);
                        double disp70 = Math.abs(70 - 100. * intensity70[i] / intensity);
                        double disp50 = Math.abs(50 - 100. * intensity50[i] / intensity);

                        if (disp90 < 10 && disp70 < 10 && disp50 < 10) {
                            n_valid++;
                            sum_disp50 += disp50;
                        }
                    }

                    int flag = 0;
                    if (n_valid > 0) {
                        flag |= 1;

                        if (n_valid > 1) {
                            flag |= 2;

                            if (n_valid > 2) {
                                flag |= 4;

                                if (n_valid > 3) {
                                    flag |= 8;

                                    if (n_valid > 4)
                                        flag |= 16;
                                }
                            }
                        }

                        sum_disp50 /= n_valid;
                    }

                    this.sections.get(chrom).addLocalQC5(position, intensity, sum_disp50, flag);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.format("%s: 19 columns expected at line %d\n", this.tableFile.getPath(), lineNumber);
                System.exit(1);
            } catch (NumberFormatException e) {
                System.err.format("%s: invalid number at line %d\n", this.tableFile.getPath(), lineNumber);
                System.exit(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void loadLocalQCs(int backgroundThreshold, boolean skip, boolean quiet) {
        int lineNumber = 0;

        try (FileInputStream fis = new FileInputStream(this.tableFile)) {

            try (BufferedReader br = new BufferedReader(isGzipped(this.tableFile) ?
                    new InputStreamReader(new GZIPInputStream(fis)) : new InputStreamReader(fis))
            ) {
                String line;

                while ((line = br.readLine()) != null) {
                    lineNumber++;

                    if (! quiet && lineNumber % 1000000 == 0)
                        System.err.format("\t%d lines parsed\n", lineNumber);

                    String[] cols = line.trim().split("\t");

                    // chr2L	500	1000	1	1	1	0
                    String chrom = cols[0];

                    if (this.sections.get(chrom) == null) {
                        if (skip)
                            continue;
                        else {
                            System.err.format("%s: unknown chromosome '%s' at line %d\n",
                                    this.tableFile.getPath(), chrom, lineNumber);
                            System.exit(1);
                        }
                    }

                    int position = Integer.parseInt(cols[1]);
                    int intensity = Integer.parseInt(cols[3]);

                    if (intensity < backgroundThreshold)
                        continue;

                    int intensity90 = Integer.parseInt(cols[4]);
                    int intensity70 = Integer.parseInt(cols[5]);
                    int intensity50 = Integer.parseInt(cols[6]);
                    double disp90 = Math.abs(90 - 100. * intensity90 / intensity);
                    double disp70 = Math.abs(70 - 100. * intensity70 / intensity);
                    double disp50 = Math.abs(50 - 100. * intensity50 / intensity);

                    if (disp90 < 10 && disp70 < 10 && disp50 < 10)
                        this.sections.get(chrom).addLocalQC(position, intensity, disp50);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.format("%s: 7 columns expected at line %d\n", this.tableFile.getPath(), lineNumber);
                System.exit(1);
            } catch (NumberFormatException e) {
                System.err.format("%s: invalid number at line %d\n", this.tableFile.getPath(), lineNumber);
                System.exit(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static boolean isGzipped(File file) {
        boolean gzipped;

        try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(file))) {
            gzipped = true;
        } catch (ZipException e) {
            gzipped = false;
        } catch (IOException e) {
            gzipped = false;
            e.printStackTrace();
            System.exit(1);
        }

        return gzipped;
    }

    public void toHDF5(File file, boolean useFiveReps) {
        IHDF5WriterConfigurator config = HDF5Factory.configure(file);
        config.overwrite();
        config.dontUseExtendableDataTypes();
        IHDF5Writer writer = config.writer();
        HDF5IntStorageFeatures intFeatures = HDF5IntStorageFeatures.createDeflation(HDF5IntStorageFeatures.DEFAULT_DEFLATION_LEVEL);
        HDF5GenericStorageFeatures features = HDF5GenericStorageFeatures.createDeflation(HDF5GenericStorageFeatures.DEFAULT_DEFLATION_LEVEL);

        for (String chrom: this.sections.keySet()) {
            Section section = this.sections.get(chrom);

            if (useFiveReps)
                writer.compound().writeArray("/" + chrom + "/localqcs", section.getLocalQC5s(), features);
            else
                writer.compound().writeArray("/" + chrom + "/localqcs", section.getLocalQCs(), features);

            writer.int32().writeMatrix("/" + chrom + "/wigs/", section.getWiggles(), intFeatures);
            writer.int32().setAttr("/" + chrom, "size", section.getSize());
            writer.int32().setAttr("/" + chrom, "span", section.getWigSpan());
            section.destroy();
        }

    }
}
