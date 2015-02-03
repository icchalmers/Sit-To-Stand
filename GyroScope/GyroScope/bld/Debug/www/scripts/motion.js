var dataContainerOrientation;
var dataContainerMotion;
var dataContainerCount;
var counter;

var xArray = [];
var yArray = [];
var zArray = [];

function init() {
    dataContainerOrientation = document.getElementById('dataContainerOrientation');
    dataContainerMotion = document.getElementById('dataContainerMotion');
    dataContainerCount = document.getElementById('counter');
    counter = 0;
}

function orient(){
    if (window.DeviceOrientationEvent) {
        window.addEventListener('deviceorientation', function (event) {
            var alpha = (Math.round(event.alpha * 100) / 100).toFixed(2);
            var beta = (Math.round(event.beta * 100) / 100).toFixed(2);
            var gamma = (Math.round(event.gamma * 100) / 100).toFixed(2);

            event.target.removeEventListener(event.type, arguments.callee);

            if (alpha != null || beta != null || gamma != null)
                dataContainerOrientation.innerHTML = 'alpha: ' + alpha + '<br/>beta: ' + beta + '<br/>gamma: ' + gamma;
        }, false);
    }
}

    
function motion(){
    if (window.DeviceMotionEvent) {
        window.addEventListener('devicemotion', function (event) {
            var x = (Math.round(event.acceleration.x * 100) / 100).toFixed(2);
            xArray.push(x);
            var y = (Math.round(event.acceleration.y * 100) / 100).toFixed(2);
            yArray.push(y);
            var z = (Math.round(event.acceleration.z * 100) / 100).toFixed(2);
            zArray.push(z);
            var r = event.rotationRate;

            event.target.removeEventListener(event.type, arguments.callee);


            //if (r != null) html += 'alpha: ' + (Math.round(r.alpha * 100) / 100).toFixed(2) + '<br />beta: ' + (Math.round(r.beta * 100) / 100).toFixed(2) + '<br/>gamma: ' + (Math.round(r.gamma * 100) / 100).toFixed(2) + '<br />';
            
        });
    }
}

function updateCount() {
  counter++;
    dataContainerCount.innerHTML = counter;
}

function getReading() {
    window.setInterval(function () {
        orient();
        motion();
        updateCount();
        if (counter % 5 == 0) {
            var xSum = 0;
            var ySum = 0;
            var zSum = 0;
            var arrLength = xArray.length;
            //for (var i = 0; i < xArray.length; i++) {
            //    xSum += parseFloat(xArray[i]);
            //    ySum += parseFloat(yArray[i]);
            //    zSum += parseFloat(zArray[i]);
            //}
            while (xArray.length > 0) {
                xSum += parseFloat(xArray.pop());
                ySum += parseFloat(yArray.pop());
                zSum += parseFloat(zArray.pop());
            }
            var html = 'Acceleration:<br />';
            html += 'x: ' + (Math.round(xSum / arrLength * 100) / 100).toFixed(2) + '<br />y: ' + (Math.round(ySum / arrLength * 100) / 100).toFixed(2) + '<br/>z: ' + (Math.round(zSum / arrLength * 100) / 100).toFixed(2) + '<br />';
            //html += 'Rotation rate:<br />';
            dataContainerMotion.innerHTML = html;
            //while (xArray.length > 0) {
            //    xArray.pop();
            //    yArray.pop();
            //    zArray.pop();
            //}
        }
    }, 100);
}