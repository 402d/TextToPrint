package ru.a402d.texttoprint;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity  {
    private final static String TAG = "Antson";
    private double fontSize = 17;
    private double printFontSize = 26;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 777;

    SharedPreferences sPref;
    final String SAVED_SIZE = "saved_size";

    final String htmlHead = "<html>" +
            "<head>" +
            "<style>" +
            "html,body{margin:0;padding:0;font-size:%.3fpx;}" +
            "body{padding:8px;font-family:monospace;white-space:pre-wrap}" +
            "@media print {" +
            "body{font-size:%.3fpx}" +
            "}" +
            "</style>" +
            "</head>" +
            "<body>";
    String htmlBody = "";
    final String htmlFooter = "</body></html>";

    WebView webView;



    FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> createWebPrintJob(webView));


        webView = findViewById(R.id.wview);

        htmlBody = readAssets("html.html");

        sPref = getPreferences(MODE_PRIVATE);
        int savedSize = sPref.getInt(SAVED_SIZE, 1);
        setFont(savedSize);

    }

    @Override
    protected void onResume() {
        super.onResume();
        fab.setEnabled(true);
    }

    // read html body from assets
    private String readAssets(@SuppressWarnings("SameParameterValue") String fileName) {
        try {
            StringBuilder buf = new StringBuilder();
            InputStream inputStream = getAssets().open(fileName);
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String str;

            while ((str = in.readLine()) != null) {
                buf.append(str);
            }
            in.close();
            return buf.toString();
        } catch (Exception e) {
            return "";
        }

    }

    private void parseIntent(Intent intent) {


        String action = intent.getAction();
        if (action == null || action.equals(Intent.ACTION_MAIN)) {
            webView.loadData(String.format(Locale.US, htmlHead, fontSize, printFontSize) + htmlBody + htmlFooter, "text/html; charset=utf-8", "utf-8");
            return;
        }
        if (action.equals(Intent.ACTION_VIEW)) {
            fileBePrinted(intent.getData());
            return;
        }

        Bundle extras;

        if (action.equals(Intent.ACTION_SEND)) {
            extras = intent.getExtras();
            if (extras == null) {
                htmlBody = "<h1>Error</h1><p>ACTION_SEND no Extras</p>";
                webView.loadData(String.format(Locale.US, htmlHead, fontSize, printFontSize) + htmlBody + htmlFooter, "text/html; charset=utf-8", "utf-8");
                return;
            }
            Uri uri = extras.getParcelable(Intent.EXTRA_STREAM);
            String stringText = extras.getString(Intent.EXTRA_TEXT);
            if (uri != null) {
                fileBePrinted(uri);
                return;
            }
            if (stringText == null || stringText.trim().length() == 0) {
                webView.loadData(String.format(Locale.US, htmlHead, fontSize, printFontSize) + htmlBody + htmlFooter, "text/html; charset=utf-8", "utf-8");
                return;
            }
            stringBePrinted(stringText);
        }

    }

    private void stringBePrinted(String s) {
        String[] separated = s.split("\n");
        StringBuilder ss = new StringBuilder();
        for (String v : separated) {
            ss.append(v);
            ss.append("<br/>");
        }
        webView.loadData(String.format(Locale.US, htmlHead, fontSize, printFontSize) + ss.toString() + htmlFooter, "text/html; charset=utf-8", "utf-8");
        // createWebPrintJob(webView);
    }

    private void fileBePrinted(Uri uri) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)));
            while ((line = br.readLine()) != null) {
                sb.append(line).append("<br>");
            }

        } catch (Exception e) {
            sb.append(e.getMessage());
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        webView.loadData(String.format(Locale.US, htmlHead, fontSize, printFontSize) + sb.toString() + htmlFooter, "text/html; charset=utf-8", "utf-8");
        // createWebPrintJob(webView);
    }


    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuCompat.setGroupDividerEnabled(menu, true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            menu.add(0, 1, 1, "Grant READ");
        }
        menu.add(1, 7, 2, "Demo text");
        menu.add(1, 9, 4, "Insert clipboard");
        menu.add(2, 2, 6, "Font A");
        menu.add(2, 3, 7, "Font B");
        menu.add(2, 4, 8, "Font C");
        menu.add(2, 5, 9, "Font D");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        } else if (id == 1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                }
            }
        } else if (id == 2) {
            saveFont(1);
            setFont(1);
        } else if (id == 3) {
            saveFont(2);
            setFont(2);
        } else if (id == 4) {
            saveFont(3);
            setFont(3);
        } else if (id == 5) {
            saveFont(4);
            setFont(4);
        } else if (id == 7) {
            htmlBody = readAssets("html.html");
            webView.loadData(String.format(Locale.US, htmlHead, fontSize, printFontSize) + htmlBody + htmlFooter, "text/html; charset=utf-8", "utf-8");

        } else if (id == 9) {
            try {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData buf = Objects.requireNonNull(clipboard.getPrimaryClip());
                stringBePrinted(buf.getItemAt(0).coerceToText(this).toString());
            } catch (Exception e) {
                e.getStackTrace();
            }

        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("ApplySharedPref")
    private void saveFont(int i) {
        sPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putInt(SAVED_SIZE, i);
        ed.commit();
    }

    private void setFont(int s) {
        switch (s) {
            case 1:
                fontSize = 17;
                printFontSize = 26;
                break;
            case 2:
                fontSize = 13;
                printFontSize = 20;
                break;
            case 3:
                fontSize = 8.5;
                printFontSize = 17;
                break;
            case 4:
                fontSize = 8.5;
                printFontSize = 14;
                break;
        }

        parseIntent(getIntent());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                parseIntent(getIntent());
            }
        }
    }



    public static class PrintDocumentAdapterWrapper extends PrintDocumentAdapter {

        private final PrintDocumentAdapter delegate;

        PrintDocumentAdapterWrapper(PrintDocumentAdapter adapter) {
            super();
            this.delegate = adapter;
        }

        @Override
        public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {
            delegate.onLayout(oldAttributes, newAttributes, cancellationSignal, callback, extras);
            Log.d(TAG, "onLayout");
        }

        @Override
        public void onWrite(PageRange[] pages, ParcelFileDescriptor destination, CancellationSignal cancellationSignal, WriteResultCallback callback) {
            delegate.onWrite(pages, destination, cancellationSignal, callback);
            Log.d(TAG, "onWrite");
        }

        public void onFinish() {
            delegate.onFinish();
            Log.d(TAG, "onFinish");
        }

    }

    //create a function to create the print job
    private void createWebPrintJob(WebView webView) {
        fab.setEnabled(false);
        //create object of print manager in your device
        PrintManager printManager = (PrintManager) this.getSystemService(Context.PRINT_SERVICE);

        //create object of print adapter
        PrintDocumentAdapterWrapper printAdapter = new PrintDocumentAdapterWrapper(webView.createPrintDocumentAdapter());

        //provide name to your newly generated pdf file
        String jobName = "Text2Print";

        //open print dialog
        if (printManager != null) {
            printManager.print(jobName, printAdapter, new PrintAttributes.Builder().setMinMargins(new PrintAttributes.Margins(0, 0, 0, 0)).build());
        } else {
            webView.loadData(String.format(Locale.US, htmlHead, fontSize, printFontSize) + "PrintManager is null" + htmlFooter, "text/html; charset=utf-8", "utf-8");

        }
    }

}
