[![Release](https://jitpack.io/v/lovoo/lastwords.svg)](https://jitpack.io/#lovoo/lastwords)

# lastwords
_lastwords_ is a little library written purely in Kotlin which notifies you when your app is terminated - that is all activities are either finishing or have been destroyed.

## A short definition of a _finished_ app
For _lastwords_ a finished app is an app whose activities are all either _finishing_ or _destroyed_. So yes, we are excluding things like services here.
This proved to be a rather flexible definition for most use cases. If you stumble upon a use case which isn't covered by this feel free to create an issue and we'll discuss.

## Import
_lastwords_ is hosted on JitPack. Therefore you can simply import it by adding

```groovy
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```

to your projects `build.gradle`.

Then add this to you app's `build.gradle`:

```groovy
dependencies {
    compile 'com.github.lovoo:lastwords:1.1.0'
}
```

## Usage
Using _lastwords_ is very simple. In your Application class add the following initialisation code to the `onCreate()` method:

 ```kotlin
override fun onCreate() {
    super.onCreate()

    LastWords.init(this)
}
 ```

In case you don't have an own subclass of the Application class yet, kindly refer to the documentation of [Application](https://developer.android.com/reference/android/app/Application.html).

Whenever you want to be notified of the app finishing just register a listener:

```kotlin
LastWords.register(object : LastWords.Listener {
    override fun onAppFinished() {
        // do whatever you want here: tracking, cleaning up, herding llamas...
        Toast.makeText(this@Application, "App finished", Toast.LENGTH_SHORT).show()
    }
})
```
Of course you can register as many listeners as you want.

To stop listening use `LastWords.unregister(listener)` - you don't want to create nasty memory leaks, do you?

If you want to kill your app process with _lastwords_ you can use:

 ```kotlin
LastWords.finishApp(killDelay)
 ```
 This will finish all Activities, then wait for the given delay and kill your process (if no other Activity is started in the meantime).
 An example would be to notify the user that changes took place after App restart and you show him an option to close the app.
 But do NOT call this if you have running tasks, like Database or Filesystem writing!!!

And that's it. Keep in mind that the system may kill the app process at any point in time (especially as it is effectively backgrounded now), so it's not _guaranteed_ that the code in the listener is executed - but what is guaranteed on our beloved Android anyway? ;)
However it will run happily in the overwhelming majority of cases which usually is better than not executing the code at all - that means it is definitely good for clean up tasks, tracking.