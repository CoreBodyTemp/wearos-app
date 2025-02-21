# Core Wear OS App

CORE Body Temperature Monitoring is an easy-to-use wearable product that using a Swiss-Made sensor and can monitor and delivery real-time, accurate core body temperature data. Find out more on [corebodytemp.com](https://corebodytemp.com/).

## You will need to have a CORE sensor for this app to work.

Order CORE at [corebodytemp.com](https://corebodytemp.com/products/core).

## _You already have your personal CORE?_

Awesome! Start using it and track your body temperature as easy as it never have been before. Explore the official apps [on the Playstore](https://play.google.com/store/apps/details?id=com.greenteg.core.app) and [on the Appstore](https://apps.apple.com/us/app/id1521866309) for mobile phones, or other platforms such as Garmin.

## _You have a wear OS device?_
**Then this is for you!** This code repository contains the source of the [CORE Wear OS app](https://play.google.com/store/apps/details?id=com.greenteg.core.wearos).
We invite you to use this as a reference for your own apps interfacing with CORE.

NOTE: This content is licensed under the Apache License, Version 2.0. More info: info@greenteg.com

> Copyright (C) 2021, greenTEG AG
>
> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this file except in compliance with the License.
> You may obtain a copy of the License at
>
> [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)
>
> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.


# How to get started

## How to prepare your watch to debug apps from AndroidStudio
You have to enable the developer mode on your watch([explained here](https://developer.android.com/training/wearables/apps/debugging)).

## How to build the app and setup android-debug-over-air (ADB)

1. Download latest version of [AndroidStudio](https://developer.android.com/studio?gclid=CjwKCAiA-_L9BRBQEiwA-bm5fgngJrqeA-ZMr0p1ZHucPuT18LjNNtI1UeSWD-3fwrI-lFoIe0NtxBoCRjkQAvD_BwE&gclsrc=aw.ds)
2. Clone this project into a folder on your computer using
`git clone git@github.com:corebodytemp/wearos-app.git` (SSH)
or
`git clone https://github.com/corebodytemp/wearos-app.git` (HTTPS)
3. import this project into AndroidStudio via File --> New... --> Import Project...
(this will take a while since all the gradle files need to be downloaded in the background)
4. Navigate to the "Terminal" in AndroidStudio.
5. In the terminal, navigate to the location of the adb.exe (android debugger), something like this:
`cd C:\Users\~yourusername\AppData\Local\Android\Sdk\platform-tools>`
6. Connect your PC and your WearOS device to the same WLAN
7. check the IP address of your WearOS device (e.g. 192.168.11.11)
8. Connect the debugger to your WearOS device running
`adb connect 172.168.11.11` in the terminal
9. If everything runs smoothly, your WearOS device will appear as a physical device in the dropdown menu next to the green triangle, click that to flash the code onto the WearOS device.

## Further resources:
In the .pdf [`CORE BLE Implementation Notes`](https://github.com/CoreBodyTemp/CoreBodyTemp/blob/main/CORE%20BLE%20Implementation%20Notes.pdf), you can find more information about your possibilities to interact with CORE.
Please also find the specifications of our custom BLE service [`Core Body Temperature Service`](https://github.com/CoreBodyTemp/CoreBodyTemp/blob/main/CoreTemp%20BLE%20Service%20Specification.pdf)

# Changelog
1.2.0 (2021-04-22)

- read battery level of CORE and display it in the main view

- The CBT value on the watchface always matches the CBT value in the app (When the app is moved to the background, the CBT value on the watchface is updated. Additionally, tapping the CBT value on the watchface will also update it to the latest value. However, it is still not live, ​meaning that if you do not open the app, the value is updated every 5 minutes, or never as long as the watch is in "ambient-mode", i. e. the black-white energy saving display mode.

- Additionally, the the "invalid/NaN" values that are sent by Core when no CBT is available, is parsed correctly.

- Additionally, the CBT is set as default data provider, if you choose the CORE watchface​


1.1.0 (2020-12-23)

- properly handle reopening of app when BLE switched off on device

- keep screen awake while establishing a connection


1.0.1 (2020-12-12)
