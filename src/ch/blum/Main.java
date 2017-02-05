package ch.blum;

/**
 * @author  Matthias Blum <mat.blum@gmail.com>
 */

import java.io.File;

public class Main {

    private static void showHelp() {
        System.out.println("\nUsage: java -jar h5ngsqc.jar BED TABLE CHROMSIZES HDF5 [options]");
        System.out.format("    %-15salignment file in the BED format. May be gzip-compressed.\n", "BED");
        System.out.format("    %-15stab-separated file containing the intensity for three random samplings. May be gzip-compressed.\n", "TABLE");
        System.out.format("    %-15stab-separated file containing the chromosome sizes for the genome assembly.\n", "CHROMSIZES");
        System.out.format("    %-15soutput HDF5 file\n\n", "HDF5");
        System.out.format("Options: -s, --span INT    span/resolution in bp for wiggles (default: 50).\n");
        System.out.format("         -e, --ext INT     read extension in bp (default: 150).\n");
        System.out.format("         --bg INT          global background threshold for localQCs (default: 0).\n");
        System.out.format("         -5                switch to '5-replicates' mode. TABLE file is expected to contain more columns.\n");
        System.out.format("         --skip            skip reads/bins on unknown chromosome instead of stopping the program.\n");
        System.out.format("         --quiet           do not display progress messages.\n");
    }

    public static void main(String[] args) {
        // Positional arguments
        File bedFile = null;
        File tableFile = null;
        File chromSizesFile = null;
        File outFile = null;

        // Optional arguments
        int wigSpan = 50;
        int readExtension = 150;
        int backgroundThreshold = 0;
        boolean useFiveReps = false;
        boolean skip = false;
        boolean quiet = false;

        int positionalCounter = 0;

        for (String arg : args) {
            if (arg.equals("-h") || arg.equals("--help")) {
                showHelp();
                System.exit(0);
            }
        }

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.equals("-s") || arg.equals("--span")) {
                if (i + 1 < args.length) {
                    try {
                        wigSpan = Integer.parseInt(args[i+1]);

                        if (wigSpan <= 0) {
                            System.err.format("option '%s' requires a non-null positive number\n", arg);
                            System.exit(1);
                        }
                    } catch (NumberFormatException e) {
                        System.err.format("option '%s': invalid int value %s\n", arg, args[i+1]);
                        System.exit(1);
                    }

                    i++;
                } else {
                    System.err.format("option '%s' requires an argument\n", arg);
                    System.exit(1);
                }
            } else if (arg.equals("-e") || arg.equals("--ext")) {
                if (i + 1 < args.length) {
                    try {
                        readExtension = Integer.parseInt(args[i+1]);

                        if (readExtension < 0) {
                            System.err.format("option '%s' requires a positive number\n", arg);
                            System.exit(1);
                        }
                    } catch (NumberFormatException e) {
                        System.err.format("option '%s': invalid int value %s\n", arg, args[i+1]);
                        System.exit(1);
                    }

                    i++;
                } else {
                    System.err.format("option '%s' requires an argument\n", arg);
                    System.exit(1);
                }
            } else if (arg.equals("--bg")) {
                if (i + 1 < args.length) {
                    try {
                        backgroundThreshold = Integer.parseInt(args[i + 1]);

                        if (readExtension < 0) {
                            System.err.format("option '%s' requires a positive number\n", arg);
                            System.exit(1);
                        }
                    } catch (NumberFormatException e) {
                        System.err.format("option '%s': invalid int value %s\n", arg, args[i + 1]);
                        System.exit(1);
                    }

                    i++;
                } else {
                    System.err.format("option '%s' requires an argument\n", arg);
                    System.exit(1);
                }
            } else if (arg.equals("-5")) {
                useFiveReps = true;
            } else if (arg.equals("--skip")) {
                skip = true;
            } else if (arg.equals("--quiet")) {
                quiet = true;
            } else if (arg.charAt(0) == '-') {
                System.err.format("invalid option '%s'\n", arg);
                System.exit(1);
            } else if (positionalCounter == 0) {
                bedFile = new File(arg);
                positionalCounter++;
            } else if (positionalCounter == 1) {
                tableFile = new File(arg);
                positionalCounter++;
            } else if (positionalCounter == 2) {
                chromSizesFile = new File(arg);
                positionalCounter++;
            } else if (positionalCounter == 3) {
                outFile = new File(arg);
                positionalCounter++;
            } else {
                System.err.format("invalid option '%s'\n", arg);
                System.exit(1);
            }
        }

        if (positionalCounter < 3) {
            System.err.println("Missing arguments. Type --help to display help message.");
            System.exit(1);
        } else if (! bedFile.isFile()) {
            System.err.format("%s: no such file or directory\n", bedFile.getPath());
            System.exit(1);
        } else if (! tableFile.isFile()) {
            System.err.format("%s: no such file or directory\n", tableFile.getPath());
            System.exit(1);
        } else if (! chromSizesFile.isFile()) {
            System.err.format("%s: no such file or directory\n", chromSizesFile.getPath());
            System.exit(1);
        }

        Assembly assembly = new Assembly(chromSizesFile);
        BinnedProfile profile = new BinnedProfile(bedFile, tableFile, assembly, wigSpan);

        if (! quiet)
            System.err.println("Loading LocalQCs");
        profile.loadLocalQCs(backgroundThreshold, useFiveReps, skip, quiet);

        if (! quiet)
            System.err.println("Loading Wiggles");
        profile.loadWiggles(readExtension, skip, quiet);

        if (! quiet)
            System.err.println("Writing HDF5");
        profile.toHDF5(outFile, useFiveReps);
    }
}
