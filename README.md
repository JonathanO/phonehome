# Phone is Home
Hello world in Scala. Sorta.

Continuously monitors to see if a given address is reachable. If it is, it'll prevent Nest going into auto-away.

Nest's auto-away stuff is all very well and good, but if I'm hiding upstairs all day it tries to freeze me to death.
This is used to monitor if one of our phones is on the home wifi, and if it is, it'll stop Nest going auto-away.

Manually setting away will still work, it's just auto-away we'll keep trying to cancel.

This is my first attempt at doing anything in Scala, and anything with the Nest API, so I have no idea if it's
even slightly sane. I'm pretty sure I've made some pretty big Scala WTFs. It's almost certainly not production
ready. There are also no tests and no instructions (if you can't work out how to get a Nest dev account, and
use that get an auth token, you probably shouldn't be trying to use this.)

It needs away read/write permissions on structure. It will fiddle with all structures on your account, I guess
I should probably make that configurable at some point, but I only have one house so SEP.