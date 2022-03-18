# Harmony Audio Video Capability Application

# HarmonyOS
Copyright (c) Huawei Technologies Co., Ltd. 2012-2022. All rights reserved.

## Table of Contents
* [Introduction](#introduction)
* [Installation](#installation)
* [Supported Environments](#supported-environments)
* [Configuration](#configuration)
* [Sample Code](#sample-code)
* [License](#license)

## Introduction

Harmony Audio Video Capability app provides the capability to streaming both audio and video in Huawei wearable and phone as well and also use Harmony distribute system to distribute streaming media on Phone and control media playback from your Huawei wearable watch itself.

Here, Use Huawei wearable watch as a remote to control a media player on a Phone/Tablet/TV. 

This app is developed for HarmonyOS app using multi-collaboration APIs to run across devices and provides audio and video playbacks and management features (including play, pause, rewind, next, previous and volume control).

To use Harmony Learning App, you need to:

* Register a HUAWEI ID on HUAWEI Developers. Then, create an app, configure app information in AppGallery Connect.
* Create a Harmony project in DevEco Studio.
* Implement Harmony distribute system, Player and Volume management APIs.

## Installation

To use functions provided by examples, please make sure Harmony OS (2.0 or later) and HMS Core (APK) version 6.1.0.300 or later has been installed on your cellphone.
There are two ways to install the sample demo:

* You can compile and build the codes in DevEco Studio. After building the .hap, you can install it on the phone/wearable watch and debug it.
* Generate the .hap file from Gradle. Use the ADB tool to install the .hap on the phone/wearable watch and debug it adb install
{YourPath}\phone\build\outputs\hap\debug\phone-debug-rich-signed.hap
{YourPath}\smartwatch\build\outputs\hap\debug\smartwatch-debug-rich-signed.hap

## Supported Environments

Harmony SDK Version >= 5 and JDK version >= 1.8 is recommended.

## Configuration

Create an app in AppGallery Connect.
In DevEco Studio, Configuring App Signature Information, Generating a Signing Certificate Fingerprint and Configuring the Signing Certificate Fingerprint in App Gallery.


## Sample Code

The Harmony Learning App Demo provides demonstration for following scenarios:

Playing local audio and video resources and also obtained from the internet.

Use Huawei wearable watch as a remote to control a media playback on a Phone/Tablet/TV.

Controlling the media volume.

* [Player API](https://developer.harmonyos.com/en/docs/documentation/doc-references/player-0000001054238943)
* [Volume Management](https://developer.harmonyos.com/en/docs/documentation/doc-guides/media-audio-volume-0000000000041089)
* [Media-Audio Playback](https://developer.harmonyos.com/en/docs/documentation/doc-guides/media-audio-playback-0000000000031734)
* [Media-Video Playback](https://developer.harmonyos.com/en/docs/documentation/doc-guides/media-video-player-0000000000044178)

## License
Harmony Audio Video Capability Application is licensed under the [Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0).