/*
This file is part of leafdigital kanjirecog.

kanjirecog is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

kanjirecog is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with kanjirecog.  If not, see <http://www.gnu.org/licenses/>.

Copyright 2011 Samuel Marshall.
*/
package com.leafdigital.kanji.android;

import java.util.LinkedList;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;

import androidx.annotation.Nullable;

public class KanjiDrawing extends View
{
	/**
	 * Maximum number of strokes permitted. (Having this maximum keeps memory
	 * use from getting out of hand; in memory, there are up to MAX_STROKES
	 * copies of an alpha channel bitmap the size of the control.)
	 */
	public final static int MAX_STROKES = 30;

	private final static float BLOB_RADIUS_DP = 2.5f;
	private final static int DEFAULT_BACKGROUND_COLOR = Color.HSVToColor(new float[] {100f, 0.05f, 0.15f});
	private final static int DEFAULT_FOREGROUND_COLOR = Color.WHITE;

	private float lastX, lastY;

	private float density;
	private int densityInt;
	private Bitmap bitmap;
	private Canvas bitmapCanvas;

	private DrawnStroke pendingStroke;
	private LinkedList<DrawnStroke> strokes = new LinkedList<DrawnStroke>();
	private LinkedList<Bitmap> undo = new LinkedList<Bitmap>();

	private Listener listener;
	private Paint drawPaint;

	public void setStrokeColor(int color) {
		if (drawPaint == null) {
			drawPaint = new Paint();
		}
		drawPaint.setColor(color);
	}

	public int getStrokeColor() {
		return drawPaint.getColor();
	}

	/**
	 * Interface for callers that want to be informed of updates.
	 */
	public interface Listener
	{
		/**
		 * Called every time a stroke is completed, undone, or cleared and the
		 * number of strokes changes.
		 * @param strokes All strokes currently in the drawing
		 */
		public void strokes(DrawnStroke[] strokes);
	}

	public KanjiDrawing(Context context) {
		super(context);
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		density = metrics.density;
		densityInt = metrics.densityDpi;
		setBackgroundColor(DEFAULT_BACKGROUND_COLOR);
		setStrokeColor(DEFAULT_FOREGROUND_COLOR);
	}

	public KanjiDrawing(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public KanjiDrawing(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		density = metrics.density;
		densityInt = metrics.densityDpi;
		TypedArray customattrs = context.getTheme().obtainStyledAttributes(attrs, R.styleable.KanjiDrawing, defStyleAttr, 0);
		setStrokeColor(customattrs.getColor(R.styleable.KanjiDrawing_strokeColor, DEFAULT_FOREGROUND_COLOR));
	}

	// Requires API level >= 21
	/*public KanjiDrawing(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		density = metrics.density;
		densityInt = metrics.densityDpi;
		TypedArray customattrs = context.getTheme().obtainStyledAttributes(attrs, R.styleable.KanjiDrawing, defStyleAttr, defStyleRes);
		setStrokeColor(customattrs.getColor(R.styleable.KanjiDrawing_strokeColor, DEFAULT_FOREGROUND_COLOR));
	}*/

	/*@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int specMode = MeasureSpec.getMode(heightMeasureSpec);
		int specSize = MeasureSpec.getSize(heightMeasureSpec);
		Log.d(getClass().getName(), String.format("height_measure=%d mode=%d size=%d min_height=%d default_height=%d", heightMeasureSpec, specMode, specSize, getSuggestedMinimumHeight(), getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec)));
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}*/

	@Override
	protected void onDraw(Canvas canvas)
	{
		int width = getWidth(), height = getHeight();
		if(bitmap == null || bitmap.getWidth() != width || bitmap.getHeight() != height)
		{
			bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
			bitmap.setDensity(densityInt);
			bitmapCanvas = new Canvas(bitmap);
		}

		// Draw background colour
		//canvas.drawColor(((ColorDrawable) getBackground()).getColor());
		getBackground().draw(canvas);

		// Draw
		canvas.drawBitmap(bitmap, 0, 0, drawPaint);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		// Ignore touch events if there are already 30 strokes
		if(strokes.size() >= 30)
		{
			return true;
		}

		float x = event.getX(), y = event.getY();

		Paint paint = new Paint();
		paint.setAlpha(255);
		paint.setAntiAlias(true);
		float radius = density * BLOB_RADIUS_DP;

		switch(event.getAction())
		{
		case MotionEvent.ACTION_DOWN:
			// Store previous bitmap as undo state (except first one)
			if(!strokes.isEmpty())
			{
				undo.addLast(bitmap.copy(bitmap.getConfig(), true));
			}

			pendingStroke = new DrawnStroke(x, y);
			lastX = x;
			lastY = y;
			// Fall through
			bitmapCanvas.drawCircle(x, y, radius, paint);
			invalidate();
			break;

		case MotionEvent.ACTION_MOVE:
			// Draw blob
			paint.setStrokeWidth(2 * radius);
			paint.setStrokeCap(Paint.Cap.ROUND);
			bitmapCanvas.drawLine(lastX, lastY, x, y, paint);
			lastX = x;
			lastY = y;
			invalidate();
			break;

		case MotionEvent.ACTION_UP:
			pendingStroke.finish(x, y);
			strokes.addLast(pendingStroke);
			updateListener();
			break;
		}
		return true;
	}

	/**
	 * Undoes the last stroke added.
	 */
	public void undo()
	{
		if(strokes.isEmpty())
		{
			return;
		}
		if(undo.isEmpty())
		{
			Paint erase = new Paint();
			erase.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			bitmapCanvas.drawPaint(erase);
		}
		else
		{
			bitmap = undo.removeLast();
			bitmapCanvas = new Canvas(bitmap);
		}
		strokes.removeLast();
		invalidate();
		updateListener();
	}

	/**
	 * Updates the listener (if present) on the new list of strokes.
	 */
	private void updateListener()
	{
		if(listener != null)
		{
			listener.strokes(getStrokes());
		}
	}

	/**
	 * Clears all strokes.
	 */
	public void clear()
	{
		if(strokes.isEmpty())
		{
			return;
		}
		strokes.clear();
		undo.clear();
		Paint erase = new Paint();
		erase.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		bitmapCanvas.drawPaint(erase);
		System.gc();
		invalidate();
		updateListener();
	}

	/**
	 * @return All strokes currently drawn in the control
	 */
	public DrawnStroke[] getStrokes()
	{
		return strokes.toArray(new DrawnStroke[0]);
	}

	/**
	 * Sets the listener that receives an update when the strokes change.
	 * @param listener Listener
	 */
	public void setListener(Listener listener)
	{
		this.listener = listener;
	}
}
