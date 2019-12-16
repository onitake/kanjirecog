# kanjirecog

![F-Droid](https://img.shields.io/f-droid/v/ch.seto.kanjirecog.svg?logo=f-droid)
![GitHub](https://img.shields.io/github/v/tag/onitake/kanjirecog?logo=github)

Copyright © 2011 Samuel Marshall.
Copyright © 2019 by Gregor Riepl.

Released under the GNU Public License v3 (see LICENSE).
Some icon files are taken from the Android SDK and are licensed under the Apache 2.0 license.
Stroke order data is licensed under the Creative Commons Attribution-Share Alike 3.0 License.

## Trying it out

This app is available on F-Droid:

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="75">](https://f-droid.org/en/packages/ch.seto.kanjirecog/)

## Build instructions

You can build kanjirecog from the command line or from Android Studio.

To build from the command line, run:

    ./gradlew assembleDebug

You can also install the built application directly to a connected device
or emulator, by running:

    ./gradlew installDebug

Please refer to the [Android SDK documentation](https://developer.android.com/studio/build/building-cmdline)
for further details.

## Updating kanji data

To update kanji stroke data, download the latest data set from
[the KanjiVG release page](https://github.com/KanjiVG/kanjivg/releases) into the `data/` folder.

Then, use the provided conversion tool to convert the SVG graphics into our internal format:

    ./gradlew run --args="$PWD/data/kanjivg-20160426.xml $PWD/app/src/main/assets/strokes.xml"

## Project layout

* `design` SVG versions of the application icons
* `data` downloaded kanji stroke data
* `lib` the kanji recognizer
* `converter` stroke file conversion tool
* `android` the Android application

## Repository information

In case you're not reading it there, this fork is available from:
https://github.com/onitake/kanjirecog

There you can do the following:

* Download the full source code (click the Download Source button or use Git).
* Report bugs or contribute improvements (click the Issues tab).

## Acknowledgements

This is a fork of the kanjirecog project at https://github.com/quen/kanjirecog with a new
Gradle-based build system and some code modifications to make it compatible with recent
Android versions.

It uses a database derived from the SVG kanji stroke order images produced by the
KanjiVG project and released under Creative Commons Attribution-Share Alike 3.0
license. The original version of this database from the KanjiVG project is in
the `data` folder, named `kanjivg-<date>.xml`.

KanjiVG home page is: http://kanjivg.tagaini.net/

In the Android app, two icons from the Android SDK are included (ic_menu_*.png).
These are licensed under the Apache 2.0 license.