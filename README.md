This Android app provides a way to back up your text message content. It reads all SMS and MMS text messages, exports the messages, images, videos, and group MMS address info to a JSON file on your phone. You can then copy it to your computer and write code to do whatever you want with it.

Audience: devs mildly familiar with Android app development.

## Using this app

1. Install Android Studio
1. Android Studio seems to change project structure every year, so create a new project
1. From the git repo, manually copy in the MainApplication.java code, modify the Manifest, tweak the strings resource
1. Hook your phone up to your machine
1. Ensure Android Studio sees it
1. Build and Run the app on your phone
1. Press Backup button
1. Watch the console log in Android Studio to see it making progress
1. Note the location of the JSON that shows on your phone's screen
1. Use the Device Explorer to navigate to the correct folder
1. Right click the file and Save-As to copy to your computer

