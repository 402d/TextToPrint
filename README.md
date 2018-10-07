# TextToPrint
### Demo how print use PrintDocumentAdapter for WebView

Since other software developers are too lazy to implement printing,
I did it for them.

The application is the simplest. One screen. WebView and print button.
Gets text through the processing of Intent.View and Intent.SEND ("Open with" and "Share"). Having received the text makes it the simplest html.

In the settings 4 font sizes.
Font A and Font B are suitable for printing on a thermal printer (58mm roll of cash tape)
Font D - the smallest (80 characters per line on A4 printer)

The application DOES NOT PRINT yourself. By the print button, a standard PrintDocumentAdapter for WebView is created.

##How to use the example of the clipboard.
Select text -> Share -> TextToPrint -> Printer Icon -> Standard Print Preview Dialog.

## App in Market
[ru.a402d.texttoprint](https://play.google.com/store/apps/details?id=ru.a402d.texttoprint)
