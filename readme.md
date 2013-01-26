# A Star Map in ClojureScript

![](http://ashenfad.github.com/stars/stars.png)

This project uses ClojureScript, canvas, and affine transforms to build a "star map" for a half-finished toy game.  The star map supports panning and zooming (click and drag to pan, scroll in or out to zoom).

The notable bits of the codebase are the [canvas namespace](https://github.com/ashenfad/stars/blob/master/src/cljs/mogul/ui/canvas.cljs) and the [affine transform namespace](https://github.com/ashenfad/stars/blob/master/src/cljs/mogul/ui/transform.cljs).  The implementation of the 2d star map was borrowed from an earlier incarnation I once did in Java 2D.  I think there's an opportunity for a nice ClojureScript/Clojure library that would accept the same 2D drawing and transform operations for either a browser canvas element or a Java Graphics object.

Anyway - to see the map in action:
http://ashenfad.github.com/stars/
