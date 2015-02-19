var calibTime = 0;

function balanceController($scope) {
    $scope.hideStart = true;
    $scope.toggleStart = function () {
        $scope.hideStart = !$scope.hideStart;
        startCalibration();
    }
}

function startCalibration() {
    calibTime = 0;
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