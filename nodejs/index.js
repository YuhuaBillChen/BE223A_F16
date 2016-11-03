var express = require('express');
var app = express();


/** SQL Query **/
var mysql = require("mysql");

/**
localhost setting:
host: "localhost",
user:"root",
password:"123456",
database:"cs223"
**/

var db_config = {
	host: "108.179.232.69",
	user: "jeiqichu_roy",
	password: "be223db",
	database: "jeiqichu_test"	
}
var con;

function db_connection(){
	con = mysql.createConnection(db_config);

	con.connect(function(err){
		if (err){
			console.log("Error connecting to database");
			setTimeout(db_connection, 2000);
		}
		console.log("Connection opened!");
	})

  	con.on('error', function(err) {
	    console.log('db error', err);
	    if(err.code === 'PROTOCOL_CONNECTION_LOST') { 
	      db_connection();                         
	    } else {                                      
	      throw err;                                  
    	}
  	});	
} 

db_connection();

app.set('port', (process.env.PORT || 5000));

app.use(express.static(__dirname + '/public'));

// views is directory for all template files
app.set('views', __dirname + '/views');
app.set('view engine', 'ejs');

app.get('/', function(request, response) {
  response.render('pages/index');
});

app.get("/demo", function(request,response){
	// Load data only once
	con.query('SELECT * FROM med_info LIMIT 20;' ,function(err,rows){
		response.render('pages/demo',{
			'results':rows
		});
		data_loaded = true;
	});		
})

app.listen(app.get('port'), function() {
  console.log('Node app is running on port', app.get('port'));
});


