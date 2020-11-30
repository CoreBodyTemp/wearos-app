# CoreWearOS

# How to get started

1. Download latest version of [AndroidStudio](https://developer.android.com/studio?gclid=CjwKCAiA-_L9BRBQEiwA-bm5fgngJrqeA-ZMr0p1ZHucPuT18LjNNtI1UeSWD-3fwrI-lFoIe0NtxBoCRjkQAvD_BwE&gclsrc=aw.ds)
2. Clone this project into a folder on your computer using
`git clone git@gitlab.com:corebodytemp/wearos-app.git` (SSH)
or 
`git clone https://gitlab.com/corebodytemp/wearos-app.git` (HTTPS)
3. import this project into AndroidStudio via File --> New... --> Import Project...
(this will take a while since all the compiler files need to be downloaded in the background)
4. Navigate to the "Terminal" in AndroidStudio.
5. In the terminal, navigate to the location of the adb.exe (android debugger), something like this:
`cd C:\Users\~yourusername\AppData\Local\Android\Sdk\platform-tools>`
6. Connect your PC and your WearOS device to the same WLAN
7. check the IP address of your WearOS device (e.g. 192.168.11.11)
8. Connect the debugger to your WearOS device running
`adb connect 172.168.11.11` in the terminal
9. If everything runs smoothly, your WearOS device will appear as a physical device in the dropdown menu next to the green triangle, click that to flash the code onto the WearOS device.