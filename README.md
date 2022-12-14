# Meerkat - Android ADS-B In display

This is an Android app to display traffic information received from an ADS-B In device such as a PingUSB https://uavionix.com/products/pingusb/
The objective is to use this app in an aircraft to detect nearby traffic equipped with ADS-B Out.

Project Status & Functionality
==============================
This is very much in the pre-release state. Currently it does
* Connect to a PingUSB device via WiFi (I believe that it *should* also work with other devices e.g. ForeFlight Sentry https://www.foreflight.com/support/sentry/ or Stratux but I haven't tested with anything other than my Ping-USB)
* Receive GDL90 messages from the device, including Traffic and Ownship messages
* Parse those messages 
* Read Settings from /data/data/com.meerkat/shared_prefs/com.meerkat_preferences.xml
* Write log files to /storage/sdcard0/Android/data/com.meerkat/files/meerkat.log
* Display logs & raw messages in a text window
* Display nearby traffic as text
* Display nearby traffic in a zoomable graphic map window, either Heading-Up, Track-Up, or North-Up
* Display and edit Preferences via a Settings screen.
* A simulator to generate "traffic" for testing (currently turned on by default!)

The Main Map Screen
===================
In the main map screen, the current GPS position is located at the lower centre of the screen. 
The background has some circles and lines to make it easier to estimate distance and direction.

When other aircraft are detected within 20nm, a red "danger" circle is drawn around the phone's GPS location. The nearer an aircraft's position (not predicted
or historical) is to the danger circle, the heavier the border is drawn. Eventually, if another aircraft is predicted to enter that circle, a warning will be issued.


Each aircraft (or ADS-B-equipped ground vehicle or obstacle) is displayed as an icon depending on its GDL90 emitter type. Unknown types (typically when only some information has been received from the sender) are shown with a UFO icon.

Each aircraft's icon is coloured to indicate the altitude relative to the phone's GPS position:
* green if 5000ft or more below, transitioning through brown/yellow/orange to red if less than 1000ft below 
* blue if 5000ft or more above, transitioning through shades of purple to red if less than 1000ft above
* black if not airborne.
* The 5000ft and 1000ft limits (and altitude units) are settable via the Settings screen.

Each icon has text alongside, giving
* The aircraft's callsign (if available)
* An exclamation mark if the CRC on the last message was incorrect
* If airborne, the altitude of the aircraft relative to the phone's GPS altitude

Each icon can also optionally (controlled by Settings) have associated with it:
* The aircraft's history track, for the previous [historySeconds] seconds
* A "linear" predicted track, assuming the aircraft continues at the same speed, rate of climb, and track for the next 60 seconds (settable via [predictionSeconds]).
* A "polynomial" predicted track, based on the previous 10 (settable via [polynomialHistorySeconds]) seconds, so it predicts a turning flight path.
These use the same colour coding as the icon

Screen Orientation
------------------
At top-right of the screen is the North arrow and screen mode indicator. Tapping on this will cycle through the Heading-Up, North-Up, Track-Up modes.

In Heading-Up mode, the phone's orientation is used to orientate the map display, so that (assuming the phone is orientated in the same direction as the aircraft), the view
on the screen should match with the view out the window... i.e. the screen display is oriented with the real world. The downside is that this will be inaccurate in accelerated flight 
(turns, acceleration/deceleration, changes in climb/descent rate). Rotating the screen while in Heading mode will continue automatically zooming in/out.
NB: In auto-zoom mode (and in manual zoom mode), *nearer* aircraft may be off the side or bottom of the screen. 

In Track-Up mode, only the phone's GPS track is used to orientate the map display. In nil-wind and non-sideslip conditions, this will be the same as Heading-Up,
and it will not be affected by accelerated flight. However, when not moving, this is not available.

In North-Up mode, the map display is aligned with Grid North.

Screen Zoom
-----------
The map screen may be in either Manual or Automatic Zoom mode (Currently only settable via the Settings screen).

In manual zoom mode, the map window is zoomable with a pinch gesture. The current zoom level is indicated by a number at the bottom right of the screen... this is the distance in [distance unit]s from the centre to the edge of the screen.
The app may also be put into an "auto-Zoom" mode via a Setting, so that it automatically zooms in or out so that the furthest aircraft is at the edge of the screen.

Switching to other screens
--------------------------
Pressing the "back" button (swiping left from the right edge of the screen on some phones) brings up the app's action bar. 
This has icons for moving to the aircraft list screen and to quit the app. It also has a menu which allows moving to the Log or Settings screen.

This action bar will automatically disappear after 5 seconds if no buttons are pressed, or can be removed by pressing the "back" button again.


Aircraft List Screen
====================
Lists the detected aircraft in a tabular fashion, in increasing distance order, updated once per second. 

Use the "back" button to return to the main Map screen.


Log Screen
==========
This is intended for debugging. It displays log entries written by the app, including raw and decoded GDL90 messages (if those have been enabled in the Settings). 

Use the "back" button to return to the main Map screen.


Settings Screen
===============

The following settings are saved to the Preferences file at /data/data/com.meerkat/shared_prefs/com.meerkat_preferences.xml, and may be altered 
via the Settings screen, or by editing the above file. Use the "back" button to return to the main Map screen.

Wifi settings
-------------
The Wifi Name needs to be set to the name of your device (e.g. Ping-6C7A for my PingUSB). The easiest way to do that is
1. Plug your device into a USB power supply.
2. Start the Meerkat app
3. If this is your first use, it will automatically open the Settings screen. If you see the Map screen, click the back button and choose Settngs from the action bar menu
4. Tap on the "Scan" button. Meerkat will scan for nearby Wifi networks, and list them.
5. Tap on the correct WiFi network (e.g. Ping-6C7A). If your device isn't listed, click on the "Scan Again" button

Alternatively, you can type your device's Wifi name into the WiFi Name text box.

| Wifi Settings                   | Usage                                                                                                                        | Default value |
|---------------------------------|------------------------------------------------------------------------------------------------------------------------------|---------------|
| wifiName                        | ssId of the Wifi network established by the device (e.g. Ping-6C7A for my PingUSB).                                          | null          |
| port                            | UDP port for comms from the device. If in doubt, use the PingUSB value of 4000                                               | 4000          |

| Units Settings                  | Usage                                                                                                                        | Default value |
|---------------------------------|------------------------------------------------------------------------------------------------------------------------------|---------------|
| distanceUnits                   | User's preferred distance units KM, NM, M                                                                                    | NM            |
| altUnits                        | User's preferred altitude units FT, M                                                                                        | FT            |
| speedUnits                      | User's preferred speed units KTS, MPH, KPH                                                                                   | KPH           |
| vertSpeedUnits                  | User's preferred vertical speed units FPM, MPS                                                                               | FPM           |

| Screen Setting Name             | Usage                                                                                                                        | Default value |
|---------------------------------|------------------------------------------------------------------------------------------------------------------------------|---------------|
| screenYPos                      | Distance of the ownShip position from the bottom of the screen, as a percentage of the screen height                         | 25%           |
| screenWidth                     | Distance that the width of the screen represents in the user's [distance unit]s                                              | 10            |
| circleStep                      | Distance apart of the circles on the screen in the user's [distance unit]s                                                   | 5             |
| dangerRadius                    | Radius of "danger" circle on the screen in the user's [distance unit]s                                                       | 1             |
| displayOrientation              | Display orientation... Heading-up, Track-up, or North-up                                                                     | Heading-up    |
| keepScreenOn                    | Keep the display on when in the Map or Aircraft List views                                                                   | true          |
| autoZoom	                       | Auto-zoom to the furthest aircraft. NB: This may mean that *nearer* aircraft are off the side or bottom of the screen.      | true          |
| gradMaxDiff                     | How many [altitude unit]s above/below the phone's GPS altitude an aircraft needs to be to be completely blue or green        | 5000          |
| gradMinDiff                     | How many [altitude unit]s above/below the phone's GPS altitude an aircraft displays as completely red                        | 1000          |
| countryCode                     | Country prefix -- stripped off when the callsign is displayed. May be blank if all letters of callsigns are to be displayed. | ZK            |

| Sensitivity Setting Name        | Usage                                                                                                                        | Default value |
|---------------------------------|------------------------------------------------------------------------------------------------------------------------------|---------------|
| sensorSmoothingConstant         | The sensitivity of the display to phone orientation change (1 - 99). Larger values make it more responsive                   | 20            |
| minGpsDistanceChangeMetres      | Minimum Gps distance between updates in metres                                        	                                     | 5             |
| minGpsUpdateIntervalSeconds     | Minimum Gps update interval in seconds                                                                                       | 10            |

| History Settings                | Usage                                                                                                                        | Default value |
|---------------------------------|------------------------------------------------------------------------------------------------------------------------------|---------------|
| historySeconds                  | How many seconds of history track to display for each aircraft.                                                              | 60            |
| purgeSeconds                    | How many seconds to wait before an aircraft is removed from the display                                                      | 60            |

| Linear prediction Settings      | Usage                                                                                                                        | Default value |
|---------------------------------|------------------------------------------------------------------------------------------------------------------------------|---------------|
| showLinearPredictionTrack       | Whether to display the "linear" predicted track for each aircraft on the screen. This assumes that the aircraft              |               |
|                                 | will continue at the same speed, rate of climb, and track for the next 60 seconds                                            | true          |
| predictionSeconds               | How many seconds into the future to predict the track of each aircraft. Applies to both linear and polynomial                |               |
|                                 | prediction                                                                                                                   | 60            |

| Polynomial prediction Settings  | Usage                                                                                                                        | Default value |
|---------------------------------|------------------------------------------------------------------------------------------------------------------------------|---------------|
| showPolynomialPredictionTrack   | Whether to display the "polynomial" predicted track for each aircraft on the screen. This predicts                           |               |
|                                 | accelerating, turning, climbing path for the next 60 seconds                                                                 | true          |
| polynomialPredictionStepSeconds | How many seconds each step of the polynomial prediction is                                                                   | 6             |
| polynomialHistorySeconds        | How many seconds history should be used by the polynomial predictor. Too large or small a value will seen poor predictions   | 10            |

Debugging settings
------------------
Logging of received messages and the app's processing of those messages is intended as an aid in debugging. Log records are sent to
Android's system log (aka logcat). They can optionally also be sent to a tab on the screen and/or to a file. Raw messages (in hex)
can be sent to the logs, and/or decoded messages.

Setting the [simulate] setting to "true" results in the app processing a series of simulated events from several simulated aircraft, and simulating the Gps position of the phone.

| Debug Setting Name | Usage                                                                                                                                           | Default value |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| version            | The version of Meerkat that saved the settings                                                                                                  | 1.0           |
| showLog            | Whether to output logs to a window on the screen                                                                                                | false         |
| fileLog            | Whether to output logs to /storage/sdcard0/Android/data/com.meerkat/files/meerkat.log.                                                          | false         |
| appendLogFile      | Whether to append to the existing log file, or erase it and start a new one.                                                                    | false         |
|                    | If this is false, this file is never erased or shortened. If left alone, this will eventually chew up all the storage at the rate of ~100MB/hr! |               |
| logLevel           | Amount of detail to write to logs... Assert, Error, Warning, Info, Debug, Verbose                                                               | I             |
| logRawMessages     | Whether to write the raw messages, as received from the device, to the logs                                                                     | false         |
| logDecodedMessages | Whether to write the decoded messages, as interpreted by the GDL90 parser, to the logs                                                          | false         |
| simulate           | Play back logged data in /storage/sdcard0/Android/data/com.meerkat/files/meerkat.save.log instead of real traffic. No Wifi connection is made.  | false         |
| simulateSpeedFactor| Speed at which simulated traffic is played	                                                                                                   | false         |


Contributing & Licensing
------------------------
This software is free (as in beer AND speech). I figure that, although I could maybe sell it for a few hundred dollars total, it is way more
valuable to me (as a pilot) if it saves one person from running into me mid-air. So, it's free. 

It is made available under a Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0) License. https://creativecommons.org/licenses/by-nc/4.0/

