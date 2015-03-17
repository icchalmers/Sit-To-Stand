var calibTime = 0;
var width = 0;
var height = 0;

var realWidth = 500;
var realHeight = 500;

var audio;

var screenNum = 0;
var angleCalibrations = [0, 0, 0];

var alpha = [];
var beta = [];
var gamma = [];

var totalAlpha = [];
var totalBeta = [];
var totalGamma = [];
var calibAlpha = 0;
var calibBeta = 0;
var calibGamma = 0;
var diffAlpha = 0;
var diffBeta = 0;
var diffGamma = 0;
var smoothedAlpha = 0;
var smoothedBeta = 0;
var smoothedGamma = 0;

var rawValues = [];

//Sent to Android to create file to log values
var message = "";


//Helper methods for controller to use
function setValues() {
    "use strict";
    width = screen.availWidth;
    height = screen.availHeight;

    if (height > width) {
        realWidth = height;
        realHeight = width;
    } else {
        realWidth = width;
        realHeight = height;
    }
}

function showTime() {
    "use strict";
    var t = "Time elapsed is: " + Math.floor(calibTime / 10) + "s\n";
    document.getElementById("time").innerHTML = t;
}

function playSound() {
    "use strict";
    audio.play();
}

function updateBar() {
    "use strict";
    calibTime += 0.1;
    document.getElementById('progress').style.width = Math.ceil(calibTime * 3.33333334) + "%";
    showTime();
}

function orient() {
    "use strict";
    var handler = function (event) {
        if (event !== null) {
            if (event.alpha !== null && event.alpha !== 0) {
                if (smoothedAlpha === 0) {
                    smoothedAlpha = event.alpha;
                }
                smoothedAlpha = event.alpha + 0.25 * (smoothedAlpha - event.alpha);
                alpha.push(smoothedAlpha);
                message += "A:" + smoothedAlpha;
            }
            if (event.beta !== null && event.beta !== 0) {
                if (smoothedBeta === 0) {
                    smoothedBeta = event.beta;
                }
                smoothedBeta = event.beta + 0.25 * (smoothedBeta - event.beta);
                beta.push(smoothedBeta);
                message += " B:" + smoothedBeta;
            }
            if (event.gamma !== null && event.gamma !== 0) {
                var g = 2*event.gamma;
                if (smoothedGamma === 0) {
                    smoothedGamma = g;
                }
                smoothedGamma = g + 0.25 * (smoothedGamma - g);
                gamma.push(smoothedGamma);
                message += " G:" + smoothedGamma;
            }
            var date = new Date();
            var hours = date.getHours().toString();
            var mins = date.getMinutes().toString();
            var secs = date.getSeconds().toString();
            var mil = date.getMilliseconds().toString();
            message += (" T:" + hours + ":" + mins + ":" + secs + ":" + mil + "\n");
        }
        //window.removeEventListener('deviceorientation', handler, false);
    };


    if (window.DeviceOrientationEvent) {
        window.addEventListener('deviceorientation', handler, false);
    }
}

function convert(type, angle) {
    "use strict";
    if (type === 'deg') {
        return angle * (180 / Math.PI);
    } else {
        return angle * (Math.PI / 180);
    }
}

function smoothAngles(calibrating) {
    "use strict";
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
        aSin += Math.sin(convert("rad", parseFloat(tempA.pop())));
    }
    while (tempB.length > 0) {
        bCos += Math.cos(convert("rad", parseFloat(tempB[0])));
        bSin += Math.sin(convert("rad", parseFloat(tempB.pop())));
    }
    while (tempG.length > 0) {
        gCos += Math.cos(convert("rad", parseFloat(tempG[0])));
        gSin += Math.sin(convert("rad", parseFloat(tempG.pop())));
    }

    var a = convert("deg", Math.atan2(aSin, aCos));
    var b = convert("deg", Math.atan2(bSin, bCos));
    var g = convert("deg", Math.atan2(gSin, gCos));

    //message += "Averaged:\nA:" + a + " B:" + b + " G:" + g + "\n";

    if (aLength * bLength * gLength !== 0) {
        angleCalibrations = [a, b, g];
        if (calibrating) {
            totalAlpha.push(a);
            totalBeta.push(b);
            totalGamma.push(g);
        } else {
            diffAlpha = convert("deg", Math.atan2(Math.sin(convert("rad", angleCalibrations[0] - calibAlpha)), Math.cos(convert("rad", angleCalibrations[0] - calibAlpha))));
            diffBeta = angleCalibrations[1] - calibBeta;
            diffGamma = angleCalibrations[2] - calibGamma;
        }
    }
}

function setSource() {
    "use strict";
    audio = document.getElementById('player');
    audio.src = 'sounds/startTone.mp3';
}

function requiresUserGesture() {
    "use strict";
    // test if play() is ignored when not called from an input event handler
    var test = document.createElement('audio');
    test.play();
    return test.paused;
}

