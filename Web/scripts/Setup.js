var width = 0;
var height = 0;

var realWidth = 0;
var realHeight = 0;

var sw = 50;

function setValues() {
    "use strict";
    width = screen.availWidth;
    height = screen.availHeight;

    if (height > width) {
        realWidth = height;
        realHeight = width;
    }
    else {
        realWidth = width;
        realHeight = height;
    }
}

function resizeBtn() {
    setValues();
    sw = realWidth;
}