package br.com.pax.demoprintapp;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.pax.a_printer.R;
import com.pax.dal.IDAL;
import com.pax.dal.IPrinter;
import com.pax.dal.exceptions.PrinterDevException;
import com.pax.gl.page.IPage;
import com.pax.gl.page.PaxGLPage;
import com.pax.neptunelite.api.NeptuneLiteUser;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Handler handler = new Handler();
    public static NeptuneLiteUser neptuneLiteUser;
    public static IDAL dal;
    public static PaxGLPage paxGLPage;
    public static IPrinter printer;

    public void onShowMessage(final String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get the entity of printer.
        neptuneLiteUser = NeptuneLiteUser.getInstance();
        try{
            dal = neptuneLiteUser.getDal(this.getApplicationContext());
        }catch (Exception e){
            e.printStackTrace();
            Log.d("getDal", e.toString());
        }

        paxGLPage = PaxGLPage.getInstance(this.getApplicationContext());

        printer = dal.getPrinter();

        Button btn_print_img = (Button) findViewById(R.id.btn_print_img);

        btn_print_img.setOnClickListener(this);
    }

    @Override
    /**
     * Button click event
     */
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_print_img:
                Toast.makeText(this, "Printing image", Toast.LENGTH_LONG).show();
                print_img();
                break;
        }
    }

    private int start(IPrinter printer) {
        try {
            while (true) {
                int ret = printer.start();
                // printer is busy, please wait
                if (ret == 1) {
                    SystemClock.sleep(100);
                    continue;
                } else if (ret == 2) {
                    onShowMessage("Printer is Out of Paper");
                    return -1;
                } else if (ret == 8) {
                    onShowMessage("Printer is too hot");
                    return -1;
                } else if (ret == 9) {
                    onShowMessage("Voltage is too low!");
                    return -1;
                } else if (ret != 0) {
                    return -1;
                }

                return 0;
            }
        } catch (PrinterDevException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Generate iPage Bitmap
     */
    private Bitmap generate(){

        IPage page = paxGLPage.createPage();

        page.adjustLineSpace(-9);

        //To set the font file
        //page.setTypeFace("/data/resource/font/DroidSansFallback.ttf");

        page.addLine().addUnit("Print test string\t\t\t\t\t10", 36, IPage.EAlign.CENTER);
        page.addLine().addUnit("Print test string", 36, IPage.EAlign.LEFT);
        page.addLine().addUnit("Print test string B", 36, IPage.EAlign.RIGHT,
                IPage.ILine.IUnit.TEXT_STYLE_UNDERLINE);
        page.addLine().addUnit("\n\n\n", 36);

        return paxGLPage.pageToBitmap(page, 384);
    }

    public void init() {
        try {
            printer.init();
        } catch (PrinterDevException e) {
            e.printStackTrace();
            Log.d("init", e.toString());
        }
    }

    protected void printBitmap(Bitmap bitmap) {
        init();
        try {
            printer.printBitmap(bitmap);
        } catch (PrinterDevException e) {
            e.printStackTrace();
            Log.d("printBitmap", e.toString());
        }
        start(printer);
    }

    /**
     *Print image
     */
    private void print_img(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                printBitmap(generate());
            }
        }).start();
    }
}
