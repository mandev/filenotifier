# File Notifier

This software detects file events by spooling and watching the file system. 
A file event is a creation, a deletion or a modification of a file or directory in the file system.
Watching the file system provides very quick and efficient detection with very low IO and CPU usage. 

### Features

File Notifier:
* can simultaneously watch events in one or more directories 
* can recursively watch files and dir from a root directory 
* can filter files with regexp rules
* saves the state of the watched directory (and subdirectories) in real time
* spools the watched directory to detect changes from its last execution (only at start)
* can output xml or txt files to batch file system events (on time and size based mode)
* works on Windows and Linux and probably on Mac OS

### How it works

1. File Notifier starts and loads the configuration file
2. File Notifier starts to watch all directories specified in the configuration file
3. File Notfier checks the watched directories with their old states saved in the database
4. If a file has changed in the last retain period, an event is generated
5. The watcher also generates an event as soon as a file is added, deleted or modified...
6. ... and the file new state is then updated in the database
7. After a delay or if the max number of events is reached, the ouput writers are called
9. The output writers batch events in xml or txt files 
10. This process goes on until File Notifier is stopped...

### Configuration

The "channels.xml" configuration file is located in the {USER\_HOME}/.filenotifier directory.

Example of channel.xml file :
```xml
<?xml version="1.0" encoding="UTF-8"?>
<channels>
  <channel id="Images" description="Watch for Images" >
    <trigger/>
    <localSpool db="E:/notifier/database/images" root="E:/notifier/data/images" subdir="true" delay="5" maxevent="1000" ext="false" includes=".*\.(JPG|jpg|PNG|png|BMP|bmp|TIF|tif)$" excludes=".*\.(xmp|tmp)$" retain="7" >
      <fileEventWriter path="E:/notifier/xml/images/images_#.xml" />
      <logEventWriter path="E:/notifier/log/images/" />
    </localSpool>
    <!-- You can add localspool here -->
  </channel>
  <!-- You can add channels here -->
</channels>
```
with :

* _channels_: all channels

* _channel_: one channel
  * _id_: the id of the channel (must be unique)
  * _description_: a small description of the channel 

* _trigger_: enable the channel if present

* localSpool : on spooler (or watcher) for a local directory.
  * db: the path of the database where the state is saved (path)
  * root: the root directory to watch (path)
  * subdir: enable or disable subdirectory watching/spooling (boolean)
  * delay: the max delay in seconds before triggering event writers  (integer)
  * maxevent: the max number of catched events before triggering event writers (integer)
  * ext:  enable or disable extended watch mode to deal with file rename events (boolean)
  * includes: a regexp filter to select the files to watch (regexp)
  * excludes: a regexp filter to select the files to exclude (regexp)
  * retain: the spool check will generate events only for files modified in this last 'retain' days from now (integer)    
  
* fileEventWriter: the standard XML event writer 
  * path: the output path of the xml event files (path)
 
* logEventWriter: the standard TXT event writer
  * path: the output path of the txt event files (path)

### Running

You need a Java 1.8+ jdk to run File Notifier.

```
java -mx256M -jar filenotifier.jar
```
