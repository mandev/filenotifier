# filenotifier
This software detects filevents (creation, deletion, modification of files on folders) in a file system. 

* It can watch events in one or more directories simultaneously
* It can recursively watch all files and dir from the root directory 
* It saves the state of the watched directory (and subdirectories) in real time
* It spools the watched directory when starting to check for changes against the saved state
* It generates an xml file in case of a file system event or change
* It works on Windows and Linux and probably on Mac OS
