# My-Wallet-V3-Android

[![Build Status](https://travis-ci.org/blockchain/My-Wallet-V3-Android.svg?branch=master)](https://travis-ci.org/blockchain/My-Wallet-V3-Android)
[![CircleCI](https://circleci.com/gh/blockchain/My-Wallet-V3-Android/tree/master.svg?style=svg)](https://circleci.com/gh/blockchain/My-Wallet-V3-Android/tree/master)

Next-generation HD (BIP32, BIP39, BIP44) bitcoin wallet. 

## Getting started

Install Android Studio: https://developer.android.com/sdk/index.html

Import as Android Studio project.

Build -> Make Project

If there are build errors, in Android Studio go to Tools -> Android -> SDK Manager and install any available updates.

## Code style

To install the Google Java Style in Android Studio, copy the IntelliJ codestyle from here:

https://raw.githubusercontent.com/google/styleguide/gh-pages/intellij-java-google-style.xml

to `~/Library/Preferences/AndroidStudiox.x/codestyles/` (on Mac OS X).

### Notes

HD classes extending Bitcoinj for BIP44 supplied using https://github.com/blockchain/My-Wallet-V3-jar

## Tests

Unit tests can be run through Android Studio using the Android emulator with results viewed in the console.

### Security

Security issues can be reported to us in the following venues:
* Email: security@blockchain.info
* Bug Bounty: https://www.crowdcurity.com/blockchain-info
