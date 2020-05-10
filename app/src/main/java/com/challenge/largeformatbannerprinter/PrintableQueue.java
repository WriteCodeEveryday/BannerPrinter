package com.challenge.largeformatbannerprinter;

import android.content.Context;
import android.graphics.Bitmap;

import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterInfo;
import com.brother.ptouch.sdk.PrinterStatus;
import java.util.Stack;

public class PrintableQueue {
    private static Stack<PrintableItem> items = new Stack<PrintableItem>();
    private static Thread runnable;

    public static void addItem(PrintableItem item, final Context ctx) {
        items.push(item);

        if (runnable == null) {
            runnable = new Thread() {
                public void run() {
                    System.out.println("Printing");
                    Printer temp = PrinterManager.getPrinter();
                    temp.startCommunication();

                    try {
                        while (items.size() > 0) {
                            PrintableItem printed = items.pop();
                            PrintableGenerator pr = new PrintableGenerator(ctx);
                            Bitmap bmp = pr.buildOutput(printed);
                            PrinterStatus result = temp.printImage(bmp);
                            if (result.errorCode != PrinterInfo.ErrorCode.ERROR_NONE) {
                                System.out.println("Error: " + result.errorCode);
                            }
                        }
                    } catch (Exception e) {
                        temp.endCommunication();
                        runnable = null;
                    }

                    temp.endCommunication();
                    runnable = null;
                }
            };
            runnable.start();
        }
    }
}
