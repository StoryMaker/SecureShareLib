SecureShareLib
==============

SecureShareLib is an Android Library that allows apps to integrate a transfer manager that runs in a background service and has some capability to retry transfer on device reboot.

This package was originally forked from the AOSP Download Provider package.  We extended it's classes to allow for both Downloads and Uploads in its database and content provider.

###Building

- You need to place your Flickr keys in res/values/flickr.xml with your own from Flickr's [create site page](https://www.flickr.com/services/apps/create/apply/)

Example:

    <?xml version="1.0" encoding="utf-8"?>
    <resources>
        <!-- insert your own keys from: https://www.flickr.com/services/apps/create/apply/ -->
        <item name="flickr_key" type="flickr">REPLACE_WITH_YOUR_KEY</item>
        <item name="flickr_secret" type="flickr">REPLACE_WITH_YOUR_KEY</item>
    </resources>


###What's next?


In the next release we will merge this code with the fork of the Download Manger ([here](https://github.com/scaliolabs/DownloadProvider)) that has been split out from being an internal platform app into being a stand alone Android SDK app.  This will allow us to build the Transfer Manager

###Future Reseach


- Plugin API

We will investigate how to allow 3rd party packages to expose their own transfer implementations at runtime via a plugin interface.  This would allow a more dynamic list of supported partner platforms.

- Resume

Some services such as Youtube or Rsync allow us to resume after an interrupted transfer.  We plan to extend our api to allow us to support this for those platforms

- Rate limiting

We intend to add the ability to lower the rate of trasnfer so as to not saturation your mobile data link

###Issues
You may get the following errors when trying to build the project:
- Unable to execute dex: Multiple dex files define Lorg/json/JSONArray;
- Conversion to Dalvik format failed: Unable to execute dex: Multiple dex files define Lorg/json/JSONArray;

These are caused when there are two duplicate jar files seen from the main project.  To fix this issue, follow the steps below:
- remove the conflicting jar from the sub-project (delete the soundcloud-api-wrapper.jar from the libs folder in SecureShareLib)
- right click the project name
- go to properties
- select 'Java Build Path' on left menu, then the 'Libraries' tab
- click 'Add JARs' button on right and point it to the soundcloud-api-wrapper.jar (located in secureshareuilibrary/external/)
- clean and rebuild

###Credits

SecureShareLib was developed by [Scal.io](http://scal.io) and [Small World News](http://smallworldnews.tv/) as part of the [StoryMaker](http://storymaker.cc/) project with the generous support of [Open Technology Fund](https://www.opentechfund.org/).

[StoryMaker - Make your story great](http://storymaker.cc/)

### Authors

- [Josh Steiner](https://github.com/vitriolix/)
- [Micah Lucas](https://github.com/micahjlucas/)
