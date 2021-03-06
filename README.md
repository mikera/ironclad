# Ironclad

Ironclad: Steam Legions - A steampunk strategy wargame in Clojure

### About

Ironclad: Steam Legions is inspired by many of the old hex-based strategy games from the 1980s and 1990s. 

Interesting technical features:

 - Written almost entirely in Clojure (http://clojure.org/)
 - The game uses a lot of functional programming techniques rather than OOP. For example, the entire game state is a single immutable data structure
 - The graphics are rendered using POVRay (http://www.povray.org/)
 - The game uses a custom steampunk-themed Swing UI, implemented in the project https://github.com/mikera/steampunk-laf

### Storyline

In the land of Europa war is brewing..... 

Take command of a steam-powered army and crush your opponents in glorious battle!

### Building and running

To build Ironclad, it is recommended to use Maven to create a single jar with all dependencies

```
mvn assembly:single
```

This will create a runnable jar in the output directory (usually `target/ironclad-0.x.x.some-extension.jar`)
