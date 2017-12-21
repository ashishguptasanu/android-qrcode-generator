package com.kurungkurawal.qrcodegenerator;

import android.net.Uri;

/**
 * Created by vinaygupta on 21/12/17.
 */

class Codes {


    String name;



    Uri mUri;
    public Codes(String name, Uri mUri){
        this.mUri = mUri;
        this.name = name;
    }
    public Uri getmUri() {
        return mUri;
    }
    public String getName() {
        return name;
    }
}
