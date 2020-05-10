package com.challenge.largeformatbannerprinter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import java.io.IOException;
import java.util.HashMap;

public class PrintableGenerator {
    public static int MAX_SIZE = 2048;
    public static float MARGIN = 0.05f;

    public static HashMap<String, Typeface> fonts = new HashMap<String, Typeface>();
    public static int fontIndex;
    public static String fontName;
    public static Typeface font;

    public PrintableGenerator(Context ctx) {
        loadFonts(ctx, "fonts/");
        loadFontSettings(ctx);

        fontIndex = -1;
        if (font == null) {
            loadNextFont(ctx);
        }
    }

    protected boolean loadFonts(Context ctx, String path) {
        String[] list = new String[0];
        try {
            list = ctx.getAssets().list(path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (list.length > 0) {
            // This is a folder
            for (String file : list) {
                if (!loadFonts(ctx,path + "/" + file))
                    return false;
                else {
                    String name = file.split("\\.")[0];
                    name = name.replace("_", " ");
                    addFont(ctx, name, file);
                }
            }
        }

        return true;
    }

    protected void loadFontSettings(Context ctx) {
        SharedPreferences prefs = ctx
                .getSharedPreferences("font_settings", Context.MODE_PRIVATE);
        String name = prefs.getString("font", null);
        if (fonts.containsKey(name)) {
            fontName = name;
            font = fonts.get(name);
        }
    }

    protected void saveFontSettings(Context ctx) {
        SharedPreferences prefs = ctx
                .getSharedPreferences("font_settings",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String name = fontName;
        editor.putString("font", name);
        editor.commit();
    }

    public void addFont(Context ctx, String name, String file) {
        fonts.put(name, Typeface.createFromAsset(ctx.getAssets(), "fonts/" + file));
    }

    public void loadNextFont(Context ctx) {
        fontIndex++;
        if (fontIndex >= fonts.size()) {
            fontIndex = 0;
        }
        fontName = fonts.keySet().toArray(new String[1])[fontIndex];
        font = fonts.get(fontName);
        saveFontSettings(ctx);
    }

    public Bitmap buildOutput(PrintableItem item) {
        //Paints for text
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (PrinterManager.getMode() != null && PrinterManager.getModel() != null
                && PrinterManager.getMode().equals("roll") && PrinterManager.getModel().contains("820")) {
            text.setColor(Color.RED);
        } else {
            text.setColor(Color.BLACK);
        }

        // Find the length of the printable
        Rect bounds = new Rect();
        int margin = (int) (MAX_SIZE * MARGIN);
        int length = margin * 2;

        String data = item.getData();
        boolean maxed = false;
        int textSize = 11;
        while (!maxed) {
            text.setTextSize((textSize + 1) * 3 * 3); //text was tiny.
            text.setFakeBoldText(true);
            text.setTypeface(PrintableGenerator.font);

            text.getTextBounds(data, 0, data.length(), bounds);
            int maximum = (MAX_SIZE - margin * 2);
            if (bounds.width() < maximum) {
                textSize++;
            } else {
                maxed = true;
            }
        }
        text.setTextSize(textSize * 3 * 3);
        length += bounds.height();

        // Draw the background of the bitmap
        Bitmap bitmap = Bitmap.createBitmap(MAX_SIZE, length, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setStyle(Paint.Style.FILL);
        bg.setColor(Color.WHITE);
        canvas.drawPaint(bg);

        // Draw the text.
        text.getTextBounds(data, 0, data.length(), bounds);
        int x = (MAX_SIZE - bounds.width()) /2;
        int y = margin + bounds.height();
        canvas.drawText(data, x, y, text);

        Matrix counter = new Matrix();
        counter.postRotate(-90);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(),
                counter, true);

        return bitmap;
    }
}
