# My-Wallet-V3-Android

[![Build Status](https://travis-ci.org/blockchain/My-Wallet-V3-Android.svg?branch=master)](https://travis-ci.org/blockchain/My-Wallet-V3-Android)
[![CircleCI](https://circleci.com/gh/blockchain/My-Wallet-V3-Android/tree/master.svg?style=svg)](https://circleci.com/gh/blockchain/My-Wallet-V3-Android/tree/master)

Next-generation HD (BIP32, BIP39, BIP44) bitcoin wallet. 

![Blockchain Wallet for Android](.github/feature_graphic.png)

## Getting started

Install Android Studio: https://developer.android.com/sdk/index.html

Import as Android Studio project.

Run the bootstrap script from terminal via `scripts/bootstrap.sh`. This will install the Google code style and remove any file header templates. The script may indicate that you need to restart Android Studio for it's changes to take affect.

Build -> Make Project

If there are build errors, in Android Studio go to Tools -> Android -> SDK Manager and install any available updates.

### Notes

HD classes extending Bitcoinj for BIP44 supplied using https://github.com/blockchain/My-Wallet-V3-jar

## Tests

Unit tests can be run through Android Studio using the Android emulator with results viewed in the console.

### Security

Security issues can be reported to us in the following venues:
* Email: security@blockchain.info
* Bug Bounty: https://www.crowdcurity.com/blockchain-info
