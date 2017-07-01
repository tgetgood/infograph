# Infograph

Graphical Interactive Interactive Infographic Development

## Purpose

I want to be able to draw a picture (with a stylus, a mouse, my finger) that
responds to real data. That't it. Why can't I do that?

Imagine something like D3 where instead of programming it like so:

```clj
(.setAttr circle "r" (:r data))
```

You see your data structure on the left, and have a vector drawing tool on the
right. You select the circle drawing widget, draw a circle, drag the value out
of the data structure into the circle, see a list of properties, and drop it on
the radius field. Now that path in the data structure is linked to the radius of
the circle and if you use new data you get a new circle. You can iterate through
a list of such data structures and get a bunch of circles. In other words you
can directly draw with your data.

## Docs

See dev.org for the dev notes.

Other docs may be forthcoming.

## License

Copyright © 2017 Thomas Getgood

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
