var dataContainerOrientation;
var counter = 0;
var alpha = [];
var beta = [];
var gamma = [];

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

        var aSum = 0;
        var bSum = 0;
        var gSum = 0;

        var aLength = tempA.length;
        var bLength = tempB.length;
        var gLength = tempG.length;

        while (tempA.length > 0) {
            aSum += parseFloat(tempA.pop());
        }
        while (tempB.length > 0) {
            bSum += parseFloat(tempB.pop());
        }
        while (tempG.length > 0) {
            gSum += parseFloat(tempG.pop());
        }

        if (aLength + bLength + gLength > 0) {
            var html = '';
            html += 'a: ' + (Math.round(aSum / aLength * 100) / 100).toFixed(2) + '&nbsp;&nbsp;&nbsp;&nbsp;y: ' + (Math.round(bSum / bLength * 100) / 100).toFixed(2) + '&nbsp;&nbsp;&nbsp;&nbsp;z: ' + (Math.round(gSum / gLength * 100) / 100).toFixed(2) + '<br />';
            dataContainerOrientation.innerHTML = html;
        }
    }, 1000);
}