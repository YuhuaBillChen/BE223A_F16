
/** SQL Query **/
var mysql = require("mysql");

var con = mysql.createConnection({
	host: "localhost",
	user: "root",
	password: "123456",
	database: "cs223"
});

con.connect(function(err){
	if (err){
		console.log("Error connecting to database");
		return;
	}
	console.log("Connection opened!");
})

var data_rows;

con.query('SELECT * FROM 223aaa LIMIT 5;' ,function(err,rows){
	if(err) throw err;
	data_rows = rows;
	console.log('Data received from Db:\n');
	for (var i = 0; i < rows.length; i++) {
		console.log(rows[i].id+"\t\t"+rows[i].study_name);
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
	htmlStr+= "<tr><td>Study Id</td><td>Study name</td><td>Criteria</td></tr>";
	for (var i = 0; i < data_rows.length; i++) {
		htmlStr+= "<tr><td>"+data_rows[i].id+"</td>"+
		"<td>"+data_rows[i].study_name+"</td>" + 
		"<td>"+data_rows[i].criteria+"</td></tr>";
	};
	htmlStr += "</table>";
	htmlStr +="<form method='post' target='_blank' onsubmit='try {return window.confirm(&quot;You are submitting information to an external page.\nAre you sure?&quot;);} catch (e) {return false;}'><p> What illness demographic would you like to consider? </p><label> You picked: </label><input type='text' size='5' value=''><input type='submit' value='Proceed to my selection'></form>";	
	htmlStr += "<h3 align='center'>CS223A Fall 16<h3></body></html>"
	response.end(htmlStr);
}).listen(8081);

console.log("Server running at localhost:8081");

