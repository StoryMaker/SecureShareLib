SecureShareLib
==============

SecureShareLib is an Android Library that allows apps to integrate a transfer manager that runs in a background service and has some capability to retry transfer on device reboot.

This package was originally forked from the AOSP Download Provider package.  We extended it's classes to allow for both Downloads and Uploads in its database and content provider.

###What's next?


In the next release we will merge this code with the fork of the Download Manger ([here](https://github.com/scaliolabs/DownloadProvider)) that has been split out from being an internal platform app into being a stand alone Android SDK app.  This will allow us to build the Transfer Manager

###Future Reseach


- Plugin API

We will investigate how to allow 3rd party packages to expose their own transfer implementations at runtime via a plugin interface.  This would allow a more dynamic list of supported partner platforms.

- Resume

Some services such as Youtube or Rsync allow us to resume after an interrupted transfer.  We plan to extend our api to allow us to support this for those platforms

- Rate limiting

We intend to add the ability to lower the rate of trasnfer so as to not saturation your mobile data link

###Credits

SecureShareLib was developed by [Scal.io](http://scal.io) and [Small World News](http://smallworldnews.tv/) as part of the [StoryMaker](http://storymaker.cc/) project with the generous support of [Open Technology Fund](https://www.opentechfund.org/).

[StoryMaker - Make your story great](http://storymaker.cc/)

### Authors

- [Josh Steiner](https://github.com/vitriolix/)
- [Micah Lucas](https://github.com/micahjlucas/)
