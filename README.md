# Meerkat - Android ADS-B In display

This is an Android app to display traffic information received from an ADS-B In device such as a PingUSB https://uavionix.com/products/pingusb/
The objective is to use this app in an aircraft to detect nearby traffic equipped with ADS-B Out.

Project Status & Functionality
------------------------------
This is very much in the pre-release state. Currently it does
* Connect to a PingUSB device via WiFi (I believe that it *should* also work with other devices e.g. ForeFlight Sentry https://www.foreflight.com/support/sentry/ or Stratux but I haven't tested with anything other than my Ping-USB)
* Receive GDL90 messages from the device, including Traffic and Ownship messages
* Parse those messages 
* Read Settings from /data/data/com.meerkat/shared_prefs/com.meerkat_preferences.xml
* Write log files to /storage/sdcard0/Android/data/com.meerkat/files/meerkat.log
* A simulator to generate "traffic" for testing (turned on by default!)
* Display logs & raw messages on the screen
* Display nearby traffic in a text window
* Display nearby traffic in a zoomable graphic map window, either Heading-Up, Track-Up, or North-Up

In the map window, the current GPS position is located at the lower centre of the screen. The background has some circles and lines to make it easier to estimate distance and direction.
The map window is zoomable with a pinch gesture.

In Heading-Up mode, the phone's orientation is used to orientate the map display, so that (assuming the phone is orientated in the same direction as the aircraft), the view
on the screen should match with the view out the window... i.e. the screen display is oriented with the real world. The downside is that this will be inaccurate in accelerated flight (turns, acceleration/decelleration, changes in climb/descent rate).

In Track-Up mode, only the aircraft GPS track is used to orientate the map display. In nil-wind and non-sideslip conditions, this will be the same as Heading-Up,
and it will not be affected by accelerated flight.

In North-Up mode, the map display is aligned with Grid North.

Each aircraft (or ADS-B-equipped ground vehicle or obstacle) is displayed as an icon depending on its GDL90 emitter type. 

Each aircraft's icon is coloured to indicate the altitude relative to the phone's GPS position:
* green if 5000ft or more below, transitioning to red if less than 1000ft below 
* blue if 5000ft or more above, transitioning to red if less than 1000ft above
* black if not airborne.
* The 5000ft and 1000ft limits are settable by changing the [gradientMaximumDiff] and [gradientMinimumDiff] (and [altUnits]) settings

Each icon has text alongside, giving
* The aircraft's callsign (if available)
* An exclamation mark if the CRC on the last message was incorrect
* If airborne, the altitude of the aircraft relative to the phone's GPS altitude

Each icon can also optionally (controlled by Settings) have associated with it:
* The aircraft's history track, for the previous [historySeconds] seconds
* A "linear" predicted track, assuming the aircraft continues at the same speed, rate of climb, and track for the next 60 seconds (settable via [predictionSeconds]).
* A "polynomial" predicted track, based on the previous 10 (settable via [polynomialHistorySeconds]) seconds, so it predicts a turning flight path.
These use the same colour coding as the icon

The app may also be put into an "auto-Zoom" mode, where it automatically zooms in or out so that the furthest aircraft is at the edge of the screen.
Rotating the screen while in Heading mode will continue automatically zooming in/out.

NB: In auto-zoom mode (and in manual zoom mode), *nearer* aircraft may be off the side or bottom of the screen. 

Contributing & Licensing
------------------------
This software is free (as in beer AND speech). I figure that, although I could maybe sell it for a few hundred dollars total, it is way more
valuable to me (as a pilot) if it saves one person from running into me mid-air. So, it's free. 

It is made available under a Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0) License. https://creativecommons.org/licenses/by-nc/4.0/

You are free to share (copy and redistribute the material in any medium or format) and
adapt (remix, transform, and build upon the material) this software under the following terms:
Attribution — You must give appropriate credit, provide a link to the license, and indicate if changes were made. You may do so in any reasonable manner, but not in any way that suggests the licensor endorses you or your use.
NonCommercial — You may not use the material for commercial purposes.

Having said that, I welcome anyone who wishes to contribute to this project. In particular, it would be good to have someone who is really up to speed with
Android development. I'd welcome someone porting it to Apple IOS. It would also be helpful to be able to make contact with people who
have devices other than the PingUSB.

SETTINGS
--------
The following settings are available via the Settings file at /data/data/com.meerkat/shared_prefs/com.meerkat_preferences.xml
Apart from wifiName, there is currently no way for the user to alter these settings except by editing the above file.

