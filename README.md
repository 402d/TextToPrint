# TextToPrint
### Demo how print use PrintDocumentAdapter for WebView

Since other software developers are too lazy to implement printing,
I did it for them.

The application is the simplest. One screen. WebView and print button.
Gets text through the processing of Intent.View and Intent.SEND ("Open with" and "Share"). Having received the text makes it the simplest html.

**see** [createWebPrintJob()](https://github.com/402d/TextToPrint/blob/master/app/src/main/java/ru/a402d/texttoprint/MainActivity.java#L375)

In the settings 4 font sizes.
Font A and Font B are suitable for printing on a thermal printer (58mm roll of cash tape)
Font D - the smallest (80 characters per line on A4 printer)

The application DOES NOT PRINT yourself. By the print button, a standard PrintDocumentAdapter for WebView is created.

## How to use the example of the clipboard.
Select text -> Share -> TextToPrint -> Printer Icon -> Standard Print Preview Dialog.

## TextToPrint on Google Play
<a href="https://play.google.com/store/apps/details?id=ru.a402d.texttoprint" target="_blank"><img src="https://play.google.com/intl/en_us/badges/images/badge_new.png" alt="Get it on Google Play" height="46"></a>


