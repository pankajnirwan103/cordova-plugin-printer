/*
    Copyright 2013-2014 appPlant UG

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/

package nirwan.cordova.plugin.printer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.os.Environment;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

public class Printer extends CordovaPlugin {

    private CallbackContext ctx;

    private String printAppIds[] = {
		"com.dynamixsoftware.printershare",			// Printer Share
        "kr.co.iconlab.BasicPrintingProfile",       // Bluetooth Smart Printing
        "com.blueslib.android.app",                 // Bluetooth SPP Printer API
        "com.brother.mfc.brprint",                  // Brother iPrint&Scan
        "com.brother.ptouch.sdk",                   // Brother Print Library
        "jp.co.canon.bsd.android.aepp.activity",    // Canon Easy-PhotoPrint
        "com.pauloslf.cloudprint",                  // Cloud Print
        "com.dlnapr1.printer",                      // CMC DLNA Print Client
        "com.dell.mobileprint",                     // Dell Mobile Print
        "com.printjinni.app.print",                 // PrintJinni
        "epson.print",                              // Epson iPrint
        "jp.co.fujixerox.prt.PrintUtil.PCL",        // Fuji Xerox Print Utility
        "jp.co.fujixerox.prt.PrintUtil.Karin",      // Fuji Xeros Print&Scan (S)
        "com.hp.android.print",                     // HP ePrint
        "com.blackspruce.lpd",                      // Let's Print Droid
        "com.threebirds.notesprint",                // NotesPrint print your notes
        "com.xerox.mobileprint",                    // Print Portal (Xerox)
        "com.zebra.kdu",                            // Print Station (Zebra)
        "net.jsecurity.printbot",                   // PrintBot
        "com.dynamixsoftware.printhand",            // PrintHand Mobile Print
        "com.dynamixsoftware.printhand.premium",    // PrintHand Mobile Print Premium
        "com.sec.print.mobileprint",                // Samsung Mobile Print
        "com.rcreations.send2printer",              // Send 2 Printer
        "com.ivc.starprint",                        // StarPrint
        "com.threebirds.easyviewer",                // WiFi Print
        "com.woosim.android.print",                 // Woosim BT printer
        "com.woosim.bt.app",                        // WoosimPrinter
        "com.zebra.android.zebrautilities",         // Zebra Utilities	
    };

    @Override
    public boolean execute (String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if ("print".equals(action)) {
            print(args, callbackContext);

            return true;
        }

        if ("isServiceAvailable".equals(action)) {
            isServiceAvailable(callbackContext);

            return true;
        }

        // Returning false results in a "MethodNotFound" error.
        return false;
    }

    private void isServiceAvailable (CallbackContext ctx) {
        JSONArray appIds  = this.getInstalledAppIds();
        Boolean available = appIds.length() > 0;
        JSONArray args    = new JSONArray();
        PluginResult result;

        args.put(available);
        args.put(appIds);

        result = new PluginResult(PluginResult.Status.OK, args);

        ctx.sendPluginResult(result);
    }


    private void print (final JSONArray args, CallbackContext ctx) {
        final Printer self = this;

        this.ctx = ctx;

        cordova.getActivity().runOnUiThread( new Runnable() {
            public void run() {
                JSONObject platformConfig = args.optJSONObject(1);
                String appId              = self.getPrintAppId(platformConfig);

                if (appId == null) {
                    self.ctx.success(4);
                    return;
                };

                String content    = args.optString(0, "<html></html>");
                Intent controller = self.getPrintController(appId);	
				
				String filename = null;  
				if(self.getJSONValue(platformConfig,"filename")!=null){
					filename = self.getJSONValue(platformConfig,"filename"); //Reading Filename to be send to print
				}
				String mimeType = null;
				if(self.getJSONValue(platformConfig,"mimeType")!=null){  //Reading mimeType
					mimeType = self.getJSONValue(platformConfig,"mimeType");
				}
                self.adjustSettingsForPrintController(controller,mimeType);
                self.loadContentIntoPrintController(content, controller,filename,mimeType);

				self.startPrinterApp(controller);
            }
        });
    }

    private String getPrintAppId (JSONObject platformConfig) {
        String appId = platformConfig.optString("appId", null);

        if (appId != null) {
            return (this.isAppInstalled(appId)) ? appId : null;
        } else {
            return this.getFirstInstalledAppId();
        }
    }
	
    private String getJSONValue (JSONObject platformConfig,String key) {
        String value = platformConfig.optString(key, null);
		return value;
    }

    private Intent getPrintController (String appId) {
        String intentId = "android.intent.action.SEND";

        if (appId.equals("com.rcreations.send2printer")) {
            intentId = "com.rcreations.send2printer.print";
        } else if (appId.equals("com.dynamixsoftware.printershare")) {
            intentId = "android.intent.action.VIEW";
        } else if (appId.equals("com.hp.android.print")) {
            intentId = "org.androidprinting.intent.action.PRINT";
        }

        Intent intent = new Intent(intentId);

        if (appId != null)
            intent.setPackage(appId);

        return intent;
    }

    private void adjustSettingsForPrintController (Intent intent,String mimeType) {
        if(mimeType == null){
			mimeType = "image/png";
		}
        String appId    = intent.getPackage();

        // Check for special cases that can receive HTML
        //if (appId.equals("com.rcreations.send2printer") || appId.equals("com.dynamixsoftware.printershare")) {
          //  mimeType = "text/html";
		   intent.setType(mimeType);
        //}

     //   intent.setType(mimeType);
    }

    private void loadContentIntoPrintController(String content, Intent intent,String filename,String mineType) {
        String mimeType = intent.getType();
		if(filename != null){			
			loadContentFromSDCardRoot(intent,filename,mimeType);
		} else if(mimeType.equals("text/html")) {
            loadContentAsHtmlIntoPrintController(content, intent);
        } else {
            loadContentAsBitmapIntoPrintController(content, intent);
        }
    }
	
	private void loadContentFromSDCardRoot(Intent intent,String filename,String mineType){
		File sdcard = Environment.getExternalStorageDirectory();
		//Get the text file form SD Card Root
		File file = new File(sdcard,filename);
		Uri printFileUri = Uri.parse("file:///"+file.getAbsolutePath());
		intent.setDataAndType(printFileUri, mineType);	
	}

    private void loadContentAsHtmlIntoPrintController (String content, Intent intent) {
        intent.putExtra(Intent.EXTRA_TEXT, content);
    }

    private void loadContentAsBitmapIntoPrintController (String content, final Intent intent) {
              Activity ctx = cordova.getActivity();
        final WebView page = new WebView(ctx);
        final Printer self = this;

        page.setVisibility(View.INVISIBLE);
        page.getSettings().setJavaScriptEnabled(false);

        page.setWebViewClient( new WebViewClient() {
            @Override
            public void onPageFinished(final WebView page, String url) {
                new Handler().postDelayed( new Runnable() {
                  @Override
                  public void run() {
                        Bitmap screenshot = self.takeScreenshot(page);
                        File tmpFile      = self.saveScreenshotToTmpFile(screenshot);
                        ViewGroup vg      = (ViewGroup)(page.getParent());

                        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tmpFile));

                        vg.removeView(page);
                  }
                }, 1000);
            }
        });

        //Set base URI to the assets/www folder
        String baseURL = webView.getUrl();
               baseURL = baseURL.substring(0, baseURL.lastIndexOf('/') + 1);

        ctx.addContentView(page, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        page.loadDataWithBaseURL(baseURL, content, "text/html", "UTF-8", null);
    }

    private Bitmap takeScreenshot (WebView page) {
        Picture picture = page.capturePicture();
        Bitmap bitmap   = Bitmap.createBitmap(picture.getWidth(), picture.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas   = new Canvas(bitmap);

        picture.draw(canvas);

        return bitmap;
    }

    private File saveScreenshotToTmpFile (Bitmap screenshot) {
        try {
            File tmpFile = File.createTempFile("screenshot", ".tmp");
            FileOutputStream stream = new FileOutputStream(tmpFile);

            screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            return tmpFile;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void startPrinterApp (Intent intent) {
        cordova.startActivityForResult(this, intent, 0);
    }

    private boolean isAppInstalled (String appId) {
        PackageManager pm = cordova.getActivity().getPackageManager();

        try {
            PackageInfo pi = pm.getPackageInfo(appId, 0);

            if (pi != null){
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {}

        return false;
    }

    private JSONArray getInstalledAppIds () {
        JSONArray appIds  = new JSONArray();

        for (int i = 0; i < printAppIds.length; i++) {
            String appId        = printAppIds[i];
            Boolean isInstalled = this.isAppInstalled(appId);

            if (isInstalled){
                appIds.put(appId);
            }
        }

        return appIds;
    }

    private String getFirstInstalledAppId () {
        for (int i = 0; i < printAppIds.length; i++) {
            String appId        = printAppIds[i];
            Boolean isInstalled = this.isAppInstalled(appId);

            if (isInstalled){
                return appId;
            }
        }

        return null;
    }
}
