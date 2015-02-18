var dataContainerOrientation;
var counter = 0;
var alpha = [];
var beta = [];
var gamma = [];


//Function to allow conversion between degrees and radians
function convert(type, angle) {
    if (type == 'deg') {
        return angle * (180 / Math.PI);
    }
    else {
        return angle * (Math.PI / 180);
    }
}

//Sets up variables
function init() {
    dataContainerOrientation = document.getElementById('dataContainerOrientation');
}

function orient() {
    if (window.DeviceOrientationEvent) {
        window.addEventListener('deviceorientation', function (event) {
            if (event.alpha != null) {
                alpha.push(event.alpha);
            }
            if (event.beta != null) {
                beta.push(event.beta);
            }
            if (event.gamma != null) {
                gamma.push(event.gamma);
            }
        });

    }
}

function getReading() {
    window.setInterval(function () {
        orient()
    }, 100);
    window.setInterval(function () {
        var tempA = alpha;
        var tempB = beta;
        var tempG = gamma;

        alpha = [];
        beta = [];
        gamma = [];

        var aSin = 0;
        var bSin = 0;
        var gSin = 0;

        var aCos = 0;
        var bCos = 0;
        var gCos = 0;

        var aLength = tempA.length;
        var bLength = tempB.length;
        var gLength = tempG.length;

        while (tempA.length > 0) {
            aCos += Math.cos(convert("rad", parseFloat(tempA[0])));
            aSin += Math.sin(convert("rad",parseFloat(tempA.pop())));
        }
        while (tempB.length > 0) {
            bCos += Math.cos(convert("rad", parseFloat(tempB[0])));
            bSin += Math.sin(convert("rad", parseFloat(tempB.pop())));
        }
        while (tempG.length > 0) {
            gCos += Math.cos(convert("rad", parseFloat(tempG[0])));
            gSin += Math.sin(convert("rad", parseFloat(tempG.pop())));
        }

        var a = (360+ convert("deg", Math.atan2(aSin , aCos)))%360;
        var b = convert("deg", Math.atan2(bSin , bCos));
        var g = convert("deg", Math.atan2(gSin , gCos));

        if (aLength + bLength + gLength > 0) {
            var html = '';
            html += 'a: ' + (Math.round(a * 100) / 100).toFixed(2) + '&nbsp;&nbsp;&nbsp;&nbsp;y: ' + (Math.round(b * 100) / 100).toFixed(2) + '&nbsp;&nbsp;&nbsp;&nbsp;z: ' + (Math.round(g * 100) / 100).toFixed(2) + '<br />';
            dataContainerOrientation.innerHTML = html;
        }
    }, 500);
}