function removeBehaviorsRestrictions() {
    "use strict";
    audio = document.getElementById('player');
    audio.load();
    window.removeEventListener('keydown', removeBehaviorsRestrictions);
    window.removeEventListener('mousedown', removeBehaviorsRestrictions);
    window.removeEventListener('touchstart', removeBehaviorsRestrictions);
    setTimeout(setSource, 100);
}

function setupSound() {
    "use strict";
    if (requiresUserGesture()) {
        window.addEventListener('keydown', removeBehaviorsRestrictions);
        window.addEventListener('mousedown', removeBehaviorsRestrictions);
        window.addEventListener('touchstart', removeBehaviorsRestrictions);
    } else {
        setSource();
    }
}

function calibrateValues() {
    "use strict";
    var aSin = 0;
    var bSin = 0;
    var gSin = 0;

    var aCos = 0;
    var bCos = 0;
    var gCos = 0;


    var i = 0;
    for (i in totalAlpha) {
        aCos += Math.cos(convert("rad", totalAlpha[i]));
        aSin += Math.sin(convert("rad", totalAlpha[i]));
    }
    calibAlpha = (360 + convert("deg", Math.atan2(aSin, aCos))) % 360;

    i = 0;
    for (i in totalBeta) {
        bCos += Math.cos(convert("rad", totalBeta[i]));
        bSin += Math.sin(convert("rad", totalBeta[i]));
    }
    calibBeta = convert("deg", Math.atan2(bSin, bCos));

    i = 0;
    for (i in totalGamma) {
        gCos += Math.cos(convert("rad", totalGamma[i]));
        gSin += Math.sin(convert("rad", totalGamma[i]));
    }
    calibGamma = convert("deg", Math.atan2(gSin, gCos));

    message += "Calibrated!\n";
}

//Controller for app set first
function balanceController($scope, $interval) {
    "use strict";
    setValues();
    $scope.getStyle = function () {
        var s = "width:" + realWidth + "px; height:" + realHeight + "px; font-size:xx-large";
        return s;
    };

    $scope.isScreen = function (num) {
        return screenNum === num;
    };

    $scope.nextScreen = function () {
        screenNum = screenNum + 1;
    };

    $scope.setScreen = function (num) {
        screenNum = num;
    };

    $scope.getAngleAverage = function (num) {
        return (Math.round(angleCalibrations[num] * 100) / 100).toFixed(2);
    };

    $scope.getCalibValue = function (num) {
        if (num === 0) {
            return (Math.round(calibAlpha * 100) / 100).toFixed(2);
        }
        if (num === 1) {
            return (Math.round(calibBeta * 100) / 100).toFixed(2);
        }
        if (num === 2) {
            return (Math.round(calibGamma * 100) / 100).toFixed(2);
        }
    };

    $scope.getDiffValue = function (num) {
        if (num === 0) {
            return (Math.round(diffAlpha * 100) / 100).toFixed(2);
        }
        if (num === 1) {
            return (Math.round(diffBeta * 100) / 100).toFixed(2);
        }
        if (num === 2) {
            return (Math.round(diffGamma * 100) / 100).toFixed(2);
        }
    };
    
    $scope.checkBT = function(){
        var ua = navigator.userAgent.toLowerCase();
        var isAndroid = ua.indexOf("android") > -1;
        if(isAndroid && typeof Android !== 'undefined'){
        Android.makeFile(message);
        }
    };


    $scope.startOver = function () {
        var ua = navigator.userAgent.toLowerCase();
        var isAndroid = ua.indexOf("android") > -1;
        if(isAndroid && typeof Android !== 'undefined'){
        Android.makeFile(message);
        }
        
        //Reset variables
        angleCalibrations = [0, 0, 0];

        alpha = [];
        beta = [];
        gamma = [];

        totalAlpha = [];
        totalBeta = [];
        totalGamma = [];
        calibAlpha = 0;
        calibBeta = 0;
        calibGamma = 0;
        diffAlpha = 0;
        diffBeta = 0;
        diffGamma = 0;
        smoothedAlpha = 0;
        smoothedBeta = 0;
        smoothedGamma = 0;

        rawValues = [];

        document.getElementById('progress').style.width = 0;

        $scope.setScreen(1);
    };

    var calib;
    $scope.startCalib = function () {
        message = "";
        calibTime = 0;
        orient();
        calib = $interval(function () {
            calibTime = calibTime + 1;
            document.getElementById('progress').style.width = Math.ceil((calibTime * 10) / 15) + "%";
            showTime();
            if (calibTime % 5 === 0 && calibTime > 0) {
                smoothAngles(true);
            }
            if (calibTime > 150) {
                playSound();
                calibrateValues();
                $scope.setScreen(3);
                $interval.cancel(calib);
                $scope.startRunning();
            }
        }, 100);
    };

    var running;
    $scope.startRunning = function () {
        orient();
        running = $interval(function () {
            smoothAngles(false);
        }, 500);
    };

    $scope.getTime = function () {
        var today = new Date();
        return today.toGMTString();
    };
}