Cordova Printer-Plugin
======================

A print plugin to print a file from android/ios cordova app using third party apps. It can print both in online/offline mode.

Modified from this plugin https://github.com/katzer/cordova-plugin-printer

## Supported Platforms
- **iOS** *(Print from iOS devices to AirPrint compatible printers or other printers using third party apps via wifi)*<br>
See [Drawing and Printing Guide for iOS](http://developer.apple.com/library/ios/documentation/2ddrawing/conceptual/drawingprintingios/Printing/Printing.html) for detailed informations and screenshots.

- **Android** *(Print through 3rd party printing apps)*

## Adding the Plugin to your project
Through the [Command-line Interface](http://cordova.apache.org/docs/en/3.0.0/guide_cli_index.md.html#The%20Command-line%20Interface):

```bash

cordova plugin add https://github.com/pankajnirwan103/cordova-plugin-printer.git
cordova build


## Removing the Plugin from your project
Through the [Command-line Interface](http://cordova.apache.org/docs/en/3.0.0/guide_cli_index.md.html#The%20Command-line%20Interface):

cordova plugin rm nirwan.cordova.plugin.printer
```

## Release Notes

#### Version 0.1.0 (02.09.2014)
- *Based on the Print plugin made by* ***KAtzer***

## Using the plugin
The plugin creates the object ```window.plugin.printer```


## Android

### Get all available printing apps on Android
The callback function will be called with a second argument which is an array, indicating which printer apps are available for printing.
```javascript
window.plugin.printer.isServiceAvailable(
    function (isAvailable, installedAppIds) {
        alert('The following print apps are installed on your device: ' + installedAppIds.join(', '));
    }
);
```

### Printing 
You need to download/save html using fileTransfer plugin https://github.com/apache/cordova-plugin-file-transfer. Make sure you use it after deviceReady event is called.

** Below example uses print share app for printing files.
```javascript
	
	var pdfHTML = document.getElementById('ELEMENT_TO_PRINT').innerHTML;
	var headstr = "<html><head><title></title></head><body>";
	var footstr = "</body>";
				
	var name = "downloaded_file.html";
					
	var filename = "cdvfile://localhost/persistent/" + name;
	
	var fileTransfer = new FileTransfer();
					
	fileTransfer.download(
		encodeURI('data:text/html;charset=utf-8,' + headstr+pdfHTML+footstr),
		filename,
		function(theFile){
						
			window.plugin.printer.isServiceAvailable(
				function (isAvailable, installedAppIds) {
							    	
					var isPrintShareInstalled = false;
							    	
					installedAppIds.forEach(function(entry) {
									    
						if(entry == 'com.dynamixsoftware.printershare') {
							isPrintShareInstalled = true;
						}
					});
									
					if(isPrintShareInstalled) { //Going to send file for printing
										
						var page = "<HTML>hi</HTML>";
						window.plugin.printer.print(page, { 'appId': 'com.dynamixsoftware.printershare','filename': name ,'mimeType': 'text/html'});
						return;
					} else {
							alert(null, "Please install PrinterShare app <br> from Google PlayStore for printing.");
						}
					}
			);
		},
		function(error) {
			alert("download error source " + error.source);
			alert("download error target " + error.target);
			alert("upload error code: " + error.code);
		},
		true
	);
```


## License

This software is released under the [Apache 2.0 License](http://opensource.org/licenses/Apache-2.0).
