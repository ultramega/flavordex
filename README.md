# Flavordex Tasting Journal

Flavordex is a customizable tasting journal for Android devices.

   * [Google Play Page](https://play.google.com/store/apps/details?id=com.ultramegasoft.flavordex2)
   * [Amazon Appstore Page](https://www.amazon.com/gp/mas/dl/android?p=com.ultramegasoft.flavordex2)

## Setup

This project will not work out of the box. The application makes use of several external APIs, and
the credentials are not included in the public repository for security reasons. You'll have to
provide your own credentials to get the project up and running.

   * Create a [Firebase](https://firebase.google.com/) project to get a *google-services.json* file.
   * For [Facebook](https://developers.facebook.com/) and [Twitter](https://apps.twitter.com/),
     place the credentials in a values resource file called *keys.xml*:
     ```xml
     <?xml version="1.0" encoding="utf-8"?>
     <resources>
         <string name="facebook_app_id" translatable="false"></string>
         <string name="com.twitter.sdk.android.CONSUMER_KEY" translatable="false"></string>
         <string name="com.twitter.sdk.android.CONSUMER_SECRET" translatable="false"></string>
     </resources>
     ```

## Backend Server

To set up a server to enable data syncing, you'll need the
[Flavordex API Server](https://github.com/ultramega/flavordex-api-server), which is a PHP/MySQL
application.

## License

The source code for Flavordex is released under the terms of the
[MIT License](http://sguidetti.mit-license.org/).
