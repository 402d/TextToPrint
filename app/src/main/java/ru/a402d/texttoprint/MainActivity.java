package ru.a402d.texttoprint;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements BillingProcessor.IBillingHandler  {
    private double fontSize = 17;
    private double printFontSize = 26;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 777;

    SharedPreferences sPref;
    final String SAVED_SIZE = "saved_size";

    private static BillingProcessor bp = null;

    String htmlHead = "<html>" +
            "<head>" +
            "<style>" +
            "html,body{margin:0;padding:0;font-size:%.3fpx;}" +
            "body{padding:8px;font-family:monospace;white-space:pre-wrap}" +
            "@media print {" +
            "body{font-size:%.3fpx}" +
            "}" +
            "</style>" +
            "</head>" +
            "<body>" ;
    String htmlBody=
            "<h2>Text To Print</h2>" +
                    "<p>It is very simle app.</p>" +
                    "<ul>" +
                    "<li>Receive Intent.SEND or Intent.VIEW with text/plain</li>" +
                    "<li>Place text into WebVIEW</li>" +
                    "<li>Create print document adapter</li>" +
                    "</ul>" +
                    "<b>Font A (32 chars on 58mm Roll)</b><br>" +
                    "123456789 123456789 123456789 12<br><br>" +
                    "<b>Font B (42 chars on 58mm Roll)</b><br>" +
                    "123456789 123456789 123456789 123456789 12<br><br>" +
                    "<b>Font C (65 chars in line on A4)</b><br>" +
                    "123456789 123456789 123456789 123456789 123456789 123456789 12345<br><br>" +
                    "<b>Font D (80 chars in line on A4)</b><br>" +
                    "123456789 123456789 123456789 123456789 123456789 123456789 123456789 1234567890<br><br>" +
                    "Lorem Ipsum - это текст-рыба, часто используемый в печати и вэб-дизайне. Lorem Ipsum является стандартной \"рыбой\" для текстов на латинице с начала XVI века. В то время некий безымянный печатник создал большую коллекцию размеров и форм шрифтов, используя Lorem Ipsum для распечатки образцов.<br><br>" +
                    "O Lorem Ipsum é um texto modelo da indústria tipográfica e de impressão. O Lorem Ipsum tem vindo a ser o texto padrão usado por estas indústrias desde o ano de 1500, quando uma misturou os caracteres de um texto para criar um espécime de livro.<br><br>" +
                    "Lorem Ipsum adalah contoh teks atau dummy dalam industri percetakan dan penataan huruf atau typesetting. Lorem Ipsum telah menjadi standar contoh teks sejak tahun 1500an, saat seorang tukang cetak yang tidak dikenal mengambil sebuah kumpulan teks dan mengacaknya untuk menjadi sebuah buku contoh huruf. <br><br>" ;
    String htmlFooter="</body></html>";

    WebView webView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab =  findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createWebPrintJob(webView);
            }
        });

        if (BillingProcessor.isIabServiceAvailable(this)) {
            bp = new BillingProcessor(this, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAi7uF/oUlFp8wDgjbbCYvBZ7zPwtcxqx4GFUm2hv+awP4zJBYLbYYeNMlfItpuqPx3z8Mjf+uLoJ68QDv23opG/nye5SFqdo6ly23k0wQGyyAAEBAGGBwOSrXX93INglHrXYohQW103oChFlw09FQ4IZ+5vBIRDv1/Qs3Nl/7Ii1rhXQ8rq+iHDNpAb9v1kNZRqmFO9qf+C+0cdiArWE+LEJ1K1IpnLZoqG7y+jX2xej53izjgtLWLU7w/2Umt2DkLd18qDQPr4itAlBWgxvvowxtOnut30NHP6df29hAbv4UUQGw4PzY4EbQ4set7pRpE/wGq6kQxKYWkD8QJm3QZwIDAQAB", this);
            bp.initialize();
        }

        webView =  findViewById(R.id.wview);

        sPref = getPreferences(MODE_PRIVATE);
        int savedSize = sPref.getInt(SAVED_SIZE, 1);
        setFont(savedSize);

    }

    private void parseIntent(Intent intent){


        String action = intent.getAction();
        if (action == null || action.equals(Intent.ACTION_MAIN)) {
            webView.loadData(String.format(Locale.US,htmlHead,fontSize,printFontSize)+htmlBody+htmlFooter,"text/html; charset=utf-8","utf-8");
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
                webView.loadData(String.format(Locale.US,htmlHead,fontSize,printFontSize)+htmlBody+htmlFooter,"text/html; charset=utf-8","utf-8");
                return;
            }
            Uri uri = extras.getParcelable(Intent.EXTRA_STREAM);
            String stringText = extras.getString(Intent.EXTRA_TEXT);
            if (uri != null) {
                fileBePrinted(uri);
                return;
            }
            if (stringText == null || stringText.trim().length() <= 0) {
                webView.loadData(String.format(Locale.US,htmlHead,fontSize,printFontSize)+htmlBody+htmlFooter,"text/html; charset=utf-8","utf-8");
                return;
            }
            stringBePrinted(stringText);
        }

    }

    private void stringBePrinted(String s){
        String[] separated = s.split("\n");
        StringBuilder ss= new StringBuilder();
        for (String v : separated ) {
            ss.append(v);
            ss.append("<br/>");
        }
        webView.loadData(String.format(Locale.US,htmlHead,fontSize,printFontSize)+ss.toString()+htmlFooter,"text/html; charset=utf-8","utf-8");
        // createWebPrintJob(webView);
    }

    private void fileBePrinted(Uri uri){
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

        webView.loadData(String.format(Locale.US,htmlHead,fontSize,printFontSize)+sb.toString()+htmlFooter,"text/html; charset=utf-8","utf-8");
        // createWebPrintJob(webView);
    }


    public boolean onCreateOptionsMenu(Menu menu) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
         menu.add(0,1,1,"Grant READ");
        }
        menu.add(0,2,2,"Font A");
        menu.add(0,3,3,"Font B");
        menu.add(0,4,4,"Font C");
        menu.add(0,5,5,"Font D");

        menu.add(0,6,6,"Buy me a coffee");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id=item.getItemId();
        if(id==android.R.id.home){
            finish();
        }else if(id==1){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                }
            }
        }else if(id==2){
            saveFont(1);
            setFont(1);
        }else if(id==3){
            saveFont(2);
            setFont(2);
        }else if(id==4){
            saveFont(3);
            setFont(3);
        }else if(id==5){
            saveFont(4);
            setFont(4);
        }else if(id==6){
            bp.purchase(this,"coffee");
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("ApplySharedPref")
    private void saveFont(int i){
        sPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putInt(SAVED_SIZE, i);
        ed.commit();
    }

    private void setFont(int s){
        switch (s){
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
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case  MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    parseIntent(getIntent());
                }
            }
        }
    }


    @Override
    public void onProductPurchased(@NonNull String productId, @Nullable TransactionDetails details) {
        webView.loadData(String.format(Locale.US,htmlHead,fontSize,printFontSize)+"<h1>Thanks !</h1>"+htmlFooter,"text/html; charset=utf-8","utf-8");
        bp.consumePurchase(productId);
    }

    @Override
    public void onPurchaseHistoryRestored() {

    }

    @Override
    public void onBillingError(int errorCode, @Nullable Throwable error) {

    }

    @Override
    public void onBillingInitialized() {

    }


    public class PrintDocumentAdapterWrapper extends PrintDocumentAdapter{

        private final PrintDocumentAdapter delegate;
        PrintDocumentAdapterWrapper(PrintDocumentAdapter adapter){
            super();
            this.delegate = adapter;
        }

        @Override
        public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {
            delegate.onLayout(oldAttributes, newAttributes,  cancellationSignal, callback,  extras);
            Log.d("ANTSON","onLayout");
        }

        @Override
        public void onWrite(PageRange[] pages, ParcelFileDescriptor destination, CancellationSignal cancellationSignal, WriteResultCallback callback) {
            delegate.onWrite( pages, destination,cancellationSignal,callback);
            Log.d("ANTSON","onWrite");
        }

        public void onFinish(){
            delegate.onFinish();
            Log.d("ANTSON","onFinish");
        }

    }

    //create a function to create the print job
    private void createWebPrintJob(WebView webView) {

        //create object of print manager in your device
        PrintManager printManager = (PrintManager) this.getSystemService(Context.PRINT_SERVICE);

        //create object of print adapter
        PrintDocumentAdapterWrapper printAdapter = new PrintDocumentAdapterWrapper(webView.createPrintDocumentAdapter());

        //provide name to your newly generated pdf file
        String jobName = "Text2Print";

        //open print dialog
        if (printManager != null) {
            printManager.print(jobName, printAdapter, new PrintAttributes.Builder().setMinMargins(new PrintAttributes.Margins(0,0,0,0)).build());
        }else{
            webView.loadData(String.format(Locale.US,htmlHead,fontSize,printFontSize)+"PrintManager is null"+htmlFooter,"text/html; charset=utf-8","utf-8");

        }
    }

}
