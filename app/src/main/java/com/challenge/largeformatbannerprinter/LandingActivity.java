package com.challenge.largeformatbannerprinter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;

import com.challenge.largeformatbannerprinter.notesforsupport.R;


public class LandingActivity extends AppCompatActivity {
    int REQUEST_WRITE_IMAGE = 1337;
    String SEPARATOR = "__END__";
    String PAGE_BREAK = "________________";

    PrintableGenerator pr;
    public static String orientation = "landscape";


    ArrayList<Bitmap> items = new ArrayList<Bitmap>();
    String[] text_data;

    @Override
    protected void onResume() {
        super.onResume();
        hideUI();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        pr = new PrintableGenerator(getApplicationContext());

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            try {
                Uri docx = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                FileInputStream file = new FileInputStream(getContentResolver()
                        .openFileDescriptor(docx, "r").getFileDescriptor());

                StringBuffer fileContent = new StringBuffer("");
                byte[] buffer = new byte[1024];

                int n;
                while ((n = file.read(buffer)) != -1)
                {
                    fileContent.append(new String(buffer, 0, n));
                }

                String parsed = fileContent.toString();
                text_data = new String[] { parsed };
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            loadTutorial();
        }

        hideUI();
        loadPrinterPreferences();
        setUpPrinterOptions();
        recreatePrintables();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_WRITE_IMAGE) {
            if (resultCode == RESULT_OK) {
                Bitmap image = items.get(0);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] bytes = stream.toByteArray();
                image.recycle();

                try {
                    OutputStream out = getContentResolver().openOutputStream(data.getData());
                    out.write(bytes);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void loadTutorial() {
        text_data = new String[]{};
    }

    private void connectToPrinter() {
        PrinterManager.setConnection(PrinterManager.CONNECTION.BLUETOOTH);
        PrinterManager.findPrinter(PrinterManager.getModel(), PrinterManager.getConnection());

        if (PrinterManager.getPrinter() == null) {
            PrinterManager.setConnection(PrinterManager.CONNECTION.WIFI);
            PrinterManager.findPrinter(PrinterManager.getModel(), PrinterManager.getConnection());
        }

        if (PrinterManager.getPrinter() != null) {
            final String[] options = PrinterManager.getLabelRoll();
            final RadioButton label = findViewById(R.id.radio_option_label);
            final RadioButton roll = findViewById(R.id.radio_option_roll);
            if (options.length == 2) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        label.setText(options[0]);
                        roll.setText(options[1]);

                        if (label.isChecked()) {
                            PrinterManager.loadLabel();
                            recreatePrintables();
                        } else if (roll.isChecked()) {
                            PrinterManager.loadRoll();
                            recreatePrintables();
                        }

                        findViewById(R.id.radio_option_label)
                                .setVisibility(View.VISIBLE);
                        findViewById(R.id.radio_option_roll)
                                .setVisibility(View.VISIBLE);
                    }
                });
            }
        }
    }

    protected void loadPrinterPreferences() {
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences("printer_settings", Context.MODE_PRIVATE);
        final String printer = prefs.getString("printer", null);
        final String mode = prefs.getString("mode", null);

        final RadioButton label = findViewById(R.id.radio_option_label);
        final RadioButton roll = findViewById(R.id.radio_option_roll);


        if(printer != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    PrinterManager.setModel(printer);
                    connectToPrinter();
                    if (mode != null) {
                        switch(mode) {
                            case "label":
                                PrinterManager.loadLabel();
                                label.setChecked(true);
                                break;
                            case "roll":
                                PrinterManager.loadRoll();
                                roll.setChecked(true);
                                break;
                        }
                    }
                }
            }).start();
        }
    }

    protected void savePrinterPreferences() {
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences("printer_settings",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String printer = PrinterManager.getModel();
        String mode = PrinterManager.getMode();

        editor.putString("printer", printer);
        editor.putString("mode", mode);


        editor.commit();
    }

    private void hideUI() {
        View mContentView = findViewById(R.id.fullscreen_layout);
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

    }

    private double round(double input) {
        return Math.round(input * 100.0) / 100.0;
    }


    private void updatePreview() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LinearLayout layout = findViewById(R.id.image_preview);
                layout.removeAllViews();

                if (items.size() > 0) {
                    double inches = 2.4;

                    if (PrinterManager.getPrinter() != null) {
                        if (PrinterManager.getModel().contains("820")) {
                            inches = 2.4;
                        } else if (PrinterManager.getModel().contains("1110")) {
                            if (PrinterManager.getMode().equals("label")) {
                                inches = 2.4;
                            } else if (PrinterManager.getMode().equals("roll")) {
                                inches = 4;
                            }
                        } else if (PrinterManager.getModel().toLowerCase().contains("rj")) {
                            if (PrinterManager.getMode().equals("label")) {
                                inches = 2;
                            } else if (PrinterManager.getMode().equals("roll")) {
                                inches = 4;
                            }
                        } else if (PrinterManager.getModel().toLowerCase().contains("pj")) {
                            inches = 8.5;
                        }
                    }

                    Bitmap image = items.get(0);
                    double[] physical = new double[] {
                            inches,
                            ((image.getHeight() * inches) / image.getWidth())
                    };

                    DisplayMetrics met = getApplicationContext()
                            .getResources()
                            .getDisplayMetrics();

                    float ratio = (float) image.getWidth() / (float) image.getHeight();
                    int height =  (int) (met.heightPixels * 0.4);
                    int width = (int) (height * ratio);

                    image  = Bitmap.createScaledBitmap(image, width, height, true);

                    int size = 3;
                    Bitmap borderImage = Bitmap.createBitmap(
                            image.getWidth() + size * 2,
                            image.getHeight() + size * 2,
                            image.getConfig());
                    Canvas canvas = new Canvas(borderImage);
                    canvas.drawColor(Color.BLACK);
                    canvas.drawBitmap(image, size, size, null);
                    image = borderImage;

                    TextView text = new TextView(getApplicationContext());
                    text.setText("Estimated Size: " + round(physical[0]) +
                            " inches wide, " + round(physical[1]) + " inches tall");
                    text.setGravity(Gravity.CENTER);
                    layout.addView(text, 0);

                    ImageView preview = new ImageView(getApplicationContext());
                    preview.setImageBitmap(image);
                    preview.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            pr.loadNextFont(getApplicationContext());
                            recreatePrintables();
                        }
                    });
                    layout.addView(preview, 0);


                    findViewById(R.id.printer_buttons).setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void recreatePrintables() {
        new Thread() {
            @Override
            public void run() {
                savePrinterPreferences();
                if (text_data.length == 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            findViewById(R.id.printer_buttons).setVisibility(View.GONE);
                        }
                    });
                }

                items.clear();
                if (text_data.length > 0) {
                    PrintableItem printable = new PrintableItem(text_data[0]);
                    Bitmap bmp = pr.buildOutput(printable);
                    items.add(bmp);
                }

                updatePreview();
            }
        }.start();
    }

    private void setUpPrinterOptions() {

        findViewById(R.id.manual_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) LandingActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(findViewById(R.id.manual_text).getWindowToken(), 0);

                EditText input = (EditText) findViewById(R.id.manual_text);
                String text = "" + input.getText();
                text_data = text.split(SEPARATOR);

                recreatePrintables();
            }
        });

        findViewById(R.id.no_margin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrintableGenerator.MARGIN = 0f;
                recreatePrintables();
            }
        });

        findViewById(R.id.small_margins).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrintableGenerator.MARGIN = 0.01f;
                recreatePrintables();
            }
        });

        findViewById(R.id.larger_margins).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrintableGenerator.MARGIN = 0.025f;
                recreatePrintables();
            }
        });

        findViewById(R.id.largest_margin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrintableGenerator.MARGIN = 0.05f;
                recreatePrintables();
            }
        });

        findViewById(R.id.print).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PrinterManager.getPrinter() == null) {
                    String name = "LargeFormatOutput.png";
                    if (text_data.length > 0) {
                        name = text_data[0].split("\n")[0]
                                .replaceAll("[^a-zA-Z0-9 ]", "")
                                .replaceAll(" ", "_")
                                .toUpperCase()
                                + ".png";
                    }
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("image/png");
                    intent.putExtra(Intent.EXTRA_TITLE, name);
                    startActivityForResult(intent, REQUEST_WRITE_IMAGE);
                } else {
                    PrintableItem printable = new PrintableItem(text_data[0]);
                    PrintableQueue.addItem(printable, getApplicationContext());
                }
            }
        });

        String currentModel = PrinterManager.getModel();
        final String currentMode = PrinterManager.getMode();

        final String[] supportedModels = PrinterManager.getSupportedModels();

        final RadioGroup printers = this.findViewById(R.id.printer_selection_group);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                printers.removeAllViews();
            }
        });

        for (int i = 0; i < supportedModels.length; i++) {
            final RadioButton button = new RadioButton(this);
            if (currentModel != null) {
                button.setChecked(supportedModels[i].equals(currentModel));
            }
            button.setText(supportedModels[i]);
            button.setId(i);
            final int j = i;
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!v.isSelected()) {
                        PrinterManager.setModel(supportedModels[j]);

                        new Thread() {
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        findViewById(R.id.radio_option_label)
                                                .setVisibility(View.GONE);
                                        findViewById(R.id.radio_option_roll)
                                                .setVisibility(View.GONE);
                                    }
                                });

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        connectToPrinter();
                                    }
                                }).start();
                                setUpPrinterOptions();
                            }
                        }.start();
                    }
                }
            });

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printers.addView(button);
                }
            });
        }

        this.findViewById(R.id.radio_option_label).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                new Thread() {
                    @Override
                    public void run() {
                        PrinterManager.loadLabel();
                        PrinterManager.setWorkingDirectory(getApplicationContext());
                        recreatePrintables();
                    }
                }.start();
            }
        });

        this.findViewById(R.id.radio_option_roll).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                new Thread() {
                    @Override
                    public void run() {
                        PrinterManager.loadRoll();
                        PrinterManager.setWorkingDirectory(getApplicationContext());
                        recreatePrintables();
                    }
                }.start();
            }
        });

        if (currentMode != null) {
            final RadioButton label = this.findViewById(R.id.radio_option_label);
            final RadioButton roll = this.findViewById(R.id.radio_option_roll);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (currentMode) {
                        case "roll":
                            roll.setChecked(true);
                            break;
                        case "label":
                            label.setChecked(true);
                            break;
                    }
                }
            });
        }
    }
}