User settings
-------------
| User Setting name               | Usage                                                                                                                                                                                             | Default value                                   |
|---------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------|
| wifiName                        | ssId of the Wifi network established by the device (e.g. Ping-6C7A for my PingUSB). Automatically saved when comms are established                                                                | Ping-6C7A -- will be null in the released version |
| port                            | TCP port for comms from the device                                                                                                                                                                | 4000 (PingUSB default)                          |
| showLinearPredictionTrack       | Whether to display the "linear" predicted track for each aircraft on the screen. This assumes that the aircraft will continue at the same speed, rate of climb, and track for the next 60 seconds | true                                            |
| showPolynomialPredictionTrack   | Whether to display the "polynomial" predicted track for each aircraft on the screen. This predicts accelerating, turning, climbing path for the next 60 seconds                                   | true                                            |
| historySeconds                  | How many seconds of history track to display for each aircraft. Also used for polynomial prediction                                                                                               | 60                                              |
| purgeSeconds                    | How many seconds to wait before an aircraft is removed from the display                                                                                                                           | 60                                              |
| predictionSeconds               | How many seconds into the future to predict the track of each aircraft. Applies to both linear and polynomial prediction                                                                          | 60                                              |
| polynomialPredictionStepSeconds | How many seconds each step of the polynomial prediction is                                                                                                                                        | 6                                               |
| polynomialHistorySeconds        | How many seconds history should be used by the polynomial predictor. Too large or small a value will seen poor predictions                                                                        | 10                                              |
| gradientMaximumDiff             | How many [altitude unit]s above/below the phone's GPS altitude an aircraft needs to be to be completely blue or green                                                                             | 5000                                            |
| gradientMinimumDiff             | How many [altitude unit]s above/below the phone's GPS altitude an aircraft displays as completely red                                                                                             | 1000                                            |
| screenYPosPercent               | Distance of the ownShip position from the bottom of the screen, as a percentage of the screen height                                                                                              | 25%                                             |
| sensorSmoothingConstant         | The sensitivity of the display to phone orientation change (0 - 1). Larger values make it more responsive                                                                                         | 0.2                                             |
| distanceUnits                   | User's preferred distance units KM, NM, M                                                                                                                                                         | NM                                              |
| altUnits                        | User's preferred altitude units FT, M                                                                                                                                                             | FT                                              |
| speedUnits                      | User's preferred speed units KTS, MPH, KPH                                                                                                                                                        | KPH                                             |
| vertSpeedUnits                  | User's preferred vertical speed units FPM, MPS                                                                                                                                                    | FPM                                             |
| screenWidth                     | Distance that the width of the screen represents in the user's [distance unit]s                                                                                                                   | 10                                              |
| circleRadiusStep                | Distance apart of the circles on the screen in the user's [distance unit]s                                                                                                                        | 5                                               |
| countryCode                     | Country prefix -- stripped off when the callsign is displayed. May be blank if all letters of callsigns are to be displayed.                                                                      | ZK                                              |
| headingUp                       | Display orientation... heading-up... if false, trackUp is used                                                                                                                                    | true                                            |
| trackUp                         | Display orientation... track-up or North-up                                                                                                                                                       | true                                            |
| keepScreenOn                    | Keep the display on when in the Map or Aircraft List views                                                                                                                                        | true                                            |
| minGpsDistanceChangeMetres      | Minimum Gps distance between updates in metres                                                                                                                                       			  | 5                                               |
| minGpsUpdateIntervalSeconds     | Minimum Gps update interval in seconds                                                                                                                                       					  | 10                                              |
| autoZoom		                  | Auto-zoom to the furthest aircraft. NB: This may mean that *nearer* aircraft are off the side or bottom of the screen.                                                                            | true                                            |

Debugging settings
------------------
Logging of received messages and the app's processing of those messages is intended as an aid in debugging. Log records are sent to
Android's system log (aka logcat). They can optionally also be sent to a tab on the screen and/or to a file. Raw messages (in hex)
can be sent to the logs, and/or decoded messages.

Setting the [simulate] setting to "true" results in the app processing a series of simulated events from several simulated aircraft.

| Setting name          | Usage                                                                                                                                                     | Default value                                     |
|---------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------|
| version                         | The version of Meerkat that saved the settings                                                                                                  | 1.0                                               |
| showLog                         | Whether to output logs to a window on the screen                                                                                                | false                                             |
| fileLog                         | Whether to output logs to /storage/sdcard0/Android/data/com.meerkat/files/meerkat.log. 															| false                                             |
| appendLogFile                   | Whether to append to the existing log file, or erase it and start a new one.                                                                    | false                                             |
|                                 | If this is false, this file is never erased or shortened. If left alone, this will eventually chew up all the storage at the rate of ~100MB/hr! |                                                   |
| logLevel                        | Amount of detail to write to logs... Assert, Error, Warning, Info, Debug, Verbose                                                               | I                                                 |
| logRawMessages                  | Whether to write the raw messages, as received from the device, to the logs                                                                     | false                                             |
| logDecodedMessages              | Whether to write the decoded messages, as interpreted by the GDL90 parser, to the logs                                                          | false                                             |
| simulate                        | Play built-in simulated traffic instead of real traffic. No Wifi connection is made.                                                            | false                                             |

TO DO
-----
This list is more-or-less in priority order. At the moment it is shrinking :)
* Ownship track... history and prediction
* Auto-zoom -- all aircraft
* Indicate Mode-C traffic presence
* Fix - Layout issue due to bottom nav bar -- Change to Fullscreen display
* Add a "Quit" button
* Add a Settings screen to enable all the settings to be changed interactively instead of needing to edit the text file
* Audio / Haptic alerts of collision threats
* Use a theme to allow black background
* Digital filtering of path to predict tracks
* Test usage with other Wifi-enabled ADS-B In devices (Stratux, Foreflight, etc)
* Improve the code style
* Write some documentation
* Lots more testing
* Internationalisation
* Port to Apple IOS.
