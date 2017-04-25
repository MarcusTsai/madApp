var express = require("express");
var app     = express();

app.use(express.static(__dirname + '/public'));
//Store all HTML files in view folder.
app.use(express.static(__dirname + '/public/static'));
//Store all JS and CSS in Scripts folder.

app.get('/',function(req,res){
  res.sendFile('index.html');
  //It will find and locate index.html from View or Scripts
});

app.listen(8080);

console.log("Running at Port 8080");
