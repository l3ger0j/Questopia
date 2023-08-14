# Questopia

![android-test](https://github.com/l3ger0j/QuestPlayerAndroid/actions/workflows/android-test.yml/badge.svg) ![android-build](https://github.com/l3ger0j/QuestPlayerAndroid/actions/workflows/android-build.yml/badge.svg)

Android application for launching text format games [QSP](https://github.com/seedhartha/qsp).

## Thanks

* **[Nikolai Reznik](https://github.com/shirrumon)** for creating a PrettyFilePicker.

## How to use the Plugin API

**At the moment, the Plugin API is still in development! Therefore, in order for your plugin to work successfully with my application, you will need to contact me.**

To get started, you will need to create an [AIDL](https://developer.android.com/guide/components/aidl) interface, be sure to place it on the path "org.qp.android.plugin". Next, you need to fill in this file with the following information:

```
interface yourInterfaceName {
    String versionPlugin();
    String titlePlugin();
    String authorPlugin();
}
```
Don't forget to return your AIDL interface with the Stub() parameter in the service class, in the onBind method, and initialize the values, and the framework of your plugin for the application is ready!

## Disclaimer

**This fork was made WhoYouAndM3. The original project (hereinafter **[QuestPlayer](https://github.com/seedhartha/QuestPlayer)**) with an no license does not bear any responsibility for this fork **[QuestPlayAndroid](https://github.com/l3ger0j/QuestPlayer.git)**.**