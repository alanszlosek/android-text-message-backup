This Android app provides a way to back up your text message content. It reads all SMS+MMS text messages and exports the messages, images, videos, group MMS addresses to a JSON file on your phone. You can then copy it to your computer and write code to do whatever you want with it.

Audience: devs mildly familiar with Android app development.

## Using this app

1. Install Android Studio
1. Android Studio seems to change project structure every year, so create a new project
1. From the src folder
    * Manually copy in the MainApplication.java code
    * Modify your project's Manifest to more closely match mine. Note permissions and default activity launch stuff
    * Tweak the strings resource
1. Hook your phone up to your computer using a USB cable
1. Ensure Android Studio sees it
1. Build and Run the app on your phone
1. Press the Backup button on the app
1. Watch the console log in Android Studio to see it making progress
1. Note the location of the JSON that shows on your phone's screen
1. Use the Device Explorer to navigate to the correct folder
1. Right click the file and Save-As to copy to your computer

