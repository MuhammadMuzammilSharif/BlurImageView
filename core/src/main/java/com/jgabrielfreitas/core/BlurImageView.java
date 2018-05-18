package com.jgabrielfreitas.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Created by JGabrielFreitas on 20/06/16.
 */
public class BlurImageView extends AppCompatImageView {


    private float defaultBitmapScale = 0.1f;
    private static final int MAX_RADIUS = 25;
    private static final int MIN_RADIUS = 1;
    Drawable imageOnView;


    public BlurImageView(Context context) {
        super(context);
        init(null);
    }

    public BlurImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public BlurImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        setImageDrawable(new BitmapDrawable(getResources(), bm));
    }

    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        //if (imageOnView == null)
        imageOnView = drawable;
    }

    private void init(AttributeSet attrs) {

        if (attrs != null && getDrawable() != null) {

            imageOnView = getDrawable();

            TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.BlurImageView, 0, 0);

            Integer radius = typedArray.getInteger(R.styleable.BlurImageView_radius, 0);

            setBlur(radius);

            typedArray.recycle();
        }
    }


    public void setBitmapScale(float bitmapScale) {
        this.defaultBitmapScale = bitmapScale;
    }

    public void setBlur(int radius) {

        if (imageOnView == null)
            imageOnView = getDrawable();

        // max radius = 25
        if (radius > MIN_RADIUS && radius <= MAX_RADIUS) {

            Bitmap blurred = blurRenderScript(((BitmapDrawable) imageOnView).getBitmap(), radius);
            setImageBitmap(blurred);
            invalidate();

        } else if (radius == 0) {
            setImageDrawable(imageOnView);
            invalidate();

        } else
            Log.e("BLUR", "actualRadius invalid: " + radius);
    }

    private Bitmap blurRenderScript(Bitmap smallBitmap, int radius) {

        int width = Math.round(smallBitmap.getWidth() * defaultBitmapScale);
        int height = Math.round(smallBitmap.getHeight() * defaultBitmapScale);

        Bitmap inputBitmap = Bitmap.createScaledBitmap(smallBitmap, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

        if (Build.VERSION.SDK_INT >= 17) {
            RenderScript renderScript = RenderScript.create(getContext());
            ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
            Allocation tmpIn = Allocation.createFromBitmap(renderScript, inputBitmap);
            Allocation tmpOut = Allocation.createFromBitmap(renderScript, outputBitmap);
            theIntrinsic.setRadius(radius);
            theIntrinsic.setInput(tmpIn);
            theIntrinsic.forEach(tmpOut);
            tmpOut.copyTo(outputBitmap);
        } else {
            outputBitmap = blurRenderScrip(smallBitmap, radius);
        }

        return outputBitmap;
    }

    @SuppressLint("NewApi")
    private Bitmap blurRenderScrip(Bitmap smallBitmap, int radius) {
        try {
            smallBitmap = RGB565toARGB888(smallBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }


        Bitmap bitmap = Bitmap.createBitmap(
                smallBitmap.getWidth(), smallBitmap.getHeight(),
                Bitmap.Config.ARGB_8888);

        RenderScript renderScript = RenderScript.create(this.getContext());

        Allocation blurInput = Allocation.createFromBitmap(renderScript, smallBitmap);
        Allocation blurOutput = Allocation.createFromBitmap(renderScript, bitmap);

        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(renderScript,
                Element.U8_4(renderScript));
        blur.setInput(blurInput);
        blur.setRadius(radius); // radius must be 0 < r <= 25
        blur.forEach(blurOutput);

        blurOutput.copyTo(bitmap);
        renderScript.destroy();

        return bitmap;

    }

    private Bitmap RGB565toARGB888(Bitmap img) throws Exception {
        int numPixels = img.getWidth() * img.getHeight();
        int[] pixels = new int[numPixels];

        //Get JPEG pixels.  Each int is the color values for one pixel.
        img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());

        //Create a Bitmap of the appropriate format.
        Bitmap result = Bitmap.createBitmap(img.getWidth(), img.getHeight(), Bitmap.Config.ARGB_8888);

        //Set RGB pixels.
        result.setPixels(pixels, 0, result.getWidth(), 0, 0, result.getWidth(), result.getHeight());
        return result;
    }
}
