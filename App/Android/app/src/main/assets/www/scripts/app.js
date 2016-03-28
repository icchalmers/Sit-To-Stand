var calibTime = 0;

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

var connected = false;

var axisFlip = false;
var lastValue = 0;

//Sent to Android to create file to log values
var message = "";

//Sent to Android to send message to Arduino
var vibration = 0;
var previousMotor;

var VIBRATION_MAX = 255; // Needs to be <= 255
var DEAD_ZONE_ANGLE = 2;
var ANGLE_MAX = 30;

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

function connectBT() {
    "use strict";
    var ua = navigator.userAgent.toLowerCase();
    var isAndroid = ua.indexOf("android") > -1;
    if (isAndroid && typeof Android !== 'undefined') {
        Android.connectBT();
    }
}

function addLog(msg) {
    "use strict";
    var ua = navigator.userAgent.toLowerCase();
    var isAndroid = ua.indexOf("android") > -1;
    if (isAndroid && typeof Android !== 'undefined') {
        Android.addLog(msg);
    }
}
function orient() {
    "use strict";
    var handler = function (event) {
        if (event !== null) {
            if (event.alpha !== null && event.alpha !== 0) {
                if (smoothedAlpha === 0) {
                    smoothedAlpha = event.alpha;
                }
                smoothedAlpha = smoothedAlpha + 0.2 * (event.alpha - smoothedAlpha );
                alpha.push(smoothedAlpha);
                message += "A:" + smoothedAlpha;
            }
            if (event.beta !== null && event.beta !== 0) {
                if (smoothedBeta === 0) {
                    smoothedBeta = event.beta;
                }
                smoothedBeta = smoothedBeta + 0.2 * (event.beta - smoothedBeta);
                message += "B:" + smoothedBeta;
            }
            if (event.gamma !== null && event.gamma !== 0) {
                var g = 2 * event.gamma; // Why does this need multiplied?!
                if (smoothedGamma === 0) {
                    smoothedGamma = g;
                }
                smoothedGamma = smoothedGamma + 0.2 * (g - smoothedGamma);
                gamma.push(smoothedGamma);
                
            }
            
            //Logic to check for gamma flipping over axis
            if (Math.abs(smoothedGamma - lastValue) > 180) {
                axisFlip = !axisFlip;
            } else {
                if (axisFlip) {
                    smoothedBeta = 180 - smoothedBeta;
                }
                beta.push(smoothedBeta);
                message += " Corrected B:" + smoothedBeta;
            }
            lastValue = smoothedGamma;

            message += " G:" + smoothedGamma;
            
            message += " Flipped:" + axisFlip;
            
            var date = new Date();
            var hours = date.getHours().toString();
            var mins = date.getMinutes().toString();
            var secs = date.getSeconds().toString();
            var mil = date.getMilliseconds().toString();
            message += (" T:" + hours + ":" + mins + ":" + secs + ":" + mil + "\n");

//            var absoluteSupported = event.absolute;
//            if (absoluteSupported) {
//                addLog("Using earth based orientation");
//            } else {
//                addLog("Using device based orientation");
//            }
        }

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
            diffBeta = convert("deg", Math.atan2(Math.sin(convert("rad", angleCalibrations[1] - calibBeta)), Math.cos(convert("rad", angleCalibrations[1] - calibBeta))));
            diffGamma = convert("deg", Math.atan2(Math.sin(convert("rad", angleCalibrations[2] - calibGamma)), Math.cos(convert("rad", angleCalibrations[2] - calibGamma))));
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


    var tempTotal = 0;
    var i = 0;
    for (i in totalAlpha) {
        aCos += Math.cos(convert("rad", totalAlpha[i]));
        aSin += Math.sin(convert("rad", totalAlpha[i]));
        tempTotal += convert("deg", Math.atan2(aSin, aCos));
    }
    calibAlpha = tempTotal / i;

    tempTotal = 0;
    i = 0;
    for (i in totalBeta) {
        bCos += Math.cos(convert("rad", totalBeta[i]));
        bSin += Math.sin(convert("rad", totalBeta[i]));
        tempTotal += convert("deg", Math.atan2(bSin, bCos));
    }
    calibBeta = tempTotal / i;

    tempTotal = 0;
    i = 0;
    for (i in totalGamma) {
        gCos += Math.cos(convert("rad", totalGamma[i]));
        gSin += Math.sin(convert("rad", totalGamma[i]));
        tempTotal += convert("deg", Math.atan2(gSin, gCos));
    }
    calibGamma = tempTotal / i;

    message += "Calibrated!\n";
}

//Controller for app set first
function balanceController($scope, $interval) {
    "use strict";
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
    
    $scope.checkBT = function () {
        connectBT();
        $scope.setScreen(1);
    };

    $scope.sendBT = function (motor, value) {
        var ua = navigator.userAgent.toLowerCase();
        var isAndroid = ua.indexOf("android") > -1;
        if (isAndroid && typeof Android !== 'undefined') {
            Android.sendBluetooth(motor, value);
        }
    };

    var running;
    $scope.startOver = function () {
        var ua = navigator.userAgent.toLowerCase();
        var isAndroid = ua.indexOf("android") > -1;
        if (isAndroid && typeof Android !== 'undefined') {
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
        document.body.style.background = 'white';
        

        $interval.cancel(running);
        $scope.sendBT("left", 0);
        $scope.sendBT("right", 0);
        $scope.setScreen(1);
    };

    var calib;
    $scope.startCalib = function () {
        if (!connected) {
            connectBT();
            connected = true;
        }
        message = "";
        calibTime = 0;
        orient();
        calib = $interval(function () {
            calibTime = calibTime + 1;
            document.getElementById('progress').style.width = Math.ceil((calibTime * 10) / 15) + "%";
            showTime();
            if (calibTime % 1 === 0 && calibTime > 0) {
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

    
    $scope.startRunning = function () {
        orient();
        var color = [];
        running = $interval(function () {
            smoothAngles(false);
            var motorSelect
            // TODO diffAlpha does work as a way of detecting which way the user is leaning.
            // Need to translate coordinate system from Earth to Device.
            if (diffAlpha > 0) {
                motorSelect = "right"
//               color = [255, 0, 0];
            } else {
                motorSelect = "left"
//               color = [0, 0, 255];
            }
            // If active side switches, ensure inactive motor turned off
            if(previousMotor != motorSelect) {
                $scope.sendBT(previousMotor, 0);
                previousMotor = motorSelect;
            }
            
            vibration = $scope.calculateVibration(diffBeta);

            $scope.sendBT(motorSelect, vibration);
            //var colorIntensity = Math.min(1, (Math.abs(diffBeta) / 30));
            //color.push(colorIntensity);
            //document.body.style.background = 'rgba(' + color.join(',') + ')';
        }, 100);
    };

    $scope.calculateVibration = function(angle) {
        angle = Math.abs(angle);
        if(angle < DEAD_ZONE_ANGLE){return 0;}
        if(angle > ANGLE_MAX){
            angle = ANGLE_MAX;
        }
        return angle / ANGLE_MAX * VIBRATION_MAX;
        
    }
    
    $scope.getTime = function () {
        var today = new Date();
        return today.toGMTString();
    };
    
}