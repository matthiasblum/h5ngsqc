package ch.blum;

/**
 * @author  Matthias Blum <mat.blum@gmail.com>
 */

import java.io.*;
import java.util.HashMap;
import java.util.Set;

public class Assembly {
    private final HashMap<String, Integer> chromSizes;

    public Assembly(File file) {
        this.chromSizes = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            boolean error = false;

            while ((line = br.readLine()) != null) {
                String[] cols = line.trim().split("\t");

                try {
                    this.chromSizes.put(cols[0], Integer.parseInt(cols[1]));
                } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                    System.err.format("%s: invalid assembly file\n", file.getPath());
                    error = true;
                    break;
                }

            }

            if (error)
                System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public Integer getChromSize(String chrom) {
        return this.chromSizes.get(chrom);
    }

    public Set<String> getChroms() {
        return this.chromSizes.keySet();
    }
}