You are free to share (copy and redistribute the material in any medium or format) and
adapt (remix, transform, and build upon the material) this software under the following terms:
Attribution ??? You must give appropriate credit, provide a link to the license, and indicate if changes were made. You may do so in any reasonable manner, but not in any way that suggests the licensor endorses you or your use.
NonCommercial ??? You may not use the material for commercial purposes.

Having said that, I welcome anyone who wishes to contribute to this project. In particular, it would be good to have someone who is really up to speed with
Android development. I'd welcome someone porting it to Apple IOS. It would also be helpful to be able to make contact with people who
have devices other than the PingUSB.

TO DO
-----
This list is more-or-less in priority order. At the moment it is shrinking :)
* Corrupted logs (fixed?)
* Logs not shown on screen
* Investigate negative speeds (fixed?)
* Ownship track... history and prediction
* Indicate Mode-C traffic presence
* Audio / Haptic alerts of collision threats
* Fix position of north arrow / track mode letter
* Use a theme to allow black background
* Digital filtering of path to predict tracks
* Handling of Float preference values (custom SeekBar?)
* Improve the code style
* Improve documentation with screenshots
* Lots more testing
* Internationalisation
* Find someone to test usage with other Wifi-enabled ADS-B In devices (Stratux, Foreflight, etc)
* Find someone to port it to Apple IOS.
