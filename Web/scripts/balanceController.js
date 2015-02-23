var calibTime = 0;
var width = 0;
var height = 0;

var realWidth = 500;
var realHeight = 500;

var sw = 50;

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

function balanceController($scope) {
    setValues();
    $scope.getStyle = function () {
        var s = "width:" + realWidth + "px; height:" + realHeight + "px; font-size:xx-large";
        return s;
    }
    $scope.hideStart = true;
    $scope.toggleStart = function () {
        $scope.hideStart = !$scope.hideStart;
        startCalibration();
    }
}

function startCalibration() {
    calibTime = -0.9;
    var calib = setInterval(function () {
        calibTime+=0.1;
        document.getElementById('progress').style.width = Math.ceil((calibTime * 1000) / 300) + "%";
        showTime();
        if (calibTime > 30.1) {
            document.getElementById('progress').classList.remove("active");
            clearInterval(calib);
            ($scope.progress).removeClass('active');
        }
    }, 100);
}

function showTime() {
    var t = "Time elapsed is: " + Math.round(calibTime) + "s";
    document.getElementById("time").innerHTML = t;
}