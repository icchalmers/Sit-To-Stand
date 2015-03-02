var calibTime = 0;
var width = 0;
var height = 0;

var realWidth = 500;
var realHeight = 500;

var sw = 50;
var audio;

var screenNum = 1;
var calibDone = false;
var angleCalibrations = [];

var alpha = [];
var beta = [];
var gamma = [];
window.navigator = window.navigator || {};

//Controller for app set first
function balanceController($scope, $interval) {
    setValues();
    $scope.getStyle = function () {
        var s = "width:" + realWidth + "px; height:" + realHeight + "px; font-size:xx-large";
        return s;
    }

    $scope.isScreen = function (num) {
        return screenNum === num;
    }

    $scope.nextScreen = function () {
        screenNum++;
    }

    $scope.getAngleAverage = function(num){
        return (Math.round(angleCalibrations[num] * 100) / 100).toFixed(2);
    }

    var calib;
    $scope.startCalib = function () {
        calibTime = 0;
        calib = $interval(function () {
            calibTime++;
            document.getElementById('progress').style.width = Math.ceil((calibTime * 10)/30) + "%";
            showTime();
            orient();
            if (calibTime % 5 === 0) {
                calibrate();
                }
            if (calibTime > 300) {
                document.getElementById('progress').classList.remove("active");
                playSound();
                screenNum++;
                $interval.cancel(calib);
            }
        }, 100);
    }
}

//Helper methods for controller to use
function setValues() {
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

function startCalibration() {
    
}

function showTime() {
    var t = "Time elapsed is: " + Math.floor(calibTime/10) + "s";
    document.getElementById("time").innerHTML = t;
}

function playSound() {
    audio.play();
}

function updateBar() {
    calibTime += 0.1;
    document.getElementById('progress').style.width = Math.ceil(calibTime * 3.33333334) + "%";
    showTime();
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

function convert(type, angle) {
    if (type == 'deg') {
        return angle * (180 / Math.PI);
    }
    else {
        return angle * (Math.PI / 180);
    }
}

function calibrate() {
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

    var a = (360 + convert("deg", Math.atan2(aSin, aCos))) % 360;
    var b = convert("deg", Math.atan2(bSin, bCos));
    var g = convert("deg", Math.atan2(gSin, gCos));

    if (aLength * bLength * gLength != 0) {
        angleCalibrations = [a, b, g];
        var html = '';
        html += 'a: ' + (Math.round(a * 100) / 100).toFixed(2) + '&nbsp;&nbsp;&nbsp;&nbsp;y: ' + (Math.round(b * 100) / 100).toFixed(2) + '&nbsp;&nbsp;&nbsp;&nbsp;z: ' + (Math.round(g * 100) / 100).toFixed(2) + '<br />';
        document.getElementById('showAngles').innerHTML = html;
    }
}

    function setSource() {
        audio = document.getElementById('player');
        audio.src = 'sounds/startTone.mp3';
    }

    function requiresUserGesture() {
        // test if play() is ignored when not called from an input event handler
        var test = document.createElement('audio');
        test.play();
        return test.paused;
    }

    function removeBehaviorsRestrictions() {
        audio = document.getElementById('player');
        audio.load();
        window.removeEventListener('keydown', removeBehaviorsRestrictions);
        window.removeEventListener('mousedown', removeBehaviorsRestrictions);
        window.removeEventListener('touchstart', removeBehaviorsRestrictions);
        setTimeout(setSource, 1000);
    }

    function setupSound() {
        if (requiresUserGesture()) {
            window.addEventListener('keydown', removeBehaviorsRestrictions);
            window.addEventListener('mousedown', removeBehaviorsRestrictions);
            window.addEventListener('touchstart', removeBehaviorsRestrictions);
        }
        else {
            setSource();
        }
    }