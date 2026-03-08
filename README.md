# OpenEQ #

This is the repository for OpenEQ, an open-source, privacy respecting, and simple Android audio equalizer app. 

### Features: ###

 - An equalizer that adapts to what the audio hardware on your phone supports
 	- Many common equalizers advertise a certain number of bands, but in reality don't always offer you that level of control as your phone simply doesn't support it. This equalizer adapts to and offers you only what is supported by your phone
 - Privacy respecting
 	- Many common equalizers contain tracking libraries, advertising, or require a subscription. This project doesn't, and never will, do any of these.
 - Simple and clean UI
 - Fast and lightweight

### Installation: ###

Please go to the releases page of this repository to download the .apk which you can then install. I intend to get this app on F-Droid in the future, and potentially on the Google Play Store, but I want to complete some more development first.

### Features in development: ###

 - Persistent EQ settings over app creation/destruction
 - Presets for different music styles
 - User-defined presets

### Known limitations/issues: ###

 - Only detects media streams starting after activating the equalizer
 - Doesn't work with all apps - some apps don't notify the system when starting a media stream, and so the equalizer can't attach to them
 - Requires Android 8.0 or higher

### Contributing: ###

I'm more than happy to have people contribute to this project! Please feel free to get in touch via email if you want.

### License: ###

This project is licensed under the GNU General Public License Version 3.
