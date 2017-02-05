package ch.blum;

/**
 * @author  Matthias Blum <mat.blum@gmail.com>
 */

public class LocalQC5 extends LocalQC {
    private int flag;

    public LocalQC5() {
        super();
        this.flag = 0;
    }

    public void update(int intensity, double dispersion, int flag) {
        this.update(intensity, dispersion);
        this.flag = flag;
    }
}
