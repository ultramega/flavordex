# Flavordex Tasting Journal

Flavordex is a customizable tasting journal for Android devices.

   * [Official Website](http://flavordex.ultramegasoft.com/)
   * [Google Play Page](https://play.google.com/store/apps/details?id=com.ultramegasoft.flavordex2)
   * [Amazon Appstore Page](https://www.amazon.com/gp/mas/dl/android?p=com.ultramegasoft.flavordex2)
   * [Windows Version](https://github.com/ultramega/flavordex-uwp)

## Setup

This project will not work out of the box. The application makes use of several external APIs, and
the credentials are not included in the public repository for security reasons. You'll have to
provide your own credentials to get the project up and running.

   * Create a [Firebase](https://firebase.google.com/) project to get a *google-services.json* file.
   * For [Fabric](https://get.fabric.io/), instead of adding the API key to the manifest, place it
     in the *fabric.properties* file using the key `apiKey`.
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

## Donate

If you would like to support this project, donations are greatly appreciated!

- [PayPal](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=NKT58XX87QL4G)
- [![Flattr this](https://button.flattr.com/flattr-badge-large.png)](https://flattr.com/submit/auto?fid=jeznxl&url=http%3A%2F%2Fflavordex.ultramegasoft.com%2F)
- Bitcoin: 14YxiZKnGhYog3sL1fWq7QCdxTcCTCguRo

## License

The source code for Flavordex is released under the terms of the
[MIT License](http://sguidetti.mit-license.org/).
