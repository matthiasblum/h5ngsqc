package ch.blum;

/**
 * @author  Matthias Blum <mat.blum@gmail.com>
 */

public class Wiggle {
    private int intensity;
    private int uniqueIntensity;

    public Wiggle() {
        this.intensity = 0;
        this.uniqueIntensity = 0;
    }

    public void add(boolean uniqueRead) {
        this.intensity++;

        if (uniqueRead)
            this.uniqueIntensity++;
    }

    public int getIntensity() {
        return intensity;
    }

    public int getUniqueIntensity() {
        return uniqueIntensity;
    }
}
