# Meerkat - Android ADS-B In display

This is an Android app to display traffic information received from an ADS-B In device such as PingUSB https://uavionix.com/products/pingusb/
The objective is to use this app in an aircraft to detect nearby traffic equipped with ADS-B Out.

Project Status & Functionality
------------------------------
This is very much in the pre-release state. Currently it does
* Connect to a PingUSB device via WiFi
* Receive GDL90 messages from the device, including Traffic and Ownship messages
* Parse those messages 
* Read Settings from /Android/data/com.meerkat/shared_prefs (or maybe not)
* Write log files to /Android/data/com.meerkat/files (or maybe not)
* Display logs & raw messages on the screen
* Display nearby traffic in a text window
* Display nearby traffic in a graphic window
* A simulator to generate "traffic" for testing (currently turned on by default!)

In the graphic window, the current GPS position is located at the centre of the screen. The background has some circles and lines to make it easier to estimate distance and direction.
This graphic window's orientation is currently only "North-up". 
Each aircraft (or ADS-B-equipped ground vehicle or obstacle) is displayed as an icon depending on its GDL90 emitter type. 

Each aircraft's icon is coloured to indicate the altitude relative to the phone's GPS position:
* green if below, fading to red 
* blue if above, fading to red
* black if not airborne.

Each icon has text alongside, giving
* The aircraft's callsign (if available)
* An exclamation mark if the CRC on the last message was incorrect
* If airborne, the altitude of the aircraft relative to the phone's GPS altitude

Each icon can also optionally (controlled by Settings) have associated with it:
* The aircraft's history track
* A "linear" predicted track, assuming the aircraft continues at the same speed, rate of climb, and track for the next 60 seconds (settable).
* A "polynomial" predicted track, based on the previous 60 (settable) seconds, so it predicts a turning flight path.

Contributing & Licensing
------------------------
This software is free (as in beer AND speech). I figure that, although it could maybe sold for a few hundred dollars total, it is way more
valuable to me (as a pilot) if it saves one person from running into me mid-air. So, it's free. It is made available under a Creative Commons 
Attribution-NonCommercial 4.0 International (CC BY-NC 4.0) License. https://creativecommons.org/licenses/by-nc/4.0/

You are free to share (copy and redistribute the material in any medium or format) and
adapt (remix, transform, and build upon the material) this software under the following terms:
Attribution — You must give appropriate credit, provide a link to the license, and indicate if changes were made. You may do so in any reasonable manner, but not in any way that suggests the licensor endorses you or your use.
NonCommercial — You may not use the material for commercial purposes.

Having said that, I welcome anyone who wishes to contribute to this project. In particular, it would be good to have someone who is really up to speed with
Android development. I guess it would be good if it could also be ported to Apple IOS. It would also be helpful to be able to make contact with people who
have devices other than the PingUSB.

TO DO
-----
* Allow changing the display orientation to "Track up" or "Heading up"
* Use a theme to allow black background
* Digital filtering of path to predict track
* Ownship track and prediction
* Check that Settings are saved and loaded correctly
* Check that log files are written correctly
* Expand to be able to use other Wifi-enabled ADS-B In devices
* Turn off the simulator
* Add a Settings screen to enable all the settings to be changed interactively instead of needing to edit the text file
* Improve the code style
* Port to Apple IOS
* Write some documentation
