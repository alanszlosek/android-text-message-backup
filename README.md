This Android app provides a way to back up your text message content. It reads all SMS+MMS text messages and exports the messages, images, videos, group MMS addresses to a JSON file on your phone. You can then copy it to your computer and write code to do whatever you want with it.

Audience: devs mildly familiar with Android app development.

## Using this app

1. Install Android Studio
1. Android Studio seems to change project structure every year, so create a new project
    * Select Empty Activity
    * On the next screen select "API 16: Android 4.1 (Jelly Bean)" as the Minimum SDK. Unless of course you need something earlier and are up for some light development work.
    * Fill out the rest of the fields (I assume you know how to use Android project)
1. From the src folder
    * Copy my code into your `MainApplication.java` file
    * Modify your project's `AndrioidManifest.xml` to closely match mine. Note permissions and default activity launch stuff
    * Modify your `strings.xml` resource file and copy in the 4 resources from mine
    * Copy my `activity_main.xml` layout file contents into yours
1. Hook your phone up to your computer using a USB cable
1. Ensure Android Studio sees it
1. Build and Run the app on your phone. Be sure to grant the app SMS and Storage privs.
1. Press the Backup button on the app
1. Watch the console log in Android Studio to see it making progress
1. Note the location of the JSON that shows on your phone's screen
1. Use the Device Explorer to navigate to the correct folder
1. Right click the file and Save-As to copy to your computer

