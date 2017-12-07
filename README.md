# File Notifier
This software detects events (creation, deletion, modification on files or dir) in a file system. 

* It can watch events in one or more directories simultaneously
* It can recursively watch all files and dir from the root directory 
* It can filter files to watch based on regexp rules
* It saves the state of the watched directory (and subdirectories) in real time
* It spools the watched directory when starting to check for changes against the saved state
* It generates a xml file in case of a file system event or change
* It works on Windows and Linux and probably on Mac OS
