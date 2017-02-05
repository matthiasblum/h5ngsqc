package ch.blum;

/**
 * @author  Matthias Blum <mat.blum@gmail.com>
 */

public class Section {
    private final int size;
    private final int wigSpan;
    private final int nLocalQCs;
    private final int nWiggles;
    private LocalQC[] localQCs;
    private LocalQC5[] localQC5s;
    private Wiggle[] wiggles;

    public Section(int size, int wigSpan) {
        this.size = size;
        this.wigSpan = wigSpan;
        this.nLocalQCs = (size + 499) / 499;
        this.nWiggles = (size + wigSpan - 1) / wigSpan;
        this.localQCs = null;
        this.localQC5s = null;
        this.wiggles = null;
    }

    public void addLocalQC(int position, int intensity, double dispersion) {
        if (this.localQCs == null) {
            this.localQCs = new LocalQC[this.nLocalQCs];

            for (int i = 0; i < this.nLocalQCs; i++) {
                this.localQCs[i] = new LocalQC();
            }
        }

        try {
            this.localQCs[position / 500].update(intensity, dispersion);
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
    }

    public void addLocalQC5(int position, int intensity, double dispersion, int flag) {
        if (this.localQC5s == null) {
            this.localQC5s = new LocalQC5[this.nLocalQCs];

            for (int i = 0; i < this.nLocalQCs; i++) {
                this.localQC5s[i] = new LocalQC5();
            }
        }

        try {
            this.localQC5s[position / 500].update(intensity, dispersion, flag);
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
    }

    public void addRead(int pos1, int pos2, boolean isUniqueRead) {
        if (this.wiggles == null) {
            this.wiggles = new Wiggle[this.nWiggles];

            for (int i = 0; i < this.nWiggles; i++) {
                this.wiggles[i] = new Wiggle();
            }
        }

        if (pos1 < 0)
            pos1 = 0;

        for (int i = pos1 / this.wigSpan; i <= pos2 / this.wigSpan; i++) {
            try {
                this.wiggles[i].add(isUniqueRead);
            } catch (ArrayIndexOutOfBoundsException e) {
                break;
            }
        }
    }

    public LocalQC[] getLocalQCs() {
        if (this.localQCs == null) {
            this.localQCs = new LocalQC[this.nLocalQCs];

            for (int i = 0; i < this.nLocalQCs; i++) {
                this.localQCs[i] = new LocalQC();
            }
        }

        return this.localQCs;
    }

    public LocalQC5[] getLocalQC5s() {
        if (this.localQC5s == null) {
            this.localQC5s = new LocalQC5[this.nLocalQCs];

            for (int i = 0; i < this.nLocalQCs; i++) {
                this.localQC5s[i] = new LocalQC5();
            }
        }

        return this.localQC5s;
    }

    public int[][] getWiggles() {
        int[][] wiggles = new int[this.nWiggles][2];

        if (this.wiggles != null) {
            for (int i = 0; i < this.nWiggles; i++) {
                wiggles[i][0] = this.wiggles[i].getIntensity();
                wiggles[i][1] = this.wiggles[i].getUniqueIntensity();
            }
        }

        return wiggles;
    }

    public void destroy() {
        this.localQCs = null;
        this.localQC5s = null;
        this.wiggles = null;
    }

    public int getSize() {
        return size;
    }

    public int getWigSpan() {
        return wigSpan;
    }
}
