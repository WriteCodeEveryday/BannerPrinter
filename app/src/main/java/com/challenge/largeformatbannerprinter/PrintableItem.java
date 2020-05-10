package com.challenge.largeformatbannerprinter;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.ArrayList;

public class PrintableItem {
    String data;
    //int MAX_SIZE = (int) (PrintableGenerator.MAX_SIZE * (1 - (2 * PrintableGenerator.MARGIN)));

    public PrintableItem(String d) {
        data = d.trim();
    }

    public String getData() { return data; };

    /*
    public String[] getPrintables(int textSize) {
        int size = MAX_SIZE;

        Rect bounds = new Rect();
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setTextSize(textSize * 3 * 3);
        text.setColor(Color.BLACK);
        text.setFakeBoldText(true);
        text.setTypeface(PrintableGenerator.font);

        String line = "";
        ArrayList<String> output = new ArrayList<String>();
        String[] breaks = data.split("\n");
        for (int i = 0; i < breaks.length; i++) {
            String[] segments = breaks[i].split(" ");

            for (int j = 0; j < segments.length; j++) {
                String temp = line + " " + segments[j];
                text.getTextBounds(temp, 0, temp.length(), bounds);
                if (bounds.width() > size) {
                    output.add(output.size(), line);
                    line = "";
                }
                line += " " + segments[j];
            }

            output.add(output.size(), line);
            line = "";
        }

        String[] array = output.toArray(new String[0]);
        for (int i = 0; i < array.length; i++) {
            array[i] = array[i].trim();
        }

        return array;
    }
     */
}
