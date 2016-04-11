# MECrawler
Crawler able to emulate several different mobile devices, throttle network. 

It is also able to load browser extensions and dump results about HTTP Requests in har.

It leverages Selenium and Browsermod proxy.


#Usage

Usage: crawlList.rb [options]

    -f, --file FILE                  File with url list
    
    -d, --directory DIRECTORY        Change directory with resources
    
    -m, --device DEVICE              Mobile device to emulate
    
    -t, --throttle THROTTLE          Network Throttling
    
    -r, --rerun REPLAYS              Number of back-to-back reruns
    
    -c, --caching CACHING            Use caching and cookies
    
    -e, --extension EXTENSION        Extension to load
    
