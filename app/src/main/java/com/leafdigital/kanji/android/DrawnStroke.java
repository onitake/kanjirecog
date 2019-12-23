package com.leafdigital.kanji.android;

import android.content.Intent;

/**
 * Represents a stroke drawn in the drawing panel.
 */
public class DrawnStroke {
    private float startX, startY, endX, endY;

    private final static String EXTRA_STROKESX = "strokesx",
        EXTRA_STROKESY = "strokesy", EXTRA_STROKEEX = "strokeex",
        EXTRA_STROKEEY = "strokeey";

    DrawnStroke(float startX, float startY) {
        this.startX = startX;
        this.startY = startY;
    }

    public void finish(float endX, float endY) {
        this.endX = endX;
        this.endY = endY;
    }

    private DrawnStroke(float startX, float startY, float endX, float endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    /**
     * @return Start X position of stroke
     */
    public float getStartX() {
        return startX;
    }

    /**
     * @return Start Y position of stroke
     */
    public float getStartY() {
        return startY;
    }

    /**
     * @return End X position of stroke
     */
    public float getEndX() {
        return endX;
    }

    /**
     * @return End Y position of stroke
     */
    public float getEndY() {
        return endY;
    }

    /**
     * Saves an array of strokes into extra data in an intent.
     *
     * @param intent  Intent
     * @param strokes Strokes to save
     */
    public static void saveToIntent(Intent intent, DrawnStroke[] strokes) {
        float[] sx = new float[strokes.length], sy = new float[strokes.length],
            ex = new float[strokes.length], ey = new float[strokes.length];
        for (int i = 0; i < strokes.length; i++) {
            sx[i] = strokes[i].startX;
            sy[i] = strokes[i].startY;
            ex[i] = strokes[i].endX;
            ey[i] = strokes[i].endY;
        }
        intent.putExtra(EXTRA_STROKESX, sx);
        intent.putExtra(EXTRA_STROKESY, sy);
        intent.putExtra(EXTRA_STROKEEX, ex);
        intent.putExtra(EXTRA_STROKEEY, ey);
    }

    /**
     * Loads extra data from an intent into an array of strokes
     *
     * @param intent Intent
     * @return Loaded strokes
     */
    public static DrawnStroke[] loadFromIntent(Intent intent) {
        float[] sx, sy, ex, ey;
        sx = intent.getFloatArrayExtra(EXTRA_STROKESX);
        sy = intent.getFloatArrayExtra(EXTRA_STROKESY);
        ex = intent.getFloatArrayExtra(EXTRA_STROKEEX);
        ey = intent.getFloatArrayExtra(EXTRA_STROKEEY);
        if (sx == null || sy == null || ex == null || ey == null
            || sx.length != sy.length || sx.length != ex.length
            || sx.length != ey.length) {
            throw new IllegalArgumentException("Missing or invalid extra data");
        }

        DrawnStroke[] result = new DrawnStroke[sx.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = new DrawnStroke(sx[i], sy[i], ex[i], ey[i]);
        }
        return result;
    }
}
