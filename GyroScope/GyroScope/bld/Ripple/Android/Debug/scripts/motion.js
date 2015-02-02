var dataContainerOrientation;
var dataContainerMotion;
var dataContainerCount;
var counter;

function init() {
    dataContainerOrientation = document.getElementById('dataContainerOrientation');
    dataContainerMotion = document.getElementById('dataContainerMotion');
    dataContainerCount = document.getElementById('counter');
    counter = 0;
}

function orient(){
    if (window.DeviceOrientationEvent) {
        window.addEventListener('deviceorientation', function (event) {
            updateCount();
            var alpha = (Math.round(event.alpha * 100) / 100).toFixed(2);
            var beta = (Math.round(event.beta * 100) / 100).toFixed(2);
            var gamma = (Math.round(event.gamma * 100) / 100).toFixed(2);

            if (alpha != null || beta != null || gamma != null)
                dataContainerOrientation.innerHTML = 'alpha: ' + alpha + '<br/>beta: ' + beta + '<br/>gamma: ' + gamma;
        }, false);
    }
}

    
function motion(){
    if (window.DeviceMotionEvent) {
        window.addEventListener('devicemotion', function (event) {
            var x = (Math.round(event.acceleration.x * 100) / 100).toFixed(2);
            var y = (Math.round(event.acceleration.y * 100) / 100).toFixed(2);
            var z = (Math.round(event.acceleration.z * 100) / 100).toFixed(2);
            var r = event.rotationRate;

            var html = 'Acceleration:<br />';
            html += 'x: ' + x + '<br />y: ' + y + '<br/>z: ' + z + '<br />';
            html += 'Rotation rate:<br />';
            if (r != null) html += 'alpha: ' + (Math.round(r.alpha * 100) / 100).toFixed(2) + '<br />beta: ' + (Math.round(r.beta * 100) / 100).toFixed(2) + '<br/>gamma: ' + (Math.round(r.gamma * 100) / 100).toFixed(2) + '<br />';
            dataContainerMotion.innerHTML = html;
            updateCount();
        });
    }
}

function updateCount() {
  counter++;
    dataContainerCount.innerHTML = counter;
}