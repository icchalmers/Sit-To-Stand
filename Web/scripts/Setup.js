var width = screen.availHeight;

var height = screen.availWidth;

var screenRatio;

var realWidth;

var realHeight;



function setValues() {
        if (window.innerHeight > window.innerWidth) { realWidth = window.innerHeight; realHeight = window.innerWidth; screenRatio = (window.innerWidth / window.innerHeight); }

        else { realWidth = window.innerWidth; realHeight = window.innerHeight; screenRatio = (window.innerHeight / window.innerWidth); }
}

function getDimensions() {
    setValues();
    var myElement = document.getElementById("gobutton");
    myElement.style.height = 0.96* realHeight + "px";
    myElement.style.width = 0.97* realWidth + "px";
}
