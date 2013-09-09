**NFC Book Scanner** is a demonstration app for reading NfcV (ISO 15693) tags (known as just "RFID tags" in 
the library world) with your Android phone, and parsing the 32 bytes of "userdata" (the 8 first blocks) 
according to the "[Danish library model](http://biblstandard.dk/rfid/dk/RFID_Data_Model_for_Libraries_July_2005.pdf)",
to get the item identifier (barcode), owner library code (International Standard Identifier for Libraries, ISIL), 
country code and AFI status (checked in / out).
Additional information about the book is found by sending the barcode to a web service retrieving info
from the library catalogue.

Only reading tags is demonstrated, but writing could easily be implemented as well using the same
[android.nfc.tech.NfcV.transceive](http://developer.android.com/reference/android/nfc/tech/NfcV.html#transceive(byte[])
method used for reading. The necessary ISO 15693 commands can be found 
e.g. [here](http://www.ti.com/lit/an/sloa141/sloa141.pdf).

To import this project into Eclipse/ADT, use `File→Import…`, and then either `Android→Existing Android Code Into Workspace` 
to add your local copy or `Git→Projects from Git` to grab it right off GitHub. 

If you clone the repo from the command line, don't forget to `git submodule init; git submodule update` to  get the libdanrfid submodule. 

![Screenshot](https://dl.dropboxusercontent.com/u/1007809/Screenshot_2013-09-09-17-50-57.png)
