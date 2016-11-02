
/** SQL Query **/
var mysql = require("mysql");

/**
localhost setting:
host: "localhost",
user:"root",
password:"123456",
database:"cs223"
**/

var con = mysql.createConnection({
	host: "108.179.232.69",
	user: "jeiqichu_roy",
	password: "be223db",
	database: "jeiqichu_test"
});

con.connect(function(err){
	if (err){
		console.log("Error connecting to database");
		return;
	}
	console.log("Connection opened!");
})

var data_rows;

con.query('SELECT * FROM med_info LIMIT 20;' ,function(err,rows){
	if(err) throw err;
	data_rows = rows;
	console.log('Data received from Db:\n');
	for (var i = 0; i < rows.length; i++) {
		console.log(rows[i].identifier+"\t\t"+rows[i].brief_title);
	};
});

con.end(function(err){
	if (err){
		console.log("Failed to close connectoin.");
		return;
	}
	console.log("Connection closed.");
});

/** HTML **/
console.log("Running HTML");
var http = require("http");

http.createServer(function (request, response){
	response.writeHead(200,{'Content-Type':'text/html'});
	var htmlStr = "<html><head><title>Nodejs with MySQL Query</title></head><body><h1 align='center'>NLP Lexical Analysis</h1><div>Applying natural language processing (NLP) techniques toward lexical analysis of language complexity</div>";
	htmlStr+= "<table border = '1'>";
	htmlStr+= "<tr><td>#id</td>"+
	"<td>identifier</td>" + 
	"<td>title</td>" + 
	"<td>gender</td>" + 
	"<td>min age</td>" + 
	"<td>max age</td>" + 
	"<td>healthy</td>" + 
	"<td>criteria</td></tr>";
	for (var i = 0; i < data_rows.length; i++) {
		htmlStr+= "<tr><td>"+data_rows[i].id+"</td>"+
		"<td>"+data_rows[i].identifier+"</td>" + 
		"<td>"+data_rows[i].brief_title+"</td>" + 
		"<td>"+data_rows[i].gender+"</td>" + 
		"<td>"+data_rows[i].minimum_age+"</td>" + 
		"<td>"+data_rows[i].maximum_age+"</td>" + 
		"<td>"+data_rows[i].healthy_volunteers+"</td>" + 
		"<td>"+data_rows[i].criteria+"</td></tr>";
	};
	htmlStr += "</table>";
	htmlStr +="<form method='post' target='_blank' onsubmit='try {return window.confirm(&quot;You are submitting information to an external page.\nAre you sure?&quot;);} catch (e) {return false;}'><p> What illness demographic would you like to consider? </p><label> You picked: </label><input type='text' size='5' value=''><input type='submit' value='Proceed to my selection'></form>";	
	htmlStr += "<h3 align='center'>CS223A Fall 16<h3></body></html>"
	response.end(htmlStr);
}).listen(8081);

console.log("Server running at localhost:8081");

