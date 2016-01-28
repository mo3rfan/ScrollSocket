ScrollSocket
============

ScrollSocket is an small app that sends mouse wheel (scroll) events to your computer as you move your finger over the android touchscreen. Obviparently, you need to install a driver on the computer and have it listening on UDP port 40118.

Only linux drivers at the moment. Client app requires android 2.2 or above. Only vertical scrolling implemented for now. 

Installation & Usage
====================

1. Run the statically linked uinput driver or build your own.
2. Install the apk on the phone go to options and set the IP.
3. Focus your mouse over a window that is scrollable.
4. Go crazy with your finger.

### inb4 "Doesn't work"

* Install uinput kernel module (modern versions of linux distros seem to have it).
* If you use Xorg, make sure Xorg-evdev module is loaded and configured.
* Config your firewall and make way for UDP port 40118.

TODO
====

* DPAD Support.
* Horizontal Scroll.
 * Lock to a single scroll axis.
* Tilt scroll.
* Windows support.
* Sensitivity settings.
* Smooth scroll/Non linear scroll.

## Credits

This is a completely dumbed down, less fancy modification of the [GfxTablet](https://gfxtablet.bitfire.at) app by Ricki Hirner (also licensed under the MIT License).

Icons generated using [Android Asset Studio](https://romannurik.github.io/AndroidAssetStudio/index.html) / [CC BY](http://creativecommons.org/licenses/by/3.0/)
