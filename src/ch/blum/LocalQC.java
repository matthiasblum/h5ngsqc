package ch.blum;

/**
 * @author  Matthias Blum <mat.blum@gmail.com>
 */

public class LocalQC {
    private int intensity;
    private double dispersion;

    public LocalQC() {
        this.intensity = 0;
        this.dispersion = 0;
    }

    public void update(int intensity, double dispersion) {
        this.intensity = intensity;
        this.dispersion = dispersion;
    }

    public String toString() {
        return Integer.toString(this.intensity) + " " + Double.toString(this.dispersion);
    }
}
