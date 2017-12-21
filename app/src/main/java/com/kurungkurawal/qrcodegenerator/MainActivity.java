package com.kurungkurawal.qrcodegenerator;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.sendgrid.SendGrid;
import com.sendgrid.SendGridException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private final String tag = "QRCGEN";
    private final int REQUEST_PERMISSION = 0xf0;

    private MainActivity self;
    private Snackbar snackbar;
    private Bitmap qrImage;

    private EditText txtQRText;
    private TextView txtSaveHint, txtProgress;
    private Button btnGenerate, btnReset;
    private ImageView imgResult;
    private ProgressBar loader;
    private Context mAppContext;
    String result;
    int j;
    ArrayList<Codes> codes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        self = this;
        txtProgress = findViewById(R.id.tv_progress);
        txtQRText   = (EditText)findViewById(R.id.txtQR);
        txtSaveHint = (TextView) findViewById(R.id.txtSaveHint);
        btnGenerate = (Button)findViewById(R.id.btnGenerate);
        btnReset    = (Button)findViewById(R.id.btnReset);
        imgResult   = (ImageView)findViewById(R.id.imgResult);
        loader      = (ProgressBar)findViewById(R.id.loader);
        txtProgress.setVisibility(View.GONE);
        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && ContextCompat.checkSelfPermission(self, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(self, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_PERMISSION);
                    return;
                }
                else {
                    generateImage(Integer.parseInt(txtQRText.getText().toString()));

                }

            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                self.reset();
            }
        });

        imgResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                self.confirm("Simpan Gambar ?", "Iya", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveImage();
                    }
                });
            }
        });

        txtQRText.setText("hello");
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                self.generateImage(Integer.parseInt(txtQRText.getText().toString()));
                //saveImage();
            } else {
                alert("Aplikasi tidak mendapat akses untuk menambahkan gambar.");
            }
        }
    }

    private void saveImage() {
        if (qrImage == null) {
            alert("Belum ada gambar.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
            return;
        }


        String fname = "qrcode-" + Calendar.getInstance().getTimeInMillis();
        boolean success = true;
        try {
            String result = MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    qrImage,
                    fname,
                    "QRCode Image"
            );
            if (result == null) {
                success = false;
            } else {
                Log.e(tag, result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }

        if (!success) {
            alert("Gagal menyimpan gambar");
        } else {
            self.snackbar("Gambar tersimpan ke gallery.");
        }
    }

    private void alert(String message){
        AlertDialog dlg = new AlertDialog.Builder(self)
                .setTitle("QRCode Generator")
                .setMessage(message)
                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        dlg.show();
    }

    private void confirm(String msg, String yesText, final AlertDialog.OnClickListener yesListener) {
        AlertDialog dlg = new AlertDialog.Builder(self)
                .setTitle("Konfirmasi")
                .setMessage(msg)
                .setNegativeButton("Batal", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(yesText, yesListener)
                .create();
        dlg.show();
    }

    private void snackbar(String msg) {
        if (self.snackbar != null) {
            self.snackbar.dismiss();
        }

        self.snackbar = Snackbar.make(
                findViewById(R.id.mainBody),
                msg, Snackbar.LENGTH_SHORT);

        self.snackbar.show();
    }

    private void endEditing(){
        txtQRText.clearFocus();
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.
                INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }


    private void generateImage(final int i){
        txtProgress.setVisibility(View.VISIBLE);
        for(j= 1; j<i+1; j++){
            self.qrImage = null;
            final String text = "Access Code " + j;
            if(text.trim().isEmpty()){
                alert("Ketik dulu data yang ingin dibuat QR Code");
                return;
            }

            endEditing();
            showLoadingVisible(true);
            final int finalJ = j;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int size = imgResult.getMeasuredWidth();
                    if( size > 1){
                        Log.e(tag, "size is set manually");
                        size = 260;
                    }


                    Map<EncodeHintType, Object> hintMap = new EnumMap<>(EncodeHintType.class);
                    hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");
                    hintMap.put(EncodeHintType.MARGIN, 1);
                    QRCodeWriter qrCodeWriter = new QRCodeWriter();
                    try {
                        BitMatrix byteMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size,
                                size, hintMap);
                        int height = byteMatrix.getHeight();
                        int width = byteMatrix.getWidth();
                        Bitmap qrImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                        for (int x = 0; x < width; x++){
                            for (int y = 0; y < height; y++){
                                qrImage.setPixel(x, y, byteMatrix.get(x,y) ? Color.BLACK : Color.WHITE);
                            }
                        }
                        String fname = "qrcode-" + finalJ;
                        boolean success = true;
                        try {
                            result = MediaStore.Images.Media.insertImage(
                                    getContentResolver(),
                                    qrImage,
                                    fname,
                                    "QRCode Image"
                            );
                            Codes mCode = new Codes("Code "+finalJ, Uri.parse(result));
                            Log.d("numFiles", String.valueOf(codes.size()));
                            codes.add(mCode);
                            if(codes.size() == 20){
                                new EmailAsyncTask(getApplicationContext(),codes).execute();
                            }
                            if (result == null) {
                                success = false;
                            } else {
                                Log.e(tag, result);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            success = false;
                        }

                        if (!success) {
                            Log.e(tag,"Gagal menyimpan gambar");
                        } else {
                            //self.snackbar("Gambar tersimpan ke gallery.");
                        }

                        self.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                txtProgress.setText(j+"/"+i);
                                //self.showImage(self.qrImage);
                                self.showLoadingVisible(false);
                                //self.snackbar("QRCode telah dibuat");
                            }
                        });

//                        saveImage();
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                reset();
//                            }
//                        });
                    } catch (WriterException e) {
                        e.printStackTrace();
                        alert(e.getMessage());
                    }
                }
            }).start();
        }


    }

    private void showLoadingVisible(boolean visible){
        if(visible){
            showImage(null);
        }

        loader.setVisibility(
                (visible) ? View.VISIBLE : View.GONE
        );
    }

    private void reset(){
        txtQRText.setText("");
        showImage(null);
        endEditing();
    }

    private void showImage(Bitmap bitmap) {
        if (bitmap == null) {
            imgResult.setImageResource(android.R.color.transparent);
            qrImage = null;
            txtSaveHint.setVisibility(View.GONE);
        } else {
            imgResult.setImageBitmap(bitmap);
            txtSaveHint.setVisibility(View.VISIBLE);
        }
    }
    private static class EmailAsyncTask extends AsyncTask<Void,Void,Void>{
        Context context;
        String mMsgResponse;
        ArrayList<Codes> codes = new ArrayList<>();
        public EmailAsyncTask(Context applicationContext, ArrayList<Codes> uri) {
            this.context = applicationContext;
            this.codes = uri;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                SendGrid sendgrid = new SendGrid("*******", "*******");

                SendGrid.Email email = new SendGrid.Email();

                // Get values from edit text to compose email
                // TODO: Validate edit texts
                email.addTo("ashishguptajiit@gmail.com");
                email.setFrom("mail@edge.io");
                email.setSubject("QR Codes");
                email.setText("Test");

                // Attach image
                if(codes.size()>0){
                    for(int x=0; x<codes.size();x++){
                        email.addAttachment(codes.get(x).getName()+".jpg", context.getContentResolver().openInputStream(codes.get(x).getmUri()));
                    }
                }



                // Send email, execute http request
                SendGrid.Response response = sendgrid.send(email);
                mMsgResponse = response.getMessage();

                Log.d("SendAppExample", mMsgResponse);

            } catch (SendGridException | IOException e) {
                Log.e("SendAppExample", e.toString());
            }
            return null;
        }
    }
}
