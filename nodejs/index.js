var express = require('express');
var app = express();


/** SQL Query **/
var mysql = require("mysql");
var pagination = require('pagination');

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

function google_chart(in_json ,number){
	var count = 0;
	var out_str = '';
	out_str = '{';
	out_str += 		'"cols":[{"id": "", "label": "Name", "pattern":"", "type": "string"},{"id": "", "label": "Percentage/Hits", "pattern":"", "type": "number"}],';
	out_str += 		'"rows":['
	var keys = Object.keys(in_json.terms);
	keys.forEach(function(k){
		if (count < number)
			out_str +=  '{"c":[{"v":"'+k+'",},{"v":'+in_json.terms[k]+'}]},'	
		count++;
	})
	out_str += '			]'
	out_str += '}'
	return out_str;
}

//Google Charts get file path by which_data
function gc_get_file(which_data){
	switch (which_data){
		case "1": 
			return "public/mm_type_precentage.json";
		case "2":
			return "public/mm_type_frequency.json";
		case "3":
			return "public/mm_type_precentageCL_Age_Restriction.json"
		case "4":
			return "public/mm_type_precentageCL_Annotation.json";
		case "5":			
			return "public/mm_type_precentageCL_Current_Restriction.json";
		case "6":
			return "public/mm_type_precentageCL_Health_Condition_Restriction.json";
		case "7":
			return "public/mm_type_precentageCL_MaleFemale_Restriction.json"
		case "8":
			return "public/mm_type_precentageCL_PriorRestriction.json";
		case "9":
			return "public/mm_type_precentageCL_Time_Restriction.json";
	}
	return null;
}

function gc_get_title(which_data){
	switch (which_data){
		case "1":
			return "Semantic Type Precentage";
		case "2":
			return "Term Hits";
		case "3":
			return "Age Restriction";
		case "4":
			return "Annotation";
		case "5":
			return "Current Restriction";
		case "6":
			return "Health Condition Restriction"
		case "7":
			return "MaleFemale Restriction";
		case "8":
			return  "PriorRestriction";
		case "9":
			return  "Time Restriction";
	}
	return null;
}

function gc_res_render(which_data,number,response, page){
	var title = gc_get_title(String(which_data));	
	page = page || "pages/meta_map";
	response.render(page,{
		'which_data':which_data,
	  	'number':number,
	  	'title': title});
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

var rowsPerPage = 20;
var currentPage = 1;
var totalResult = 6653;

function createPaginator(currentPage, totalResult){
	var boostrapPaginator = new pagination.TemplatePaginator({
		prelink:'/trail', current: currentPage, rowsPerPage: rowsPerPage,
		totalResult: totalResult, slashSeparator: true,
		template: function(result) {
			var i, len, prelink;
			var html = '<div><ul class="pagination .pagination-lg">';
			if(result.pageCount < 2) {
				html += '</ul></div>';
				return html;
			}
			prelink = this.preparePreLink(result.prelink);
			if(result.previous) {
				html += '<li><a href="' + prelink + result.previous + '">' + this.options.translator('PREVIOUS') + '</a></li>';
			}
			if(result.range.length) {
				for( i = 0, len = result.range.length; i < len; i++) {
					if(result.range[i] === result.current) {
						html += '<li class="active"><a href="' + prelink + result.range[i] + '">' + result.range[i] + '</a></li>';
					} else {
						html += '<li><a href="' + prelink + result.range[i] + '">' + result.range[i] + '</a></li>';
					}
				}
			}
			if(result.next) {
				html += '<li><a href="' + prelink + result.next + '" class="paginator-next">' + this.options.translator('NEXT') + '</a></li>';
			}
			html += '</ul></div>';
			return html;
		}
	});
	return boostrapPaginator;
}

app.get("/get_json/w/:which_data/n/:number", function(request,response){
	var jsonfile = require('jsonfile')
	var which_data = request.params.which_data;
	var file = gc_get_file(which_data);
	var number = request.params.number;
	jsonfile.readFile(file, function(err, obj) {
	  response.send(google_chart(obj,number));
	})
})

app.get("/msa_charts/", function(request, response){
	var which_data = 3;
	var number = 20;
	var title = gc_get_title(which_data);
	gc_res_render(which_data,number,response,"pages/msa_charts");	
})


app.get("/msa_charts/w/:which_data/n/:number", function(request, response){
	var which_data = request.params.which_data;
	var number = request.params.number;
	var title = gc_get_title(which_data);
	gc_res_render(which_data,number,response, "pages/msa_charts");	
})

// Metamap Visuals
app.get("/meta_map/",function(request,response){
	var which_data = 1;
	var number = 20;
	var title = gc_get_title(which_data);
	gc_res_render(which_data,number,response);
})

app.get("/meta_map/w/:which_data/n/:number",function(request,response,next){
	var which_data = request.params.which_data;
	var number = request.params.number;
	var title = gc_get_title(which_data);
	gc_res_render(which_data,number,response);
})

app.get("/trail",function(request, response){
	boostrapPaginator = createPaginator(1,totalResult)
	con.query('SELECT * FROM med_info LIMIT 0,'+rowsPerPage+";" ,function(err,rows){
		response.render('pages/trail',{
				'results':rows,
				'nav_render':boostrapPaginator.render()			
		});
	});
})

app.get("/trail/page/:pageNum", function(request, response, next){
	pageNum = request.params.pageNum;
	boostrapPaginator = createPaginator(pageNum,totalResult)
	con.query('SELECT * FROM med_info LIMIT '+(rowsPerPage*(pageNum-1))+','+rowsPerPage+";" ,function(err,rows){
		response.render('pages/trail',{
				'results':rows,
				'nav_render':boostrapPaginator.render()			
		});
	});
})

app.get("/trail/id/:id",function(request, response, next){
	id = request.params.id;
	con.query('SELECT * FROM med_info where id = '+id+";" ,function(err,rows){
		response.render('pages/trail_single',{
				'r':rows[0]		
		});
	});	
})

app.listen(app.get('port'), function() {
  console.log('Node app is running on port', app.get('port'));
});


